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
package com.evolveum.midpoint.model.util;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;

/**
 * @author semancik
 *
 */
public abstract class AbstractSearchIterativeResultHandler<O extends ObjectType> implements ResultHandler<O> {

	private Task task;
	private String taskOperationPrefix;
	private String processShortName;
	private String contextDesc;
	private long progress;
	private long errors;
	private boolean stopOnError;
	private boolean logObjectProgress;
	
	private static final transient Trace LOGGER = TraceManager.getTrace(AbstractSearchIterativeResultHandler.class);
	
	public AbstractSearchIterativeResultHandler(Task task, String taskOperationPrefix, String processShortName,
			String contextDesc) {
		super();
		this.task = task;
		this.taskOperationPrefix = taskOperationPrefix;
		this.processShortName = processShortName;
		this.contextDesc = contextDesc;
		progress = 0;
		errors = 0;
		stopOnError = true;
	}

	protected String getProcessShortName() {
		return processShortName;
	}
	
	protected String getProcessShortNameCapitalized() {
		return StringUtils.capitalize(processShortName);
	}

	public void setProcessShortName(String processShortName) {
		this.processShortName = processShortName;
	}

	public String getContextDesc() {
		if (contextDesc == null) {
			return "";
		}
		return contextDesc;
	}

	public void setContextDesc(String contextDesc) {
		this.contextDesc = contextDesc;
	}

	public Task getTask() {
		return task;
	}

	public String getTaskOperationPrefix() {
		return taskOperationPrefix;
	}

	public boolean isLogObjectProgress() {
		return logObjectProgress;
	}

	public void setLogObjectProgress(boolean logObjectProgress) {
		this.logObjectProgress = logObjectProgress;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.schema.ResultHandler#handle(com.evolveum.midpoint.prism.PrismObject, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public boolean handle(PrismObject<O> object, OperationResult parentResult) {
		if (object.getOid() == null) {
			throw new IllegalArgumentException("Object has null OID");
		}
		
		progress++;

		Long startTime = System.currentTimeMillis();

		OperationResult result = parentResult.createSubresult(taskOperationPrefix + ".handle");
		result.addParam("object", object);
		result.addContext(OperationResult.CONTEXT_PROGRESS, progress);
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("{} starting for {} {}",new Object[] {
					getProcessShortNameCapitalized(), object, getContextDesc()});
		}
		
		boolean cont;
		try {

            task.setProgressImmediate(progress, parentResult);              // this is necessary for the progress to be immediately available in GUI

            // The meat
			cont = handleObject(object, result);
			
			if (logObjectProgress) {
				if (LOGGER.isInfoEnabled()) {
					long endTime = System.currentTimeMillis();
					LOGGER.info("{} object {} {} done ({} ms)",new Object[]{
							getProcessShortNameCapitalized(), object,
							getContextDesc(), endTime - startTime});
				}
			}
			
			// We do not want to override the result set by handler. This is just a fallback case
			if (result.isUnknown()) {
				result.computeStatus();
			}
			
			if (result.isError()) {
				// Alternative way how to indicate an error.
				return processError(object, null, result);
			}
			
		} catch (Exception ex) {
			return processError(object, ex, result);
		} finally {
            // FIXME: hack. Hardcoded ugly summarization of successes. something like
            // AbstractSummarizingResultHandler [lazyman]
            if (result.isSuccess()) {
                result.getSubresults().clear();
            }
            result.summarize();
        }
		
		

		if (!cont) {
			// we assume that the handleObject really knows what he's doing and already set up result accordingly
			return false;
		}
		
		// Check if we haven't been interrupted
		if (task.canRun()) {
			// Everything OK, signal to continue
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("{} finished for {} {}, result:\n{}", new Object[]{
						getProcessShortNameCapitalized(), object, getContextDesc(), result.debugDump()});
			}
			return true;
		} else {
			parentResult.recordPartialError("Interrupted");
			// Signal to stop
			if (LOGGER.isWarnEnabled()) {
				LOGGER.warn("{} {} interrupted",new Object[]{
						getProcessShortNameCapitalized(), getContextDesc()});
			}
			return false;
		}
	}
	
	private boolean processError(PrismObject<O> object, Exception ex, OperationResult result) {
		errors++;
		String message;
		if (ex != null) {
			message = ex.getMessage();
		} else {
			message = result.getMessage();
		}
		if (LOGGER.isErrorEnabled()) {
			LOGGER.error("{} of object {} {} failed: {}", new Object[] {
					getProcessShortNameCapitalized(),
					object, getContextDesc(), message, ex });
		}
		// We do not want to override the result set by handler. This is just a fallback case
		if (result.isUnknown()) {
			result.recordFatalError("Failed to "+getProcessShortName()+": "+ex.getMessage(), ex);
		}
		result.summarize();
		return !isStopOnError();
	}

	public long heartbeat() {
		// If we exist then we run. So just return the progress count.
		return progress;
	}

	public long getProgress() {
		return progress;
	}
	
	public long getErrors() {
		return errors;
	}
	
	public boolean isStopOnError() {
		return stopOnError;
	}
	
	public void setStopOnError(boolean stopOnError) {
		this.stopOnError = stopOnError;
	}

	protected abstract boolean handleObject(PrismObject<O> object, OperationResult result) throws CommonException;


}
