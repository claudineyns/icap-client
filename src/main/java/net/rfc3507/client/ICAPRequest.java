package net.rfc3507.client;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;

public class ICAPRequest {

	private String service;

	public static enum Mode {
		REQMOD, RESPMOD;
	}
	
	private Mode mode;
	
	public ICAPRequest(String service, Mode mode) {
		this.service = service;
		this.mode = mode;
	}
	
	public String getService() {
		return service;
	}
	
	public Mode getMode() {
		return mode;
	}
	
	private int preview = -1;
	
	public void setPreview(int preview) {
		this.preview = preview;
	}
	
	public int getPreview() {
		return preview;
	}
	
	private String httpHost;
	
	public void setHttpHost(String httpHost) {
		this.httpHost = httpHost;
	}
	
	public String getHttpHost() {
		return httpHost;
	}
	
	private Integer httpPort = null;
	
	public void setHttpPort(int httpPort) {
		this.httpPort = Integer.valueOf(httpPort);
	}
	
	public Integer getHttpPort() {
		return httpPort;
	}
	
	private byte[] httpRequestHeader;
	
	public byte[] getHttpRequestHeader() {
		return httpRequestHeader;
	}
	
	public void setHttpRequestHeader(byte[] httpRequestHeader) {
		this.httpRequestHeader = httpRequestHeader;
	}
	
	private byte[] httpRequestBody;
	
	public void setHttpRequestBody(byte[] httpRequestBody) {
		this.httpRequestBody = httpRequestBody;
	}
	
	public void setHttpRequestBody(File body) throws ICAPException {
		this.httpRequestBody = readFile(body);
	}
	
	public void setHttpRequestBody(URL resource) throws ICAPException {
		this.httpRequestBody = readURL(resource);
	}
	
	public byte[] getHttpRequestBody() {
		return httpRequestBody;
	}
	
	private byte[] httpResponseHeader;
	
	public byte[] getHttpResponseHeader() {
		return httpResponseHeader;
	}
	
	public void setHttpResponseHeader(byte[] httpResponseHeader) {
		this.httpResponseHeader = httpResponseHeader;
	}
	
	private byte[] httpResponseBody;
	
	public void setHttpResponseBody(byte[] httpResponseBody) {
		this.httpResponseBody = httpResponseBody;
	}
	
	public void setHttpResponseBody(File body) throws ICAPException {
		this.httpResponseBody = readFile(body);
	}
	
	public void setHttpResponseBody(URL resource) throws ICAPException {
		this.httpResponseBody = readURL(resource);
	}
	
	public byte[] getHttpResponseBody() {
		return httpResponseBody;
	}
	
	private String resourceName;
	
	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}
	
	public String getResourceName() {
		return resourceName;
	}
	
	private byte[] readFile(File body) throws ICAPException {
		
		InputStream is = null;
		try {
			is = new FileInputStream(body);
		} catch(IOException e) {
			throw new ICAPException(e);
		}
		
		return readResourceAndClose(is);
		
	}
	
	private byte[] readURL(URL resource) throws ICAPException {
		
		InputStream is = null;
		try {
			is = resource.openStream();
		} catch(IOException e) {
			throw new ICAPException(e);
		}
		
		return readResourceAndClose(is);
		
	}
	
	private byte[] readResourceAndClose(InputStream is) throws ICAPException {
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		try {
			IOUtils.copy(is, out);
		} catch(IOException e) {
			throw new ICAPException(e);
		} finally {
			try { is.close(); } catch(IOException f){}
		}
		
		return out.toByteArray();
		
	}
	
}
