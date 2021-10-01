/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.async;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.sync.tasks.ResourceObjectClassSpecification;
import com.evolveum.midpoint.model.impl.sync.tasks.SyncItemProcessingRequest;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.provisioning.api.AsyncUpdateEvent;
import com.evolveum.midpoint.provisioning.api.AsyncUpdateEventHandler;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.task.ActivityReportingOptions;
import com.evolveum.midpoint.repo.common.task.ErrorHandlingStrategyExecutor;
import com.evolveum.midpoint.repo.common.task.ItemProcessingRequest;
import com.evolveum.midpoint.repo.common.task.PlainIterativeActivityExecution;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.*;

public class AsyncUpdateActivityExecution
        extends PlainIterativeActivityExecution
        <AsyncUpdateEvent,
                AsyncUpdateWorkDefinition,
                AsyncUpdateActivityHandler,
                AbstractActivityWorkStateType> {

    private ResourceObjectClassSpecification objectClassSpecification;

    AsyncUpdateActivityExecution(
            @NotNull ExecutionInstantiationContext<AsyncUpdateWorkDefinition, AsyncUpdateActivityHandler> context) {
        super(context, "Async update");
    }

    @Override
    public @NotNull ActivityReportingOptions getDefaultReportingOptions() {
        return new ActivityReportingOptions()
                .defaultDetermineBucketSize(ActivityItemCountingOptionType.NEVER)
                .defaultDetermineOverallSize(ActivityOverallItemCountingOptionType.NEVER)
                .persistentStatistics(true)
                .enableActionsExecutedStatistics(true)
                .enableSynchronizationStatistics(true);
    }

    @Override
    public void beforeExecution(OperationResult result) throws ActivityExecutionException, CommonException {
        RunningTask runningTask = getRunningTask();
        ResourceObjectSetType resourceObjectSet = getResourceObjectSet();

        objectClassSpecification = getModelBeans().syncTaskHelper
                .createObjectClassSpec(resourceObjectSet, runningTask, result);

        objectClassSpecification.checkNotInMaintenance();
    }

    @Override
    protected @NotNull ObjectReferenceType getDesiredTaskObjectRef() {
        return objectClassSpecification.getResourceRef();
    }

    @Override
    public void iterateOverItemsInBucket(OperationResult opResult)
            throws CommunicationException, ObjectNotFoundException, SchemaException,
            ConfigurationException, ExpressionEvaluationException {

        AsyncUpdateEventHandler handler = (event, hResult) -> {
            SyncItemProcessingRequest<AsyncUpdateEvent> request =
                    new SyncItemProcessingRequest<>(event, this);
            return coordinator.submit(request, hResult);
        };

        RunningTask runningTask = getRunningTask();
        ModelImplUtils.clearRequestee(runningTask);
        getModelBeans().provisioningService
                .processAsynchronousUpdates(objectClassSpecification.getCoords(), handler, runningTask, opResult);
    }

    @Override
    public boolean processItem(@NotNull ItemProcessingRequest<AsyncUpdateEvent> request,
            @NotNull RunningTask workerTask, OperationResult result)
            throws CommonException, ActivityExecutionException {
        AsyncUpdateEvent event = request.getItem();
        if (event.isComplete()) {
            ResourceObjectShadowChangeDescription changeDescription = event.getChangeDescription();
            changeDescription.setItemProcessingIdentifier(request.getIdentifier());
            changeDescription.setSimulate(isPreview());
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
        return getWorkDefinition().getResourceObjectSetSpecification();
    }

    private @NotNull ModelBeans getModelBeans() {
        return getActivityHandler().getModelBeans();
    }

    @Override
    @NotNull
    public ErrorHandlingStrategyExecutor.FollowUpAction getDefaultErrorAction() {
        return ErrorHandlingStrategyExecutor.FollowUpAction.STOP; // We do not want to miss any change by default
    }

    @Override
    public boolean isExcludedFromStalenessChecking() {
        // This task does not have regularly updated progress. It cannot be watched for staleness (for now).
        return true;
    }
}
