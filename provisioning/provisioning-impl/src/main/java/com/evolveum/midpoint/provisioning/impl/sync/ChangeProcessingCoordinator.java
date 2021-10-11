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
import java.util.concurrent.*;
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

    private static final long WORKER_THREAD_WAIT_FOR_REQUEST = 500L;
    private static final long REQUEST_QUEUE_OFFER_TIMEOUT = 1000L;

    private static final String OP_HANDLE_ASYNCHRONOUSLY = ChangeProcessingCoordinator.class.getName() + ".handleAsynchronously";

    @NotNull private final Supplier<Boolean> canRunSupplier;
    @NotNull private final ChangeProcessor changeProcessor;
    @NotNull private final Task coordinatorTask;
    @Nullable private final TaskPartitionDefinitionType taskPartition;

    private final boolean multithreaded;
    private final List<OperationResult> workerSpecificResults;
    private final BlockingQueue<ProcessChangeRequest> waitingRequestsQueue;
    private final AffinityController affinityController;

    private volatile boolean allItemsSubmitted;

    ChangeProcessingCoordinator(@NotNull Supplier<Boolean> canRunSupplier, @NotNull ChangeProcessor changeProcessor,
            @NotNull Task coordinatorTask, @Nullable TaskPartitionDefinitionType taskPartition) {
        this.canRunSupplier = canRunSupplier;
        this.changeProcessor = changeProcessor;
        this.coordinatorTask = coordinatorTask;
        this.taskPartition = taskPartition;

        int threadsCount = getWorkerThreadsCount();
        if (threadsCount > 0) {
            int queueSize = threadsCount*2;                // actually, size of threadsCount should be sufficient but it doesn't hurt if queue is larger
            multithreaded = true;
            waitingRequestsQueue = new ArrayBlockingQueue<>(queueSize);
            workerSpecificResults = new ArrayList<>(threadsCount);
            affinityController = new AffinityController();
            createWorkerTasks(threadsCount);
        } else {
            multithreaded = false;
            waitingRequestsQueue = null;
            workerSpecificResults = null;
            affinityController = null;
        }
    }

    public void submit(ProcessChangeRequest request, OperationResult result) throws InterruptedException {
        if (multithreaded) {
            while (!waitingRequestsQueue.offer(request, REQUEST_QUEUE_OFFER_TIMEOUT, TimeUnit.MILLISECONDS)) {
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
        private OperationResult workerSpecificResult;

        private WorkerHandler(OperationResult workerSpecificResult) {
            this.workerSpecificResult = workerSpecificResult;
        }

        @Override
        public void run(RunningTask workerTask) {

            assert multithreaded;
            assert waitingRequestsQueue != null;
            assert affinityController != null;

            // temporary hack: how to see thread name for this task
            workerTask.setName(workerTask.getName().getOrig() + " (" + Thread.currentThread().getName() + ")");
            workerSpecificResult.addArbitraryObjectAsContext("subtaskName", workerTask.getName());

            while (workerTask.canRun() && canRunSupplier.get()) {
                workerTask.refreshLowLevelStatistics();
                ProcessChangeRequest preAssigned = affinityController.getAssigned(workerTask.getTaskIdentifier());
                ProcessChangeRequest request;
                if (preAssigned != null) {
                    LOGGER.trace("Got pre-assigned request {}", preAssigned);
                    request = preAssigned;
                } else {
                    try {
                        request = waitingRequestsQueue.poll(WORKER_THREAD_WAIT_FOR_REQUEST, TimeUnit.MILLISECONDS);
                        LOGGER.trace("Got request {}", request);
                    } catch (InterruptedException e) {
                        LOGGER.trace("Interrupted when waiting for next request", e);
                        workerTask.refreshLowLevelStatistics();
                        break;
                    }
                }
                workerTask.refreshLowLevelStatistics();
                if (request != null) {
                    if (!affinityController.bind(workerTask.getTaskIdentifier(), request)) {
                        continue;
                    }
                    try {
                        changeProcessor.execute(request, workerTask, coordinatorTask, taskPartition, workerSpecificResult);
                    } finally {
                        request.setDone(true);          // probably set already -- but better twice than not at all
                        affinityController.unbind(workerTask.getTaskIdentifier(), request);

                        workerSpecificResult.computeStatus(true);
                        // We do NOT try to summarize/cleanup the whole results hierarchy.
                        // There could be some accesses to the request's subresult from the thread that originated it.
                        workerSpecificResult.summarize(false);
                        workerSpecificResult.cleanupResult();
                    }
                } else {
                    if (allItemsSubmitted) {
                        LOGGER.trace("queue is empty and nothing more is expected - exiting");
                        break;
                    }
                }
            }
            int assigned = affinityController.hasAssigned(workerTask.getTaskIdentifier());
            if (assigned > 0) {
                LOGGER.warn("Worker task exiting but it has {} change requests assigned", assigned);
            }
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
