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
package com.evolveum.midpoint.model.impl.util;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.LightweightTaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author semancik
 *
 */
public abstract class AbstractSearchIterativeResultHandler<O extends ObjectType> implements ResultHandler<O> {

	public static final int WORKER_THREAD_WAIT_FOR_REQUEST = 500;
	private final TaskManager taskManager;
	private Task coordinatorTask;
	private String taskOperationPrefix;
	private String processShortName;
	private String contextDesc;
	private AtomicInteger objectsProcessed = new AtomicInteger();
	private AtomicLong totalTimeProcessing = new AtomicLong();
	private AtomicInteger errors = new AtomicInteger();
	private boolean stopOnError;
	private boolean logObjectProgress;
	private BlockingQueue<ProcessingRequest> requestQueue;
	private AtomicBoolean stopRequestedByAnyWorker = new AtomicBoolean(false);

	private OperationResult collectedResults;

	private static final transient Trace LOGGER = TraceManager.getTrace(AbstractSearchIterativeResultHandler.class);
	private volatile boolean allItemsSubmitted = false;

	public AbstractSearchIterativeResultHandler(Task coordinatorTask, String taskOperationPrefix, String processShortName,
			String contextDesc, TaskManager taskManager) {
		super();
		this.coordinatorTask = coordinatorTask;
		this.taskOperationPrefix = taskOperationPrefix;
		this.processShortName = processShortName;
		this.contextDesc = contextDesc;
		this.taskManager = taskManager;
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

	public Task getCoordinatorTask() {
		return coordinatorTask;
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

		ProcessingRequest request = new ProcessingRequest(object);
		if (requestQueue != null) {
			// by not putting anything in the parent result we hope the status will be SUCCESS
			try {
				requestQueue.put(request);				// blocking if no free space in the queue
			} catch (InterruptedException e) {
				recordInterrupted(parentResult);
				return false;
			}
		} else {
			processRequest(request, coordinatorTask, parentResult);			// coordinator is also a worker here
		}

		// stop can be requested either internally (by handler or error in any worker thread)
		// or externally (by the task manager)

		if (stopRequestedByAnyWorker.get()) {
			return false;
		}

		if (!coordinatorTask.canRun()) {
			recordInterrupted(parentResult);
			return false;
		}

		return true;
	}

	private void recordInterrupted(OperationResult parentResult) {
		parentResult.createSubresult(taskOperationPrefix + ".handle").recordPartialError("Interrupted");
		if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("{} {} interrupted",new Object[]{
                    getProcessShortNameCapitalized(), getContextDesc()});
        }
	}

	public void signalAllItemsSubmitted() {
		allItemsSubmitted = true;
	}

	public Float getAverageTime() {
		long count = getProgress();
		if (count > 0) {
			long total = totalTimeProcessing.get();
			return (float) total / (float) count;
		} else {
			return null;
		}
	}

	public void waitForCompletion(OperationResult opResult) {
		taskManager.waitForTransientChildren(coordinatorTask, opResult);
	}

	public void updateOperationResult(OperationResult opResult) {
		if (collectedResults != null) {								// not null in the parallelized case
			collectedResults.computeStatus();
		}
		opResult.computeStatus("Errors during processing");			// collectedResults is a subresult of opResult

		if (getErrors() > 0) {
			opResult.setStatus(OperationResultStatus.PARTIAL_ERROR);
		}
	}

	public void completeProcessing(OperationResult result) {
		signalAllItemsSubmitted();
		waitForCompletion(result);      		// in order to provide correct statistics results, we have to wait until all child tasks finish
		updateOperationResult(result);
	}

	class WorkerHandler implements LightweightTaskHandler {
		@Override
		public void run(Task workerTask) {
			while (workerTask.canRun()) {
				ProcessingRequest request;
				try {
					request = requestQueue.poll(WORKER_THREAD_WAIT_FOR_REQUEST, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					LOGGER.trace("Interrupted when waiting for next request", e);
					return;
				}
				if (request != null) {
					processRequest(request, workerTask, collectedResults);
				} else {
					if (allItemsSubmitted) {
						LOGGER.trace("queue is empty and nothing more is expected - exiting");
						return;
					}
				}
			}
		}
	}

