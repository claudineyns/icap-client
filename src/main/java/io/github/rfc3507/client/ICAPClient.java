package io.github.rfc3507.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.rfc3507.utilities.LogService;

public class ICAPClient {

	private static final String JAVA_VERSION = System.getProperty("java.version");
	private static final String JAVA_VENDOR = System.getProperty("java.vendor");

	private static final String VERSION = "1.0";
	private static final String USER_AGENT = "ICAP-Client/" + VERSION + " (Java " + JAVA_VERSION + "; " + JAVA_VENDOR + ")";
	private static final String END_LINE_DELIMITER = "\r\n";

	private static Pattern LINE_STATUS_PATTERN = Pattern.compile("(ICAP)\\/(1.0)\\s(\\d{3})\\s(.*)");

	private static final int MAX_PACKET_SIZE = 65536;

	private static final Charset ASCII = StandardCharsets.US_ASCII;

	private final String host;
	private final int port;

	private final LogService logger = LogService.getInstance("ICAP Client");

	private ICAPClient(final String host, int port) {
		this.host = host;
		this.port = port;
	}

	private boolean debugOnOff = false;

	public static ICAPClient instance(final String host, final int port) {
		return new ICAPClient(host, port);
	}

	public ICAPClient debug(final boolean onOff) {
		this.debugOnOff = onOff;
		return this;
	}

	public String getIcapHost() {
		return host;
	}

	public int getIcapPort() {
		return port;
	}

	public static String getIcapVersion() {
		return VERSION;
	}

	public ICAPResponse options(final String icapService) throws ICAPException {
		try {
			return sendOptions(icapService);
		} catch (IOException e) {
			throw new ICAPException(e);
		}
	}

	private int connect_timeout = 5000;

	public int getConnectTimeout() {
		return connect_timeout;
	}

	public ICAPClient setConnectTimeout(int connect_timeout) {
		this.connect_timeout = connect_timeout;
		return this;
	}

	private int read_timeout = 15000;

	public int getReadTimeout() {
		return read_timeout;
	}

	public ICAPClient setReadTimeout(int read_timeout) {
		this.read_timeout = read_timeout;
		return this;
	}

	public ICAPResponse execute(ICAPRequest request) throws ICAPException {
		try {
			return performAdaptation(request);
		} catch (IOException e) {
			throw new ICAPException(e);
		}
	}

	private void info(final String message, Object... args) {
		if (this.debugOnOff) {
			logger.info(message, args);
		}
	}

	private Socket connect() throws IOException {
		final InetAddress inetAddress = InetAddress.getByName(this.host);
		final SocketAddress socketAddress = new InetSocketAddress(inetAddress, this.port);

		info("Connecting...");

		final Socket socket = new Socket();
		socket.setSoTimeout(this.read_timeout);
		socket.connect(socketAddress, this.connect_timeout);

		info("Connected");

		return socket;
	}

	private static final int ICAP_STATUS_CONTINUE = 100;
	private static final int ICAP_STATUS_NO_CONTENT = 204;
	private static final int ICAP_STATUS_REQUEST_FAILURE_FAMILY = 400;

	private ICAPResponse sendOptions(final String icapService) throws IOException {
		try (final Socket socket = connect()) {

			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();

			final String requestHeader = ""
					+ "OPTIONS icap://" + host + "/" + icapService + " ICAP/" + VERSION + END_LINE_DELIMITER
					+ "Host: " + host + END_LINE_DELIMITER
					+ "User-Agent: " + USER_AGENT + END_LINE_DELIMITER
					+ "Encapsulated: null-body=0" + END_LINE_DELIMITER
					+ END_LINE_DELIMITER;

			info("\n{}", requestHeader);
			os.write(requestHeader.getBytes(ASCII));
			os.flush();

			final ICAPResponse options = new ICAPResponse();
			parseResponse(options, is);

			is.close();
			os.close();

			return options;
		}

	}

