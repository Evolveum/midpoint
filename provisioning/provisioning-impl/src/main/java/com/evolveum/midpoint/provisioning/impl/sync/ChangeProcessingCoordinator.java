/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.sync;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.LightweightTaskHandler;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 *  Coordinates processing of changes in single- or multi-threaded environment. This cannot be a singleton, because it contains
 *  operation-specific state.
 *
 *  Functionally it is a bit similar to multi-threading support in iterative task handler(s).
 */
public class ChangeProcessingCoordinator {

    private static final Trace LOGGER = TraceManager.getTrace(ChangeProcessingCoordinator.class);

    private static final long WORKER_THREAD_WAIT_FOR_REQUEST = 100L;

    private static final String OP_HANDLE_ASYNCHRONOUSLY = ChangeProcessingCoordinator.class.getName() + ".handleAsynchronously";

    @NotNull private final Supplier<Boolean> canRunSupplier;
    @NotNull private final ChangeProcessor changeProcessor;
    @NotNull private final Task coordinatorTask;
    @Nullable private final TaskPartitionDefinitionType taskPartition;

    private final boolean multithreaded;
    private final List<OperationResult> workerSpecificResults;
    private final RequestsBuffer requestsBuffer;

    private volatile boolean allItemsSubmitted;

    ChangeProcessingCoordinator(@NotNull Supplier<Boolean> canRunSupplier, @NotNull ChangeProcessor changeProcessor,
            @NotNull Task coordinatorTask, @Nullable TaskPartitionDefinitionType taskPartition) {
        this.canRunSupplier = canRunSupplier;
        this.changeProcessor = changeProcessor;
        this.coordinatorTask = coordinatorTask;
        this.taskPartition = taskPartition;

        int threadsCount = getWorkerThreadsCount();
        if (threadsCount > 0) {
            multithreaded = true;
            workerSpecificResults = new ArrayList<>(threadsCount);
            requestsBuffer = new RequestsBuffer(threadsCount);
            createWorkerTasks(threadsCount);
        } else {
            multithreaded = false;
            workerSpecificResults = null;
            requestsBuffer = null;
        }
    }

    public void submit(ProcessChangeRequest request, OperationResult result) throws InterruptedException {
        if (multithreaded) {
            while (!requestsBuffer.offer(request)) {
                if (!canRunSupplier.get()) {
                    result.recordStatus(OperationResultStatus.WARNING, "Could not submit request as the processing was interrupted");
                    return;
                }
            }
            // This is perhaps better than IN PROGRESS (e.g. because of tests).
            // The processing will continue in a separate thread.
            result.recordStatus(OperationResultStatus.SUCCESS, "Request submitted for processing");
        } else {
            changeProcessor.execute(request, coordinatorTask, null, taskPartition, result);
        }
    }

    private void createWorkerTasks(int threadsCount) {

        RunningTask runningCoordinatorTask = (RunningTask) coordinatorTask;

        // remove subtasks that could have been created previously
        runningCoordinatorTask.deleteLightweightAsynchronousSubtasks();

        for (int i = 0; i < threadsCount; i++) {
            // we intentionally do not put worker specific result under main operation result until the handler is done
            // (because of concurrency issues - adding subresults vs e.g. putting main result into the task)
            OperationResult workerSpecificResult = new OperationResult(OP_HANDLE_ASYNCHRONOUSLY);
            workerSpecificResult.addContext("subtaskIndex", i+1);
            workerSpecificResults.add(workerSpecificResult);

            RunningTask subtask = runningCoordinatorTask.createSubtask(new WorkerHandler(workerSpecificResult));
            subtask.resetIterativeTaskInformation(null);
            subtask.resetSynchronizationInformation(null);
            subtask.resetActionsExecutedInformation(null);
            subtask.setCategory(runningCoordinatorTask.getCategory());
            subtask.setResult(new OperationResult(ChangeProcessingCoordinator.class.getName() + ".executeWorker", OperationResultStatus.IN_PROGRESS, (String) null));
            subtask.setName("Worker thread " + (i+1) + " of " + threadsCount);
            subtask.setExecutionEnvironment(CloneUtil.clone(coordinatorTask.getExecutionEnvironment()));
            subtask.startLightweightHandler();
            LOGGER.trace("Worker subtask {} created", subtask);
        }
    }

    private class WorkerHandler implements LightweightTaskHandler {
        private final OperationResult workerSpecificResult;

        private WorkerHandler(OperationResult workerSpecificResult) {
            this.workerSpecificResult = workerSpecificResult;
        }

        @Override
        public void run(RunningTask workerTask) {

            assert multithreaded;
            assert requestsBuffer != null;

            // temporary hack: how to see thread name for this task
            workerTask.setName(workerTask.getName().getOrig() + " (" + Thread.currentThread().getName() + ")");
            workerSpecificResult.addArbitraryObjectAsContext("subtaskName", workerTask.getName());

            String taskIdentifier = workerTask.getTaskIdentifier();

            while (workerTask.canRun() && canRunSupplier.get()) {

                workerTask.refreshLowLevelStatistics();
                ProcessChangeRequest request = requestsBuffer.poll(taskIdentifier);

                if (request != null) {
                    try {
                        changeProcessor.execute(request, workerTask, coordinatorTask, taskPartition, workerSpecificResult);
                    } finally {
                        request.setDone(true); // probably set already -- but better twice than not at all
                        requestsBuffer.markProcessed(request, taskIdentifier);
                        treatOperationResultAfterOperation();
                    }
                } else {
                    if (allItemsSubmitted) {
                        LOGGER.trace("Queue is empty and nothing more is expected - exiting");
                        break;
                    } else {
                        LOGGER.trace("No requests to be processed but expecting some to come. Waiting for {} msec", WORKER_THREAD_WAIT_FOR_REQUEST);
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
            workerTask.refreshLowLevelStatistics();
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
        if (coordinatorTask instanceof RunningTask && workerThreadsPrismProperty != null && workerThreadsPrismProperty.getRealValue() != null) {
            return defaultIfNull(workerThreadsPrismProperty.getRealValue(), 0);
        } else {
            return 0;
        }
    }

    void setAllItemsSubmitted() {
        this.allItemsSubmitted = true;
    }

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
