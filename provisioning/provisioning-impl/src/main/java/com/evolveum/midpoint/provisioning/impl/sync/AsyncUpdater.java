/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.sync;

import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.provisioning.impl.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.ucf.api.async.ChangeListener;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Implements Async Update functionality. (Currently not much, but this might change as we'll implement multi-threading.
 * Then we'll maybe find some code common with LiveSynchronizer.)
 */
@Component
public class AsyncUpdater {

    @SuppressWarnings("unused")
    private static final Trace LOGGER = TraceManager.getTrace(AsyncUpdater.class);

    private static final long WAIT_FOR_REQUEST_COMPLETION = 10000L;

    @Autowired private ProvisioningContextFactory ctxFactory;
    @Autowired private ResourceObjectConverter resourceObjectConverter;
    @Autowired private ChangeProcessor changeProcessor;

    public void processAsynchronousUpdates(ResourceShadowDiscriminator shadowCoordinates, Task callerTask, OperationResult callerResult)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            ExpressionEvaluationException {
        InternalMonitor.recordCount(InternalCounters.PROVISIONING_ALL_EXT_OPERATION_COUNT);

        ProvisioningContext globalContext = ctxFactory.create(shadowCoordinates, callerTask, callerResult);

        ChangeProcessingCoordinator coordinator = new ChangeProcessingCoordinator(globalContext::canRun, changeProcessor,
                callerTask, null);

        ChangeListener listener = (change, listenerTask, listenerResult) -> {
            /*
             * This code can execute in arbitrary thread. It can be the caller one (e.g. for passive sources)
             * or provider-created one (e.g. for AMQP client library).
             *
             * But we need to execute the requests in the context of the caller task or its working threads (LATs).
             * This is necessary e.g. to correctly report low-level statistics that are stored in thread-local structures.
             */
            ProcessChangeRequest request = new ProcessChangeRequest(change, globalContext, false) {
                @Override
                public void setDone(boolean done) {
                    super.setDone(done);
                    synchronized (this) {
                        notifyAll();
                    }
                }

                @Override
                public void onCompletion(@NotNull Task workerTask, Task coordinatorTask, @NotNull OperationResult result) {
                    if (workerTask instanceof RunningTask) {
                        ((RunningTask) workerTask).incrementProgressAndStoreStatsIfNeeded();

                        if (coordinatorTask instanceof RunningTask) {
                            //noinspection SynchronizationOnLocalVariableOrMethodParameter
                            synchronized (coordinatorTask) {
                                // TODO factor out progress computation to RunningTaskQuartzImpl
                                Collection<? extends RunningTask> subtasks = ((RunningTask) coordinatorTask)
                                        .getLightweightAsynchronousSubtasks();
                                long totalProgress = 0;
                                for (RunningTask subtask : subtasks) {
                                    totalProgress += subtask.getProgress();
                                }
                                coordinatorTask.setProgress(totalProgress);

                                // todo report current op result?
                                // FIXME this probably should not be called from the worker task! Or can it be?
                                ((RunningTask) coordinatorTask).storeOperationStatsIfNeeded();  // includes flushPendingModifications
                            }
                        }
                    }
                }
            };

            /*
             * IMPORTANT! Do not manipulate with coordinator nor worker tasks in this method. This code is executed in
             * a more or less random thread. Use overridden methods in the request object.
             */

            try {
                /*
                 * Let us submit the request for processing. We assume there are working threads set for the task, so
                 * the request will be processed asynchronously - in some of the workers.
                 *
                 * Note that even if this method works synchronously (i.e. there are no working threads configured for the task),
                 * it's not a big problem: the whole execution will occur in the context of wrong thread. So the reporting
                 * will not be accurate. But there should be no other negative effects.
                 */
                LOGGER.trace("Submitting request for processing: {}", request);
                coordinator.submit(request, listenerResult);

                /*
                 * Let's wait for the request completion.
                 */
                LOGGER.trace("Waiting for the request to be done: {}", request);
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (request) {
                    while (!request.isDone()) {
                        request.wait(WAIT_FOR_REQUEST_COMPLETION);
                    }
                }
                LOGGER.trace("Request done: {}", request);
            } catch (InterruptedException e) {
                LOGGER.warn("Execution was interrupted in {} (caller task: {})", listenerTask, callerTask);
                return false;
            }
            return request.isSuccess();
        };
        resourceObjectConverter.listenForAsynchronousUpdates(globalContext, listener, callerResult);

        // We expect no more messages (either we got the last one, or the task is going down, or whatever).
        // So we want the worker threads to stop.
        coordinator.setAllItemsSubmitted();
    }
}
