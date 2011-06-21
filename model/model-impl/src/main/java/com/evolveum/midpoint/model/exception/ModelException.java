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
 */
package com.evolveum.midpoint.model.exception;

import com.evolveum.midpoint.common.result.OperationResult;

/**
 * 
 * @author lazyman
 * 
 */
public class ModelException extends Exception {

	private static final long serialVersionUID = -3919194484725388148L;
	private OperationResult result;

	public ModelException(String message, OperationResult result) {
		super(message);
		this.result = result;
	}

	public ModelException(String message, Throwable throwable, OperationResult result) {
		super(message, throwable);
		this.result = result;
	}

	public OperationResult getResult() {
		return result;
	}
}
