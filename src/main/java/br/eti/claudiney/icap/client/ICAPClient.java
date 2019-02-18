package br.eti.claudiney.icap.client;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
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
	
	public String getHost() {
		return host;
	}
	
	public int getPort() {
		return port;
	}
	
	public static void main(String[] args) throws Exception {
		
		ICAPClient client = new ICAPClient("lab.ptr.net.br", 1344);
		
		File file = new File("c:\\temp\\eicar.com.txt");
//		File file = new File("c:\\temp\\DAS-PGMEI-32081807000192.pdf");
		ICAPResponse response = client.virus_scan(file);
//		ICAPResponse response = client.echo(file);
//		ICAPResponse response = client.info();
		
		System.out.println(response);
		Map<String, List<String>> headers = response.getHeaderEntries();
		Set<Map.Entry<String, List<String>>> entries = headers.entrySet();
		for( Map.Entry<String, List<String>> entry: entries ) {
			System.out.print(String.format("%s=%s\n", entry.getKey(), entry.getValue()));
		}
		System.out.println();
		
		if( response.getBody() != null ) {
			System.err.println(new String(response.getBody(), "UTF-8"));
		}
		
	}
	
	public ICAPResponse info() throws ICAPException {
		
		String service = "info";
		
		ICAPResponse option = options(service);
		
		ICAPRequest request = new ICAPRequest(service, Mode.REQMOD);
		
		request.setPreview(option.getIntegerHeader("Preview"));
		
        String requestHeader 
        		= "GET /info HTTP/1.1"+END_LINE_DELIMITER
        		+ "Host: " + getHost() + ":" + getPort() + END_MESSAGE_DELIMITER;
        
        request.setRequestHeader(requestHeader);
        
		return sendRequest(request);

	}
	
	public ICAPResponse echo(File file) throws ICAPException {
		
		String service = "echo";
		
		ICAPResponse option = options(service);

		ICAPRequest request = new ICAPRequest(service, Mode.RESPMOD);
		
		request.setPreview(option.getIntegerHeader("Preview"));
		try {
			request.setContent(file);
		} catch(IOException e) {
			throw new ICAPException(e);
		}

        // First part of header
        String requestHeader 
        		= "GET /" + request.getResourceName() + " HTTP/1.1"+END_LINE_DELIMITER
        		+ "Host: " + getHost() + ":" + getPort() + END_MESSAGE_DELIMITER;
        
        String responseHeader
        		= "HTTP/1.1 200 OK" + END_LINE_DELIMITER
        		+ "Transfer-Encoding: chunked" + END_LINE_DELIMITER
        		+ "Content-Length: "+request.getContent().length + END_MESSAGE_DELIMITER;
		
        request.setRequestHeader(requestHeader);
        request.setResponseHeader(responseHeader);
        
		return sendRequest(request);

	}
	
	public ICAPResponse virus_scan(File file) throws ICAPException {
		
		String service = "virus_scan";
		
		ICAPResponse option = options(service);

		ICAPRequest request = new ICAPRequest(service, Mode.RESPMOD);
		
		request.setPreview(option.getIntegerHeader("Preview"));
		try {
			request.setContent(file);
		} catch(IOException e) {
			throw new ICAPException(e);
		}

        // First part of header
        String requestHeader 
        		= "GET /" + request.getResourceName() + " HTTP/1.1"+END_LINE_DELIMITER
        		+ "Host: " + getHost() + ":" + getPort() + END_MESSAGE_DELIMITER;
        
        String responseHeader
        		= "HTTP/1.1 200 OK" + END_LINE_DELIMITER
        		+ "Transfer-Encoding: chunked" + END_LINE_DELIMITER
        		+ "Content-Length: "+request.getContent().length + END_MESSAGE_DELIMITER;
		
        request.setRequestHeader(requestHeader);
        request.setResponseHeader(responseHeader);
        
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
	
	private ICAPResponse makeRequest(ICAPRequest request) throws IOException {
        
        byte[] content = request.getContent();
        if(content == null) {
        	content = new byte[]{};
        }

		Socket socket = new Socket(host, port);
		
		InputStream is = socket.getInputStream();
		OutputStream os = socket.getOutputStream();
        
        int previewSize = request.getPreview();
        if (content.length < request.getPreview()){
            previewSize = content.length;
        }
        
        StringBuilder encapsulated = new StringBuilder();
        encapsulated.append("req-hdr=0");
        
        int responseHeaderStart = 0;
        int responseBodyStart = 0;
        
        int requestHeaderSize = 0;
        if( request.getRequestHeader() != null ) {
        	requestHeaderSize = request.getRequestHeader().length();
        	responseHeaderStart = requestHeaderSize;
        	responseBodyStart = requestHeaderSize;
        }
        
        int responseHeaderSize = 0;
        if( request.getResponseHeader() != null ) {
        	responseHeaderSize = request.getResponseHeader().length();
        	responseBodyStart += responseHeaderSize; 
        }
        
        if( previewSize > 0 ) {
        	encapsulated.append(", res-hdr="+responseHeaderStart);
        	encapsulated.append(", res-body="+responseBodyStart);
        } else {
        	encapsulated.append(", null-body="+responseBodyStart);
        }
		
        String icapRequestHeader = 
        		request.getMode().name()+" icap://"+host+"/"+request.getService()+" ICAP/"+VERSION+END_LINE_DELIMITER
              + "Host: "+host+END_LINE_DELIMITER
              + "Connection: close"+END_LINE_DELIMITER
              + "User-Agent: "+USER_AGENT+END_LINE_DELIMITER
              + "Allow: 204"+END_LINE_DELIMITER
              + "Preview: "+previewSize+END_LINE_DELIMITER
              +"Encapsulated: "+encapsulated.toString()+END_LINE_DELIMITER
              + END_LINE_DELIMITER;
        
        os.write(icapRequestHeader.getBytes());

        if( requestHeaderSize > 0 ) {
        	os.write(request.getRequestHeader().getBytes());
        }
        
        if( responseHeaderSize > 0 ) {
        	os.write(request.getResponseHeader().getBytes());
        }
        
        if(previewSize > 0) {
	        
	        os.write(Integer.toHexString(previewSize).getBytes());
	        os.write(END_LINE_DELIMITER.getBytes());
	        
        	os.write(content, 0, previewSize);
        	os.write(END_LINE_DELIMITER.getBytes());
        	
        	if (content.length <= previewSize){
        		// Fim da transmissão
        		os.write("0; ieof".getBytes());
        		os.write(END_MESSAGE_DELIMITER.getBytes());
        	} else if (previewSize != 0){
        		// Ainda tem mais para transmitir
        		os.write("0".getBytes());
        		os.write(END_MESSAGE_DELIMITER.getBytes());
        	}
        	
        }
        
        os.flush();
        
        ICAPResponse response = new ICAPResponse();
        extractResponse(response, is);
        
        if( response.getStatus() == 100 /*continue*/ ) {
        	
        	int remaining = (content.length - previewSize);
            os.write(Integer.toHexString(remaining).getBytes());
            os.write(END_LINE_DELIMITER.getBytes());
            
            os.write(content, previewSize, remaining);
            os.write(END_LINE_DELIMITER.getBytes());
            
        	os.write("0".getBytes());
        	os.write(END_MESSAGE_DELIMITER.getBytes());
        	
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
		
        String content = null;
        
        ByteArrayOutputStream cache = new ByteArrayOutputStream();
        int reader = -1;
        
        while((reader = is.read()) != -1) {
        	cache.write(reader);
        	byte[] data = cache.toByteArray();
        	if( data.length >= 4 ) {
	        	if(        data[data.length - 4] == '\r' 
	        			&& data[data.length - 3] == '\n' 
	        			&& data[data.length - 2] == '\r' 
	        			&& data[data.length - 1] == '\n' ) {
	        		break;
	        	}
        	}
        }
        
        content = new String(cache.toByteArray(), "UTF-8");
        
        extractHeaders(response, content);
        
        if( response.getStatus() == 100 
        		|| response.getStatus() == 204 
        		|| response.getStatus() > 400 ) {
        	return;
        }
        
        if( ! response.containHeaderValue("Encapsulated", "null-body") ) {
	        cache = new ByteArrayOutputStream();
	        while((reader = is.read()) != -1) {
	        	cache.write(reader);
	        	byte[] data = cache.toByteArray();
	        	if( data.length >= 5 ) {
		        	if(        data[data.length - 5] == '0'
		        			&& data[data.length - 4] == '\r' 
		        			&& data[data.length - 3] == '\n' 
		        			&& data[data.length - 2] == '\r' 
		        			&& data[data.length - 1] == '\n' ) {
		        		break;
		        	}
	        	}
	        }
        }
        
        if( cache.size() > 0 ) {
        	response.setBody(cache.toByteArray());
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
	
//	private static class ResponseListener implements Runnable, Closeable {
//		
//		private InputStream is;
//		
//		ResponseListener(InputStream is) {
//			this.is = is;
//		}
//		
//		private boolean running = false;
//		private boolean closed = false;
//		
//		public void close() throws IOException {
//			synchronized(this) {
//				if(running) running = false;
//				if(!closed) {
//					closed = true;
//					is.close();
//				}
//			}
//		}
//		
//		public void run() {
//			
//			running = true;
//			
//			int reader = -1;
//			try {
//				while((reader = is.read()) != -1) {
//					System.err.print((char)reader);
//				}
//			} catch(Exception e) {
////				Logger.getGlobal().log(Level.SEVERE, "Response Listener General Exception", e);
//			}
//			
//			synchronized(this) {
//				if(running) running = false;
//				if(!closed) closed = true;
//			}
//			
//		}
//		
//	}
//	
//	static {
//		
//		Logger.getGlobal().addHandler(new Handler() {
//			
//			public void publish(LogRecord record) {
//				
//			}
//			
//			public void flush() {
//				
//			}
//			
//			public void close() throws SecurityException {
//				
//			}
//			
//		});
//		
//	}
	
}
