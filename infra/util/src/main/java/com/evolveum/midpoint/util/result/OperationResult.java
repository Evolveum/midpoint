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
package com.evolveum.midpoint.util.result;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * This class provides informations for better error handling in complex
 * operations.
 * 
 * @author lazyman
 * 
 */
public class OperationResult implements Serializable {

	private static final long serialVersionUID = -2467406395542291044L;
	private String operation;
	private OperationResultStatus status;
	private Map<String, Object> params;
	private long token;
	private String messageCode;
	private String message;
	private String details;
	private Exception cause;
	private List<OperationResult> partialResults;

	public OperationResult(String operation, String messageCode, String message) {
		this(operation, null, OperationResultStatus.SUCCESS, 0, messageCode, message, null, null, null);
	}

	public OperationResult(String operation, long token, String messageCode, String message) {
		this(operation, null, OperationResultStatus.SUCCESS, token, messageCode, message, null, null, null);
	}

	public OperationResult(String operation, OperationResultStatus status, String messageCode, String message) {
		this(operation, null, status, 0, messageCode, message, null, null, null);
	}

	public OperationResult(String operation, OperationResultStatus status, long token, String messageCode,
			String message) {
		this(operation, null, status, token, messageCode, message, null, null, null);
	}

	public OperationResult(String operation, OperationResultStatus status, long token, String messageCode,
			String message, Exception cause) {
		this(operation, null, status, token, messageCode, message, null, cause, null);
	}

	public OperationResult(String operation, Map<String, Object> params, OperationResultStatus status,
			long token, String messageCode, String message) {
		this(operation, params, status, token, messageCode, message, null, null, null);
	}

	public OperationResult(String operation, Map<String, Object> params, OperationResultStatus status,
			long token, String messageCode, String message, List<OperationResult> partialResults) {
		this(operation, params, status, token, messageCode, message, null, null, partialResults);
	}

	public OperationResult(String operation, Map<String, Object> params, OperationResultStatus status,
			long token, String messageCode, String message, String details, Exception cause,
			List<OperationResult> partialResults) {
		if (StringUtils.isEmpty(operation)) {
			throw new IllegalArgumentException("Operation argument must not be null or empty.");
		}
		if (status == null) {
			throw new IllegalArgumentException("Operation status must not be null.");
		}
		if (StringUtils.isEmpty(message)) {
			throw new IllegalArgumentException("Operation message must not be null or empty.");
		}
		this.operation = operation;
		this.params = params;
		this.status = status;
		this.token = token;
		this.messageCode = messageCode;
		this.message = message;
		this.details = details;
		this.cause = cause;
		this.partialResults = partialResults;
	}

	/**
	 * Contains operation name. Operation name must be defined as {@link String}
	 * constant in module interface with description and possible parameters. It
	 * can be used for further processing. It will be used as key for
	 * translation in admin-gui.
	 * 
	 * @return always return non null, non empty string
	 */
	public String getOperation() {
		return operation;
	}

	/**
	 * Method returns list of operation partial results @{link
	 * {@link OperationResult}.
	 * 
	 * @return never returns null
	 */
	public List<OperationResult> getPartialResults() {
		if (partialResults == null) {
			partialResults = new ArrayList<OperationResult>();
		}
		return partialResults;
	}

	/**
	 * Contains operation status as defined in {@link OperationResultStatus}
	 * 
	 * @return never returns null
	 */
	public OperationResultStatus getStatus() {
		return status;
	}

	/**
	 * Method returns {@link Map} with operation parameters. Parameters keys are
	 * described in module interface for every operation.
	 * 
	 * @return never returns null
	 */
	public Map<String, Object> getParams() {
		if (params == null) {
			params = new HashMap<String, Object>();
		}
		return params;
	}

	/**
	 * @return Contains random long number, for better searching in logs.
	 */
	public long getToken() {
		return token;
	}

	/**
	 * Contains mesage code based on module error catalog.
	 * 
	 * @return Can return null.
	 */
	public String getMessageCode() {
		return messageCode;
	}

	/**
	 * @return Method returns operation result message. Message is required. It
	 *         will be key for translation in admin-gui.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @return Method returns operation result detail message. Not required, can
	 *         be null.
	 */
	public String getDetails() {
		return details;
	}

	/**
	 * @return Method returns operation result exception. Not required, can be
	 *         null.
	 */
	public Exception getCause() {
		return cause;
	}
}
