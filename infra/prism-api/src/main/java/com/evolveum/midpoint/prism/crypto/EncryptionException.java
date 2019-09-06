/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.crypto;

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
