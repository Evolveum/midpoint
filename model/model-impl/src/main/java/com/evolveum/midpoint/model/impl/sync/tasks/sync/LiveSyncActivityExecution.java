/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.sync;

import com.evolveum.midpoint.model.impl.sync.tasks.TargetInfo;
import com.evolveum.midpoint.provisioning.api.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResultStatus;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.sync.tasks.SyncItemProcessingRequest;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
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

import static com.evolveum.midpoint.repo.common.task.ErrorHandlingStrategyExecutor.FollowUpAction.CONTINUE;
import static com.evolveum.midpoint.repo.common.task.ErrorHandlingStrategyExecutor.FollowUpAction.STOP;

import static org.apache.commons.lang3.BooleanUtils.isNotFalse;

public class LiveSyncActivityExecution
        extends AbstractIterativeActivityExecution
        <LiveSyncEvent,
                LiveSyncWorkDefinition,
                LiveSyncActivityHandler,
                AbstractActivityWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(LiveSyncActivityExecution.class);
    private static final String SHORT_NAME = "LiveSync";

    /**
     * Local sequence number of a change that is being processed in the current thread.
     * Actually, it is a hack to enable testing: The code in mappings can obtain this
     * information and do some asserts on it. When the information will be propagated into
     * e.g. lensContext, we should remove this hack.
     */
    @VisibleForTesting
    public static final ThreadLocal<Integer> CHANGE_BEING_PROCESSED = new ThreadLocal<>();

    private TargetInfo targetInfo;
    private SynchronizationResult syncResult;

    LiveSyncActivityExecution(
            @NotNull ExecutionInstantiationContext<LiveSyncWorkDefinition, LiveSyncActivityHandler> context) {
        super(context, SHORT_NAME);
    }

    @Override
    public @NotNull ActivityReportingOptions getDefaultReportingOptions() {
        return new ActivityReportingOptions()
                .enableSynchronizationStatistics(true);
    }

    @Override
    protected void initializeExecution(OperationResult opResult) throws ActivityExecutionException, CommonException {
        RunningTask runningTask = getTaskExecution().getRunningTask();
        ResourceObjectSetType resourceObjectSet = getResourceObjectSet();

        targetInfo = getModelBeans().syncTaskHelper
                .createTargetInfo(resourceObjectSet, runningTask, opResult);

        targetInfo.checkNotInMaintenance();
    }

    @Override
    protected void finishExecution(OperationResult opResult) throws SchemaException {
        LOGGER.trace("LiveSyncTaskHandler.run stopping (resource {}); changes processed: {}", targetInfo.resource, syncResult);
        opResult.createSubresult(OperationConstants.LIVE_SYNC_STATISTICS)
                .recordStatus(OperationResultStatus.SUCCESS, "Changes processed: " + syncResult);
    }

    @Override
    protected void processItems(OperationResult opResult)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, PolicyViolationException {

        LiveSyncEventHandler handler = new LiveSyncEventHandler() {
            @Override
            public boolean handle(LiveSyncEvent event, OperationResult opResult) {
                SyncItemProcessingRequest<LiveSyncEvent> request =
                        new SyncItemProcessingRequest<>(event, LiveSyncActivityExecution.this);
                return coordinator.submit(request, opResult);
            }

            @Override
            public void allEventsSubmitted(OperationResult result) {
                coordinator.finishProcessing(result);
            }
        };

        ModelImplUtils.clearRequestee(getRunningTask());
        syncResult = getModelBeans().provisioningService
                .synchronize(targetInfo.getCoords(), getRunningTask(), isSimulate(), handler, opResult);
    }

    @Override
    protected @NotNull ItemProcessor<LiveSyncEvent> createItemProcessor(OperationResult opResult) {
        return this::processLiveSyncEvent;
    }

    private boolean processLiveSyncEvent(ItemProcessingRequest<LiveSyncEvent> request, RunningTask workerTask,
            OperationResult result) {
        LiveSyncEvent event = request.getItem();

        CHANGE_BEING_PROCESSED.set(event.getSequentialNumber());
        try {
            if (event.isComplete()) {
                ResourceObjectShadowChangeDescription changeDescription = event.getChangeDescription();
                changeDescription.setItemProcessingIdentifier(request.getIdentifier()); // hack?
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
        } finally {
            CHANGE_BEING_PROCESSED.remove();
        }
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
        // This could be a bit tricky if combined with partially-specified error handling strategy.
        // So, please, do NOT combine these two! If you specify a strategy, do not use retryLiveSyncErrors extension item.
        boolean retryErrors = isNotFalse(getRunningTask().getExtensionPropertyRealValue(
                SchemaConstants.MODEL_EXTENSION_RETRY_LIVE_SYNC_ERRORS)); // FIXME!!!
        return retryErrors ? STOP : CONTINUE;
    }
}
