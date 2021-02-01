/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync.tasks;

import javax.annotation.PostConstruct;

import com.evolveum.midpoint.model.impl.tasks.AbstractModelTaskHandler;
import com.evolveum.midpoint.provisioning.api.*;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.schema.constants.Channel;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.sync.tasks.SyncTaskHelper.TargetInfo;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;

/**
 * The task handler for a live synchronization.
 *
 * This handler takes care of executing live synchronization "runs". It means that the handler "run" method will
 * be called every few seconds. The responsibility is to scan for changes that happened since the last run.
 *
 * @author Radovan Semancik
 */
@Component
@TaskExecutionClass(LiveSyncTaskHandler.TaskExecution.class)
@PartExecutionClass(LiveSyncTaskHandler.PartExecution.class)
public class LiveSyncTaskHandler
        extends AbstractModelTaskHandler<LiveSyncTaskHandler, LiveSyncTaskHandler.TaskExecution> {

    public static final String HANDLER_URI = ModelConstants.NS_SYNCHRONIZATION_TASK_PREFIX + "/live-sync/handler-3";

    private static final Trace LOGGER = TraceManager.getTrace(LiveSyncTaskHandler.class);
    private static final String CONTEXT = "Live Sync";

    /**
     * Local sequence number of a change that is being processed in the current thread.
     * Actually, it is a hack to enable testing: The code in mappings can obtain this
     * information and do some asserts on it. When the information will be propagated into
     * e.g. lensContext, we should remove this hack.
     */
    @VisibleForTesting
    public static final ThreadLocal<Integer> CHANGE_BEING_PROCESSED = new ThreadLocal<>();

    protected LiveSyncTaskHandler() {
        super(LOGGER, "Live sync", OperationConstants.LIVE_SYNC);
        reportingOptions.setEnableSynchronizationStatistics(true);
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.LIVE_SYNCHRONIZATION;
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_LIVE_SYNC_TASK.value();
    }

    @Override
    public String getDefaultChannel() {
        return Channel.LIVE_SYNC.getUri();
    }

    public class TaskExecution extends AbstractTaskExecution<LiveSyncTaskHandler, TaskExecution> {

        private TargetInfo targetInfo;
        private SynchronizationResult syncResult;

        public TaskExecution(RunningTask localCoordinatorTask, WorkBucketType workBucket,
                TaskPartitionDefinitionType partDefinition, TaskWorkBucketProcessingResult previousRunResult) {
            super(LiveSyncTaskHandler.this, localCoordinatorTask, workBucket, partDefinition, previousRunResult);
        }

        @Override
        protected void initialize(OperationResult opResult)
                throws TaskException, CommunicationException, SchemaException, ConfigurationException, ObjectNotFoundException,
                SecurityViolationException, ExpressionEvaluationException {
            super.initialize(opResult);

            targetInfo = syncTaskHelper.getTargetInfo(LOGGER, localCoordinatorTask, opResult, CONTEXT);
        }

        @Override
        protected void finish(OperationResult opResult, Throwable t) throws TaskException, SchemaException {
            LOGGER.trace("LiveSyncTaskHandler.run stopping (resource {}); changes processed: {}", targetInfo.resource, syncResult);
            opResult.createSubresult(OperationConstants.LIVE_SYNC_STATISTICS)
                    .recordStatus(OperationResultStatus.SUCCESS, "Changes processed: " + syncResult);
        }
    }

    @ItemProcessorClass(PartExecution.ItemProcessor.class)
    public class PartExecution extends AbstractIterativeTaskPartExecution
            <LiveSyncEvent, LiveSyncTaskHandler, TaskExecution, PartExecution, PartExecution.ItemProcessor> {

        private final ErrorHandlingStrategyExecutor errorHandlingStrategyExecutor;

        public PartExecution(@NotNull TaskExecution taskExecution) {
            super(taskExecution);
            errorHandlingStrategyExecutor = new ErrorHandlingStrategyExecutor(taskExecution.localCoordinatorTask,
                    prismContext, repositoryService);
        }

        @Override
        protected void processItems(OperationResult opResult)
                throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
                ConfigurationException, ExpressionEvaluationException, PolicyViolationException, PreconditionViolationException {

            LiveSyncEventHandler handler = new LiveSyncEventHandler() {
                @Override
                public boolean handle(LiveSyncEvent event, OperationResult opResult) {
                    SyncItemProcessingRequest<LiveSyncEvent> request = new SyncItemProcessingRequest<>(event, itemProcessor);
                    return coordinator.submit(request, opResult);
                }

                @Override
                public void allEventsSubmitted(OperationResult result) {
                    coordinator.finishProcessing(result);
                }
            };

            ModelImplUtils.clearRequestee(localCoordinatorTask);
            taskExecution.syncResult = provisioningService.synchronize(taskExecution.targetInfo.coords,
                    localCoordinatorTask, taskExecution.partDefinition, handler, opResult);
        }

        @Override
        public boolean providesTracingAndDynamicProfiling() {
            return true;
        }

        public class ItemProcessor extends AbstractIterativeItemProcessor
                <LiveSyncEvent, LiveSyncTaskHandler, TaskExecution, PartExecution, ItemProcessor> {

            public ItemProcessor() {
                super(PartExecution.this);
            }

            @Override
            public boolean process(ItemProcessingRequest<LiveSyncEvent> request, RunningTask workerTask, OperationResult result)
                    throws CommonException, PreconditionViolationException {
                LiveSyncEvent event = request.getItem();

                CHANGE_BEING_PROCESSED.set(event.getSequentialNumber());
                try {
                    if (event.isComplete()) {
                        changeNotificationDispatcher.notifyChange(event.getChangeDescription(), workerTask, result);
                    } else if (event.isSkip()) {
                        result.recordNotApplicable();
                    } else {
                        // TODO error criticality
                        assert event.isError();
                        result.recordFatalError("Item was not pre-processed correctly: " + event.getErrorMessage());
                    }
                    return true;
                } finally {
                    CHANGE_BEING_PROCESSED.remove();
                }
            }
        }

        @Override
        public boolean getContinueOnError(OperationResultStatus status, @NotNull Throwable exception, ItemProcessingRequest<?> request, OperationResult result) {
            // TODO generalize for all tasks
            // TODO provide the exception
            String shadowOid = getShadowOid(request);
            ErrorHandlingStrategyExecutor.Action action = errorHandlingStrategyExecutor.determineAction(exception, status, shadowOid, result);
            switch (action) {
                case CONTINUE:
                    return true;
                case SUSPEND:
                    taskExecution.setPermanentErrorEncountered(exception);
                case STOP:
                default:
                    return false;
            }
        }

        // FIXME
        private String getShadowOid(ItemProcessingRequest<?> request) {
            Object item = request.getItem();
            if (item instanceof LiveSyncEvent) {
                LiveSyncEvent event = (LiveSyncEvent) item;
                return event.getShadowOid();
            } else {
                throw new IllegalStateException("Expected LiveSyncEvent, got " + item);
            }
        }
    }
}
