/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.sync;

import com.evolveum.midpoint.model.impl.sync.tasks.ResourceObjectClassSpecification;
import com.evolveum.midpoint.provisioning.api.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResultStatus;

import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.sync.tasks.SyncItemProcessingRequest;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetType;

import static com.evolveum.midpoint.repo.common.task.ErrorHandlingStrategyExecutor.FollowUpAction.CONTINUE;
import static com.evolveum.midpoint.repo.common.task.ErrorHandlingStrategyExecutor.FollowUpAction.STOP;

import static org.apache.commons.lang3.BooleanUtils.isNotFalse;

public class LiveSyncActivityExecutionSpecifics
        extends BasePlainIterativeExecutionSpecificsImpl
        <LiveSyncEvent,
                LiveSyncWorkDefinition,
                LiveSyncActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(LiveSyncActivityExecutionSpecifics.class);

    /**
     * Local sequence number of a change that is being processed in the current thread.
     * Actually, it is a hack to enable testing: The code in mappings can obtain this
     * information and do some asserts on it. When the information will be propagated into
     * e.g. lensContext, we should remove this hack.
     */
    @VisibleForTesting
    public static final ThreadLocal<Integer> CHANGE_BEING_PROCESSED = new ThreadLocal<>();

    private ResourceObjectClassSpecification objectClassSpecification;

    public LiveSyncActivityExecutionSpecifics(@NotNull PlainIterativeActivityExecution<LiveSyncEvent, LiveSyncWorkDefinition,
                    LiveSyncActivityHandler, ?> activityExecution) {
        super(activityExecution);
    }

    @Override
    public @NotNull ActivityReportingOptions getDefaultReportingOptions() {
        return new ActivityReportingOptions()
                .persistentStatistics(true)
                .enableActionsExecutedStatistics(true)
                .enableSynchronizationStatistics(true);
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
    public void afterExecution(OperationResult opResult) throws SchemaException {
        int itemsProcessed = activityExecution.getTransientExecutionStatistics().getItemsProcessed();
        LOGGER.trace("LiveSyncTaskHandler.run stopping (resource {}); changes processed: {}",
                objectClassSpecification.resource, itemsProcessed);
        opResult.createSubresult(OperationConstants.LIVE_SYNC_STATISTICS)
                .recordStatus(OperationResultStatus.SUCCESS, "Changes processed: " + itemsProcessed);
    }

    @Override
    public void iterateOverItemsInBucket(@NotNull WorkBucketType bucket, OperationResult opResult)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, PolicyViolationException {

        LiveSyncEventHandler handler = new LiveSyncEventHandler() {
            @Override
            public boolean handle(LiveSyncEvent event, OperationResult opResult) {
                SyncItemProcessingRequest<LiveSyncEvent> request =
                        new SyncItemProcessingRequest<>(event, activityExecution);
                return getProcessingCoordinator().submit(request, opResult);
            }

            @Override
            public void allEventsSubmitted(OperationResult result) {
                getProcessingCoordinator().finishProcessing(result);
            }
        };

        LiveSyncOptions options = createLiveSyncOptions();
        ActivityTokenStorageImpl tokenStorage = new ActivityTokenStorageImpl(activityExecution);

        ModelImplUtils.clearRequestee(getRunningTask());
        getModelBeans().provisioningService
                .synchronize(objectClassSpecification.getCoords(), options, tokenStorage, handler, getRunningTask(), opResult);
    }

    @NotNull
    private LiveSyncOptions createLiveSyncOptions() {
        LiveSyncWorkDefinition def = getActivity().getWorkDefinition();
        return new LiveSyncOptions(def.getExecutionMode(), def.getBatchSize(), def.isUpdateLiveSyncTokenInDryRun());
    }

    @Override
    public boolean processItem(ItemProcessingRequest<LiveSyncEvent> request, RunningTask workerTask, OperationResult result)
            throws CommonException, ActivityExecutionException {
        LiveSyncEvent event = request.getItem();

        CHANGE_BEING_PROCESSED.set(event.getSequentialNumber());
        try {
            if (event.isComplete()) {
                ResourceObjectShadowChangeDescription changeDescription = event.getChangeDescription();
                changeDescription.setItemProcessingIdentifier(request.getIdentifier()); // hack?
                changeDescription.setSimulate(activityExecution.isPreview());
                getModelBeans().eventDispatcher.notifyChange(changeDescription, workerTask, result);
            } else if (event.isNotApplicable()) {
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

    private @NotNull ResourceObjectSetType getResourceObjectSet() {
        return getWorkDefinition().getResourceObjectSetSpecification();
    }

    private @NotNull ModelBeans getModelBeans() {
        return getActivityHandler().getModelBeans();
    }

    @Override
    public @NotNull ErrorHandlingStrategyExecutor.FollowUpAction getDefaultErrorAction() {
        // This could be a bit tricky if combined with partially-specified error handling strategy.
        // So, please, do NOT combine these two! If you specify a strategy, do not use retryLiveSyncErrors extension item.
        //
        // TODO remove in the next schema cleanup
        boolean retryErrors = isNotFalse(
                getRunningTask().getExtensionPropertyRealValue(SchemaConstants.MODEL_EXTENSION_RETRY_LIVE_SYNC_ERRORS));
        return retryErrors ? STOP : CONTINUE;
    }
}
