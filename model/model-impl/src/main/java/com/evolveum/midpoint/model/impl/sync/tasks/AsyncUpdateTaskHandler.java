/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks;

import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.sync.tasks.SyncTaskHelper.TargetInfo;
import com.evolveum.midpoint.model.impl.tasks.AbstractModelTaskHandler;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.provisioning.api.*;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.schema.constants.Channel;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Task handler for controlled processing of asynchronous updates.
 */
@Component
@TaskExecutionClass(AsyncUpdateTaskHandler.TaskExecution.class)
@PartExecutionClass(AsyncUpdateTaskHandler.PartExecution.class)
public class AsyncUpdateTaskHandler
        extends AbstractModelTaskHandler<AsyncUpdateTaskHandler, AsyncUpdateTaskHandler.TaskExecution> {

    private static final Trace LOGGER = TraceManager.getTrace(AsyncUpdateTaskHandler.class);

    public static final String HANDLER_URI = ModelConstants.NS_SYNCHRONIZATION_TASK_PREFIX + "/async-update/handler-3";

    private static final String CONTEXT = "Async Update";

    protected AsyncUpdateTaskHandler() {
        super(LOGGER, CONTEXT, OperationConstants.ASYNC_UPDATE);
        reportingOptions.setEnableSynchronizationStatistics(true);
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    public class TaskExecution extends AbstractTaskExecution<AsyncUpdateTaskHandler, TaskExecution> {

        private TargetInfo targetInfo;

        public TaskExecution(RunningTask localCoordinatorTask, WorkBucketType workBucket,
                TaskPartitionDefinitionType partDefinition, TaskWorkBucketProcessingResult previousRunResult) {
            super(AsyncUpdateTaskHandler.this, localCoordinatorTask, workBucket, partDefinition, previousRunResult);
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
            opResult.createSubresult(OperationConstants.LIVE_SYNC_STATISTICS).recordStatus(OperationResultStatus.SUCCESS, "TODO");
        }
    }

    @ItemProcessorClass(PartExecution.ItemProcessor.class)
    public class PartExecution extends AbstractIterativeTaskPartExecution
            <AsyncUpdateEvent, AsyncUpdateTaskHandler, TaskExecution, PartExecution, PartExecution.ItemProcessor> {

        public PartExecution(@NotNull AsyncUpdateTaskHandler.TaskExecution taskExecution) {
            super(taskExecution);
        }

        @Override
        public boolean providesTracingAndDynamicProfiling() {
            return true;
        }

        @Override
        protected void processItems(OperationResult opResult)
                throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
                ConfigurationException, ExpressionEvaluationException, PolicyViolationException, PreconditionViolationException {

            AsyncUpdateEventHandler handler = (event, hResult) -> {
                SyncItemProcessingRequest<AsyncUpdateEvent> request = new SyncItemProcessingRequest<>(event, itemProcessor);
                return coordinator.submit(request, hResult);
            };

            ModelImplUtils.clearRequestee(localCoordinatorTask);
            provisioningService.processAsynchronousUpdates(taskExecution.targetInfo.coords,
                    handler, localCoordinatorTask, opResult);
        }

        public class ItemProcessor extends AbstractIterativeItemProcessor
                <AsyncUpdateEvent,
                        AsyncUpdateTaskHandler,
                        AsyncUpdateTaskHandler.TaskExecution,
                        AsyncUpdateTaskHandler.PartExecution,
                        ItemProcessor> {

            public ItemProcessor() {
                super(PartExecution.this);
            }

            @Override
            public boolean process(ItemProcessingRequest<AsyncUpdateEvent> request, RunningTask workerTask,
                    OperationResult result) throws CommonException, PreconditionViolationException {
                AsyncUpdateEvent event = request.getItem();
                if (event.isComplete()) {
                    ResourceObjectShadowChangeDescription changeDescription = event.getChangeDescription();
                    changeDescription.setItemProcessingIdentifier(request.getIdentifier());
                    changeDescription.setSimulate(partExecution.isSimulate());
                    changeNotificationDispatcher.notifyChange(changeDescription, workerTask, result);
                } else if (event.isNotApplicable()) {
                    result.recordNotApplicable();
                } else {
                    // TODO error criticality
                    assert event.isError();
                    result.recordFatalError("Item was not pre-processed correctly: " + event.getErrorMessage());
                }
                return true;
            }
        }

        @Override
        protected @NotNull ErrorHandlingStrategyExecutor.Action getDefaultErrorAction() {
            return ErrorHandlingStrategyExecutor.Action.STOP; // We do not want to miss any change by default
        }
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.ASYNCHRONOUS_UPDATE;
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_ASYNC_UPDATE_TASK.value();
    }

    @Override
    public String getDefaultChannel() {
        return Channel.ASYNC_UPDATE.getUri();
    }
}
