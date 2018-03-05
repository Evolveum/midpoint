/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.statistics.StatisticsUtil;
import com.evolveum.midpoint.task.api.LightweightTaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.SystemException;
import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.ArrayList;
import java.util.List;
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
	protected static final long REQUEST_QUEUE_OFFER_TIMEOUT = 1000L;

	private final TaskManager taskManager;
	private final Task coordinatorTask;
	private final String taskOperationPrefix;
	private final String processShortName;
	private String contextDesc;
	private AtomicInteger objectsProcessed = new AtomicInteger();
	private AtomicLong totalTimeProcessing = new AtomicLong();
	private AtomicInteger errors = new AtomicInteger();
	private boolean stopOnError;
	private boolean logObjectProgress;
	private boolean logErrors = true;
	private boolean recordIterationStatistics = true;				// whether we want to do these ourselves or we let someone else do that for us
	private boolean enableIterationStatistics = true;				// whether we want to collect these statistics at all
	private boolean enableSynchronizationStatistics = false;		// whether we want to collect sync statistics
	private boolean enableActionsExecutedStatistics = false;		// whether we want to collect repo objects statistics
	private BlockingQueue<ProcessingRequest> requestQueue;
	private AtomicBoolean stopRequestedByAnyWorker = new AtomicBoolean(false);
	private final long startTime;

	private static final transient Trace LOGGER = TraceManager.getTrace(AbstractSearchIterativeResultHandler.class);
	private volatile boolean allItemsSubmitted = false;

	private List<OperationResult> workerSpecificResults;

	public AbstractSearchIterativeResultHandler(Task coordinatorTask, String taskOperationPrefix, String processShortName,
			String contextDesc, TaskManager taskManager) {
		super();
		this.coordinatorTask = coordinatorTask;
		this.taskOperationPrefix = taskOperationPrefix;
		this.processShortName = processShortName;
		this.contextDesc = contextDesc;
		this.taskManager = taskManager;
		stopOnError = true;
		startTime = System.currentTimeMillis();
	}

	protected String getProcessShortName() {
		return processShortName;
	}

	protected String getProcessShortNameCapitalized() {
		return StringUtils.capitalize(processShortName);
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

	public boolean isRecordIterationStatistics() {
		return recordIterationStatistics;
	}

	public void setRecordIterationStatistics(boolean recordIterationStatistics) {
		this.recordIterationStatistics = recordIterationStatistics;
	}

	public boolean isEnableIterationStatistics() {
		return enableIterationStatistics;
	}

	public void setEnableIterationStatistics(boolean enableIterationStatistics) {
		this.enableIterationStatistics = enableIterationStatistics;
	}

	public boolean isEnableSynchronizationStatistics() {
		return enableSynchronizationStatistics;
	}

	public void setEnableSynchronizationStatistics(boolean enableSynchronizationStatistics) {
		this.enableSynchronizationStatistics = enableSynchronizationStatistics;
	}

	public boolean isEnableActionsExecutedStatistics() {
		return enableActionsExecutedStatistics;
	}

	public void setEnableActionsExecutedStatistics(boolean enableActionsExecutedStatistics) {
		this.enableActionsExecutedStatistics = enableActionsExecutedStatistics;
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
				while (!requestQueue.offer(request, REQUEST_QUEUE_OFFER_TIMEOUT, TimeUnit.MILLISECONDS)) {
					if (shouldStop(parentResult)) {
						return false;
					}
				}
			} catch (InterruptedException e) {
				recordInterrupted(parentResult);
				return false;
			}
		} else {
			processRequest(request, coordinatorTask, parentResult);			// coordinator is also a worker here
		}

		return !shouldStop(parentResult);
	}

	// stop can be requested either internally (by handler or error in any worker thread)
	// or externally (by the task manager)
	private boolean shouldStop(OperationResult parentResult) {
		if (stopRequestedByAnyWorker.get()) {
			return true;
		}

		if (!coordinatorTask.canRun()) {
			recordInterrupted(parentResult);
			return true;
		}
		return false;
	}

	private void recordInterrupted(OperationResult parentResult) {
		parentResult.createSubresult(taskOperationPrefix + ".handle").recordWarning("Interrupted");
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

	public Float getWallAverageTime() {
		long count = getProgress();
		if (count > 0) {
			return (float) getWallTime() / (float) count;
		} else {
			return null;
		}
	}

	public long getWallTime() {
		return System.currentTimeMillis() - startTime;
	}

	public void waitForCompletion(OperationResult opResult) {
		taskManager.waitForTransientChildren(coordinatorTask, opResult);
	}

	public void updateOperationResult(OperationResult opResult) {
		if (workerSpecificResults != null) {								// not null in the parallelized case
			for (OperationResult workerSpecificResult : workerSpecificResults) {
				workerSpecificResult.computeStatus();
				workerSpecificResult.summarize();
				opResult.addSubresult(workerSpecificResult);
			}
		}
		opResult.computeStatus("Issues during processing");

		if (getErrors() > 0) {
			opResult.setStatus(OperationResultStatus.PARTIAL_ERROR);
		}
	}

	public void completeProcessing(Task task, OperationResult result) {
		signalAllItemsSubmitted();
		waitForCompletion(result);      		// in order to provide correct statistics results, we have to wait until all child tasks finish
		updateOperationResult(result);
	}

	class WorkerHandler implements LightweightTaskHandler {
		private OperationResult workerSpecificResult;

		WorkerHandler(OperationResult workerSpecificResult) {
			this.workerSpecificResult = workerSpecificResult;
		}

		@Override
		public void run(Task workerTask) {

			// temporary hack: how to see thread name for this task
			workerTask.setName(workerTask.getName().getOrig() + " (" + Thread.currentThread().getName() + ")");
			workerSpecificResult.addArbitraryObjectAsContext("subtaskName", workerTask.getName());

			while (workerTask.canRun()) {
				ProcessingRequest request;
				try {
					request = requestQueue.poll(WORKER_THREAD_WAIT_FOR_REQUEST, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					LOGGER.trace("Interrupted when waiting for next request", e);
					return;
				}
				if (request != null) {
					processRequest(request, workerTask, workerSpecificResult);
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

		String objectName = PolyString.getOrig(object.getName());
		String objectDisplayName = getDisplayName(object);

		OperationResult result = parentResult.createSubresult(taskOperationPrefix + ".handle");
		result.addParam("object", object);

		boolean cont;

		long startTime = System.currentTimeMillis();

		try {

			RepositoryCache.enter();

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("{} starting for {} {}", getProcessShortNameCapitalized(), object, getContextDesc());
			}

			if (isRecordIterationStatistics()) {
				workerTask.recordIterativeOperationStart(objectName, objectDisplayName,
						null /* TODO */, object.getOid());
			}

			// The meat
			cont = handleObject(object, workerTask, result);

			// We do not want to override the result set by handler. This is just a fallback case
			if (result.isUnknown() || result.isInProgress()) {
				result.computeStatus();
			}

			if (result.isError()) {
				// Alternative way how to indicate an error.
				if (isRecordIterationStatistics()) {
					workerTask.recordIterativeOperationEnd(objectName, objectDisplayName,
							null /* TODO */, object.getOid(), startTime, getException(result));
				}
				cont = processError(object, null, result);
			} else {
				if (isRecordIterationStatistics()) {
					workerTask.recordIterativeOperationEnd(objectName, objectDisplayName,
							null /* TODO */, object.getOid(), startTime, null);
				}
				if (result.isSuccess()) {
					// FIXME: hack. Hardcoded ugly summarization of successes. something like
					// AbstractSummarizingResultHandler [lazyman]
					result.getSubresults().clear();
				}
			}

		} catch (CommonException | PreconditionViolationException | Error | RuntimeException e) {
			if (isRecordIterationStatistics()) {
				workerTask.recordIterativeOperationEnd(objectName, objectDisplayName,
						null /* TODO */, object.getOid(), startTime, e);
			}
			cont = processError(object, e, result);
		} finally {
			RepositoryCache.exit();

			long duration = System.currentTimeMillis()-startTime;
			long total = totalTimeProcessing.addAndGet(duration);
			int progress = objectsProcessed.incrementAndGet();

			result.addContext(OperationResult.CONTEXT_PROGRESS, progress);

			// parentResult is worker-thread-specific result (because of concurrency issues)
			// or parentResult as obtained in handle(..) method in single-thread scenario
			parentResult.summarize();

			synchronized (coordinatorTask) {
				coordinatorTask.setProgress(progress);
				if (requestQueue != null) {
					workerTask.setProgress(workerTask.getProgress()+1);
				}
				// todo report current op result?
				coordinatorTask.storeOperationStatsIfNeeded();  // includes savePendingModifications
			}

			if (logObjectProgress) {
				if (LOGGER.isInfoEnabled()) {
					LOGGER.info("{} object {} {} done with status {} (this one: {} ms, avg: {} ms) (total progress: {}, wall clock avg: {} ms)",
							getProcessShortNameCapitalized(), object,
							getContextDesc(), result.getStatus(),
							duration, total/progress, progress,
							(System.currentTimeMillis()-this.startTime)/progress);
				}
			}
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("{} finished for {} {}, result:\n{}", getProcessShortNameCapitalized(), object, getContextDesc(),
					result.debugDump());
		}

		if (!cont) {
			stopRequestedByAnyWorker.set(true);
		}
	}

	// may be overridden
	protected String getDisplayName(PrismObject<O> object) {
		return StatisticsUtil.getDisplayName(object);
	}

	// TODO implement better
	protected Throwable getException(OperationResult result) {
		if (result.getCause() != null) {
			return result.getCause();
		} else {
			return new SystemException(result.getMessage());
		}
	}

	private boolean processError(PrismObject<O> object, Throwable ex, OperationResult result) {
		int errorsCount = errors.incrementAndGet();
		LOGGER.trace("Processing error, count: {}", errorsCount);

		String message;
		if (ex != null) {
			message = ex.getMessage();
		} else {
			message = result.getMessage();
		}
		if (logErrors && LOGGER.isErrorEnabled()) {
			LOGGER.error("{} of object {} {} failed: {}", getProcessShortNameCapitalized(), object, getContextDesc(), message, ex);
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

	public boolean isLogErrors() {
		return logErrors;
	}

	public void setLogErrors(boolean logErrors) {
		this.logErrors = logErrors;
	}

	protected abstract boolean handleObject(PrismObject<O> object, Task workerTask, OperationResult result) throws CommonException, PreconditionViolationException;

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

		workerSpecificResults = new ArrayList<>(threadsCount);

		for (int i = 0; i < threadsCount; i++) {
			// we intentionally do not put worker specific result under main operation result until the handler is done
			// (because of concurrency issues - adding subresults vs e.g. putting main result into the task)
			OperationResult workerSpecificResult = new OperationResult(taskOperationPrefix + ".handleAsynchronously");
			workerSpecificResult.addContext("subtaskIndex", i+1);
			workerSpecificResults.add(workerSpecificResult);

			Task subtask = coordinatorTask.createSubtask(new WorkerHandler(workerSpecificResult));
			if (isEnableIterationStatistics()) {
				subtask.resetIterativeTaskInformation(null);
			}
			if (isEnableSynchronizationStatistics()) {
				subtask.resetSynchronizationInformation(null);
			}
			if (isEnableActionsExecutedStatistics()) {
				subtask.resetActionsExecutedInformation(null);
			}
			subtask.setCategory(coordinatorTask.getCategory());
			subtask.setResult(new OperationResult(taskOperationPrefix + ".executeWorker", OperationResultStatus.IN_PROGRESS, (String) null));
			subtask.setName("Worker thread " + (i+1) + " of " + threadsCount);
			subtask.startLightweightHandler();
			LOGGER.trace("Worker subtask {} created", subtask);
		}
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
