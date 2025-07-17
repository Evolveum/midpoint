/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.sync;

import com.evolveum.midpoint.model.impl.sync.tasks.ProcessingScope;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.api.*;
import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResultStatus;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.sync.tasks.SyncItemProcessingRequest;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import static com.evolveum.midpoint.repo.common.activity.run.ErrorHandlingStrategyExecutor.FollowUpAction.CONTINUE;
import static com.evolveum.midpoint.repo.common.activity.run.ErrorHandlingStrategyExecutor.FollowUpAction.STOP;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityEventLoggingOptionType.NONE;

import static org.apache.commons.lang3.BooleanUtils.isNotFalse;

public final class LiveSyncActivityRun
        extends PlainIterativeActivityRun
        <LiveSyncEvent,
                LiveSyncWorkDefinition,
                LiveSyncActivityHandler,
                LiveSyncWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(LiveSyncActivityRun.class);

    /**
     * Local sequence number of a change that is being processed in the current thread.
     * Actually, it is a hack to enable testing: The code in mappings can obtain this
     * information and do some asserts on it. When the information will be propagated into
     * e.g. lensContext, we should remove this hack.
     */
    @VisibleForTesting
    public static final ThreadLocal<Integer> CHANGE_BEING_PROCESSED = new ThreadLocal<>();

    /** What objects to synchronize (resource + OC + kind + intent). */
    private ProcessingScope processingScope;

    // TODO shouldn't we use PostSearchFilter as well? (Beware of skipping objects.)

    public LiveSyncActivityRun(
            @NotNull ActivityRunInstantiationContext<LiveSyncWorkDefinition, LiveSyncActivityHandler> activityRun) {
        super(activityRun, "LiveSync");
        setInstanceReady();
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .determineOverallSizeDefault(ActivityOverallItemCountingOptionType.NEVER)
                .bucketCompletionLoggingDefault(NONE) // To avoid log noise.
                .actionsExecutedStatisticsSupported(true)
                .synchronizationStatisticsSupported(true)
                .progressCommitPointsSupported(true); // This is to be rethought.
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
    public void afterRun(OperationResult result) {
        int itemsProcessed = transientRunStatistics.getItemsProcessed();
        LOGGER.trace("LiveSyncTaskHandler.run stopping (resource {}); changes processed: {}",
                processingScope.resource, itemsProcessed);
        result.createSubresult(OperationConstants.LIVE_SYNC_STATISTICS)
                .recordStatus(OperationResultStatus.SUCCESS, "Changes processed: " + itemsProcessed);
    }

    @Override
    public void iterateOverItemsInBucket(OperationResult opResult)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, PolicyViolationException {

        LiveSyncEventHandler handler = new LiveSyncEventHandler() {
            @Override
            public boolean handle(LiveSyncEvent event, OperationResult opResult) {
                SyncItemProcessingRequest<LiveSyncEvent> request =
                        new SyncItemProcessingRequest<>(event, LiveSyncActivityRun.this);
                return coordinator.submit(request, opResult);
            }

            @Override
            public void allEventsSubmitted(OperationResult result) {
                coordinator.finishProcessing(result);
            }
        };

        LiveSyncOptions options = createLiveSyncOptions();
        ActivityTokenStorageImpl tokenStorage = new ActivityTokenStorageImpl(this);

        ProvisioningOperationContext context = new ProvisioningOperationContext();
        // todo set request identifier

        ModelImplUtils.clearRequestee(getRunningTask());
        getModelBeans().provisioningService
                .synchronize(
                        processingScope.getCoords(),
                        options,
                        tokenStorage,
                        handler,
                        context,
                        getRunningTask(),
                        opResult);
    }

    @NotNull
    private LiveSyncOptions createLiveSyncOptions() {
        LiveSyncWorkDefinition def = activity.getWorkDefinition();
        return new LiveSyncOptions(
                activity.getExecutionMode(),
                def.getBatchSize(),
                def.isUpdateLiveSyncTokenInDryRun(),
                def.isUpdateLiveSyncTokenInPreviewMode());
    }

    @Override
    public boolean processItem(
            @NotNull ItemProcessingRequest<LiveSyncEvent> request,
            @NotNull RunningTask workerTask,
            @NotNull OperationResult result)
            throws CommonException, ActivityRunException {
        LiveSyncEvent event = request.getItem();

        CHANGE_BEING_PROCESSED.set(event.getSequentialNumber());
        try {
            if (event.isComplete()) {
                PrismObject<ShadowType> shadowedObject = event.getShadowedObjectRequired();
                if (!processingScope.getPostSearchFilter().matches(shadowedObject)) {
                    LOGGER.trace("Skipping {} because it does not match objectClass/kind/intent", shadowedObject);
                    workerTask.onSynchronizationExclusion(
                            request.getIdentifier(), SynchronizationExclusionReasonType.NOT_APPLICABLE_FOR_TASK);
                    result.setNotApplicable("Skipped because it does not match objectClass/kind/intent");
                    return true;
                }
                ResourceObjectShadowChangeDescription changeDescription = event.getChangeDescription();
                changeDescription.setItemProcessingIdentifier(request.getIdentifier()); // hack?
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

    @Override
    protected String getChannelOverride() {
        return SchemaConstants.CHANNEL_LIVE_SYNC_URI;
    }
}
