/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.async;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.sync.tasks.SyncItemProcessingRequest;
import com.evolveum.midpoint.model.impl.sync.tasks.ResourceObjectClassSpecification;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.provisioning.api.AsyncUpdateEvent;
import com.evolveum.midpoint.provisioning.api.AsyncUpdateEventHandler;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetType;

import org.jetbrains.annotations.NotNull;

public class AsyncUpdateActivityExecution
        extends AbstractIterativeActivityExecution
        <AsyncUpdateEvent,
                AsyncUpdateWorkDefinition,
                AsyncUpdateActivityHandler,
                AbstractActivityWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(AsyncUpdateActivityExecution.class);

    private static final String SHORT_NAME = "AsyncUpdate";

    private ResourceObjectClassSpecification objectClassSpecification;

    AsyncUpdateActivityExecution(
            @NotNull ExecutionInstantiationContext<AsyncUpdateWorkDefinition, AsyncUpdateActivityHandler> context) {
        super(context, SHORT_NAME);
    }

    @Override
    public @NotNull ActivityReportingOptions getDefaultReportingOptions() {
        return new ActivityReportingOptions()
                .enableActionsExecutedStatistics(true)
                .enableSynchronizationStatistics(true); // TODO ok?
    }

    @Override
    protected void initializeExecution(OperationResult opResult) throws ActivityExecutionException, CommonException {
        RunningTask runningTask = getTaskExecution().getRunningTask();
        ResourceObjectSetType resourceObjectSet = getResourceObjectSet();

        objectClassSpecification = getModelBeans().syncTaskHelper
                .createObjectClassSpec(resourceObjectSet, runningTask, opResult);

        objectClassSpecification.checkNotInMaintenance();
    }

    @Override
    protected void processItems(OperationResult opResult)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, PolicyViolationException {

        AsyncUpdateEventHandler handler = (event, hResult) -> {
            SyncItemProcessingRequest<AsyncUpdateEvent> request =
                    new SyncItemProcessingRequest<>(event, AsyncUpdateActivityExecution.this);
            return coordinator.submit(request, hResult);
        };

        ModelImplUtils.clearRequestee(getRunningTask());
        getModelBeans().provisioningService
                .processAsynchronousUpdates(objectClassSpecification.getCoords(), handler, getRunningTask(), opResult);
    }

    @Override
    protected @NotNull ItemProcessor<AsyncUpdateEvent> createItemProcessor(OperationResult opResult) {
        return this::processAsyncUpdateEvent;
    }

    private boolean processAsyncUpdateEvent(ItemProcessingRequest<AsyncUpdateEvent> request, RunningTask workerTask,
            OperationResult result) {
        AsyncUpdateEvent event = request.getItem();
        if (event.isComplete()) {
            ResourceObjectShadowChangeDescription changeDescription = event.getChangeDescription();
            changeDescription.setItemProcessingIdentifier(request.getIdentifier());
            changeDescription.setSimulate(isSimulate());
            getModelBeans().eventDispatcher.notifyChange(changeDescription, workerTask, result);
        } else if (event.isNotApplicable()) {
            result.recordNotApplicable();
        } else {
            // TODO error criticality
            assert event.isError();
            result.recordFatalError("Item was not pre-processed correctly: " + event.getErrorMessage());
        }
        return true;
    }


    private @NotNull ResourceObjectSetType getResourceObjectSet() {
        return activity.getWorkDefinition().getResourceObjectSetSpecification();
    }

    private @NotNull ModelBeans getModelBeans() {
        return getActivityHandler().getModelBeans();
    }

    @Override
    public boolean providesTracingAndDynamicProfiling() {
        return true;
    }

    @Override
    protected @NotNull ErrorHandlingStrategyExecutor.FollowUpAction getDefaultErrorAction() {
        return ErrorHandlingStrategyExecutor.FollowUpAction.STOP; // We do not want to miss any change by default
    }

}