	private ICAPResponse performAdaptation(ICAPRequest request) throws IOException {
		final byte[] defaultContent = new byte[] {};

		final byte[] httpRequestHeader = Optional.ofNullable(request.getHttpRequestHeader()).orElse(defaultContent);
		final byte[] httpRequestBody = Optional.ofNullable(request.getHttpRequestBody()).orElse(defaultContent);
		final byte[] httpResponseHeader = Optional.ofNullable(request.getHttpResponseHeader()).orElse(defaultContent);
		final byte[] httpResponseBody = Optional.ofNullable(request.getHttpResponseBody()).orElse(defaultContent);

		byte[] content = httpRequestBody;
		if (content.length == 0) {
			content = httpResponseBody;
		}

		try (final Socket socket = connect()) {
			final InputStream is = socket.getInputStream();
			final OutputStream os = socket.getOutputStream();

			int preview = request.getPreview();
			if (preview >= 0 && content.length < request.getPreview()) {
				preview = content.length;
			}

			final StringBuilder encapsulated = new StringBuilder();
			final int[] encapsulatedOffset = new int[]{0};

			mountRequestHeader("req-hdr", encapsulated, encapsulatedOffset, httpRequestHeader);
			mountRequestHeader("req-body", encapsulated, encapsulatedOffset, httpRequestBody);
			mountRequestHeader("res-hdr", encapsulated, encapsulatedOffset, httpResponseHeader);
			mountRequestHeader("res-body", encapsulated, encapsulatedOffset, httpResponseBody);

			if (httpRequestBody.length == 0 && httpResponseBody.length == 0) {
				mountRequestHeader("null-body", encapsulated, encapsulatedOffset, new byte[]{});
			}

			final String icapRequestHeader = ""
					+ request.getMode().name() + " icap://" + host + "/" + request.getService() + " ICAP/" + VERSION + END_LINE_DELIMITER
					+ "Host: " + host + END_LINE_DELIMITER
					+ "User-Agent: " + USER_AGENT + END_LINE_DELIMITER
					+ "Allow: 204" + END_LINE_DELIMITER
					+ (preview >= 0 ? ("Preview: " + preview + END_LINE_DELIMITER) : "")
					+ "Encapsulated: " + encapsulated.toString() + END_LINE_DELIMITER
					+ END_LINE_DELIMITER;

			info("\n{}", icapRequestHeader);
			os.write(icapRequestHeader.getBytes(ASCII));

			os.write(httpRequestHeader);
			os.write(httpResponseHeader);
			os.write(mountPreviewOrFullContent(preview, content));

			os.flush();

			final ICAPResponse response = fetchResponseWithPreviewData(preview, content, is, os);

			is.close();
			os.close();
			return response;

		}

	}

	private void mountRequestHeader(
		final String header,
		final StringBuilder encapsulated,
		final int[] encapsulatedOffset,
		final byte[] data
	) {
		if (data.length > 0) {
			encapsulated.append(encapsulated.length() > 0 ? ", " : "");
			encapsulated.append(header).append("=").append(encapsulatedOffset[0]);
			encapsulatedOffset[0] += data.length;
		}
	}

	private byte[] mountPreviewOrFullContent(final int preview, final byte[] content) throws IOException {
		try (final ByteArrayOutputStream os = new ByteArrayOutputStream()) {

			if (preview > 0) {
				// Send partial (preview) data

				os.write(Integer.toHexString(preview).getBytes(ASCII));
				os.write(END_LINE_DELIMITER.getBytes(ASCII));
				os.write(content, 0, preview);
				os.write(END_LINE_DELIMITER.getBytes(ASCII));

				os.write(("0" + (content.length == preview ? "; ieof" : "") + END_LINE_DELIMITER + END_LINE_DELIMITER).getBytes(ASCII));
			} else if (preview == -1) {
				// Send whole data in a single package

				os.write(Integer.toHexString(content.length).getBytes(ASCII));
				os.write(END_LINE_DELIMITER.getBytes(ASCII));
				os.write(content);
				os.write(END_LINE_DELIMITER.getBytes(ASCII));

				os.write(("0; ieof" + END_LINE_DELIMITER + END_LINE_DELIMITER).getBytes(ASCII));
			} else {
				// No body data; only headers

				os.write(("0" + END_LINE_DELIMITER + END_LINE_DELIMITER).getBytes(ASCII));
			}

			return os.toByteArray();
		}
	}

	private ICAPResponse fetchResponseWithPreviewData(
		final int preview,
		final byte[] content,
		final InputStream is,
		final OutputStream os
	) throws IOException {

		ICAPResponse response = new ICAPResponse();
		parseResponse(response, is);

		if (response.getStatus() == ICAP_STATUS_CONTINUE ) {
			int remaining = (content.length - preview);

			while (remaining > 0) {

				int amount = remaining;
				if (amount > MAX_PACKET_SIZE) {
					amount = MAX_PACKET_SIZE;
				}

				os.write(Integer.toHexString(amount).getBytes(ASCII));
				os.write(END_LINE_DELIMITER.getBytes(ASCII));

				os.write(content, preview, amount);
				os.write(END_LINE_DELIMITER.getBytes(ASCII));

				remaining -= amount;
			}

			os.write(("0" + END_LINE_DELIMITER + END_LINE_DELIMITER).getBytes(ASCII));
			os.flush();

			response = new ICAPResponse();
			parseResponse(response, is);
		}

		return response;
	}

