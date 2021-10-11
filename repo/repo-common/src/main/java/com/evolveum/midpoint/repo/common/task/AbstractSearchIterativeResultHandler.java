/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.repo.common.util.RepoCommonUtils;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResultBuilder;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.statistics.StatisticsUtil;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.logging.LevelOverrideTurboFilter;
import com.evolveum.midpoint.util.logging.TracingAppender;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

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

    private static final int WORKER_THREAD_WAIT_FOR_REQUEST = 500;
    private static final long REQUEST_QUEUE_OFFER_TIMEOUT = 1000L;

    private final TaskManager taskManager;
    private final RunningTask coordinatorTask;
    private final String taskOperationPrefix;
    private final String processShortName;
    private String contextDesc;
    private AtomicInteger objectsProcessed = new AtomicInteger();
    private long initialProgress;
    private AtomicLong totalTimeProcessing = new AtomicLong();
    private AtomicInteger errors = new AtomicInteger();
    private boolean stopOnError;
    private boolean logObjectProgress;
    private boolean logErrors = true;
    private boolean recordIterationStatistics = true;                // whether we want to do these ourselves or we let someone else do that for us
    private boolean enableIterationStatistics = true;                // whether we want to collect these statistics at all
    private boolean enableSynchronizationStatistics = false;        // whether we want to collect sync statistics
    private boolean enableActionsExecutedStatistics = false;        // whether we want to collect repo objects statistics
    private BlockingQueue<ProcessingRequest> requestQueue;
    private AtomicBoolean stopRequestedByAnyWorker = new AtomicBoolean(false);
    private volatile Throwable exceptionEncountered;
    private final long startTime;

    private static final Trace LOGGER = TraceManager.getTrace(AbstractSearchIterativeResultHandler.class);
    private volatile boolean allItemsSubmitted = false;

    private List<OperationResult> workerSpecificResults;

    private TaskPartitionDefinitionType stageType;

    public AbstractSearchIterativeResultHandler(RunningTask coordinatorTask, String taskOperationPrefix, String processShortName,
            String contextDesc, TaskManager taskManager) {
        this(coordinatorTask, taskOperationPrefix, processShortName, contextDesc, null, taskManager);
    }

    public AbstractSearchIterativeResultHandler(RunningTask coordinatorTask, String taskOperationPrefix, String processShortName,
            String contextDesc, TaskPartitionDefinitionType taskStageType, TaskManager taskManager) {
        super();
        this.coordinatorTask = coordinatorTask;
        this.taskOperationPrefix = taskOperationPrefix;
        this.processShortName = processShortName;
        this.contextDesc = contextDesc;
        this.taskManager = taskManager;

        this.stageType = taskStageType;

        stopOnError = true;
        startTime = System.currentTimeMillis();
        initialProgress = coordinatorTask.getProgress();
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

    public void setLogObjectProgress(boolean logObjectProgress) {
        this.logObjectProgress = logObjectProgress;
    }

    private boolean isRecordIterationStatistics() {
        return recordIterationStatistics;
    }

    @SuppressWarnings("SameParameterValue")
    protected void setRecordIterationStatistics(boolean recordIterationStatistics) {
        this.recordIterationStatistics = recordIterationStatistics;
    }

    private boolean isEnableIterationStatistics() {
        return enableIterationStatistics;
    }

    void setEnableIterationStatistics(boolean enableIterationStatistics) {
        this.enableIterationStatistics = enableIterationStatistics;
    }

    private boolean isEnableSynchronizationStatistics() {
        return enableSynchronizationStatistics;
    }

    public void setEnableSynchronizationStatistics(boolean enableSynchronizationStatistics) {
        this.enableSynchronizationStatistics = enableSynchronizationStatistics;
    }

    private boolean isEnableActionsExecutedStatistics() {
        return enableActionsExecutedStatistics;
    }

    public void setEnableActionsExecutedStatistics(boolean enableActionsExecutedStatistics) {
        this.enableActionsExecutedStatistics = enableActionsExecutedStatistics;
    }

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
            processRequest(request, coordinatorTask, parentResult);            // coordinator is also a worker here
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
        LOGGER.warn("{} {} interrupted", getProcessShortNameCapitalized(), getContextDesc());
    }

    private void signalAllItemsSubmitted() {
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

    long getWallTime() {
        return System.currentTimeMillis() - startTime;
    }

    private void waitForCompletion(OperationResult opResult) {
        taskManager.waitForTransientChildren(coordinatorTask, opResult);
    }

    private void updateOperationResult(OperationResult opResult) {
        if (workerSpecificResults != null) {                                // not null in the parallel case
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
        waitForCompletion(result);              // in order to provide correct statistics results, we have to wait until all child tasks finish
        updateOperationResult(result);
    }

    class WorkerHandler implements LightweightTaskHandler {
        private OperationResult workerSpecificResult;

        private WorkerHandler(OperationResult workerSpecificResult) {
            this.workerSpecificResult = workerSpecificResult;
        }

        @Override
        public void run(RunningTask workerTask) {

            // temporary hack: how to see thread name for this task
            workerTask.setName(workerTask.getName().getOrig() + " (" + Thread.currentThread().getName() + ")");
            workerSpecificResult.addArbitraryObjectAsContext("subtaskName", workerTask.getName());

            while (workerTask.canRun() && !stopRequestedByAnyWorker.get()) {
                workerTask.refreshLowLevelStatistics();
                ProcessingRequest request;
                try {
                    request = requestQueue.poll(WORKER_THREAD_WAIT_FOR_REQUEST, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    LOGGER.trace("Interrupted when waiting for next request", e);
                    return;
                } finally {
                    workerTask.refreshLowLevelStatistics();
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

    private void processRequest(ProcessingRequest request, RunningTask workerTask, OperationResult parentResult) {

        PrismObject<O> object = request.object;

        String objectName = PolyString.getOrig(object.getName());
        String objectDisplayName = getDisplayName(object);

        // just in case an exception occurs between this place and the moment where real result is created
        OperationResult result = new OperationResult("dummy");
        boolean tracingRequested = false;

        boolean cont;

        long startTime = System.currentTimeMillis();

        RepositoryCache.enter(taskManager.getCacheConfigurationManager());

        try {
            if (!isNonScavengingWorker()) {     // todo configure this somehow
                int objectsSeen = coordinatorTask.getAndIncrementObjectsSeen();
                workerTask.startDynamicProfilingIfNeeded(coordinatorTask, objectsSeen);
                workerTask.requestTracingIfNeeded(coordinatorTask, objectsSeen, TracingRootType.ITERATIVE_TASK_OBJECT_PROCESSING);
            }
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("{} starting for {} {}", getProcessShortNameCapitalized(), object, getContextDesc());
            }

            if (isRecordIterationStatistics()) {
                workerTask.recordIterativeOperationStart(objectName, objectDisplayName,
                        null /* TODO */, object.getOid());
            }

            OperationResultBuilder builder = parentResult.subresult(taskOperationPrefix + ".handle")
                    .addParam("object", object);
            if (workerTask.getTracingRequestedFor().contains(TracingRootType.ITERATIVE_TASK_OBJECT_PROCESSING)) {
                tracingRequested = true;
                builder.tracingProfile(taskManager.getTracer().compileProfile(workerTask.getTracingProfile(), parentResult));
            }
            result = builder.build();

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
                            null /* TODO */, object.getOid(), startTime, RepoCommonUtils.getResultException(result));
                }

                cont = processError(object, workerTask, RepoCommonUtils.getResultException(result), result);
            } else {
                if (isRecordIterationStatistics()) {
                    workerTask.recordIterativeOperationEnd(objectName, objectDisplayName,
                            null /* TODO */, object.getOid(), startTime, null);
                }
            }

        } catch (CommonException | PreconditionViolationException | Error | RuntimeException e) {
            if (isRecordIterationStatistics()) {
                workerTask.recordIterativeOperationEnd(objectName, objectDisplayName,
                        null /* TODO */, object.getOid(), startTime, e);
            }
            cont = processError(object, workerTask, e, result);

        } finally {
            RepositoryCache.exit();
            workerTask.stopDynamicProfiling();
            workerTask.stopTracing();

            long duration = System.currentTimeMillis()-startTime;
            long total = totalTimeProcessing.addAndGet(duration);
            long progress = initialProgress + objectsProcessed.incrementAndGet();

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
                // FIXME this should not be called from the worker task!
                coordinatorTask.storeOperationStatsIfNeeded();  // includes flushPendingModifications
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

            if (tracingRequested) {
                taskManager.getTracer().storeTrace(workerTask, result, parentResult);
                TracingAppender.terminateCollecting();  // todo reconsider
                LevelOverrideTurboFilter.cancelLoggingOverride();   // todo reconsider
            }
            if (result.isSuccess() && !tracingRequested && !result.isTraced()) {
                // FIXME: hack. Hardcoded ugly summarization of successes. something like
                //   AbstractSummarizingResultHandler [lazyman]
                result.getSubresults().clear();
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

    private boolean isNonScavengingWorker() {
        return coordinatorTask.getWorkManagement() != null &&
                coordinatorTask.getWorkManagement().getTaskKind() == TaskKindType.WORKER &&
                !Boolean.TRUE.equals(coordinatorTask.getWorkManagement().isScavenger());
    }

    public Throwable getExceptionEncountered() {
        return exceptionEncountered;
    }

    // may be overridden
    protected String getDisplayName(PrismObject<O> object) {
        return StatisticsUtil.getDisplayName(object);
    }

    // TODO implement better

    // @pre: result is "error" or ex is not null
    private boolean processError(PrismObject<O> object, Task task, Throwable ex, OperationResult result) {
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
            assert ex != null;
            result.recordFatalError("Failed to "+getProcessShortName()+": "+ex.getMessage(), ex);
        }
        result.summarize();
        return canContinue(task, ex, result);
//        return !isStopOnError();
    }

    private boolean canContinue(Task task, Throwable ex, OperationResult result) {
        if (stageType == null) {
            return !stopOnError;
        }

        CriticalityType criticality = ExceptionUtil.getCriticality(stageType.getErrorCriticality(), ex, CriticalityType.PARTIAL);
        try {
            RepoCommonUtils.processErrorCriticality(task, criticality, ex, result);
        } catch (Throwable e) {
            exceptionEncountered = e;
            return false;
        }

        return !stopOnError;
    }

    /**
     * @return the stageType
     */
    protected TaskPartitionDefinitionType getStageType() {
        return stageType;
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

    public void setStopOnError(boolean stopOnError) {
        this.stopOnError = stopOnError;
    }

    @SuppressWarnings("SameParameterValue")
    protected void setLogErrors(boolean logErrors) {
        this.logErrors = logErrors;
    }

    protected abstract boolean handleObject(PrismObject<O> object, RunningTask workerTask, OperationResult result) throws CommonException, PreconditionViolationException;

    public class ProcessingRequest {
        public PrismObject<O> object;

        private ProcessingRequest(PrismObject<O> object) {
            this.object = object;
        }
    }

    public void createWorkerThreads(RunningTask coordinatorTask) {
        Integer threadsCount = getWorkerThreadsCount(coordinatorTask);
        if (threadsCount == null || threadsCount == 0) {
            return;             // nothing to do
        }

        // remove subtasks that could have been created during processing of previous buckets
        coordinatorTask.deleteLightweightAsynchronousSubtasks();

        int queueSize = threadsCount*2;                // actually, size of threadsCount should be sufficient but it doesn't hurt if queue is larger
        requestQueue = new ArrayBlockingQueue<>(queueSize);

        workerSpecificResults = new ArrayList<>(threadsCount);

        for (int i = 0; i < threadsCount; i++) {
            // we intentionally do not put worker specific result under main operation result until the handler is done
            // (because of concurrency issues - adding subresults vs e.g. putting main result into the task)
            OperationResult workerSpecificResult = new OperationResult(taskOperationPrefix + ".handleAsynchronously");
            workerSpecificResult.addContext("subtaskIndex", i+1);
            workerSpecificResults.add(workerSpecificResult);

            RunningTask subtask = coordinatorTask.createSubtask(new WorkerHandler(workerSpecificResult));
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
            subtask.setExecutionEnvironment(CloneUtil.clone(coordinatorTask.getExecutionEnvironment()));
            subtask.startLightweightHandler();
            LOGGER.trace("Worker subtask {} created", subtask);
        }
    }

    protected Integer getWorkerThreadsCount(Task task) {
        PrismProperty<Integer> workerThreadsPrismProperty = task.getExtensionPropertyOrClone(SchemaConstants.MODEL_EXTENSION_WORKER_THREADS);
        if (workerThreadsPrismProperty != null && workerThreadsPrismProperty.getRealValue() != null) {
            return workerThreadsPrismProperty.getRealValue();
        } else {
            return null;
        }
    }

}
