package br.eti.claudiney.icap.client;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.eti.claudiney.icap.client.ICAPRequest.Mode;

public class ICAPClient {

	private String host;
	private int port = 0;
	
	private static final String VERSION = "1.0";
	private static final String USER_AGENT = "Java-ICAP-Client/1.1";
	private static final String END_LINE_DELIMITER = "\r\n";
	private static final String END_MESSAGE_DELIMITER = "\r\n\r\n";
	
	private static Pattern LINE_STATUS_PATTERN = Pattern.compile("(ICAP\\/1.0)\\s(\\d{3})\\s(.*)");
	
	public ICAPClient(String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	public String getICAPHost() {
		return host;
	}
	
	public int getICAPPort() {
		return port;
	}
	
	public static String getICAPVersion() {
		return VERSION;
	}
	
	public ICAPResponse info() throws ICAPException {
		
		String service = "info";
		
		ICAPResponse option = options(service);
		
		ICAPRequest request = new ICAPRequest(service, Mode.REQMOD);
		
		request.setPreview(option.getIntegerHeader("Preview"));
		
		String httpHost = request.getHttpHost();
		Integer httpPort = request.getHttpPort();
		
		if(httpHost == null) {
			httpHost = getICAPHost();
		}
		
		if(httpPort == null) {
			httpPort = Integer.valueOf(getICAPPort());
		}
		
        String requestHeader 
        		= "GET / HTTP/1.1"+END_LINE_DELIMITER
        		+ "Host: "+httpHost+":"+httpPort+END_LINE_DELIMITER
        		+ "User-Agent: "+USER_AGENT+END_LINE_DELIMITER
        		+ END_LINE_DELIMITER;
        
        request.setHttpRequestHeader(requestHeader.getBytes());
        
		return sendRequest(request);

	}
	
	public ICAPResponse echo(File file) throws ICAPException {
		String service = "echo";
		ICAPRequest request = new ICAPRequest(service, Mode.RESPMOD);
		request.setResourceName(file.getName());
		request.setHttpResponseBody(file);
		return echo(request);
	}
	
	public ICAPResponse echo(URL url) throws ICAPException {
		String service = "echo";
		ICAPRequest request = new ICAPRequest(service, Mode.RESPMOD);
		request.setResourceName(url.getFile());
		request.setHttpResponseBody(url);
		return echo(request);
	}
	
	private ICAPResponse echo(ICAPRequest request) throws ICAPException {
		
		ICAPResponse option = options(request.getService());
		
		request.setPreview(option.getIntegerHeader("Preview"));

		String resourceName=request.getResourceName();
		if( resourceName != null ) {
			while(resourceName.startsWith("/")) {
				resourceName = resourceName.substring(1);
			}
		}
		
		String httpHost = request.getHttpHost();
		Integer httpPort = request.getHttpPort();
		
		if(httpHost == null) {
			httpHost = getICAPHost();
		}
		
		if(httpPort == null) {
			httpPort = Integer.valueOf(getICAPPort());
		}
		
        String requestHeader 
        		= "GET /"+resourceName+" HTTP/1.1"+END_LINE_DELIMITER
        		+ "Host: "+httpHost+":"+httpPort+END_LINE_DELIMITER
        		+ "User-Agent: "+USER_AGENT+END_LINE_DELIMITER
        		+ END_LINE_DELIMITER;
        
        String responseHeader
        		= "HTTP/1.1 200 OK" + END_LINE_DELIMITER
        		+ "Content-Type: application/octet-stream"+END_LINE_DELIMITER
        		+ "Content-Length: "+request.getHttpResponseBody().length+END_LINE_DELIMITER
        		+ "Transfer-Encoding: chunked" + END_LINE_DELIMITER
        		+ END_LINE_DELIMITER;
		
        request.setHttpRequestHeader(requestHeader.getBytes());
        
        request.setHttpResponseHeader(responseHeader.getBytes());
        
		return sendRequest(request);

	}
	
	public ICAPResponse virus_scan(File file) throws ICAPException {
		String service = "virus_scan";
		ICAPRequest request = new ICAPRequest(service, Mode.RESPMOD);
		request.setHttpResponseBody(file);
		return virus_scan(request);
	}
	
	public ICAPResponse virus_scan(URL url) throws ICAPException {
		String service = "virus_scan";
		ICAPRequest request = new ICAPRequest(service, Mode.RESPMOD);
		request.setHttpHost(url.getHost());
		request.setHttpPort(url.getPort());
		request.setResourceName(url.getFile());
		request.setHttpResponseBody(url);
		return virus_scan(request);
	}
	
	private ICAPResponse virus_scan(ICAPRequest request) throws ICAPException {
		
		ICAPResponse option = options(request.getService());
		
		request.setPreview(option.getIntegerHeader("Preview"));

		String resourceName=request.getResourceName();
		if( resourceName != null ) {
			while(resourceName.startsWith("/")) {
				resourceName = resourceName.substring(1);
			}
		}
		
		String httpHost = request.getHttpHost();
		Integer httpPort = request.getHttpPort();
		
		if(httpHost == null) {
			httpHost = getICAPHost();
		}
		
		if(httpPort == null) {
			httpPort = Integer.valueOf(getICAPPort());
		}
		
        // First part of header
        String requestHeader 
        		= "GET /" + resourceName + " HTTP/1.1"+END_LINE_DELIMITER
        		+ "Host: "+httpHost+":"+httpPort+END_LINE_DELIMITER
        		+ "User-Agent: "+USER_AGENT+END_LINE_DELIMITER
        		+ END_LINE_DELIMITER;
        
        String responseHeader
        		= "HTTP/1.1 200 OK" + END_LINE_DELIMITER
        		+ "Content-Type: application/octet-stream"+END_LINE_DELIMITER
        		+ "Content-Length: "+request.getHttpResponseBody().length + END_LINE_DELIMITER
        		+ "Transfer-Encoding: chunked" + END_LINE_DELIMITER
        		+ END_LINE_DELIMITER;
		
        request.setHttpRequestHeader(requestHeader.getBytes());
        
        request.setHttpResponseHeader(responseHeader.getBytes());
        
		return sendRequest(request);

	}
	
	public ICAPResponse options(String icapService) throws ICAPException {
		
		try {
			return sendOptions(icapService);
		} catch(IOException e){
			throw new ICAPException(e);
		}
		
	}
	
	private ICAPResponse sendOptions(String icapService) throws IOException {
		
		Socket socket = new Socket(host, port);
		
		InputStream is = socket.getInputStream();
		OutputStream os = socket.getOutputStream();
		
        String requestHeader = 
                "OPTIONS icap://"+host+"/"+icapService+" ICAP/"+VERSION+END_LINE_DELIMITER
              + "Host: "+host+END_LINE_DELIMITER
              + "User-Agent: "+USER_AGENT+END_LINE_DELIMITER
              + "Encapsulated: null-body=0"+END_LINE_DELIMITER
              + END_LINE_DELIMITER;
        
        os.write(requestHeader.getBytes());
        os.flush();
        
        ICAPResponse options = new ICAPResponse();
        
        extractResponse(options, is);
        
        is.close();
        os.close();
        socket.close();
        
        return options;
        
	}
	
	public ICAPResponse sendRequest(ICAPRequest request) throws ICAPException {
		
		try {
			return makeRequest(request);
		}  catch(IOException e) {
			throw new ICAPException(e);
		}
		
	}
	
	private static byte[] getContentOrDefault(byte[] content) {
		if(content == null) return new byte[]{};
		return content;
	}
	
	private ICAPResponse makeRequest(ICAPRequest request) throws IOException {
        
        byte[] httpRequestHeader = getContentOrDefault(request.getHttpRequestHeader());
        byte[] httpRequestBody = getContentOrDefault(request.getHttpRequestBody());
        byte[] httpResponseHeader = getContentOrDefault(request.getHttpResponseHeader());
        byte[] httpResponseBody = getContentOrDefault(request.getHttpResponseBody());
        
        byte[] content = httpRequestBody;
        if(content.length == 0) {
        	content = httpResponseBody;
        }
        
		Socket socket = new Socket(host, port);
		
		InputStream is = socket.getInputStream();
		OutputStream os = socket.getOutputStream();
        
        int preview = request.getPreview();
        if (content.length < request.getPreview()){
            preview = content.length;
        }
        
        StringBuilder encapsulated = new StringBuilder();
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
		
        String icapRequestHeader = 
        		request.getMode().name()+" icap://"+host+"/"+request.getService()+" ICAP/"+VERSION+END_LINE_DELIMITER
              + "Host: "+host+END_LINE_DELIMITER
              + "User-Agent: "+USER_AGENT+END_LINE_DELIMITER
              + "Allow: 204"+END_LINE_DELIMITER
              + "Preview: "+preview+END_LINE_DELIMITER
              + "Encapsulated: "+encapsulated.toString()+END_LINE_DELIMITER
              + END_LINE_DELIMITER;
        
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
        	
        	if( content.length <= preview ){
        		// Fim da transmissão
        		os.write( ("0; ieof"+END_MESSAGE_DELIMITER).getBytes() );
        	} else if (preview != 0){
        		// Ainda tem mais para transmitir
        		os.write( ("0"+END_MESSAGE_DELIMITER).getBytes() );
        	}
        	
        }
        
        os.flush();
        
        ICAPResponse response = new ICAPResponse();
        extractResponse(response, is);
        
        if( response.getStatus() == 100 /*continue*/ ) {
        	
        	int remaining = (content.length - preview);
            os.write(Integer.toHexString(remaining).getBytes());
            os.write(END_LINE_DELIMITER.getBytes());
            
            os.write(content, preview, remaining);
            os.write(END_LINE_DELIMITER.getBytes());
            
        	os.write( ("0"+END_MESSAGE_DELIMITER).getBytes() );
        	
        	os.flush();
        	
        	response = new ICAPResponse();
            extractResponse(response, is);
        	
        }
        
        is.close();
        os.close();
        socket.close();
        
        return response;
        
	}
	
