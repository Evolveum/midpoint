package com.evolveum.midpoint.util;

import com.evolveum.midpoint.util.result.OperationResult;

/**
 * This class is used as base class for exceptions which are thrown in complex
 * operations. It provides @{link OperationResult} object for better error
 * handling.
 * 
 * @author lazyman
 * 
 */
public abstract class MidPointException extends Exception {

	private static final long serialVersionUID = -1203865058138765240L;
	private OperationResult result;

	public MidPointException(String message, OperationResult result) {
		this(message, null, result);
	}

	public MidPointException(String message, Throwable ex, OperationResult result) {
		super(message, ex);
		if (result == null) {
			throw new IllegalArgumentException("Operation result must not be null.");
		}
		this.result = result;
	}

	public OperationResult getResult() {
		return result;
	}
}
