package io.github.rfc3507.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.rfc3507.utilities.LogService;

public class ICAPClient {
	
	private static final String VERSION = "1.0";
	private static final String USER_AGENT = "Java-ICAP-Client/1.1";
	private static final String END_LINE_DELIMITER = "\r\n";
	
	private static Pattern LINE_STATUS_PATTERN = Pattern.compile("(ICAP)\\/(1.0)\\s(\\d{3})\\s(.*)");
	
	private static final int MAX_PACKET_SIZE = 65536;
	
	private String host;
	private int port = 0;
	
	private final LogService logger = LogService.getInstance("ICAP-CLIENT");

	public ICAPClient(String host, int port) {
		this.host = host;
		this.port = port;
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
	
	public ICAPResponse options(String icapService) throws ICAPException {
		try {
			return sendOptions(icapService);
		} catch(IOException e){
			throw new ICAPException(e);
		}
	}

	int connect_timeout = 5000;
	public int getConnectTimeout() {
		return connect_timeout;
	}

	public void setConnectTimeout(int connect_timeout) {
		this.connect_timeout = connect_timeout;
	}

	int read_timeout = 15000;
	public int getReadTimeout() {
		return read_timeout;
	}

	public void setReadTimeout(int read_timeout) {
		this.read_timeout = read_timeout;
	}
	
	private Socket connect() throws IOException {
		final InetAddress inetAddress = InetAddress.getByName(this.host);
		final SocketAddress socketAddress = new InetSocketAddress(inetAddress, this.port);

		logger.info("Connecting...");

		final Socket socket = new Socket(); 
		socket.setSoTimeout(read_timeout);
		socket.connect(socketAddress, connect_timeout);

		logger.info("Connected");
		
		return socket;
	}
	
	private ICAPResponse sendOptions(String icapService) throws IOException {
		final Socket socket = connect();

		InputStream is = socket.getInputStream(); 
		OutputStream os = socket.getOutputStream();
		
        String requestHeader = 
                "OPTIONS icap://"+host+"/"+icapService+" ICAP/"+VERSION+END_LINE_DELIMITER
              + "Host: "+host+END_LINE_DELIMITER
              + "User-Agent: "+USER_AGENT+END_LINE_DELIMITER
              + "Encapsulated: null-body=0"+END_LINE_DELIMITER
              + END_LINE_DELIMITER;

        logger.info("\n{}", requestHeader);
        os.write(requestHeader.getBytes());
        os.flush();

        ICAPResponse options = new ICAPResponse();
        extractResponse(options, is);
        
        is.close();
        os.close();
        socket.close();
        
        return options;
	}
	
	public ICAPResponse execute(ICAPRequest request) throws ICAPException {
		try {
			return performAdaptation(request);
		}  catch(IOException e) {
			throw new ICAPException(e);
		}
	}
	
	private static byte[] getContentOrDefault(byte[] content) {
		if(content == null) return new byte[]{};
		return content;
	}
	
	private ICAPResponse performAdaptation(ICAPRequest request) throws IOException {
        final byte[] httpRequestHeader = getContentOrDefault(request.getHttpRequestHeader());
        final byte[] httpRequestBody = getContentOrDefault(request.getHttpRequestBody());
        final byte[] httpResponseHeader = getContentOrDefault(request.getHttpResponseHeader());
        final byte[] httpResponseBody = getContentOrDefault(request.getHttpResponseBody());
        
        byte[] content = httpRequestBody;
        if(content.length == 0) {
        	content = httpResponseBody;
        }
        
		final Socket socket = connect();
		final InputStream is = socket.getInputStream();
		final OutputStream os = socket.getOutputStream();
        
        int preview = request.getPreview();
        if( preview >= 0 && content.length < request.getPreview() ){
            preview = content.length;
        }
        
        final StringBuilder encapsulated = new StringBuilder();
        int encapsulatedOffset = 0;
        
        if(httpRequestHeader.length > 0) {
        	if(encapsulated.length()>0) encapsulated.append(", ");
        	encapsulated.append("req-hdr="+encapsulatedOffset);
        	encapsulatedOffset += httpRequestHeader.length; 
        }
        
        if(httpRequestBody.length > 0) {
        	if(encapsulated.length()>0) encapsulated.append(", ");
        	encapsulated.append("req-body="+encapsulatedOffset);
        	encapsulatedOffset += httpRequestBody.length; 
        }
        
        if(httpResponseHeader.length > 0) {
        	if(encapsulated.length()>0) encapsulated.append(", ");
        	encapsulated.append("res-hdr="+encapsulatedOffset);
        	encapsulatedOffset += httpResponseHeader.length; 
        }
        
        if(httpResponseBody.length > 0) {
        	if(encapsulated.length()>0) encapsulated.append(", ");
        	encapsulated.append("res-body="+encapsulatedOffset);
        	encapsulatedOffset += httpResponseBody.length; 
        }
        
        if( httpRequestBody.length ==0 && httpResponseBody.length == 0 ) {
        	if(encapsulated.length()>0) encapsulated.append(", ");
        	encapsulated.append("null-body="+encapsulatedOffset);
        }
		
        final String icapRequestHeader = 
        		request.getMode().name()+" icap://"+host+"/"+request.getService()+" ICAP/"+VERSION+END_LINE_DELIMITER
              + "Host: "+host+END_LINE_DELIMITER
              + "User-Agent: "+USER_AGENT+END_LINE_DELIMITER
              + "Allow: 204"+END_LINE_DELIMITER
              + (preview >= 0 ? ("Preview: "+preview+END_LINE_DELIMITER):"")
              + "Encapsulated: "+encapsulated.toString()+END_LINE_DELIMITER
              + END_LINE_DELIMITER;
        
        logger.info("\n{}", icapRequestHeader);

        os.write(icapRequestHeader.getBytes());
        
        if( httpRequestHeader.length > 0 ) {
        	os.write(httpRequestHeader);
        }
        
        if( httpResponseHeader.length > 0 ) {
        	os.write(httpResponseHeader);
        }
        
        if( preview > 0 ) {
        	
	        os.write(Integer.toHexString(preview).getBytes());
	        os.write(END_LINE_DELIMITER.getBytes());
        	os.write(content, 0, preview);
        	os.write(END_LINE_DELIMITER.getBytes());
            
        	if( content.length == preview ){
        		os.write( ("0; ieof"+END_LINE_DELIMITER+END_LINE_DELIMITER).getBytes() );
        	} else {
        		os.write( ("0"+END_LINE_DELIMITER+END_LINE_DELIMITER).getBytes() );
        	}
        	
        } else if( preview == -1 ) {
        	
        	/*
        	 * Envia tudo em um unico lote
        	 */
        	
	        os.write(Integer.toHexString(content.length).getBytes());
	        os.write(END_LINE_DELIMITER.getBytes());
        	os.write(content);
        	os.write(END_LINE_DELIMITER.getBytes());
        	os.write( ("0; ieof"+END_LINE_DELIMITER+END_LINE_DELIMITER).getBytes() );
        	
        } else {
        	
        	// Transmite vazio (ou seja, vai somente header)
    		os.write( ("0"+END_LINE_DELIMITER+END_LINE_DELIMITER).getBytes() );
        	
        }
        
        os.flush();
        
        ICAPResponse response = new ICAPResponse();
        extractResponse(response, is);
        
        if( response.getStatus() == 100 /*continue*/ ) {
        	
        	int remaining = (content.length - preview);
        	
        	while( remaining > 0 ) {
        		
        		int amount = remaining;
        		if(amount > MAX_PACKET_SIZE) {
        			amount = MAX_PACKET_SIZE;
        		}
        		
	            os.write(Integer.toHexString(amount).getBytes());
	            os.write(END_LINE_DELIMITER.getBytes());
	            
	            os.write(content, preview, amount);
	            os.write(END_LINE_DELIMITER.getBytes());
	            
	            remaining -= amount;
	            
        	}
            
        	os.write( ("0"+END_LINE_DELIMITER+END_LINE_DELIMITER).getBytes() );
        	
        	os.flush();
        	
        	response = new ICAPResponse();
            extractResponse(response, is);
            
        }
        
        is.close();
        os.close();
        socket.close();
        
        return response;
	}
	
	static final int ICAP_STATUS_CONTINUE = 100;
	static final int ICAP_STATUS_NO_CONTENT = 204;
	static final int ICAP_STATUS_REQUEST_FAILURE_FAMILY = 400;
	
	private void extractResponse(
			ICAPResponse response, 
			InputStream is) throws IOException {
        
		ByteArrayOutputStream cache = new ByteArrayOutputStream();
		readHeaders(is, cache);
        
        final String icapResponseHeaders = new String(cache.toByteArray(), "UTF-8");
        extractHeaders(response, icapResponseHeaders);
        
        if( response.getStatus() == ICAP_STATUS_CONTINUE 
        		|| response.getStatus() == ICAP_STATUS_NO_CONTENT 
        		|| response.getStatus() > ICAP_STATUS_REQUEST_FAILURE_FAMILY ) {
        	return;
        }

        int httpRequestHeaderSize = 0;
        int httpResponseHeaderSize = 0;
        
        String lastOffsetLabel = "";
        
        int lastOffsetValue = 0;
        
        List<String> encapsulatedValues = response.getHeaderValues("Encapsulated");
        if(encapsulatedValues!=null)
        for(String offset: encapsulatedValues) {
        	
        	String offsetParser[] = offset.split("=");
        	
        	String offsetLabel = offsetParser[0];
        	
        	int offsetValue = Integer.parseInt(offsetParser[1]);
        	
        	switch(lastOffsetLabel) {
        		
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
        
        if( httpRequestHeaderSize > 0 ) {
        	parseContent = new byte[httpRequestHeaderSize];
        	is.read(parseContent);
        	response.setHttpRequestHeader(parseContent);
        }
    	
    	if( "req-body".equals(lastOffsetLabel) ) {
        	cache = new ByteArrayOutputStream();
        	readBody(is, cache);
        	response.setHttpRequestBody(cache.toByteArray()); 
    	}
        
        if( httpResponseHeaderSize > 0 ) {
        	parseContent = new byte[httpResponseHeaderSize];
        	is.read(parseContent);
        	response.setHttpResponseHeader(parseContent);
        }
    	
    	if( "res-body".equals(lastOffsetLabel) ) {
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

        while((octet = is.read()) != -1) {
        	octet0 = octet1;
        	octet1 = octet2;
        	octet2 = octet3;
        	octet3 = octet;

        	out.write(octet);
        	
        	if(		octet0 == '\r' 
        		&&	octet1 == '\n' 
        		&&	octet2 == '\r' 
        		&&	octet3 == '\n' ) {
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

        while((octet = is.read()) != -1) {
        	
        	octet0 = octet1;
        	octet1 = octet2;
        	octet2 = octet3;
        	octet3 = octet4;
        	octet4 = octet;
        	
        	out.write(octet);
        	
        	if(        octet0 == '0'
        			&& octet1 == '\r' 
        			&& octet2 == '\n' 
        			&& octet3 == '\r' 
        			&& octet4 == '\n' ) {
        		break;
        	}
        	
        }
		
	}

	private void extractHeaders(ICAPResponse response, String content) {
        final String statusLine = content.substring(0, content.indexOf('\r'));
        
        Matcher matcher = LINE_STATUS_PATTERN.matcher(statusLine);
        if(matcher.matches()) {
        	response.setProtocol(matcher.group(1));
        	response.setVersion(matcher.group(2));
        	response.setStatus(Integer.parseInt(matcher.group(3)));
        	response.setMessage(matcher.group(4));
        }
        
        content = content.substring(content.indexOf('\r')+2);
        
		Map<StringBuilder, StringBuilder> extraction = new LinkedHashMap<>();
		
		final byte SIDE_HEADER = 1;
		final byte SIDE_VALUE = 2;

		StringBuilder t_header = new StringBuilder("");
		StringBuilder t_value = null;
		char[] raw = content.toCharArray();
		byte side = SIDE_HEADER;
		boolean breakLine = true;
		boolean incomplete = false;
		
		for( int offset = 0; offset < raw.length; ++offset) {
			
			char c = raw[offset];
			
			switch(c) {
			case ':': 
				breakLine = false;
				side = SIDE_VALUE;
				continue;
				
			case '\r':
				continue;
				
			case '\n':
				if(breakLine) {
					continue;
				}
				if(incomplete) {
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
				if(breakLine) {
					side = SIDE_HEADER;
					incomplete = false;
					breakLine = false;
					t_value = new StringBuilder("");
				}
			}
			
			if(side == SIDE_HEADER) {
				t_header.append(c);
			} else {
				if(c == ' ' && t_value.length() == 0) {
					continue;
				}
				t_value.append(c);
			}
			
		}
		
		Set<Map.Entry<StringBuilder, StringBuilder>> entries = extraction.entrySet();
		for(Map.Entry<StringBuilder, StringBuilder> entry: entries) {
			
			String header = entry.getKey().toString();
			String value = entry.getValue().toString();
			
        	String[] headerValues = null;
        	if(header.equalsIgnoreCase("Date")) {
        		headerValues = new String[] {value};
        	} else {
        		headerValues = value.split("\\,");
        	}
        	
        	for(String v: headerValues) {
        		response.addHeader(header, v.trim());
        	}
			
		}
		
	}
	
}
