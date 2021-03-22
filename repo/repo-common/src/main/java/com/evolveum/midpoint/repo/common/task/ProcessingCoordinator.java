/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.evolveum.midpoint.task.api.RunningLightweightTask;
import com.evolveum.midpoint.task.api.TaskManager;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.LightweightTaskHandler;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.logging.Trace;

/**
 * TODO
 */
public class ProcessingCoordinator<I> {

    @NotNull private final Trace logger;

    private static final long WORKER_THREAD_WAIT_FOR_REQUEST = 100L;

    private static final String OP_HANDLE_ASYNCHRONOUSLY = ProcessingCoordinator.class.getName() + ".handleAsynchronously";
    private static final String OP_EXECUTE_WORKER = ProcessingCoordinator.class.getName() + ".executeWorker";

    @NotNull private final RunningTask coordinatorTask;

    private final int threadsCount;
    private final boolean multithreaded;
    private final List<OperationResult> workerSpecificResults;
    private final RequestsBuffer<I> requestsBuffer;

    @NotNull private final TaskManager taskManager;

    /**
     * True if any worker requested the processing to be stopped.
     * Currently this is possible only by returning false from the {@link ItemProcessingRequest#process(RunningTask, OperationResult)} method.
     * *TODO Is that OK?*
     */
    private final AtomicBoolean stopRequestedByAnyWorker = new AtomicBoolean(false);

    /**
     * Set to true when no more items are expected to arrive into the queue.
     * It is informing worker threads that they can stop.
     */
    private final AtomicBoolean allItemsSubmitted = new AtomicBoolean(false);

    ProcessingCoordinator(@NotNull AbstractTaskHandler<?, ?> taskHandler, @NotNull RunningTask coordinatorTask) {
        this.logger = taskHandler.getLogger();
        this.coordinatorTask = coordinatorTask;
        this.taskManager = taskHandler.getTaskManager();

        threadsCount = getWorkerThreadsCount();
        if (threadsCount > 0) {
            multithreaded = true;
            workerSpecificResults = new ArrayList<>(threadsCount);
            requestsBuffer = new RequestsBuffer<>(threadsCount);
        } else {
            multithreaded = false;
            workerSpecificResults = null;
            requestsBuffer = null;
        }
    }

    public boolean submit(ItemProcessingRequest<I> request, OperationResult result) {
        if (!canRun()) {
            recordInterrupted(result);
            request.acknowledge(false, result);
            return false;
        }

        if (multithreaded) {
            try {
                while (!requestsBuffer.offer(request)) {
                    if (!canRun()) {
                        recordInterrupted(result);
                        request.acknowledge(false, result);
                        return false;
                    }
                }
            } catch (InterruptedException e) {
                recordInterrupted(result);
                request.acknowledge(false, result);
                return false;
            }

            // This is perhaps better than IN PROGRESS (e.g. because of tests).
            // The processing will continue in a separate thread.
            result.recordStatus(OperationResultStatus.SUCCESS, "Request submitted for processing");
            return true;
        } else {
            // In this case the coordinator task is the worker.
            return request.process(coordinatorTask, result);
        }
    }

    /**
     * Stop can be requested either internally (by handler or error in any worker thread)
     * or externally (by the task manager)
     */
    private boolean canRun() {
        return coordinatorTask.canRun() && !stopRequestedByAnyWorker.get();
    }

    /**
     * The above + checking also the worker task.
     */
    private boolean canRun(RunningTask workerTask) {
        return workerTask.canRun() && canRun();
    }

    private void recordInterrupted(OperationResult result) {
        result.recordStatus(OperationResultStatus.WARNING, "Could not submit request as the processing was interrupted");
        logger.warn("Processing was interrupted in {}", coordinatorTask);
    }

    void createWorkerTasks(@NotNull TaskReportingOptions reportingOptions) {
        if (threadsCount == 0) {
            return;
        }

        // remove subtasks that could have been created previously
        coordinatorTask.deleteLightweightAsynchronousSubtasks();

        for (int i = 0; i < threadsCount; i++) {
            // we intentionally do not put worker specific result under main operation result until the handler is done
            // (because of concurrency issues - adding subresults vs e.g. putting main result into the task)
            OperationResult workerSpecificResult = new OperationResult(OP_HANDLE_ASYNCHRONOUSLY);
            workerSpecificResult.addContext("subtaskIndex", i+1);
            workerSpecificResults.add(workerSpecificResult);

            RunningLightweightTask subtask = coordinatorTask.createSubtask(new WorkerHandler(workerSpecificResult));
            initializeStatisticsCollection(subtask, reportingOptions);
            subtask.setCategory(coordinatorTask.getCategory());
            subtask.setResult(new OperationResult(OP_EXECUTE_WORKER, OperationResultStatus.IN_PROGRESS, (String) null));
            subtask.setName("Worker thread " + (i+1) + " of " + threadsCount);
            subtask.setExecutionEnvironment(CloneUtil.clone(coordinatorTask.getExecutionEnvironment()));
            subtask.startLightweightHandler();
            logger.trace("Worker subtask {} created", subtask);
        }
    }