	private void parseResponse(
			final ICAPResponse response,
			final InputStream is
	) throws IOException {

		ByteArrayOutputStream cache = new ByteArrayOutputStream();
		readHeaders(is, cache);

		final String icapResponseHeaders = new String(cache.toByteArray(), "UTF-8");
		extractHeaders(response, icapResponseHeaders);

		if (response.getStatus() == ICAP_STATUS_CONTINUE
				|| response.getStatus() == ICAP_STATUS_NO_CONTENT
				|| response.getStatus() > ICAP_STATUS_REQUEST_FAILURE_FAMILY) {
			return;
		}

		int httpRequestHeaderSize = 0;
		int httpResponseHeaderSize = 0;

		String lastOffsetLabel = "";

		int lastOffsetValue = 0;

		List<String> encapsulatedValues = response.getHeaderValues("Encapsulated");
		if (encapsulatedValues != null)
			for (String offset : encapsulatedValues) {

				String offsetParser[] = offset.split("=");

				String offsetLabel = offsetParser[0];

				int offsetValue = Integer.parseInt(offsetParser[1]);

				switch (lastOffsetLabel) {

					case "req-hdr":
						httpRequestHeaderSize = (offsetValue - lastOffsetValue);
						break;

					case "res-hdr":
						httpResponseHeaderSize = (offsetValue - lastOffsetValue);
						break;

				}

				lastOffsetLabel = offsetLabel;
				lastOffsetValue = offsetValue;

			}

		byte[] parseContent = null;

		if (httpRequestHeaderSize > 0) {
			parseContent = new byte[httpRequestHeaderSize];
			is.read(parseContent);
			response.setHttpRequestHeader(parseContent);
		}

		if ("req-body".equals(lastOffsetLabel)) {
			cache = new ByteArrayOutputStream();
			readBody(is, cache);
			response.setHttpRequestBody(cache.toByteArray());
		}

		if (httpResponseHeaderSize > 0) {
			parseContent = new byte[httpResponseHeaderSize];
			is.read(parseContent);
			response.setHttpResponseHeader(parseContent);
		}

		if ("res-body".equals(lastOffsetLabel)) {
			cache = new ByteArrayOutputStream();
			readBody(is, cache);
			response.setHttpResponseBody(cache.toByteArray());
		}

	}

	private void readHeaders(InputStream is, OutputStream out) throws IOException {
		int octet = -1;

		int octet0 = -1;
		int octet1 = -1;
		int octet2 = -1;
		int octet3 = -1;

		while ((octet = is.read()) != -1) {
			octet0 = octet1;
			octet1 = octet2;
			octet2 = octet3;
			octet3 = octet;

			out.write(octet);

			if (octet0 == '\r'
					&& octet1 == '\n'
					&& octet2 == '\r'
					&& octet3 == '\n') {
				break;
			}

		}

	}

	private void readBody(InputStream is, OutputStream out) throws IOException {
		int octet = -1;

		int octet0 = -1;
		int octet1 = -1;
		int octet2 = -1;
		int octet3 = -1;
		int octet4 = -1;

		while ((octet = is.read()) != -1) {

			octet0 = octet1;
			octet1 = octet2;
			octet2 = octet3;
			octet3 = octet4;
			octet4 = octet;

			out.write(octet);

			if (octet0 == '0'
					&& octet1 == '\r'
					&& octet2 == '\n'
					&& octet3 == '\r'
					&& octet4 == '\n') {
				break;
			}

		}

	}

	private void extractHeaders(ICAPResponse response, String content) {
		final String statusLine = content.substring(0, content.indexOf('\r'));

		Matcher matcher = LINE_STATUS_PATTERN.matcher(statusLine);
		if (matcher.matches()) {
			response.setProtocol(matcher.group(1));
			response.setVersion(matcher.group(2));
			response.setStatus(Integer.parseInt(matcher.group(3)));
			response.setMessage(matcher.group(4));
		}

		content = content.substring(content.indexOf('\r') + 2);

		Map<StringBuilder, StringBuilder> extraction = new LinkedHashMap<>();

		final byte SIDE_HEADER = 1;
		final byte SIDE_VALUE = 2;

		StringBuilder t_header = new StringBuilder("");
		StringBuilder t_value = null;
		char[] raw = content.toCharArray();
		byte side = SIDE_HEADER;
		boolean breakLine = true;
		boolean incomplete = false;

		for (int offset = 0; offset < raw.length; ++offset) {
			final char c = raw[offset];

			switch (c) {
				case ':':
					breakLine = false;
					side = SIDE_VALUE;
					continue;

				case '\r':
					continue;

				case '\n':
					if (breakLine) {
						continue;
					}
					if (incomplete) {
						breakLine = true;
						continue;
					}
					breakLine = true;
					side = SIDE_HEADER;
					extraction.put(t_header, t_value);
					t_header = new StringBuilder("");
					continue;

				case '\t':
					breakLine = false;
					side = SIDE_VALUE;
					t_value.append('\n');
					incomplete = true;
					break;

				default:
					if (breakLine) {
						side = SIDE_HEADER;
						incomplete = false;
						breakLine = false;
						t_value = new StringBuilder("");
					}
			}

			if (side == SIDE_HEADER) {
				t_header.append(c);
			} else {
				if (c == ' ' && t_value.length() == 0) {
					continue;
				}
				t_value.append(c);
			}

		}

		extraction.entrySet().forEach(entry -> 
		{
			final String header = entry.getKey().toString();
			final String value = entry.getValue().toString();

			final String[] headerValues = header.equalsIgnoreCase("Date") ? new String[] { value } : value.split("\\,");

			Arrays.asList(headerValues).forEach(v -> 
			{
				response.addHeader(header, v.trim());
			});

		});

	}

}
