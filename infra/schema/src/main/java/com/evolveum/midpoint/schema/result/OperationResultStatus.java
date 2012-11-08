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
package com.evolveum.midpoint.schema.result;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.OperationResultStatusType;

/**
 * 
 * @author lazyman
 * 
 */
public enum OperationResultStatus {

	/**
	 * No information about operation is present.
	 * Presence of this status usually means programming bug, e.g. someone forgot to
	 * set or compute appropriate operation result.
	 */
	UNKNOWN,

	/**
	 * Used when operation and sub operations finish successfully.
	 */
	SUCCESS,

	/**
	 * Used when operation finish successfully, but minor problem occurred. For
	 * example operation code recovered from some error and after that operation
	 * finished successfully.
	 */
	WARNING,

	/**
	 * Used when operation contains at least one operation witch status
	 * SUCCESS/WARNING and at least one operation with status FATAL_ERROR.
	 */
	PARTIAL_ERROR,

	/**
	 * Used when operation didn't finish correctly.
	 */
	FATAL_ERROR,
	
	/**
	 * The operation didn't finish correctly but that was expected and handled. It is
	 * equivalent to success for all practical cases except for displaying the result. But using
	 * success status for this situation might be misleading.
	 */
	HANDLED_ERROR,
	
	/**
	 * Result does not make any sense for the operation. This is useful in cases that the
	 * operation is not supported (e.g. an optional part of the interface).
	 * This is different than UNKNOWN, as in this case we really know that it result is not
	 * applicable. In UNKNOWN case we know nothing.
	 */
	NOT_APPLICABLE,
	
	/*
	 * The operation is being executed. This is set for operations that are executed
	 * asynchronously or take a significant amount of time. Short synchronous operations
	 * do not need to set this status, they may go well with the default UNKNOWN status.
	 */
	IN_PROGRESS;

	public static OperationResultStatus parseStatusType(OperationResultStatusType statusType) {
		if (statusType == null) {
			return UNKNOWN;
		}

		switch (statusType) {
			case FATAL_ERROR:
				return FATAL_ERROR;
			case PARTIAL_ERROR:
				return PARTIAL_ERROR;
			case HANDLED_ERROR:
				return HANDLED_ERROR;
			case SUCCESS:
				return SUCCESS;
			case WARNING:
				return WARNING;
			case NOT_APPLICABLE:
				return NOT_APPLICABLE;
			case IN_PROGRESS:
				return IN_PROGRESS;
			default:
				return UNKNOWN;
		}
	}

	public static OperationResultStatusType createStatusType(OperationResultStatus status) {
		if (status == null) {
			return OperationResultStatusType.UNKNOWN;
		}

		switch (status) {
			case SUCCESS:
				return OperationResultStatusType.SUCCESS;
			case WARNING:
				return OperationResultStatusType.WARNING;
			case FATAL_ERROR:
				return OperationResultStatusType.FATAL_ERROR;
			case PARTIAL_ERROR:
				return OperationResultStatusType.PARTIAL_ERROR;
			case HANDLED_ERROR:
				return OperationResultStatusType.HANDLED_ERROR;
			case NOT_APPLICABLE:
				return OperationResultStatusType.NOT_APPLICABLE;
			case IN_PROGRESS:
				return OperationResultStatusType.IN_PROGRESS;
			default:
				return OperationResultStatusType.UNKNOWN;
		}
	}

	public OperationResultStatusType createStatusType() {
		return createStatusType(this);
	}
}
