/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.async;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.sync.tasks.ResourceObjectClassSpecification;
import com.evolveum.midpoint.model.impl.sync.tasks.SyncItemProcessingRequest;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.provisioning.api.AsyncUpdateEvent;
import com.evolveum.midpoint.provisioning.api.AsyncUpdateEventHandler;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetType;

public class AsyncUpdateActivityExecutionSpecifics
        extends BasePlainIterativeExecutionSpecificsImpl<AsyncUpdateEvent, AsyncUpdateWorkDefinition, AsyncUpdateActivityHandler> {

    private ResourceObjectClassSpecification objectClassSpecification;

    AsyncUpdateActivityExecutionSpecifics(
            @NotNull PlainIterativeActivityExecution<AsyncUpdateEvent, AsyncUpdateWorkDefinition, AsyncUpdateActivityHandler, ?> activityExecution) {
        super(activityExecution);
    }

    @Override
    public @NotNull ActivityReportingOptions getDefaultReportingOptions() {
        return new ActivityReportingOptions()
                .persistentStatistics(true)
                .enableActionsExecutedStatistics(true)
                .enableSynchronizationStatistics(true); // TODO ok?
    }

    @Override
    public void beforeExecution(OperationResult opResult) throws ActivityExecutionException, CommonException {
        RunningTask runningTask = activityExecution.getRunningTask();
        ResourceObjectSetType resourceObjectSet = getResourceObjectSet();

        objectClassSpecification = getModelBeans().syncTaskHelper
                .createObjectClassSpec(resourceObjectSet, runningTask, opResult);

        objectClassSpecification.checkNotInMaintenance();
    }

    @Override
    public void iterateOverItems(OperationResult opResult)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, PolicyViolationException {

        AsyncUpdateEventHandler handler = (event, hResult) -> {
            SyncItemProcessingRequest<AsyncUpdateEvent> request =
                    new SyncItemProcessingRequest<>(event, activityExecution);
            return getProcessingCoordinator().submit(request, hResult);
        };

        RunningTask runningTask = activityExecution.getRunningTask();
        ModelImplUtils.clearRequestee(runningTask);
        getModelBeans().provisioningService
                .processAsynchronousUpdates(objectClassSpecification.getCoords(), handler, runningTask, opResult);
    }

    @Override
    public boolean processItem(ItemProcessingRequest<AsyncUpdateEvent> request, RunningTask workerTask, OperationResult result)
            throws CommonException, ActivityExecutionException {
        AsyncUpdateEvent event = request.getItem();
        if (event.isComplete()) {
            ResourceObjectShadowChangeDescription changeDescription = event.getChangeDescription();
            changeDescription.setItemProcessingIdentifier(request.getIdentifier());
            changeDescription.setSimulate(activityExecution.isSimulate());
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
}
