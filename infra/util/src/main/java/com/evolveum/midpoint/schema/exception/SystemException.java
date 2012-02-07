package com.evolveum.midpoint.schema.exception;

public class SystemException extends RuntimeException {

	private static final long serialVersionUID = -611042093339023362L;

	public SystemException() {
	}

	public SystemException(String message) {
		super(message);
	}

	public SystemException(Throwable throwable) {
		super(throwable);
	}

	public SystemException(String message, Throwable throwable) {
		super(message, throwable);
	}

}
