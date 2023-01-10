package io.github.rfc3507.client;

public class ICAPException extends Exception {

	public ICAPException() { /***/ }

	public ICAPException(String message) {
		super(message);
	}
	
	public ICAPException(Throwable throwable) {
		super(throwable);
	}
	
	public ICAPException(String message, Throwable throwable) {
		super(message, throwable);
	}
	
}