	private void processRequest(ProcessingRequest request, Task workerTask, OperationResult parentResult) {

		PrismObject<O> object = request.object;

		OperationResult result = parentResult.createSubresult(taskOperationPrefix + ".handle");
		result.addParam("object", object);

		boolean cont;

		long startTime = System.currentTimeMillis();

		try {

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("{} starting for {} {}",new Object[] {
						getProcessShortNameCapitalized(), object, getContextDesc()});
			}

			// The meat
			cont = handleObject(object, workerTask, result);

			// We do not want to override the result set by handler. This is just a fallback case
			if (result.isUnknown() || result.isInProgress()) {
				result.computeStatus();
			}

			if (result.isError()) {
				// Alternative way how to indicate an error.
				cont = processError(object, null, result);
			} else if (result.isSuccess()) {
				// FIXME: hack. Hardcoded ugly summarization of successes. something like
				// AbstractSummarizingResultHandler [lazyman]
				result.getSubresults().clear();
			}

		} catch (CommonException|RuntimeException e) {
			cont = processError(object, e, result);
		} finally {
			long duration = System.currentTimeMillis()-startTime;
			long total = totalTimeProcessing.addAndGet(duration);
			int progress = objectsProcessed.incrementAndGet();

			result.addContext(OperationResult.CONTEXT_PROGRESS, progress);

			try {
				coordinatorTask.setProgressImmediate(progress, result);              // this is necessary for the progress to be immediately available in GUI
			} catch (ObjectNotFoundException|SchemaException|RuntimeException e) {
				LoggingUtils.logException(LOGGER, "Couldn't record progress for task {}", e, coordinatorTask);
			}

			if (logObjectProgress) {
				if (LOGGER.isInfoEnabled()) {
					LOGGER.info("{} object {} {} done with status {} (this one: {} ms, avg: {} ms)", new Object[]{
							getProcessShortNameCapitalized(), object,
							getContextDesc(), result.getStatus(),
							duration, total/progress});
				}
			}

			parentResult.summarize();					// to prevent parent result from growing above reasonable size
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("{} finished for {} {}, result:\n{}", new Object[]{
					getProcessShortNameCapitalized(), object, getContextDesc(), result.debugDump()});
		}

		if (!cont) {
			stopRequestedByAnyWorker.set(true);
		}
	}

	private boolean processError(PrismObject<O> object, Exception ex, OperationResult result) {
		int errorsCount = errors.incrementAndGet();
		LOGGER.trace("Processing error, count: {}", errorsCount);

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
		if (result.isUnknown() || result.isInProgress()) {
			result.recordFatalError("Failed to "+getProcessShortName()+": "+ex.getMessage(), ex);
		}
		result.summarize();
		return !isStopOnError();
	}

	public long heartbeat() {
		// If we exist then we run. So just return the progress count.
		return getProgress();
	}

	public long getProgress() {
		return objectsProcessed.get();
	}
	
	public long getErrors() {
		return errors.get();
	}
	
	public boolean isStopOnError() {
		return stopOnError;
	}
	
	public void setStopOnError(boolean stopOnError) {
		this.stopOnError = stopOnError;
	}

	protected abstract boolean handleObject(PrismObject<O> object, Task workerTask, OperationResult result) throws CommonException;

	public class ProcessingRequest {
		public PrismObject<O> object;

		public ProcessingRequest(PrismObject<O> object) {
			this.object = object;
		}
	}

	public void createWorkerThreads(Task coordinatorTask, OperationResult opResult) {
		Integer threadsCount = getWorkerThreadsCount(coordinatorTask);
		if (threadsCount == null || threadsCount == 0) {
			return;             // nothing to do
		}

		int queueSize = threadsCount*2;				// actually, size of threadsCount should be sufficient but it doesn't hurt if queue is larger
		requestQueue = new ArrayBlockingQueue<>(queueSize);

		for (int i = 0; i < threadsCount; i++) {
			Task subtask = coordinatorTask.createSubtask(new WorkerHandler());
			subtask.setName("Worker thread " + (i+1) + " of " + threadsCount);
			subtask.startLightweightHandler();
			LOGGER.trace("Worker subtask {} created", subtask);
		}

		collectedResults = opResult.createSubresult(taskOperationPrefix + ".handleAsynchronously");
	}

	protected Integer getWorkerThreadsCount(Task task) {
		PrismProperty<Integer> workerThreadsPrismProperty = task.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_WORKER_THREADS);
		if (workerThreadsPrismProperty != null && workerThreadsPrismProperty.getRealValue() != null) {
			return workerThreadsPrismProperty.getRealValue();
		} else {
			return null;
		}
	}


}
