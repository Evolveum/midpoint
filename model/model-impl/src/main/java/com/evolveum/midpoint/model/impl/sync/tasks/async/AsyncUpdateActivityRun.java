/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.async;

import com.evolveum.midpoint.model.impl.sync.tasks.ProcessingScope;
import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.sync.tasks.SyncItemProcessingRequest;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.provisioning.api.AsyncUpdateEvent;
import com.evolveum.midpoint.provisioning.api.AsyncUpdateEventHandler;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.*;

public final class AsyncUpdateActivityRun
        extends PlainIterativeActivityRun
        <AsyncUpdateEvent,
                AsyncUpdateWorkDefinition,
                AsyncUpdateActivityHandler,
                AbstractActivityWorkStateType> {

    /** What we want to process. Currently we use only resourceRef from here. */
    private ProcessingScope processingScope;

    AsyncUpdateActivityRun(
            @NotNull ActivityRunInstantiationContext<AsyncUpdateWorkDefinition, AsyncUpdateActivityHandler> context) {
        super(context, "Async update");
        setInstanceReady();
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .determineOverallSizeDefault(ActivityOverallItemCountingOptionType.NEVER)
                .actionsExecutedStatisticsSupported(true)
                .synchronizationStatisticsSupported(true)
                .progressCommitPointsSupported(false);
    }

    @Override
    public boolean beforeRun(OperationResult result) throws ActivityRunException, CommonException {
        if (!super.beforeRun(result)) {
            return false;
        }

        RunningTask runningTask = getRunningTask();
        ResourceObjectSetType resourceObjectSet = getResourceObjectSet();

        processingScope = getModelBeans().syncTaskHelper
                .getProcessingScopeCheckingMaintenance(resourceObjectSet, runningTask, result);
        return true;
    }

    @Override
    protected @NotNull ObjectReferenceType getDesiredTaskObjectRef() {
        return processingScope.getResourceRef();
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
                .processAsynchronousUpdates(processingScope.getCoords(), handler, runningTask, opResult);
    }

    @Override
    public boolean processItem(@NotNull ItemProcessingRequest<AsyncUpdateEvent> request,
            @NotNull RunningTask workerTask, OperationResult result)
            throws CommonException, ActivityRunException {
        AsyncUpdateEvent event = request.getItem();
        if (event.isComplete()) {
            ResourceObjectShadowChangeDescription changeDescription = event.getChangeDescription();
            changeDescription.setItemProcessingIdentifier(request.getIdentifier());
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

    @Override
    protected String getChannelOverride() {
        return SchemaConstants.CHANNEL_ASYNC_UPDATE_URI;
    }

    @Override
    protected boolean canUpdateThreadLocalStatistics() {
        return false; // See MID-7464 and the comment in the overridden method.
    }
}
