/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util.exception;

import com.evolveum.midpoint.util.LocalizableMessage;

/**
 * @author katka
 *
 */
public class ThresholdPolicyViolationException extends PolicyViolationException {

	private static final long serialVersionUID = 1L;

	public ThresholdPolicyViolationException() {
	}

	public ThresholdPolicyViolationException(String message) {
		super(message);
	}

	public ThresholdPolicyViolationException(LocalizableMessage userFriendlyMessage) {
		super(userFriendlyMessage);
	}

	public ThresholdPolicyViolationException(Throwable cause) {
		super(cause);
	}

	public ThresholdPolicyViolationException(LocalizableMessage userFriendlyMessage, Throwable cause) {
		super(userFriendlyMessage, cause);
	}

	public ThresholdPolicyViolationException(String message, Throwable cause) {
		super(message, cause);
	}
	
	@Override
	public String getErrorTypeMessage() {
		return "Threshold policy violation";
	}
}
