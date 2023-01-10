package io.github.rfc3507.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class ICAPRequest {

	private String service;

	public static enum Mode {
		REQMOD, RESPMOD;
	}
	
	private Mode mode;
	
	private ICAPRequest(String service, Mode mode) {
		this.service = service;
		this.mode = mode;
	}

	public static ICAPRequest instance(String service, Mode mode) {
		return new ICAPRequest(service, mode);
	}
	
	public String getService() {
		return service;
	}
	
	public Mode getMode() {
		return mode;
	}
	
	private int preview = -1;
	
	public ICAPRequest setPreview(int preview) {
		this.preview = preview;
		return this;
	}
	
	public int getPreview() {
		return preview;
	}
	
	private byte[] httpRequestHeader;
	
	public byte[] getHttpRequestHeader() {
		return httpRequestHeader;
	}
	
	public ICAPRequest setHttpRequestHeader(byte[] httpRequestHeader) {
		this.httpRequestHeader = httpRequestHeader;
		return this;
	}
	
	private byte[] httpRequestBody;
	
	public ICAPRequest setHttpRequestBody(byte[] httpRequestBody) {
		this.httpRequestBody = httpRequestBody;
		return this;
	}
	
	public ICAPRequest setHttpRequestBody(File body) throws ICAPException {
		this.httpRequestBody = readFile(body);
		return this;
	}
	
	public ICAPRequest setHttpRequestBody(URL resource) throws ICAPException {
		this.httpRequestBody = readURL(resource);
		return this;
	}
	
	public byte[] getHttpRequestBody() {
		return httpRequestBody;
	}
	
	private byte[] httpResponseHeader;
	
	public byte[] getHttpResponseHeader() {
		return httpResponseHeader;
	}
	
	public ICAPRequest setHttpResponseHeader(byte[] httpResponseHeader) {
		this.httpResponseHeader = httpResponseHeader;
		return this;
	}
	
	private byte[] httpResponseBody;
	
	public ICAPRequest setHttpResponseBody(byte[] httpResponseBody) {
		this.httpResponseBody = httpResponseBody;
		return this;
	}
	
	public ICAPRequest setHttpResponseBody(File body) throws ICAPException {
		this.httpResponseBody = readFile(body);
		return this;
	}
	
	public ICAPRequest setHttpResponseBody(URL resource) throws ICAPException {
		this.httpResponseBody = readURL(resource);
		return this;
	}
	
	public byte[] getHttpResponseBody() {
		return httpResponseBody;
	}
	
	private String resourceName;
	
	public ICAPRequest setResourceName(String resourceName) {
		this.resourceName = resourceName;
		return this;
	}
	
	public String getResourceName() {
		return resourceName;
	}
	
	private byte[] readFile(File body) throws ICAPException {
		try(final InputStream is = new FileInputStream(body)) {
			return is.readAllBytes();
		} catch(IOException e) {
			throw new ICAPException(e);
		}
	}
	
	private byte[] readURL(URL resource) throws ICAPException {
		try(final InputStream is = resource.openStream()) {
			return is.readAllBytes();
		} catch(IOException e) {
			throw new ICAPException(e);
		}
	}
	
}
