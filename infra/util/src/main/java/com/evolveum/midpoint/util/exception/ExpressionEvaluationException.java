/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util.exception;

import com.evolveum.midpoint.util.LocalizableMessage;

/**
 * Error during evaluation of expression. The expressions are defined by system administrator.
 *
 * @author Radovan Semancik
 *
 */
public class ExpressionEvaluationException extends CommonException {
	private static final long serialVersionUID = 5615419722362251191L;

	public ExpressionEvaluationException() {
	}

	public ExpressionEvaluationException(String message) {
		super(message);
	}

	public ExpressionEvaluationException(LocalizableMessage userFriendlyMessage) {
		super(userFriendlyMessage);
	}

	public ExpressionEvaluationException(Throwable cause) {
		super(cause);
	}

	public ExpressionEvaluationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ExpressionEvaluationException(String message, Throwable cause, LocalizableMessage userFriendlyMessage) {
		super(message, cause, userFriendlyMessage);
	}

	public ExpressionEvaluationException(LocalizableMessage userFriendlyMessage, Throwable cause) {
		super(userFriendlyMessage, cause);
	}

	@Override
	public String getErrorTypeMessage() {
		return "Expression error";
	}

}