    private void initializeStatisticsCollection(RunningLightweightTask subtask, TaskReportingOptions reportingOptions) {
        subtask.resetIterativeTaskInformation(null);
        if (reportingOptions.isEnableSynchronizationStatistics()) {
            subtask.resetSynchronizationInformation(null);
        }
        if (reportingOptions.isEnableActionsExecutedStatistics()) {
            subtask.resetActionsExecutedInformation(null);
        }
        // Note we never maintain structured progress for LATs.
    }

    public boolean isMultithreaded() {
        return multithreaded;
    }

    /**
     * Tells the workers that they should not expect any more work and waits for their completion
     * (which can occur either because of queue is empty or because canRun is false).
     * Acknowledges any pending requests.
     */
    public void finishProcessing(OperationResult result) {
        logger.trace("ProcessingCoordinator: finishing processing. Coordinator task canRun = {}", coordinatorTask.canRun());

        allItemsSubmitted.set(true);
        waitForWorkersFinish(result);
        nackQueuedRequests(result);
    }

    private void waitForWorkersFinish(OperationResult result) {
        logger.debug("Waiting for workers to finish");
        taskManager.waitForTransientChildrenAndCloseThem(coordinatorTask, result);
        logger.debug("Waiting for workers to finish done");
    }

    private void nackQueuedRequests(OperationResult result) {
        if (multithreaded) {
            logger.trace("Acknowledging (release=false) all pending requests");
            int count = requestsBuffer.nackAllRequests(result);
            logger.trace("Acknowledged {} pending requests", count);
        }
    }

    private class WorkerHandler implements LightweightTaskHandler {
        private final OperationResult workerSpecificResult;

        private WorkerHandler(OperationResult workerSpecificResult) {
            this.workerSpecificResult = workerSpecificResult;
        }

        @Override
        public void run(RunningLightweightTask workerTask) {

            assert multithreaded;
            assert requestsBuffer != null;

            // temporary hack: how to see thread name for this task
            workerTask.setName(workerTask.getName().getOrig() + " (" + Thread.currentThread().getName() + ")");
            workerSpecificResult.addArbitraryObjectAsContext("subtaskName", workerTask.getName());

            String taskIdentifier = workerTask.getTaskIdentifier();

            while (canRun(workerTask)) {

                workerTask.refreshThreadLocalStatistics();
                ItemProcessingRequest<I> request = requestsBuffer.poll(taskIdentifier);

                if (request != null) {
                    try {
                        if (!request.process(workerTask, workerSpecificResult)) {
                            stopRequestedByAnyWorker.set(true);
                        }
                    } finally {
                        requestsBuffer.markProcessed(request, taskIdentifier);
                        treatOperationResultAfterOperation();
                        workerTask.setProgress(workerTask.getProgress() + 1);
                    }
                } else {
                    if (allItemsSubmitted.get()) {
                        logger.trace("Queue is empty and nothing more is expected - exiting");
                        break;
                    } else {
                        logger.trace("No requests to be processed but expecting some to come. Waiting for {} msec", WORKER_THREAD_WAIT_FOR_REQUEST);
                        try {
                            //noinspection BusyWait
                            Thread.sleep(WORKER_THREAD_WAIT_FOR_REQUEST);
                        } catch (InterruptedException e) {
                            logger.trace("Waiting interrupted, exiting");
                            break;
                        }
                    }
                }
            }

            int reservedRequests = requestsBuffer.getReservedRequestsCount(taskIdentifier);
            if (reservedRequests > 0) {
                logger.warn("Worker task exiting but it has {} reserved (pre-assigned) change requests", reservedRequests);
            }
            workerTask.refreshThreadLocalStatistics();
        }

        private void treatOperationResultAfterOperation() {
            workerSpecificResult.computeStatus(true);
            // We do NOT try to summarize/cleanup the whole results hierarchy.
            // There could be some accesses to the request's subresult from the thread that originated it.
            workerSpecificResult.summarize(false);
            workerSpecificResult.cleanupResult();
        }
    }

    private int getWorkerThreadsCount() {
        PrismProperty<Integer> workerThreadsPrismProperty = coordinatorTask
                .getExtensionPropertyOrClone(SchemaConstants.MODEL_EXTENSION_WORKER_THREADS);
        if (workerThreadsPrismProperty != null && workerThreadsPrismProperty.getRealValue() != null) {
            return defaultIfNull(workerThreadsPrismProperty.getRealValue(), 0);
        } else {
            return 0;
        }
    }

    // TODO decide on this
    void updateOperationResult(OperationResult opResult) {
        if (multithreaded) {
            assert workerSpecificResults != null;
            for (OperationResult workerSpecificResult : workerSpecificResults) {
                workerSpecificResult.computeStatus();
                workerSpecificResult.summarize(true);
                workerSpecificResult.cleanupResultDeeply();
                opResult.addSubresult(workerSpecificResult);
            }
            // In single-threaded case the status should be already computed
            opResult.computeStatus("Issues during processing");
        }
    }
}
