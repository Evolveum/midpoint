/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.schema.result;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

import com.evolveum.midpoint.prism.util.CloneUtil;

import com.evolveum.midpoint.util.DebugUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ParamsTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LocalizedMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

/**
 * Nested Operation Result.
 * 
 * This class provides information for better error handling in complex
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
public class OperationResult implements Serializable, DebugDumpable, Cloneable {

	private static final long serialVersionUID = -2467406395542291044L;
	private static final String INDENT_STRING = "    ";

    /**
     * This constant provides count threshold for same subresults (same operation and
     * status) during summarize operation.
     */
    private static final int SUBRESULT_STRIP_THRESHOLD = 10;
	
	public static final String CONTEXT_IMPLEMENTATION_CLASS = "implementationClass";
	public static final String CONTEXT_PROGRESS = "progress";
	public static final String CONTEXT_OID = "oid";
	public static final String CONTEXT_OBJECT = "object";
	public static final String CONTEXT_ITEM = "item";
	public static final String CONTEXT_TASK = "task";
	public static final String CONTEXT_RESOURCE = "resource";
	
	public static final String PARAM_OID = "oid";
	public static final String PARAM_NAME = "name";
	public static final String PARAM_TYPE = "type";
	public static final String PARAM_OPTIONS = "options";
	public static final String PARAM_TASK = "task";
	public static final String PARAM_OBJECT = "object";
	public static final String PARAM_QUERY = "query";
	
	public static final String RETURN_COUNT = "count";
	public static final String RETURN_BACKGROUND_TASK_OID = "backgroundTaskOid";

	private static long TOKEN_COUNT = 1000000000000000000L;
	private String operation;
	private OperationResultStatus status;
	private Map<String, Serializable> params;
	private Map<String, Serializable> context;
	private Map<String, Serializable> returns;
	private long token;
	private String messageCode;
	private String message;
	private String localizationMessage;
	private List<Serializable> localizationArguments;
	private Throwable cause;
	private int count = 1;
	private int hiddenRecordsCount;
	private List<OperationResult> subresults;
	private List<String> details;
	private boolean summarizeErrors;
	private boolean summarizePartialErrors;
	private boolean summarizeSuccesses;
	private boolean minor = false;
	
	/**
	 * Reference to an asynchronous operation that can be used to retrieve
	 * the status of the running operation. This may be a task identifier,
	 * identifier of a ticket in ITSM system or anything else. The exact
	 * format of this reference depends on the operation which is being
	 * executed.
	 */
	private String asynchronousOperationReference;
	
	private static final Trace LOGGER = TraceManager.getTrace(OperationResult.class);

	public OperationResult(String operation) {
		this(operation, null, OperationResultStatus.UNKNOWN, 0, null, null, null, null, null);
	}

	public OperationResult(String operation, String messageCode, String message) {
		this(operation, null, OperationResultStatus.SUCCESS, 0, messageCode, message, null, null, null);
	}

	public OperationResult(String operation, long token, String messageCode, String message) {
		this(operation, null, OperationResultStatus.SUCCESS, token, messageCode, message, null, null, null);
	}

	public OperationResult(String operation, OperationResultStatus status, String message) {
		this(operation, null, status, 0, null, message, null, null, null);
	}

	public OperationResult(String operation, OperationResultStatus status, String messageCode, String message) {
		this(operation, null, status, 0, messageCode, message, null, null, null);
	}

	public OperationResult(String operation, OperationResultStatus status, long token, String messageCode,
			String message) {
		this(operation, null, status, token, messageCode, message, null, null, null);
	}

	public OperationResult(String operation, OperationResultStatus status, long token, String messageCode,
			String message, Throwable cause) {
		this(operation, null, status, token, messageCode, message, null, cause, null);
	}

	public OperationResult(String operation, Map<String, Serializable> params, OperationResultStatus status,
			long token, String messageCode, String message) {
		this(operation, params, status, token, messageCode, message, null, null, null);
	}

	public OperationResult(String operation, Map<String, Serializable> params, OperationResultStatus status,
			long token, String messageCode, String message, List<OperationResult> subresults) {
		this(operation, params, status, token, messageCode, message, null, null, subresults);
	}

	public OperationResult(String operation, Map<String, Serializable> params, OperationResultStatus status,
			long token, String messageCode, String message, String localizationMessage, Throwable cause,
			List<OperationResult> subresults) {
		this(operation, params, status, token, messageCode, message, localizationMessage, null, cause,
				subresults);
	}
	
	public OperationResult(String operation, Map<String, Serializable> params, OperationResultStatus status,
			long token, String messageCode, String message, String localizationMessage,
			List<Serializable> localizationArguments, Throwable cause, List<OperationResult> subresults) {
		this(operation, params, null, null, status, token, messageCode, message, localizationMessage, null, cause,
				subresults);
	}

	public OperationResult(String operation, Map<String, Serializable> params, Map<String, Serializable> context, Map<String, Serializable> returns, OperationResultStatus status,
			long token, String messageCode, String message, String localizationMessage,
			List<Serializable> localizationArguments, Throwable cause, List<OperationResult> subresults) {
		if (StringUtils.isEmpty(operation)) {
			throw new IllegalArgumentException("Operation argument must not be null or empty.");
		}
		if (status == null) {
			throw new IllegalArgumentException("Operation status must not be null.");
		}
		this.operation = operation;
		this.params = params;
		this.context = context;
		this.returns = returns;
		this.status = status;
		this.token = token;
		this.messageCode = messageCode;
		this.message = message;
		this.localizationMessage = localizationMessage;
		this.localizationArguments = localizationArguments;
		this.cause = cause;
		this.subresults = subresults;
		this.details = new ArrayList<>();
	}

	public OperationResult createSubresult(String operation) {
		OperationResult subresult = new OperationResult(operation);
		addSubresult(subresult);
		return subresult;
	}
	
	public OperationResult createMinorSubresult(String operation) {
		OperationResult subresult = createSubresult(operation);
		subresult.minor = true;
		return subresult;
	}
	
	/**
	 * Reference to an asynchronous operation that can be used to retrieve
	 * the status of the running operation. This may be a task identifier,
	 * identifier of a ticket in ITSM system or anything else. The exact
	 * format of this reference depends on the operation which is being
	 * executed.
	 */
	public String getAsynchronousOperationReference() {
		return asynchronousOperationReference;
	}

	public void setAsynchronousOperationReference(String asyncronousOperationReference) {
		this.asynchronousOperationReference = asyncronousOperationReference;
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
	
	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
	
	public void incrementCount() {
		this.count++;
	}

	public int getHiddenRecordsCount() {
		return hiddenRecordsCount;
	}

	public void setHiddenRecordsCount(int hiddenRecordsCount) {
		this.hiddenRecordsCount = hiddenRecordsCount;
	}

	public boolean representsHiddenRecords() {
		return this.hiddenRecordsCount > 0;
	}

	public boolean isSummarizeErrors() {
		return summarizeErrors;
	}

	public void setSummarizeErrors(boolean summarizeErrors) {
		this.summarizeErrors = summarizeErrors;
	}

	public boolean isSummarizePartialErrors() {
		return summarizePartialErrors;
	}

	public void setSummarizePartialErrors(boolean summarizePartialErrors) {
		this.summarizePartialErrors = summarizePartialErrors;
	}

	public boolean isSummarizeSuccesses() {
		return summarizeSuccesses;
	}

	public void setSummarizeSuccesses(boolean summarizeSuccesses) {
		this.summarizeSuccesses = summarizeSuccesses;
	}
	
	public boolean isEmpty() {
		return (status == null || status == OperationResultStatus.UNKNOWN) &&
				(subresults == null || subresults.isEmpty());
	}


	/**
	 * Method returns list of operation subresults @{link
	 * {@link OperationResult}.
	 * 
	 * @return never returns null
	 */
	public List<OperationResult> getSubresults() {
		if (subresults == null) {
			subresults = new ArrayList<>();
		}
		return subresults;
	}

    /**
     * @return last subresult, or null if there are no subresults.
     */
    public OperationResult getLastSubresult() {
        if (subresults == null || subresults.isEmpty()) {
            return null;
        } else {
            return subresults.get(subresults.size()-1);
        }
    }

    public void removeLastSubresult() {
        if (subresults != null && !subresults.isEmpty()) {
            subresults.remove(subresults.size()-1);
        }
    }

    /**
     * @return last subresult status, or null if there are no subresults.
     */
    public OperationResultStatus getLastSubresultStatus() {
        OperationResult last = getLastSubresult();
        return last != null ? last.getStatus() : null;
    }

	public void addSubresult(OperationResult subresult) {
		getSubresults().add(subresult);
	}
	
	public OperationResult findSubresult(String operation) {
		if (subresults == null) {
			return null;
		}
		for (OperationResult subResult: getSubresults()) {
			if (operation.equals(subResult.getOperation())) {
				return subResult;
			}
		}
		return null;
	}
	
	public List<OperationResult> findSubresults(String operation) {
		List<OperationResult> found = new ArrayList<>();
		if (subresults == null) {
			return found;
		}
		for (OperationResult subResult: getSubresults()) {
			if (operation.equals(subResult.getOperation())) {
				found.add(subResult);
			}
		}
		return found;
	}

	/**
	 * Contains operation status as defined in {@link OperationResultStatus}
	 * 
	 * @return never returns null
	 */
	public OperationResultStatus getStatus() {
		return status;
	}

	public void setStatus(OperationResultStatus status) {
		this.status = status;
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

	public boolean isWarning() {
		return status == OperationResultStatus.WARNING;
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

    public boolean isInProgress() {
        return (status == OperationResultStatus.IN_PROGRESS);
    }
	
	public boolean isError() {
		return (status == OperationResultStatus.FATAL_ERROR) ||
					(status == OperationResultStatus.PARTIAL_ERROR);
	}

	public boolean isFatalError(){
		return (status == OperationResultStatus.FATAL_ERROR);
	}
	
	public boolean isPartialError() {
		return (status == OperationResultStatus.PARTIAL_ERROR);
	}
	
	public boolean isHandledError() {
		return (status == OperationResultStatus.HANDLED_ERROR);
	}
	
	public boolean isNotApplicable() {
		return (status == OperationResultStatus.NOT_APPLICABLE);
	}

	/**
	 * Set all error status in this result and all subresults as handled.
	 */
	public void setErrorsHandled() {
		if (isError()) {
			setStatus(OperationResultStatus.HANDLED_ERROR);
		}
		for(OperationResult subresult: getSubresults()) {
			subresult.setErrorsHandled();
		}
	}


	/**
	 * Computes operation result status based on subtask status and sets an
	 * error message if the status is FATAL_ERROR.
	 * 
	 * @param errorMessage
	 *            error message
	 */
	public void computeStatus(String errorMessage) {
		computeStatus(errorMessage, errorMessage);
	}

	public void computeStatus(String errorMessage, String warnMessage) {
		Validate.notEmpty(errorMessage, "Error message must not be null.");

		// computeStatus sets a message if none is set,
		// therefore we need to check before calling computeStatus
		boolean noMessage = StringUtils.isEmpty(message);
		computeStatus();
		
		switch (status) {
			case FATAL_ERROR:
			case PARTIAL_ERROR:
				if (noMessage) {
					message = errorMessage;
				}
				break;
			case UNKNOWN:
			case WARNING:
			case NOT_APPLICABLE:
				if (noMessage) {
					if (StringUtils.isNotEmpty(warnMessage)) {
						message = warnMessage;
					} else {
						message = errorMessage;
					}
				}
				break;
		}
	}

	/**
	 * Computes operation result status based on subtask status.
	 */
	public void computeStatus() {
		if (getSubresults().isEmpty()) {
			if (status == OperationResultStatus.UNKNOWN) {
				status = OperationResultStatus.SUCCESS;
			}
			return;
		}
        if (status == OperationResultStatus.FATAL_ERROR) {
            return;
        }
		OperationResultStatus newStatus = OperationResultStatus.UNKNOWN;
		boolean allSuccess = true;
		boolean allNotApplicable = true;
		String newMessage = null;
		for (OperationResult sub : getSubresults()) {
			if (sub.getStatus() != OperationResultStatus.NOT_APPLICABLE) {
				allNotApplicable = false;
			}
			if (sub.getStatus() == OperationResultStatus.FATAL_ERROR) {
				status = OperationResultStatus.FATAL_ERROR;
				if (message == null) {
					message = sub.getMessage();
				} else {
					message = message + ": " + sub.getMessage();
				}
				return;
			}
			if (sub.getStatus() == OperationResultStatus.IN_PROGRESS) {
				status = OperationResultStatus.IN_PROGRESS;
				if (message == null) {
					message = sub.getMessage();
				} else {
					message = message + ": " + sub.getMessage();
				}
				if (asynchronousOperationReference == null) {
					asynchronousOperationReference = sub.getAsynchronousOperationReference();
				}
				return;
			}
			if (sub.getStatus() == OperationResultStatus.PARTIAL_ERROR) {
				newStatus = OperationResultStatus.PARTIAL_ERROR;
				newMessage = sub.getMessage();
			}
			if (newStatus != OperationResultStatus.PARTIAL_ERROR){
			if (sub.getStatus() == OperationResultStatus.HANDLED_ERROR) {
				newStatus = OperationResultStatus.HANDLED_ERROR;
				newMessage = sub.getMessage();
			}
			}
			if (sub.getStatus() != OperationResultStatus.SUCCESS
					&& sub.getStatus() != OperationResultStatus.NOT_APPLICABLE) {
				allSuccess = false;
			}
			if (newStatus != OperationResultStatus.HANDLED_ERROR) {
				if (sub.getStatus() == OperationResultStatus.WARNING) {
					newStatus = OperationResultStatus.WARNING;
					newMessage = sub.getMessage();
				}
			}
		}
		
		if (allNotApplicable && !getSubresults().isEmpty()) {
			status = OperationResultStatus.NOT_APPLICABLE;
		}
		if (allSuccess && !getSubresults().isEmpty()) {
			status = OperationResultStatus.SUCCESS;
		} else {
			status = newStatus;
			if (message == null) {
				message = newMessage;
			} else {
				message = message + ": " + newMessage;
			}
		}
	}

	/**
	 * Used when the result contains several composite sub-result that are of equivalent meaning.
	 * If all of them fail the result will be fatal error as well. If only some of them fail the
	 * result will be partial error. Handled error is considered a success.
	 */
	public void computeStatusComposite() {
		if (getSubresults().isEmpty()) {
			if (status == OperationResultStatus.UNKNOWN) {
				status = OperationResultStatus.NOT_APPLICABLE;
			}
			return;
		}

		boolean allFatalError = true;
		boolean allNotApplicable = true;
		boolean hasInProgress = false;
		boolean hasHandledError = false;
        boolean hasError = false;
        boolean hasWarning = false;
		for (OperationResult sub : getSubresults()) {
			if (sub.getStatus() != OperationResultStatus.NOT_APPLICABLE) {
				allNotApplicable = false;
			}
            if (sub.getStatus() != OperationResultStatus.FATAL_ERROR) {
                allFatalError = false;
            }
			if (sub.getStatus() == OperationResultStatus.FATAL_ERROR) {
                hasError = true;
				if (message == null) {
					message = sub.getMessage();
				} else {
					message = message + ", " + sub.getMessage();
				}
			}
            if (sub.getStatus() == OperationResultStatus.PARTIAL_ERROR) {
                hasError = true;
                if (message == null) {
                    message = sub.getMessage();
                } else {
                    message = message + ", " + sub.getMessage();
                }
            }
            if (sub.getStatus() == OperationResultStatus.HANDLED_ERROR) {
                hasHandledError = true;
                if (message == null) {
                    message = sub.getMessage();
                } else {
                    message = message + ", " + sub.getMessage();
                }
            }
			if (sub.getStatus() == OperationResultStatus.IN_PROGRESS) {
				hasInProgress = true;
				if (message == null) {
					message = sub.getMessage();
				} else {
					message = message + ", " + sub.getMessage();
				}
				if (asynchronousOperationReference == null) {
					asynchronousOperationReference = sub.getAsynchronousOperationReference();
				}
			}
            if (sub.getStatus() == OperationResultStatus.WARNING) {
                hasWarning = true;
                if (message == null) {
                    message = sub.getMessage();
                } else {
                    message = message + ", " + sub.getMessage();
                }
            }
		}
		
		if (allNotApplicable) {
			status = OperationResultStatus.NOT_APPLICABLE;
		} else if (allFatalError) {
            status = OperationResultStatus.FATAL_ERROR;
        } else if (hasInProgress) {
            status = OperationResultStatus.IN_PROGRESS;
        } else if (hasError) {
            status = OperationResultStatus.PARTIAL_ERROR;
        } else if (hasWarning) {
            status = OperationResultStatus.WARNING;
        } else if (hasHandledError) {
            status = OperationResultStatus.HANDLED_ERROR;
        } else {
            status = OperationResultStatus.SUCCESS;
        }
	}
	
	public OperationResultStatus getComputeStatus() {
		OperationResultStatus origStatus = status;
		String origMessage = message;
		computeStatus();
		OperationResultStatus computedStatus = status;
		status = origStatus;
		message = origMessage;
		return computedStatus;
	}

    public void computeStatusIfUnknown() {
        if (isUnknown()) {
            computeStatus();
        }
    }

    public void recomputeStatus() {
		// Only recompute if there are subresults, otherwise keep original
		// status
		if (subresults != null && !subresults.isEmpty()) {
			computeStatus();
		}
	}
	
	public void recomputeStatus(String message) {
		// Only recompute if there are subresults, otherwise keep original
		// status
		if (subresults != null && !subresults.isEmpty()) {
			computeStatus(message);
		}
	}

	public void recomputeStatus(String errorMessage, String warningMessage) {
		// Only recompute if there are subresults, otherwise keep original
		// status
		if (subresults != null && !subresults.isEmpty()) {
			computeStatus(errorMessage, warningMessage);
		}
	}

	public void recordSuccessIfUnknown() {
		if (isUnknown()) {
			recordSuccess();
		}
	}
	
	public void recordNotApplicableIfUnknown() {
		if (isUnknown()) {
			status = OperationResultStatus.NOT_APPLICABLE;
		}
	}

	/**
	 * Method returns {@link Map} with operation parameters. Parameters keys are
	 * described in module interface for every operation.
	 * 
	 * @return never returns null
	 */
	public Map<String, Serializable> getParams() {
		if (params == null) {
			params = new HashMap<>();
		}
		return params;
	}

	public void addParam(String paramName, Serializable paramValue) {
		getParams().put(paramName, paramValue);
	}

    public void addArbitraryObjectAsParam(String paramName, Object paramValue) {
        addParam(paramName, String.valueOf(paramValue));
    }

    // Copies a collection to a OperationResult's param field. Primarily used to overcome the fact that Collection is not Serializable
    public void addCollectionOfSerializablesAsParam(String paramName, Collection<? extends Serializable> paramValue) {
        addParam(paramName, paramValue != null ? new ArrayList<>(paramValue) : null);
    }

    public void addCollectionOfSerializablesAsReturn(String name, Collection<? extends Serializable> value) {
        addReturn(name, value != null ? new ArrayList<>(value) : null);
    }

    public void addArbitraryCollectionAsParam(String paramName, Collection values) {
        if (values != null) {
            ArrayList<String> valuesAsStrings = new ArrayList<>();
            for (Object value : values) {
                valuesAsStrings.add(String.valueOf(value));
            }
            addParam(paramName, valuesAsStrings);
        } else {
            addParam(paramName, null);
        }
    }


    public void addParams(String[] names, Serializable... objects) {
		if (names.length != objects.length) {
			throw new IllegalArgumentException("Bad result parameters size, names '" + names.length
					+ "', objects '" + objects.length + "'.");
		}

		for (int i = 0; i < names.length; i++) {
			addParam(names[i], objects[i]);
		}
	}

	public Map<String, Serializable> getContext() {
		if (context == null) {
			context = new HashMap<>();
		}
		return context;
	}

	@SuppressWarnings("unchecked")
	public <T> T getContext(Class<T> type, String contextName) {
		return (T) getContext().get(contextName);
	}

	public void addContext(String contextName, Serializable value) {
		getContext().put(contextName, value);
	}

	public Map<String, Serializable> getReturns() {
		if (returns == null) {
			returns = new HashMap<>();
		}
		return returns;
	}

	public void addReturn(String returnName, Serializable value) {
		getReturns().put(returnName, value);
	}

	public Serializable getReturn(String returnName) {
		return getReturns().get(returnName);
	}

	/**
	 * @return Contains random long number, for better searching in logs.
	 */
	public long getToken() {
		if (token == 0) {
			token = TOKEN_COUNT++;
		}
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

	public void setMessage(String message) {
		this.message = message;
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
	public List<Serializable> getLocalizationArguments() {
		return localizationArguments;
	}

	/**
	 * @return Method returns operation result exception. Not required, can be
	 *         null.
	 */
	public Throwable getCause() {
		return cause;
	}

	public void recordSuccess() {
		// Success, no message or other explanation is needed.
		status = OperationResultStatus.SUCCESS;
	}

    public void recordInProgress() {
        status = OperationResultStatus.IN_PROGRESS;
    }

    public void recordUnknown() {
        status = OperationResultStatus.UNKNOWN;
    }

    public void recordFatalError(Throwable cause) {
		recordStatus(OperationResultStatus.FATAL_ERROR, cause.getMessage(), cause);
	}
	
	/**
	 * If the operation is an error then it will switch the status to EXPECTED_ERROR.
	 * This is used if the error is expected and properly handled.
	 */
	public void muteError() {
		if (isError()) {
			status = OperationResultStatus.HANDLED_ERROR;
		}
	}
	
	public void muteLastSubresultError() {
		OperationResult lastSubresult = getLastSubresult();
		if (lastSubresult != null) {
			lastSubresult.muteError();
		}
	}

	public void recordPartialError(Throwable cause) {
		recordStatus(OperationResultStatus.PARTIAL_ERROR, cause.getMessage(), cause);
	}

	public void recordWarning(Throwable cause) {
		recordStatus(OperationResultStatus.WARNING, cause.getMessage(), cause);
	}

	public void recordStatus(OperationResultStatus status, Throwable cause) {
		this.status = status;
		this.cause = cause;
		// No other message was given, so use message from the exception
		// not really correct, but better than nothing.
		message = cause.getMessage();
	}

	public void recordFatalError(String message, Throwable cause) {
		recordStatus(OperationResultStatus.FATAL_ERROR, message, cause);
	}

	public void recordPartialError(String message, Throwable cause) {
		recordStatus(OperationResultStatus.PARTIAL_ERROR, message, cause);
	}

	public void recordWarning(String message, Throwable cause) {
		recordStatus(OperationResultStatus.WARNING, message, cause);
	}
	
	public void recordHandledError(String message) {
		recordStatus(OperationResultStatus.HANDLED_ERROR, message);
	}
	
	public void recordHandledError(String message, Throwable cause) {
		recordStatus(OperationResultStatus.HANDLED_ERROR, message, cause);
	}
	
	public void recordHandledError(Throwable cause) {
		recordStatus(OperationResultStatus.HANDLED_ERROR, cause.getMessage(), cause);
	}

	public void recordStatus(OperationResultStatus status, String message, Throwable cause) {
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

	public void recordWarning(String message) {
		recordStatus(OperationResultStatus.WARNING, message);
	}

	/**
	 * Records result from a common exception type. This automatically
	 * determines status and also sets appropriate message.
	 * 
	 * @param exception
	 *            common exception
	 */
	public void record(CommonException exception) {
		// TODO: switch to a localized message later
		// Exception is a fatal error in this context
		recordFatalError(exception.getOperationResultMessage(), exception);
	}

	public void recordStatus(OperationResultStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	/**
	 * Returns true if result status is UNKNOWN or any of the subresult status
	 * is unknown (recursive).
	 * 
	 * May come handy in tests to check if all the operations fill out the
	 * status as they should.
	 */
	public boolean hasUnknownStatus() {
		if (status == OperationResultStatus.UNKNOWN) {
			return true;
		}
		for (OperationResult subresult : getSubresults()) {
			if (subresult.hasUnknownStatus()) {
				return true;
			}
		}
		return false;
	}

	public void appendDetail(String detailLine) {
		// May be switched to a more structured method later
		details.add(detailLine);
	}

	public List<String> getDetail() {
		return details;
	}

	@Override
	public String toString() {
		return "R(" + operation + " " + status + " " + message + ")";
	}



	public static OperationResult createOperationResult(OperationResultType result) {
		if (result == null) {
            return null;
        }

		Map<String, Serializable> params = ParamsTypeUtil.fromParamsType(result.getParams());
		Map<String, Serializable> context = ParamsTypeUtil.fromParamsType(result.getContext());
		Map<String, Serializable> returns = ParamsTypeUtil.fromParamsType(result.getReturns());

		List<OperationResult> subresults = null;
		if (!result.getPartialResults().isEmpty()) {
			subresults = new ArrayList<>();
			for (OperationResultType subResult : result.getPartialResults()) {
				subresults.add(createOperationResult(subResult));
			}
		}

		LocalizedMessageType message = result.getLocalizedMessage();
		String localizedMessage = message == null ? null : message.getKey();
		List<Serializable> localizedArguments = message == null ? null : (List<Serializable>) (List) message.getArgument();         // FIXME: brutal hack

		OperationResult opResult = new OperationResult(result.getOperation(), params, context, returns, 
				OperationResultStatus.parseStatusType(result.getStatus()), result.getToken(),
				result.getMessageCode(), result.getMessage(), localizedMessage, localizedArguments, null,
				subresults);
		if (result.getCount() != null) {
			opResult.setCount(result.getCount());
		}
		if (result.getHiddenRecordsCount() != null) {
			opResult.setHiddenRecordsCount(result.getHiddenRecordsCount());
		}
		return opResult;
	}

	public OperationResultType createOperationResultType() {
		return createOperationResultType(this);
	}

	private OperationResultType createOperationResultType(OperationResult opResult) {
		OperationResultType result = new OperationResultType();
		result.setToken(opResult.getToken());
		result.setStatus(OperationResultStatus.createStatusType(opResult.getStatus()));
		if (opResult.getCount() != 1) {
			result.setCount(opResult.getCount());
		}
		if (opResult.getHiddenRecordsCount() != 0) {
			result.setHiddenRecordsCount(opResult.getHiddenRecordsCount());
		}
		result.setOperation(opResult.getOperation());
		result.setMessage(opResult.getMessage());
		result.setMessageCode(opResult.getMessageCode());

		if (opResult.getCause() != null || !opResult.details.isEmpty()) {
			StringBuilder detailsb = new StringBuilder();

			// Record text messages in details (if present)
			if (!opResult.details.isEmpty()) {
				for (String line : opResult.details) {
					detailsb.append(line);
					detailsb.append("\n");
				}
			}

			// Record stack trace in details if a cause is present
			if (opResult.getCause() != null) {
				Throwable ex = opResult.getCause();
				detailsb.append(ex.getClass().getName());
				detailsb.append(": ");
				detailsb.append(ex.getMessage());
				detailsb.append("\n");
				StackTraceElement[] stackTrace = ex.getStackTrace();
				for (StackTraceElement aStackTrace : stackTrace) {
					detailsb.append(aStackTrace.toString());
					detailsb.append("\n");
				}
			}

			result.setDetails(detailsb.toString());
		}

		if (StringUtils.isNotEmpty(opResult.getLocalizationMessage())) {
			LocalizedMessageType message = new LocalizedMessageType();
			message.setKey(opResult.getLocalizationMessage());
			if (opResult.getLocalizationArguments() != null) {
				message.getArgument().addAll(opResult.getLocalizationArguments());
			}
			result.setLocalizedMessage(message);
		}

		result.setParams(ParamsTypeUtil.toParamsType(opResult.getParams()));
		result.setContext(ParamsTypeUtil.toParamsType(opResult.getContext()));
		result.setReturns(ParamsTypeUtil.toParamsType(opResult.getReturns()));

		for (OperationResult subResult : opResult.getSubresults()) {
			result.getPartialResults().add(opResult.createOperationResultType(subResult));
		}

		return result;
	}

	public void summarize() {
		summarize(false);
	}

	public void summarize(boolean alsoSubresults) {

		// first phase: summarizing records if explicitly requested
		Iterator<OperationResult> iterator = getSubresults().iterator();
		while (iterator.hasNext()) {
			OperationResult subresult = iterator.next();
			if (subresult.getCount() > 1) {
				// Already summarized
				continue;
			}
			if (subresult.isError() && summarizeErrors) {
				// go on
			} else if (subresult.isPartialError() && summarizePartialErrors) {
				// go on
			} else if (subresult.isSuccess() && summarizeSuccesses) {
				// go on
			} else {
				continue;
			}
			OperationResult similar = findSimilarSubresult(subresult);
			if (similar == null) {
				// Nothing to summarize to
				continue;
			}
			merge(similar, subresult);
			iterator.remove();
		}

		// second phase: summarizing (better said, eliminating or hiding) subresults if there are too many of them
		// (we strip subresults that have same operation name and status, if there are more of them than given threshold)
		//
		// We implement quite a complex algorithm to ensure "incremental stripping", i.e. calling summarize() repeatedly
		// on an OperationResult to which new standard entries are continually added. The requirement is that there must
		// be at most one summarization record, and it must be placed after all standard records of given type.
		Map<OperationStatusKey, OperationStatusCounter> recordsCounters = new HashMap<>();
		iterator = getSubresults().iterator();
		while (iterator.hasNext()) {
			OperationResult sr = iterator.next();
			OperationStatusKey key = new OperationStatusKey(sr.getOperation(), sr.getStatus());
			if (recordsCounters.containsKey(key)) {
				OperationStatusCounter counter = recordsCounters.get(key);
				if (!sr.representsHiddenRecords()) {
					if (counter.shownRecords < SUBRESULT_STRIP_THRESHOLD) {
						counter.shownRecords++;
						counter.shownCount += sr.count;
					} else {
						counter.hiddenCount += sr.count;
						iterator.remove();
					}
				} else {
					counter.hiddenCount += sr.hiddenRecordsCount;
					iterator.remove();		// will be re-added at the end (potentially with records counters)
				}
			} else {
				OperationStatusCounter counter = new OperationStatusCounter();
				if (!sr.representsHiddenRecords()) {
					counter.shownRecords = 1;
					counter.shownCount = sr.count;
				} else {
					counter.hiddenCount = sr.hiddenRecordsCount;
					iterator.remove();		// will be re-added at the end (potentially with records counters)
				}
				recordsCounters.put(key, counter);
			}
		}
		for (Map.Entry<OperationStatusKey, OperationStatusCounter> repeatingEntry : recordsCounters.entrySet()) {
			int shownCount = repeatingEntry.getValue().shownCount;
			int hiddenCount = repeatingEntry.getValue().hiddenCount;
			if (hiddenCount > 0) {
				OperationStatusKey key = repeatingEntry.getKey();
				OperationResult hiddenRecordsEntry = new OperationResult(key.operation, key.status,
						hiddenCount + " record(s) were hidden to save space. Total number of records: " + (shownCount + hiddenCount));
				hiddenRecordsEntry.setHiddenRecordsCount(hiddenCount);
				addSubresult(hiddenRecordsEntry);
			}
		}

		// And now, summarize each of the subresults
		if (alsoSubresults) {
			iterator = getSubresults().iterator();
			while (iterator.hasNext()) {
				iterator.next().summarize(true);
			}
		}
	}

	private void merge(OperationResult target, OperationResult source) {
		mergeMap(target.getParams(), source.getParams());
		mergeMap(target.getContext(), source.getContext());
		mergeMap(target.getReturns(), source.getReturns());
		target.incrementCount();
	}

	private void mergeMap(Map<String, Serializable> targetMap, Map<String, Serializable> sourceMap) {
		for (Entry<String, Serializable> targetEntry: targetMap.entrySet()) {
			String targetKey = targetEntry.getKey();
			Serializable targetValue = targetEntry.getValue();
			if (targetValue instanceof VariousValues) {
				continue;
			}
			Serializable sourceValue = sourceMap.get(targetKey);
			if (MiscUtil.equals(targetValue, sourceValue)) {
				// Entries match, nothing to do
				continue;
			}
			// Entries do not match. The target entry needs to be marked as VariousValues
			targetEntry.setValue(new VariousValues());
		}
		for (Entry<String, Serializable> sourceEntry: sourceMap.entrySet()) {
			String sourceKey = sourceEntry.getKey();
			if (!targetMap.containsKey(sourceKey)) {
				targetMap.put(sourceKey, new VariousValues());
			}
		}
	}

	private OperationResult findSimilarSubresult(OperationResult subresult) {
		OperationResult similar = null;
		for (OperationResult sub: getSubresults()) {
			if (sub == subresult) {
				continue;
			}
			if (!sub.operation.equals(subresult.operation)) {
				continue;
			}
			if (sub.status != subresult.status) {
				continue;
			}
			if (!MiscUtil.equals(sub.message, subresult.message)) {
				continue;
			}
			if (similar == null || (similar.count < sub.count)) {
				similar = sub;
			}
		}
		return similar;
	}

	/**
	 * Removes all the successful minor results. Also checks if the result is roughly consistent
	 * and complete. (e.g. does not have unknown operation status, etc.)
	 */
	public void cleanupResult() {
		cleanupResult(null);
	}
	
	/**
	 * Removes all the successful minor results. Also checks if the result is roughly consistent
	 * and complete. (e.g. does not have unknown operation status, etc.)
	 * 
	 * The argument "e" is for easier use of the cleanup in the exceptions handlers. The original exception is passed
	 * to the IAE that this method produces for easier debugging.
	 */
	public void cleanupResult(Throwable e) {
		if (status == OperationResultStatus.UNKNOWN) {
			LOGGER.error("Attempt to cleanup result of operation " + operation + " that is still UNKNOWN:\n{}", this.debugDump());
			throw new IllegalStateException("Attempt to cleanup result of operation "+operation+" that is still UNKNOWN");
		}
		if (subresults == null) {
			return;
		}
		Iterator<OperationResult> iterator = subresults.iterator();
		while (iterator.hasNext()) {
			OperationResult subresult = iterator.next();
			if (subresult.getStatus() == OperationResultStatus.UNKNOWN) {
				String message = "Subresult "+subresult.getOperation()+" of operation "+operation+" is still UNKNOWN during cleanup";
				LOGGER.error("{}:\n{}", message, this.debugDump(), e);
				if (e == null) {
					throw new IllegalStateException(message);
				} else {
					throw new IllegalStateException(message+"; during handling of exception "+e, e);
				}
			}
			if (subresult.canCleanup()) {
				iterator.remove();
			}
		}
	}

	private boolean canCleanup() {
		if (!minor) {
			return false;
		}
		return status == OperationResultStatus.SUCCESS || status == OperationResultStatus.NOT_APPLICABLE;
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		dumpIndent(sb, indent, true);
		return sb.toString();
	}
	
	public String dump(boolean withStack) {
		StringBuilder sb = new StringBuilder();
		dumpIndent(sb, 0, withStack);
		return sb.toString();
	}

	private void dumpIndent(StringBuilder sb, int indent, boolean printStackTrace) {
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("*op* ");
		sb.append(operation);
		sb.append(", st: ");
		sb.append(status);
		if (minor) {
			sb.append(", MINOR");
		}
		sb.append(", msg: ");
		sb.append(message);
		if (count > 1) {
			sb.append(" x");
			sb.append(count);
		}
		if (asynchronousOperationReference != null) {
			sb.append("\n");
			DebugUtil.debugDumpWithLabel(sb, "asyncronousOperationReference", asynchronousOperationReference, indent + 2);
		}
		sb.append("\n");

		for (Map.Entry<String, Serializable> entry : getParams().entrySet()) {
			DebugUtil.indentDebugDump(sb, indent + 2);
			sb.append("[p]");
			sb.append(entry.getKey());
			sb.append("=");
			sb.append(dumpEntry(indent+2, entry.getValue()));
			sb.append("\n");
		}

		for (Map.Entry<String, Serializable> entry : getContext().entrySet()) {
			DebugUtil.indentDebugDump(sb, indent + 2);
			sb.append("[c]");
			sb.append(entry.getKey());
			sb.append("=");
			sb.append(dumpEntry(indent+2, entry.getValue()));
			sb.append("\n");
		}
		
		for (Map.Entry<String, Serializable> entry : getReturns().entrySet()) {
			DebugUtil.indentDebugDump(sb, indent + 2);
			sb.append("[r]");
			sb.append(entry.getKey());
			sb.append("=");
			sb.append(dumpEntry(indent+2, entry.getValue()));
			sb.append("\n");
		}

		for (String line : details) {
			DebugUtil.indentDebugDump(sb, indent + 2);
			sb.append("[d]");
			sb.append(line);
			sb.append("\n");
		}
		
		if (cause != null) {
			DebugUtil.indentDebugDump(sb, indent + 2);
			sb.append("[cause]");
			sb.append(cause.getClass().getSimpleName());
			sb.append(":");
			sb.append(cause.getMessage());
			sb.append("\n");
			if (printStackTrace) {
				dumpStackTrace(sb, cause.getStackTrace(), indent + 4);
				dumpInnerCauses(sb, cause.getCause(), indent + 3);
			}
		}

		for (OperationResult sub : getSubresults()) {
			sub.dumpIndent(sb, indent + 1, printStackTrace);
		}
	}

	private String dumpEntry(int indent, Serializable value) {
		if (value instanceof Element) {
			Element element = (Element)value;
			if (SchemaConstants.C_VALUE.equals(DOMUtil.getQName(element))) {
				try {
					String cvalue = SchemaDebugUtil.prettyPrint(XmlTypeConverter.toJavaValue(element));
					return DebugUtil.fixIndentInMultiline(indent, INDENT_STRING, cvalue);
				} catch (Exception e) {
					return DebugUtil.fixIndentInMultiline(indent, INDENT_STRING, "value: " + element.getTextContent());
				}
			}
		}
		return DebugUtil.fixIndentInMultiline(indent, INDENT_STRING, SchemaDebugUtil.prettyPrint(value));
	}

	private void dumpInnerCauses(StringBuilder sb, Throwable innerCause, int indent) {
		if (innerCause == null) {
			return;
		}
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("Caused by ");
		sb.append(innerCause.getClass().getName());
		sb.append(": ");
		sb.append(innerCause.getMessage());
		sb.append("\n");
		dumpStackTrace(sb, innerCause.getStackTrace(), indent + 1);
		dumpInnerCauses(sb, innerCause.getCause(), indent);
	}

	private static void dumpStackTrace(StringBuilder sb, StackTraceElement[] stackTrace, int indent) {
		for (StackTraceElement aStackTrace : stackTrace) {
			DebugUtil.indentDebugDump(sb, indent);
			StackTraceElement element = aStackTrace;
			sb.append(element.toString());
			sb.append("\n");
		}
	}

	public void setBackgroundTaskOid(String oid) {
		addReturn(RETURN_BACKGROUND_TASK_OID, oid);
	}

	public String getBackgroundTaskOid() {
		Object oid = getReturns().get(RETURN_BACKGROUND_TASK_OID);
		return oid != null ? String.valueOf(oid) : null;
	}

	public void setMinor(boolean value) {
		this.minor = value;
	}

	// primitive implementation - uncomment it if needed
//    public OperationResult clone() {
//        return CloneUtil.clone(this);
//    }

    private static class OperationStatusKey {

        private String operation;
        private OperationResultStatus status;

        private OperationStatusKey(String operation, OperationResultStatus status) {
            this.operation = operation;
            this.status = status;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            OperationStatusKey that = (OperationStatusKey) o;

            if (operation != null ? !operation.equals(that.operation) : that.operation != null) return false;
            if (status != that.status) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = operation != null ? operation.hashCode() : 0;
            result = 31 * result + (status != null ? status.hashCode() : 0);
            return result;
        }
    }

    private static class OperationStatusCounter {
    	private int shownRecords;		// how many actual records will be shown (after this wave of stripping)
		private int shownCount;			// how many entries will be shown (after this wave of stripping)
		private int hiddenCount;		// how many entries will be hidden (after this wave of stripping)
	}

    public OperationResult clone() {
        OperationResult clone = new OperationResult(operation);

        clone.status = status;
        clone.params = CloneUtil.clone(params);
        clone.context = CloneUtil.clone(context);
        clone.returns = CloneUtil.clone(returns);
        clone.token = token;
        clone.messageCode = messageCode;
        clone.message = message;
        clone.localizationMessage = localizationMessage;
        clone.localizationArguments = CloneUtil.clone(localizationArguments);
        clone.cause = CloneUtil.clone(cause);
        clone.count = count;
		clone.hiddenRecordsCount = hiddenRecordsCount;
        if (subresults != null) {
            clone.subresults = new ArrayList<>(subresults.size());
            for (OperationResult subresult : subresults) {
                if (subresult != null) {
                    clone.subresults.add(subresult.clone());
                }
            }
        }
        clone.details = CloneUtil.clone(details);
        clone.summarizeErrors = summarizeErrors;
        clone.summarizePartialErrors = summarizePartialErrors;
        clone.summarizeSuccesses = summarizeSuccesses;
        clone.minor = minor;
        clone.asynchronousOperationReference = asynchronousOperationReference;

        return clone;
    }


}
