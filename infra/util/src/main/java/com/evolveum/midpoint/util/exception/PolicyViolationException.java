/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util.exception;

import com.evolveum.midpoint.util.LocalizableMessage;

/**
 * @author semancik
 *
 */
public class PolicyViolationException extends CommonException {
	private static final long serialVersionUID = 1L;

	public PolicyViolationException() {
	}

	public PolicyViolationException(String message) {
		super(message);
	}

	public PolicyViolationException(LocalizableMessage userFriendlyMessage) {
		super(userFriendlyMessage);
	}

	public PolicyViolationException(Throwable cause) {
		super(cause);
	}

	public PolicyViolationException(LocalizableMessage userFriendlyMessage, Throwable cause) {
		super(userFriendlyMessage, cause);
	}

	public PolicyViolationException(String message, Throwable cause) {
		super(message, cause);
	}

	@Override
	public String getErrorTypeMessage() {
		return "Policy violation";
	}

}
