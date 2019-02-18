package br.eti.claudiney.icap.client;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("serial")
public class ICAPResponse implements Serializable {
	
	private Map<String, List<String>> headerEntries = new LinkedHashMap<>(); 

	private Set<String> headers = new LinkedHashSet<>();
	
	private String protocol;
	private int status;
	private String message;
	
	private byte[] body;
	
	ICAPResponse() {
		
	}
	
	void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	
	public String getProtocol() {
		return protocol;
	}
	
	void setStatus(int status) {
		this.status = status;
	}
	
	public int getStatus() {
		return status;
	}
	
	void setMessage(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}
	
	void addHeader(String header, String value) {
		
		headers.add(header);
		
		String h = header.toLowerCase();
		
		List<String> values = headerEntries.get(h);
		if(values == null) {
			values = new LinkedList<>();
			headerEntries.put(h, values);
		}
		values.add(value);
		
	}
	
	public Set<String> getHeaders() {
		return Collections.unmodifiableSet(headers);
	}
	
	public Map<String, List<String>> getHeaderEntries() {
		return Collections.unmodifiableMap(headerEntries);
	}
	
	public List<String> getHeaderValues(String header) {
		return headerEntries.get(header.toLowerCase());
	}
	
	public boolean containHeaderValue(String header, String content) {
		
		List<String> values = headerEntries.get(header.toLowerCase());
		
		if(values == null) {
			return false;
		}
		
		for(String value: values) {
			if(value.contains(content)) {
				return true;
			}
		}
		
		return false;
		
	}
	
	public String getHeader(String header) {
		return headerEntries.get(header.toLowerCase()).get(0);
	}
	
	public int getIntegerHeader(String header) {
		return Integer.parseInt(headerEntries.get(header.toLowerCase()).get(0));
	}
	
	void setBody(byte[] body) {
		this.body = body;
	}
	
	public byte[] getBody() {
		return body;
	}
	
	@Override
	public String toString() {
		return "Protocol="+protocol+"; Status="+status+"; Message="+message;
	}
	
}
