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

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired private TaskManager taskManager;
    @Autowired private ProvisioningService provisioningService;
    @Autowired private ResourceObjectChangeListener resourceObjectChangeListener;
    @Autowired private SyncTaskHelper helper;

    private static final Trace LOGGER = TraceManager.getTrace(LiveSyncTaskHandler.class);
    private static final String CONTEXT = "Live Sync";

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

        public TaskExecution(LiveSyncTaskHandler taskHandler, RunningTask localCoordinatorTask, WorkBucketType workBucket,
                TaskPartitionDefinitionType partDefinition, TaskWorkBucketProcessingResult previousRunResult) {
            super(taskHandler, localCoordinatorTask, workBucket, partDefinition, previousRunResult);
        }

        @Override
        protected void initialize(OperationResult opResult)
                throws TaskException, CommunicationException, SchemaException, ConfigurationException, ObjectNotFoundException,
                SecurityViolationException, ExpressionEvaluationException {
            super.initialize(opResult);

            targetInfo = helper.getTargetInfo(LOGGER, localCoordinatorTask, opResult, CONTEXT);
            LOGGER.trace("Task target: {}", targetInfo);
        }

        @Override
        protected void finish(OperationResult opResult, Throwable t) throws TaskException, SchemaException {
            LOGGER.trace("LiveSyncTaskHandler.run stopping (resource {}); changes processed: {}", targetInfo.resource, syncResult);
            opResult.createSubresult(OperationConstants.LIVE_SYNC_STATISTICS)
                    .recordStatus(OperationResultStatus.SUCCESS, "Changes processed: " + syncResult);
        }
    }

    public class PartExecution extends AbstractIterativeTaskPartExecution
            <LiveSyncEvent, LiveSyncTaskHandler, TaskExecution, PartExecution, ItemProcessor> {

        PartExecution(@NotNull TaskExecution taskExecution) {
            super(taskExecution);
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
                public void allEventsSubmitted() {
                    coordinator.allItemsSubmitted();
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
    }

    public class ItemProcessor extends AbstractIterativeItemProcessor
            <LiveSyncEvent, LiveSyncTaskHandler, TaskExecution, PartExecution, ItemProcessor> {

        protected ItemProcessor(@NotNull PartExecution partExecution) {
            super(partExecution);
        }

        @Override
        public boolean process(ItemProcessingRequest<LiveSyncEvent> request, RunningTask workerTask, OperationResult result)
                throws CommonException, PreconditionViolationException {
            if (request.getItem().isComplete()) {
                resourceObjectChangeListener.notifyChange(request.getItem().getChangeDescription(), workerTask, result);
            } else {
                // TODO
                result.recordFatalError("Item was not pre-processed correctly");
            }
            return true;
        }
    }
}
