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
package com.evolveum.midpoint.common.result;

import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultStatusType;

/**
 * 
 * @author lazyman
 * 
 */
public enum OperationResultStatus {

	/**
	 * No information about operation is present.
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
	FATAL_ERROR;

	public static OperationResultStatus parseStatusType(OperationResultStatusType statusType) {
		if (statusType == null) {
			return UNKNOWN;
		}
		
		switch (statusType) {
			case FATAL_ERROR:
				return FATAL_ERROR;
			case PARTIAL_ERROR:
				return PARTIAL_ERROR;
			case SUCCESS:
				return SUCCESS;
			case WARNING:
				return WARNING;
			default:
				return UNKNOWN;
		}
	}

	public OperationResultStatusType createStatusType() {
		switch (this) {
			case SUCCESS:
				return OperationResultStatusType.SUCCESS;
			case WARNING:
				return OperationResultStatusType.WARNING;
			case FATAL_ERROR:
				return OperationResultStatusType.FATAL_ERROR;
			case PARTIAL_ERROR:
				return OperationResultStatusType.PARTIAL_ERROR;
			default:
				return OperationResultStatusType.WARNING;
		}
	}
}
