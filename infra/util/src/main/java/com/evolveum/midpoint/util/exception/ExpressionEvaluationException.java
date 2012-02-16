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

	public ExpressionEvaluationException(Throwable cause) {
		super(cause);
	}

	public ExpressionEvaluationException(String message, Throwable cause) {
		super(message, cause);
	}

	@Override
	public String getOperationResultMessage() {
		return "Expression error";
	}

}
