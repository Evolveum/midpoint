package com.evolveum.midpoint.common.crypto;

public class EncryptionException extends Exception {

	private static final long serialVersionUID = 8289563205061329615L;

	public EncryptionException(String message) {
		super(message);
	}

	public EncryptionException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public EncryptionException(Throwable throwable) {
		super(throwable);
	}
}
