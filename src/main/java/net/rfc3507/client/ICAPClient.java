package net.rfc3507.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ICAPClient {

	private Logger logger = Logger.getLogger(getClass().getCanonicalName());
	
	private String host;
	private int port = 0;
	
	private static final String VERSION = "1.0";
	private static final String USER_AGENT = "Java-ICAP-Client/1.1";
	private static final String END_LINE_DELIMITER = "\r\n";
	
	private static Pattern LINE_STATUS_PATTERN = Pattern.compile("(ICAP)\\/(1.0)\\s(\\d{3})\\s(.*)");
	
	private static final int MAX_PACKET_SIZE = 65536;
	
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
        
        info("\n### (SEND) ICAP REQUEST ###\n"+requestHeader);
        
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
        if( preview >= 0 && content.length < request.getPreview() ){
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
              + (preview >= 0 ? ("Preview: "+preview+END_LINE_DELIMITER):"")
              + "Encapsulated: "+encapsulated.toString()+END_LINE_DELIMITER
              + END_LINE_DELIMITER;
        
        info("\n### (SEND) ICAP REQUEST ###\n"+icapRequestHeader);
        
        os.write(icapRequestHeader.getBytes());
        
        if( httpRequestHeader.length > 0 ) {
        	info("\n### (SEND) HTTP REQUEST HEADER ###\n"+new String(httpRequestHeader));
        	os.write(httpRequestHeader);
        }
        
        if( httpResponseHeader.length > 0 ) {
        	info("\n### (SEND) HTTP RESPONSE HEADER ###\n"+new String(httpResponseHeader));
        	os.write(httpResponseHeader);
        }
        
        if( preview > 0 ) {
        	
        	info("\n### (SEND) ICAP PREVIEW: ###\n"+preview);
        	
        	
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
        	
        	info("\n### (SEND) REMAINING HTTP BODY PAYLOAD ###");
        	
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
            info("\n### (RECEIVE) ICAP RESPONSE HEADER ###\n"+new String(httpResponseHeader));
            
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
        	info("\n### (RECEIVE) HTTP REQUEST HEADER ###\n"+new String(parseContent));
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
        	info("\n### (RECEIVE) HTTP RESPONSE HEADER ###\n"+new String(parseContent));
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
	
	private void extractHeaders(ICAPResponse response, String content) {
		
        String statusLine = content.substring(0, content.indexOf('\r'));
        
        Matcher matcher = LINE_STATUS_PATTERN.matcher(statusLine);
        if(matcher.matches()) {
        	response.setProtocol(matcher.group(1));
        	response.setVersion(matcher.group(2));
        	response.setStatus(Integer.parseInt(matcher.group(3)));
        	response.setMessage(matcher.group(4));
        	info("\n### (RECEIVE) ICAP RESPONSE STATUS ###\n"+statusLine);
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
        		response.addHeader(header, v.trim());
        	}
			
		}
		
	}
	
	private void info(Object message) {
		logger.info(message.toString());
	}
	
}
