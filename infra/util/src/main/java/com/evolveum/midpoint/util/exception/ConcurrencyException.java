/**
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
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.util.exception;

/**
 * Exceptional concurrency state or operation invocation.
 * 
 * This exception is thrown in case of race conditions and similar conflicting concurrency conditions.
 * It is also thrown in an attempt to acquire already acquired locks and similar cases.
 * 
 * This condition is implemented as exception in a hope that it will help avoid silently ignoring the
 * concurrency problems and that the developers will be forced to handle the condition.
 * It is much easier to ignore a return value than to ignore an exception.
 * 
 * @author Radovan Semancik
 *
 */
public class ConcurrencyException extends CommonException {

	public ConcurrencyException() {
	}

	public ConcurrencyException(String message) {
		super(message);
	}

	public ConcurrencyException(Throwable cause) {
		super(cause);
	}

	public ConcurrencyException(String message, Throwable cause) {
		super(message, cause);
	}

	@Override
	public String getOperationResultMessage() {
		return "Concurrency exception";
	}
	
	

}
