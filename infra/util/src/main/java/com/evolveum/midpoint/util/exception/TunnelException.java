/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util.exception;

/**
 * Exception used for tunneling checked exceptions through places where checked exceptipons are not allowed (e.g. callbacks).
 *
 * This exception must not appear to the "outside", it must be caught and transformed back to the original form.
 *
 * @author Radovan Semancik
 *
 */
public class TunnelException extends RuntimeException {
	private static final long serialVersionUID = -3745473492409029661L;

	public TunnelException() {
		super();
	}

	public TunnelException(String message, Throwable cause) {
		super(message, cause);
	}

	public TunnelException(String message) {
		super(message);
	}

	public TunnelException(Throwable cause) {
		super(cause);
	}

}
