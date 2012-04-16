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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.util.exception;

/**
 * Exception indicating violation of security policies.
 * It is SecurityViolationException to avoid confusion with java.lang.SecurityException
 * 
 * @author Radovan Semancik
 *
 */
public class SecurityViolationException extends CommonException {

	public SecurityViolationException() {
	}

	public SecurityViolationException(String message) {
		super(message);
	}

	public SecurityViolationException(Throwable cause) {
		super(cause);
	}

	public SecurityViolationException(String message, Throwable cause) {
		super(message, cause);
	}

	@Override
	public String getOperationResultMessage() {
		return "Security violation";
	}

}