	private void extractResponse(
			ICAPResponse response, 
			InputStream is) throws IOException {
        
		ByteArrayOutputStream cache = new ByteArrayOutputStream();
		readHeaders(is, cache);
        
        String icapResponseHeaders = 
        		new String(cache.toByteArray(), "UTF-8");
        
        extractHeaders(response, icapResponseHeaders);
        
        if( response.getStatus() == 100 
        		|| response.getStatus() == 204 
        		|| response.getStatus() > 400 ) {
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
        
        int reader = -1;
        
        int mark1 = -1, mark2 = -1, mark3 = -1, mark4 = -1;
        
        while((reader = is.read()) != -1) {
        	
        	mark1 = mark2;
        	mark2 = mark3;
        	mark3 = mark4;
        	mark4 = reader;
        	
        	out.write(reader);
        	
        	if(        mark1 == '\r' 
        			&& mark2 == '\n' 
        			&& mark3 == '\r' 
        			&& mark4 == '\n' ) {
        		break;
        	}
        	
        }
		
	}
	
	private void readBody(InputStream is, OutputStream out) throws IOException {
        
        int reader = -1;
        
        int mark1 = -1, mark2 = -1, mark3 = -1, mark4 = -1, mark5 = -1;
        
        while((reader = is.read()) != -1) {
        	
        	mark1 = mark2;
        	mark2 = mark3;
        	mark3 = mark4;
        	mark4 = mark5;
        	mark5 = reader;
        	
        	out.write(reader);
        	
        	if(        mark1 == '0'
        			&& mark2 == '\r' 
        			&& mark3 == '\n' 
        			&& mark4 == '\r' 
        			&& mark5 == '\n' ) {
        		break;
        	}
        	
        }
		
	}
	
	private void extractHeaders(ICAPResponse reponse, String content) {
		
        String statusLine = content.substring(0, content.indexOf('\r'));
        
        Matcher matcher = LINE_STATUS_PATTERN.matcher(statusLine);
        if(matcher.matches()) {
        	reponse.setProtocol(matcher.group(1));
        	reponse.setStatus(Integer.parseInt(matcher.group(2)));
        	reponse.setMessage(matcher.group(3));
        }
        
        content = content.substring(content.indexOf('\r')+2);
        
		Map<StringBuilder, StringBuilder> extraction = new LinkedHashMap<>();
		
		StringBuilder t_header = new StringBuilder("");
		StringBuilder t_value = null;
		char[] raw = content.toCharArray();
		byte side = 1; //1=header; 2=value
		boolean breakLine = true;
		boolean incomplete = false;
		
		for( int offset = 0; offset < raw.length; ++offset) {
			
			char c = raw[offset];
			
			switch(c) {
			case ':': 
				breakLine = false;
				side=2;
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
				side = 1;
				extraction.put(t_header, t_value);
				t_header = new StringBuilder("");
				continue;
				
			case '\t':
				// força a continuação do valor anterior em andamento
				breakLine = false;
				side = 2;
				t_value.append('\n');
				incomplete = true;
				break;
				
			default:
				if(breakLine) {
					side = 1;
					incomplete = false;
					breakLine = false;
					t_value = new StringBuilder("");
				}
			}
			
			if(side == 1) {
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
        		reponse.addHeader(header, v.trim());
        	}
			
		}
		
	}
	
}
