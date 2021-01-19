/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.repo.common.util.RepoCommonUtils;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultBuilder;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.statistics.StatisticsUtil;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LevelOverrideTurboFilter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.util.logging.TracingAppender;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * <p>Processes individual objects found by the iterative search.</p>
 *
 * <p>Main responsibilities:</p>
 *
 * <ol>
 *     <li>Multithreading support (using worker tasks) - similar to ChangeProcessingCoordinator in provisioning module</li>
 *     <li>Error handling</li>
 *     <li>Progress, error, and statistics reporting</li>
 * </ol>
 *
 * TODO Finish cleaning up the code
 * TODO Factor out multithreading support (like ChangeProcessor vs. ChangeProcessingCoordinator in provisioning)
 * TODO Should we really enter/exit repository cache here? And the other responsibilities?
 *
 * @author semancik
 */
public abstract class AbstractSearchIterativeResultHandler<
        O extends ObjectType,
        TH extends AbstractSearchIterativeTaskHandler<TH, TE>,
        TE extends AbstractSearchIterativeTaskExecution<TH, TE>,
        PE extends AbstractSearchIterativeTaskPartExecution<O, TH, TE, PE, RH>,
        RH extends AbstractSearchIterativeResultHandler<O, TH, TE, PE, RH>>
        implements ResultHandler<O> {

    private static final int WORKER_THREAD_WAIT_FOR_REQUEST = 500;
    private static final long REQUEST_QUEUE_OFFER_TIMEOUT = 1000L;

    /**
     * Execution of the containing task part.
     */
    @NotNull protected final PE partExecution;

    /**
     * Execution of the containing task.
     */
    @NotNull protected final TE taskExecution;

    /**
     * Handler of the containing task.
     */
    @NotNull protected final TH taskHandler;

    /**
     * The local coordinator task, i.e. the one that issues the search call.
     */
    @NotNull protected final RunningTask coordinatorTask;

    /**
     * Controls how are errors, progress, and various statistics related to the task reported.
     */
    @NotNull private final TaskReportingOptions reportingOptions;

    /**
     * TODO
     */
    private final String processShortName;

    /**
     * TODO
     */
    @NotNull protected final String contextDesc;

    // Information to be reported: progress, statistics, errors, and so on.

    private final AtomicInteger objectsProcessed = new AtomicInteger();
    private final long initialProgress;
    private final AtomicLong totalTimeProcessing = new AtomicLong();
    private final AtomicInteger errors = new AtomicInteger();
    private final long startTime;

    // Worker threads support

    private BlockingQueue<ProcessingRequest> requestQueue;
    private final AtomicBoolean stopRequestedByAnyWorker = new AtomicBoolean(false);
    private volatile Throwable exceptionEncountered;
    private volatile boolean allItemsSubmitted = false;
    private List<OperationResult> workerSpecificResults;

    private static final Trace LOGGER = TraceManager.getTrace(AbstractSearchIterativeResultHandler.class);

    public AbstractSearchIterativeResultHandler(PE partExecution) {
        this(partExecution, null);
    }

    public AbstractSearchIterativeResultHandler(PE partExecution, String contextDesc) {
        this(partExecution, partExecution.getTaskHandler().getTaskTypeName(), contextDesc);
    }

    public AbstractSearchIterativeResultHandler(PE partExecution, String processShortName, String contextDesc) {
        this.taskExecution = partExecution.getTaskExecution();
        this.partExecution = partExecution;
        this.taskHandler = partExecution.getTaskHandler();

        this.coordinatorTask = partExecution.localCoordinatorTask;
        this.processShortName = processShortName;
        this.contextDesc = ObjectUtils.defaultIfNull(contextDesc, "");

        startTime = System.currentTimeMillis();
        initialProgress = coordinatorTask.getProgress();
        reportingOptions = taskHandler.getReportingOptions();
    }

    private String getProcessShortNameCapitalized() {
        return StringUtils.capitalize(processShortName);
    }

    @Override
    public boolean handle(PrismObject<O> object, OperationResult parentResult) {
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
            processRequest(request, coordinatorTask, parentResult); // coordinator is also a worker here
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
        parentResult.createSubresult(getTaskOperationPrefix() + ".handle").recordWarning("Interrupted");
        LOGGER.warn("{} {} interrupted", getProcessShortNameCapitalized(), contextDesc);
    }

    private void signalAllItemsSubmitted() {
        allItemsSubmitted = true;
    }

    final Float getAverageTime() {
        long count = getProgress();
        if (count > 0) {
            long total = totalTimeProcessing.get();
            return (float) total / (float) count;
        } else {
            return null;
        }
    }

    final Float getWallAverageTime() {
        long count = getProgress();
        if (count > 0) {
            return (float) getWallTime() / (float) count;
        } else {
            return null;
        }
    }

    final long getWallTime() {
        return System.currentTimeMillis() - startTime;
    }

    private void waitForCompletion(OperationResult opResult) {
        getTaskManager().waitForTransientChildren(coordinatorTask, opResult);
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
        waitForCompletion(result); // in order to provide correct statistics results, we have to wait until all child tasks finish
        updateOperationResult(result);
    }

    protected Function<ItemPath, ItemDefinition<?>> getIdentifierDefinitionProvider() {
        return null;
    }

    class WorkerHandler implements LightweightTaskHandler {
        private final OperationResult workerSpecificResult;

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
        QName objectType = object.getDefinition().getTypeName();

        // just in case an exception occurs between this place and the moment where real result is created
        OperationResult result = new OperationResult("dummy");
        boolean tracingRequested = false;

        boolean cont;

        long startTime = System.currentTimeMillis();

        RepositoryCache.enterLocalCaches(getCacheConfigurationManager()); // why?

        try {
            if (!isNonScavengingWorker()) {     // todo configure this somehow
                int objectsSeen = coordinatorTask.getAndIncrementObjectsSeen();
                workerTask.startDynamicProfilingIfNeeded(coordinatorTask, objectsSeen);
                workerTask.requestTracingIfNeeded(coordinatorTask, objectsSeen, TracingRootType.ITERATIVE_TASK_OBJECT_PROCESSING);
            }

            LOGGER.trace("{} starting for {} {}", getProcessShortNameCapitalized(), object, contextDesc);
            workerTask.recordIterativeOperationStart(objectName, objectDisplayName, objectType, object.getOid());

            OperationResultBuilder builder = parentResult.subresult(getTaskOperationPrefix() + ".handle")
                    .addParam("object", object);
            if (workerTask.getTracingRequestedFor().contains(TracingRootType.ITERATIVE_TASK_OBJECT_PROCESSING)) {
                tracingRequested = true;
                builder.tracingProfile(getTracer().compileProfile(workerTask.getTracingProfile(), parentResult));
            }
            result = builder.build();

            // The meat
            cont = handleObject(object, workerTask, result);
            cont = cont && workerTask.canRun();

            // We do not want to override the result set by handler. This is just a fallback case
            if (result.isUnknown() || result.isInProgress()) {
                result.computeStatus();
            }

            Throwable resultException;
            if (result.isError()) {
                // An error without visible top-level exception
                resultException = RepoCommonUtils.getResultException(result);

                workerTask.recordIterativeOperationEnd(objectName, objectDisplayName, objectType,
                        object.getOid(), startTime, resultException);
                cont = processError(object, workerTask, resultException, result);

            } else {
                // Success
                resultException = null;
                workerTask.recordIterativeOperationEnd(objectName, objectDisplayName,
                        objectType, object.getOid(), startTime, null);
            }
            writeOperationExecutionRecord(object, resultException, result);

        } catch (CommonException | PreconditionViolationException | Error | RuntimeException e) {
            // An error with top-level exception
            workerTask.recordIterativeOperationEnd(objectName, objectDisplayName,
                    objectType, object.getOid(), startTime, e);
            cont = processError(object, workerTask, e, result);
            writeOperationExecutionRecord(object, e, result);

        } finally {
            RepositoryCache.exitLocalCaches();
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

            // TODO make this configurable per task or per task type; or switch to DEBUG
            LOGGER.info("{} object {} {} done with status {} (this one: {} ms, avg: {} ms) (total progress: {}, wall clock avg: {} ms)",
                    getProcessShortNameCapitalized(), object,
                    contextDesc, result.getStatus(),
                    duration, total/progress, progress,
                    (System.currentTimeMillis()-this.startTime)/progress);

            if (tracingRequested) {
                getTracer().storeTrace(workerTask, result, parentResult);
                TracingAppender.terminateCollecting(); // todo reconsider
                LevelOverrideTurboFilter.cancelLoggingOverride(); // todo reconsider
            }
            if (result.isSuccess() && !tracingRequested && !result.isTraced()) {
                // FIXME: hack. Hardcoded ugly summarization of successes. something like
                //   AbstractSummarizingResultHandler [lazyman]
                result.getSubresults().clear();
            }
        }

        LOGGER.trace("{} finished for {} {}, result:\n{}", getProcessShortNameCapitalized(), object, contextDesc,
                result.debugDumpLazily());

        if (!cont) {
            stopRequestedByAnyWorker.set(true);
        }
    }

    private void writeOperationExecutionRecord(PrismObject<O> object, Throwable resultException, OperationResult result) {
        taskHandler.getOperationExecutionRecorder().recordOperationExecution(object, resultException,
                taskExecution.localCoordinatorTask, result);
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
        if (reportingOptions.isLogErrors()) {
            LOGGER.error("{} of object {} {} failed: {}", getProcessShortNameCapitalized(), object, contextDesc, message, ex);
        }
        // We do not want to override the result set by handler. This is just a fallback case
        if (result.isUnknown() || result.isInProgress()) {
            assert ex != null;
            result.recordFatalError("Failed to "+processShortName+": "+ex.getMessage(), ex);
        }
        result.summarize();
        return canContinue(task, ex, result);
    }

    private boolean canContinue(Task task, Throwable ex, OperationResult result) {
        TaskPartitionDefinitionType partDef = taskExecution.partDefinition;
        if (partDef == null) {
            return true;
        }

        CriticalityType criticality = ExceptionUtil.getCriticality(partDef.getErrorCriticality(), ex, CriticalityType.PARTIAL);
        try {
            RepoCommonUtils.processErrorCriticality(task, criticality, ex, result);
        } catch (Throwable e) {
            exceptionEncountered = e;
            return false;
        }

        return true;
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

    protected abstract boolean handleObject(PrismObject<O> object, RunningTask workerTask, OperationResult result)
            throws CommonException, PreconditionViolationException;

    public class ProcessingRequest {
        public PrismObject<O> object;

        private ProcessingRequest(PrismObject<O> object) {
            this.object = object;
        }
    }

    void createWorkerThreads() {
        Integer threadsCount = getWorkerThreadsCount();
        if (threadsCount == null || threadsCount == 0) {
            return;
        }

        // remove subtasks that could have been created during processing of previous buckets
        coordinatorTask.deleteLightweightAsynchronousSubtasks();

        // actually, size of threadsCount should be sufficient but it doesn't hurt if queue is larger
        int queueSize = threadsCount*2;
        requestQueue = new ArrayBlockingQueue<>(queueSize);

        workerSpecificResults = new ArrayList<>(threadsCount);

        for (int i = 0; i < threadsCount; i++) {
            // we intentionally do not put worker specific result under main operation result until the handler is done
            // (because of concurrency issues - adding subresults vs e.g. putting main result into the task)
            OperationResult workerSpecificResult = new OperationResult(getTaskOperationPrefix() + ".handleAsynchronously");
            workerSpecificResult.addContext("subtaskIndex", i+1);
            workerSpecificResults.add(workerSpecificResult);

            RunningTask subtask = coordinatorTask.createSubtask(new WorkerHandler(workerSpecificResult));
            if (reportingOptions.isEnableIterationStatistics()) {
                subtask.resetIterativeTaskInformation(null);
            }
            if (reportingOptions.isEnableSynchronizationStatistics()) {
                subtask.resetSynchronizationInformation(null);
            }
            if (reportingOptions.isEnableActionsExecutedStatistics()) {
                subtask.resetActionsExecutedInformation(null);
            }
            subtask.setCategory(coordinatorTask.getCategory());
            subtask.setResult(new OperationResult(getTaskOperationPrefix() + ".executeWorker", OperationResultStatus.IN_PROGRESS, (String) null));
            subtask.setName("Worker thread " + (i+1) + " of " + threadsCount);
            subtask.setExecutionEnvironment(CloneUtil.clone(coordinatorTask.getExecutionEnvironment()));
            subtask.startLightweightHandler();
            LOGGER.trace("Worker subtask {} created", subtask);
        }
    }

    private Integer getWorkerThreadsCount() {
        return taskExecution.getTaskPropertyRealValue(SchemaConstants.MODEL_EXTENSION_WORKER_THREADS);
    }

    protected void ensureNoWorkerThreads() {
        Integer tasks = getWorkerThreadsCount();
        if (tasks != null && tasks != 0) {
            throw new UnsupportedOperationException("Unsupported number of worker threads: " + tasks +
                    ". This task cannot be run with worker threads. Please remove workerThreads "
                    + "extension property or set its value to 0.");
        }
    }

    private TaskManager getTaskManager() {
        return taskHandler.getTaskManager();
    }

    private CacheConfigurationManager getCacheConfigurationManager() {
        return getTaskManager().getCacheConfigurationManager();
    }

    private Tracer getTracer() {
        return getTaskManager().getTracer();
    }

    private String getTaskOperationPrefix() {
        return taskHandler.taskOperationPrefix;
    }
}
