/**
 * Copyright (c) 2013 Evolveum
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
 * Portions Copyrighted 2013 [name of copyright owner]
 */
package com.evolveum.midpoint.model.util;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
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
	
	private static final transient Trace LOGGER = TraceManager.getTrace(AbstractSearchIterativeResultHandler.class);
	
	protected AbstractSearchIterativeResultHandler(Task task, String taskOperationPrefix, String processShortName,
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

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.schema.ResultHandler#handle(com.evolveum.midpoint.prism.PrismObject, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public boolean handle(PrismObject<O> object, OperationResult parentResult) {
		if (object.getOid() == null) {
			throw new IllegalArgumentException("Object has null OID");
		}
		
		progress++;
		
		Long startTime = null;
		if (LOGGER.isTraceEnabled()) {
			startTime = System.currentTimeMillis();
		}
		
		OperationResult result = parentResult.createSubresult(taskOperationPrefix + ".handle");
		result.addParam("object", object);
		result.addContext(OperationResult.CONTEXT_PROGRESS, progress);
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("{} starting for {} {}",new Object[] {
					getProcessShortNameCapitalized(), object, getContextDesc()});
		}
		
		boolean cont;
		try {
			
			// The meat
			cont = handleOject(object, result);
			
			if (LOGGER.isInfoEnabled()) {
				long endTime = System.currentTimeMillis();
				LOGGER.info("{} object {} {} done ({} ms)",new Object[]{
						getProcessShortNameCapitalized(), object,
						getContextDesc(), endTime - startTime});
			}
		} catch (Exception ex) {
			errors++;
			if (LOGGER.isErrorEnabled()) {
				LOGGER.error("{} of object {} {} failed: {}", new Object[] {
						getProcessShortNameCapitalized(),
						object, getContextDesc(), ex.getMessage(), ex });
			}
			result.recordPartialError("Failed to "+getProcessShortName()+": "+ex.getMessage(), ex);
			return !isStopOnError();
		}
		
		// FIXME: hack. Hardcoded ugly summarization of successes
		if (result.isSuccess()) {
			result.getSubresults().clear();
		}

		if (!cont) {
			// we assume that the handleObject really knows what he's doing and already set up result accordingly
			return false;
		}
		
		// Check if we haven't been interrupted
		if (task.canRun()) {
			result.computeStatus();
			// Everything OK, signal to continue
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("{} finished for {} {}, result:\n{}", new Object[]{
						getProcessShortNameCapitalized(), object, getContextDesc(), result.dump()});
			}
			return true;
		} else {
			result.recordPartialError("Interrupted");
			// Signal to stop
			if (LOGGER.isWarnEnabled()) {
				LOGGER.warn("{} {} interrupted",new Object[]{
						getProcessShortNameCapitalized(), getContextDesc()});
			}
			return false;
		}
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

	protected abstract boolean handleOject(PrismObject<O> object, OperationResult result);


}
