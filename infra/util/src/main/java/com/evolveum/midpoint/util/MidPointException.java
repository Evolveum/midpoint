/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2011 Viliam Repan
 */
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
