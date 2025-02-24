/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.evolveum.midpoint.repo.common.activity.run.IterativeActivityRun;
import com.evolveum.midpoint.task.api.RunningLightweightTask;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.LightweightTaskHandler;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.logging.Trace;

/**
 * Responsible for distributing instances of {@link ItemProcessingRequest} to individual worker threads.
 */
public class ProcessingCoordinator<I> {

    private static final Trace LOGGER = TraceManager.getTrace(ProcessingCoordinator.class);

    private static final long WORKER_THREAD_WAIT_FOR_REQUEST = 100L;

    private static final String OP_HANDLE_ASYNCHRONOUSLY = ProcessingCoordinator.class.getName() + ".handleAsynchronously";
    private static final String OP_EXECUTE_WORKER = ProcessingCoordinator.class.getName() + ".executeWorker";
    private static final String OP_SUBMIT_ITEM = ProcessingCoordinator.class.getName() + ".submitItem";

    @NotNull private final RunningTask coordinatorTask;

    private final int threadsCount;
    private final boolean multithreaded;
    private final List<OperationResult> workerSpecificResults;
    private final RequestsBuffer<I> requestsBuffer;

    @NotNull private final IterativeActivityRun<I, ?, ?, ?> activityRun;

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

    public ProcessingCoordinator(int threadsCount, @NotNull IterativeActivityRun<I, ?, ?, ?> activityRun) {
        this.coordinatorTask = activityRun.getRunningTask();
        this.activityRun = activityRun;

        this.threadsCount = threadsCount;
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

    public boolean submit(ItemProcessingRequest<I> request, OperationResult parentResult) {
        // For single-threaded case, this is is only a thin wrapper around request.process(..) method.
        // But for the multi-threaded case, the coordinator thread can spend some time here, waiting for
        // the request buffer to accept the request. Hence, it makes sense to provide an operation result here.
        OperationResult result = parentResult.subresult(OP_SUBMIT_ITEM)
                .addParam("requestIdentifier", request.getIdentifier())
                .build();
        try {
            if (!canRun()) {
                recordInterrupted(request, result);
                request.acknowledge(false, result);
                return false;
            }

            if (multithreaded) {
                assert requestsBuffer != null;
                try {
                    while (!requestsBuffer.offer(request)) {
                        if (!canRun()) {
                            recordInterrupted(request, result);
                            request.acknowledge(false, result);
                            return false;
                        } else {
                            updateCoordinatorTaskStatistics(result);
                        }
                    }
                    updateCoordinatorTaskStatistics(result);
                } catch (InterruptedException e) {
                    recordInterrupted(request, result);
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
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    /**
     * This method updates coordinator task statistics. It's here to ensure regular update
     * even in cases when item processing takes too long, so the update in {@link ItemProcessingGatekeeper}
     * is not invoked for significant time.
     */
    private void updateCoordinatorTaskStatistics(OperationResult result) {
        try {
            activityRun.updateStatistics(true, result);
        } catch (SchemaException | ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Couldn't update task statistics for {}", e, activityRun);
            // ignoring the exception
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

    private void recordInterrupted(ItemProcessingRequest<I> request, OperationResult result) {
        result.recordStatus(OperationResultStatus.WARNING, "Could not submit request as the processing was interrupted");
        LOGGER.warn("Processing was interrupted while processing {} in {}", request, coordinatorTask);
    }

    public void createWorkerThreads() throws ConfigurationException {
        if (threadsCount == 0) {
            return;
        }
        assert workerSpecificResults != null;

        // remove subtasks that could have been created previously
        coordinatorTask.deleteLightweightAsynchronousSubtasks();

        for (int i = 0; i < threadsCount; i++) {
            // we intentionally do not put worker specific result under main operation result until the handler is done
            // (because of concurrency issues - adding subresults vs e.g. putting main result into the task)
            OperationResult workerSpecificResult = new OperationResult(OP_HANDLE_ASYNCHRONOUSLY);
            workerSpecificResult.addContext("subtaskIndex", i+1);
            workerSpecificResults.add(workerSpecificResult);

            RunningLightweightTask subtask = coordinatorTask.createSubtask(new WorkerHandler(workerSpecificResult));
            subtask.setResult(new OperationResult(OP_EXECUTE_WORKER, OperationResultStatus.IN_PROGRESS, (String) null));
            subtask.setName("Worker thread " + (i+1) + " of " + threadsCount);
            subtask.setExecutionEnvironment(CloneUtil.clone(coordinatorTask.getExecutionEnvironment()));
            subtask.setExecutionMode(activityRun.getTaskExecutionMode());
            subtask.startLightweightHandler();
            LOGGER.trace("Worker subtask {} created", subtask);
        }
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
        LOGGER.trace("ProcessingCoordinator: finishing processing. Coordinator task canRun = {}", coordinatorTask.canRun());

        allItemsSubmitted.set(true);
        waitForWorkersFinish(result);
        nackQueuedRequests(result);
    }

    private void waitForWorkersFinish(OperationResult result) {
        LOGGER.debug("Waiting for workers to finish");
        activityRun.getBeans().taskManager
                .waitForTransientChildrenAndCloseThem(coordinatorTask, result);
        LOGGER.debug("Waiting for workers to finish done");
    }

    private void nackQueuedRequests(OperationResult result) {
        if (multithreaded) {
            assert requestsBuffer != null;
            LOGGER.trace("Acknowledging (release=false) all pending requests");
            int count = requestsBuffer.nackAllRequests(result);
            LOGGER.trace("Acknowledged {} pending requests", count);
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
                    }
                } else {
                    if (allItemsSubmitted.get()) {
                        LOGGER.trace("Queue is empty and nothing more is expected - exiting");
                        break;
                    } else {
                        LOGGER.trace("No requests to be processed but expecting some to come. Waiting for {} msecs",
                                WORKER_THREAD_WAIT_FOR_REQUEST);
                        try {
                            //noinspection BusyWait
                            Thread.sleep(WORKER_THREAD_WAIT_FOR_REQUEST);
                        } catch (InterruptedException e) {
                            LOGGER.trace("Waiting interrupted, exiting");
                            break;
                        }
                    }
                }
            }

            int reservedRequests = requestsBuffer.getReservedRequestsCount(taskIdentifier);
            if (reservedRequests > 0) {
                LOGGER.warn("Worker task exiting but it has {} reserved (pre-assigned) change requests", reservedRequests);
            }
            workerTask.refreshThreadLocalStatistics();
        }

        private void treatOperationResultAfterOperation() {
            workerSpecificResult.computeStatus(true);
            // We do NOT try to summarize/cleanup the whole results hierarchy.
            // There could be some accesses to the request's subresult from the thread that originated it.
            workerSpecificResult.summarize(false);
            workerSpecificResult.cleanup();
        }
    }
}
