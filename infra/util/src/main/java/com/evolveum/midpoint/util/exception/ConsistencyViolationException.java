/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * 
 */
package com.evolveum.midpoint.util.exception;

/**
 * <p>Consistency constraint violation prohibits completion of an operation.</p>
 * <p>
 * The operation that consists of several steps have partially failed. However the operation cannot
 * be finished as finishing the operation would lead to an inconsistent system.
 * </p>
 * <p>
 * Example: Attempt do delete a user fails with this exception if deleting of any of the
 * linked accounts fails. The user cannot be deleted as deleting the user would result in
 * an orphan account that may never be deleted.
 * </p>
 * 
 * @author Radovan Semancik
 *
 */
@Deprecated
public class ConsistencyViolationException extends CommonException {
	private static final long serialVersionUID = -4194650066561884619L;

	public ConsistencyViolationException() {
		super();
	}

	public ConsistencyViolationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConsistencyViolationException(String message) {
		super(message);
	}

	public ConsistencyViolationException(Throwable cause) {
		super(cause);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.schema.exception.CommonException#getOperationResultMessage()
	 */
	@Override
	public String getOperationResultMessage() {
		return "Consistency violation";
	}

}
