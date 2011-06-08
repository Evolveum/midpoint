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
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.schema.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.EntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LocalizedMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;

/**
 * Nested Operation Result.
 * 
 * This class provides informations for better error handling in complex
 * operations. It contains a status (success, failure, warning, ...) and an
 * error message. It also contains a set of sub-results - results on inner
 * operations.
 * 
 * This object can be used by GUI to display smart (and interactive) error
 * information. It can also be used by the client code to detect deeper problems
 * in the invocations, retry or otherwise compensate for the errors or decide
 * how severe the error was and it is possible to proceed.
 * 
 * @author lazyman
 * @author Radovan Semancik
 * 
 */
public class OperationResult implements Serializable {

	private static final long serialVersionUID = -2467406395542291044L;
	private static final String INDENT_STRING = "  ";
	public static final String CONTEXT_IMPLEMENTATION_CLASS = "implementationClass";
	private String operation;
	private OperationResultStatus status;
	private Map<String, Object> params;
	private Map<String, Object> context;
	private long token;
	private String messageCode;
	private String message;
	private String localizationMessage;
	private List<Object> localizationArguments;
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
			long token, String messageCode, String message, String localizationMessage, Exception cause,
			List<OperationResult> subresults) {
		this(operation, params, status, token, messageCode, message, localizationMessage, null, cause,
				subresults);
	}

	public OperationResult(String operation, Map<String, Object> params, OperationResultStatus status,
			long token, String messageCode, String message, String localizationMessage,
			List<Object> localizationArguments, Exception cause, List<OperationResult> subresults) {
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
		this.localizationMessage = localizationMessage;
		this.localizationArguments = localizationArguments;
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
		return (status == OperationResultStatus.SUCCESS);
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
		return (status != OperationResultStatus.FATAL_ERROR);
	}

	public boolean isUnknown() {
		return (status == OperationResultStatus.UNKNOWN);
	}

	/**
	 * Computes operation result status based on subtask status and sets an
	 * error message if the status is FATAL_ERROR.
	 * 
	 * @param errorMessage
	 *            error message
	 */
	public void computeStatus(String errorMessage) {
		computeStatus();
		if (status == OperationResultStatus.FATAL_ERROR && message == null) {
			message = errorMessage;
		}
	}

	/**
	 * Computes operation result status based on subtask status.
	 */
	public void computeStatus() {
		OperationResultStatus newStatus = OperationResultStatus.UNKNOWN;
		boolean allSuccess = true;
		for (OperationResult sub : getSubresults()) {
			if (sub.getStatus() == OperationResultStatus.FATAL_ERROR) {
				status = OperationResultStatus.FATAL_ERROR;
				return;
			}
			if (sub.getStatus() != OperationResultStatus.SUCCESS) {
				allSuccess = false;
			}
			if (sub.getStatus() == OperationResultStatus.PARTIAL_ERROR) {
				newStatus = OperationResultStatus.PARTIAL_ERROR;
			}
			if (newStatus != OperationResultStatus.PARTIAL_ERROR) {
				if (sub.getStatus() == OperationResultStatus.WARNING) {
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

	public void addParam(String paramName, Object paramValue) {
		getParams().put(paramName, paramValue);
	}

	public Map<String, Object> getContext() {
		if (context == null) {
			context = new HashMap<String, Object>();
		}
		return context;
	}

	public void addContext(String contextName, Object value) {
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
	 * @return Method returns message key for translation, can be null.
	 */
	public String getLocalizationMessage() {
		return localizationMessage;
	}

	/**
	 * @return Method returns arguments if needed for localization, can be null.
	 */
	public List<Object> getLocalizationArguments() {
		return localizationArguments;
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
		recordStatus(OperationResultStatus.FATAL_ERROR, cause);
	}

	public void recordPartialError(Exception cause) {
		recordStatus(OperationResultStatus.PARTIAL_ERROR, cause);
	}

	public void recordStatus(OperationResultStatus status, Exception cause) {
		this.status = status;
		this.cause = cause;
		// No other message was given, so use message from the exception
		// not really correct, but better than nothing.
		message = cause.getMessage();
	}

	public void recordFatalError(String message, Exception cause) {
		recordStatus(OperationResultStatus.FATAL_ERROR, message, cause);
	}

	public void recordPartialError(String message, Exception cause) {
		recordStatus(OperationResultStatus.PARTIAL_ERROR, message, cause);
	}

	public void recordStatus(OperationResultStatus status, String message, Exception cause) {
		this.status = status;
		this.message = message;
		this.cause = cause;
	}

	public void recordFatalError(String message) {
		recordStatus(OperationResultStatus.FATAL_ERROR, message);
	}

	public void recordPartialError(String message) {
		recordStatus(OperationResultStatus.PARTIAL_ERROR, message);
	}
	
	// TODO: switch to a localized message later
	public void record(CommonException exception) {
		// Exception is a fatal error in this context
		recordFatalError(exception.getOperationResultMessage(), exception);
	}

	public void recordStatus(OperationResultStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	@Override
	public String toString() {
		return OperationResult.class.getSimpleName() + "(" + operation + " " + status + " " + message;
	}

	public String debugDump() {
		StringBuilder sb = new StringBuilder();
		debugDumpIndent(sb, 0);
		return sb.toString();
	}

	private void debugDumpIndent(StringBuilder sb, int indent) {
		for (int i = 0; i < indent; i++) {
			sb.append(INDENT_STRING);
		}
		sb.append("op: ");
		sb.append(operation);
		sb.append(", st: ");
		sb.append(status);
		sb.append(", msg: ");
		sb.append(message);
		if (cause != null) {
			sb.append(", cause: ");
			sb.append(cause.getClass().getSimpleName());
			sb.append(":");
			sb.append(cause.getMessage());
		}
		sb.append("\n");
		
		for (Map.Entry<String, Object> entry : getParams().entrySet()) {
			for (int i = 0; i < indent+2; i++) {
				sb.append(INDENT_STRING);
			}
			sb.append("p:");
			sb.append(entry.getKey());
			sb.append(": ");
			sb.append(entry.getValue());
			sb.append("\n");
		}

		for (Map.Entry<String, Object> entry : getContext().entrySet()) {
			for (int i = 0; i < indent+2; i++) {
				sb.append(INDENT_STRING);
			}
			sb.append("c:");
			sb.append(entry.getKey());
			sb.append(": ");
			sb.append(entry.getValue());
			sb.append("\n");
		}

		for (OperationResult sub : getSubresults()) {
			sub.debugDumpIndent(sb, indent + 1);
		}
	}

	public static OperationResult createOperationResult(OperationResultType result) {
		Validate.notNull(result, "Result type must not be null.");

		Map<String, Object> params = null;
		if (result.getParams() != null) {
			params = new HashMap<String, Object>();
			for (EntryType entry : result.getParams().getEntry()) {
				params.put(entry.getKey(), entry.getAny());
			}
		}

		List<OperationResult> subresults = null;
		if (!result.getPartialResults().isEmpty()) {
			subresults = new ArrayList<OperationResult>();
			for (OperationResultType subResult : result.getPartialResults()) {
				subresults.add(createOperationResult(subResult));
			}
		}

		LocalizedMessageType message = result.getLocalizedMessage();
		String localizedMessage = message == null ? null : message.getKey();
		List<Object> localizedArguments = message == null ? null : message.getArgument();

		return new OperationResult(result.getOperation(), params,
				OperationResultStatus.parseStatusType(result.getStatus()), result.getToken(),
				result.getMessageCode(), result.getMessage(), localizedMessage, localizedArguments, null,
				subresults);
	}

	public OperationResultType createOperationResultType() {
		// TODO: finish this!

		return new OperationResultType();
	}
}
