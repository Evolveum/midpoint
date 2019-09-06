/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util.exception;

import com.evolveum.midpoint.util.LocalizableMessage;

/**
 * Generic communication exception.
 *
 * May happen in case of various network communication errors, including
 * (but not limited to) connection refused and timeouts.
 *
 * TODO
 *
 * @author Radovan Semancik
 *
 */
public class CommunicationException extends CommonException {
	private static final long serialVersionUID = 1L;

	public CommunicationException() {
	}

	public CommunicationException(String message) {
		super(message);
	}

	public CommunicationException(LocalizableMessage userFriendlyMessage) {
		super(userFriendlyMessage);
	}

	public CommunicationException(Throwable cause) {
		super(cause);
	}

	public CommunicationException(String message, Throwable cause) {
		super(message, cause);
	}

	public CommunicationException(LocalizableMessage userFriendlyMessage, Throwable cause) {
		super(userFriendlyMessage, cause);
	}

	@Override
	public String getErrorTypeMessage() {
		return "Communication error";
	}

}
