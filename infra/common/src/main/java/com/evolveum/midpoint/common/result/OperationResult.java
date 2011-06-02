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
 * @author Radovan Semancik
 * 
 */
public class OperationResult implements Serializable {

	private static final long serialVersionUID = -2467406395542291044L;
	private static final String INDENT_STRING = "  ";
	private String operation;
	private OperationResultStatus status;
	private Map<String, Object> params;
	private Map<String, Object> context;
	private long token;
	private String messageCode;
	private String message;
	private String details;
	private Exception cause;
	private List<OperationResult> subresults;

	public OperationResult(String operation) {
		this(operation, null, OperationResultStatus.UNKNOWN, 0, null, null, null, null, null);
	}
	
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
			long token, String messageCode, String message, List<OperationResult> subresults) {
		this(operation, params, status, token, messageCode, message, null, null, subresults);
	}

	public OperationResult(String operation, Map<String, Object> params, OperationResultStatus status,
			long token, String messageCode, String message, String details, Exception cause,
			List<OperationResult> subresults) {
		if (StringUtils.isEmpty(operation)) {
			throw new IllegalArgumentException("Operation argument must not be null or empty.");
		}
		if (status == null) {
			throw new IllegalArgumentException("Operation status must not be null.");
		}
		this.operation = operation;
		this.params = params;
		this.status = status;
		this.token = token;
		this.messageCode = messageCode;
		this.message = message;
		this.details = details;
		this.cause = cause;
		this.subresults = subresults;
	}
	
	public OperationResult createSubresult(String operation) {
		OperationResult subresult = new OperationResult(operation);
		addSubresult(subresult);
		return subresult;
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
	 * Method returns list of operation subresults @{link
	 * {@link OperationResult}.
	 * 
	 * @return never returns null
	 */
	public List<OperationResult> getSubresults() {
		if (subresults == null) {
			subresults = new ArrayList<OperationResult>();
		}
		return subresults;
	}
	
	public void addSubresult(OperationResult subresult) {
		getSubresults().add(subresult);
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
	 * Returns true if the result is success.
	 * 
	 * This returns true if the result is absolute success. Presence of partial
	 * failures or warnings fail this test.
	 * 
	 * @return true if the result is success.
	 */
	public boolean isSuccess() {
		return (status==OperationResultStatus.SUCCESS);
	}
	
	/**
	 * Returns true if the result is acceptable for further processing.
	 * 
	 * In other words: if there were no fatal errors. Warnings and partial
	 * errors are acceptable. Yet, this test also fails if the operation state
	 * is not known.
	 * 
	 * @return true if the result is acceptable for further processing.
	 */
	public boolean isAcceptable() {
		return (status!=OperationResultStatus.FATAL_ERROR);
	}
	
	public void computeStatus() {
		OperationResultStatus newStatus = OperationResultStatus.UNKNOWN;
		boolean allSuccess = true;
		for (OperationResult sub : getSubresults()) {
			if (sub.getStatus()==OperationResultStatus.FATAL_ERROR) {
				status = OperationResultStatus.FATAL_ERROR;
				return;
			}
			if (sub.getStatus()!=OperationResultStatus.SUCCESS) {
				allSuccess = false;
			}			
			if (sub.getStatus()==OperationResultStatus.PARTIAL_ERROR) {
				newStatus = OperationResultStatus.PARTIAL_ERROR;
			}
			if (newStatus!=OperationResultStatus.PARTIAL_ERROR) {
				if (sub.getStatus()==OperationResultStatus.WARNING) {
					newStatus = OperationResultStatus.WARNING;
				}
			}
		}
		if (allSuccess && !getSubresults().isEmpty()) {
			status = OperationResultStatus.SUCCESS;
		} else {
			status = newStatus;
		}
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
	
	public void addParam(String paramName,Object paramValue) {
		getParams().put(paramName, paramValue);
	}

	public Map<String, Object> getContext() {
		if (context == null) {
			context = new HashMap<String, Object>();
		}
		return context;
	}

	public void addContext(String contextName,Object value) {
		getContext().put(contextName, value);
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
	
	public void recordSuccess() {
		// Success, no message or other explanation is needed.
		status = OperationResultStatus.SUCCESS;
	}

	public void recordFatalError(Exception cause) {
		recordError(OperationResultStatus.FATAL_ERROR,cause);
	}
	
	public void recordError(OperationResultStatus status, Exception cause) {
		this.status = status;
		this.cause = cause;
		// No other message was given, so use message from the exception
		// not really correct, but better than nothing.
		message = cause.getMessage();
	}

	public void recordFatalError(String message, Exception cause) {
		recordError(OperationResultStatus.FATAL_ERROR,message,cause);
	}

	public void recordError(OperationResultStatus status, String message, Exception cause) {
		this.status = status;
		this.message = message;
		this.cause = cause;		
	}

	public void recordFatalError(String message) {
		recordError(OperationResultStatus.FATAL_ERROR,message);
	}

	public void recordError(OperationResultStatus status, String message) {
		this.status = status;
		this.message = message;
	}
	
	@Override
	public String toString() {
		return OperationResult.class.getSimpleName() + "(" +
				operation + " " +
				status + " " +
				message;
	}
	
	public String debugDump() {
		StringBuilder sb = new StringBuilder();
		debugDumpIndent(sb,0);
		return sb.toString();
	}
	
	private void debugDumpIndent(StringBuilder sb,int indent) {
		for (int i=0;i<indent;i++) {
			sb.append(INDENT_STRING);
		}
		sb.append(operation);
		sb.append(" ");
		sb.append(status);
		sb.append(" ");
		sb.append(message);
		sb.append("\n");
		
		for (OperationResult sub : getSubresults()) {
			sub.debugDumpIndent(sb,indent+1);
		}
	}

}
