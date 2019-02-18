package br.eti.claudiney.icap.client;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

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
	
	private int preview = 0;
	
	public void setPreview(int preview) {
		this.preview = preview;
	}
	
	public int getPreview() {
		return preview;
	}
	
	private String requestHeader;
	
	public void setRequestHeader(String requestHeader) {
		this.requestHeader = requestHeader;
	}
	
	public String getRequestHeader() {
		return requestHeader;
	}
	
	private String responseHeader;
	
	public void setResponseHeader(String responseHeader) {
		this.responseHeader = responseHeader;
	}
	
	public String getResponseHeader() {
		return responseHeader;
	}
	
	private String resourceName;
	
	void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}
	
	public String getResourceName() {
		return resourceName;
	}
	
	private byte[] content;
	
	public byte[] getContent() {
		return content;
	}
	
	public void setContent(byte[] content, String resourceName) {
		this.content = content;
		this.resourceName = resourceName;
	}
	
	public void setContent(File file) throws IOException {
		
		InputStream is = new FileInputStream(file);
		ByteArrayOutputStream cache = new ByteArrayOutputStream();
		
		try {
			IOUtils.copy(is, cache);
		} finally {
			is.close();
		}
		
		this.content = cache.toByteArray();
		this.resourceName = file.getName();
		
	}
	
	public void setContent(InputStream is, String resourceName) throws IOException {
		
		ByteArrayOutputStream cache = new ByteArrayOutputStream();
		
		IOUtils.copy(is, cache);
		
		this.content = cache.toByteArray();
		this.resourceName = resourceName; 
		
	}
	
}
