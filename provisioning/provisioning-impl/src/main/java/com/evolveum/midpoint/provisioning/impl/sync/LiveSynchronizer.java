/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.sync;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.provisioning.impl.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.impl.sync.ErrorHandlingStrategyExecutor.Action;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.LiveSyncChangeListener;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.util.RepoCommonUtils;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.LiveSyncCapabilityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * Implements Live synchronization functionality.
 */
@Component
public class LiveSynchronizer {

    private static final Trace LOGGER = TraceManager.getTrace(LiveSynchronizer.class);

    @Autowired private ProvisioningContextFactory ctxFactory;
    @Autowired private ResourceObjectConverter resourceObjectConverter;
    @Autowired private ChangeProcessor changeProcessor;
    @Autowired private TaskManager taskManager;
    @Autowired private PrismContext prismContext;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    @NotNull
    public SynchronizationOperationResult synchronize(ResourceShadowDiscriminator shadowCoordinates,
            Task task, TaskPartitionDefinitionType partition, OperationResult result) throws ObjectNotFoundException,
            CommunicationException, GenericFrameworkException, SchemaException, ConfigurationException,
            SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException {

        SynchronizationOperationResult syncResult = new SynchronizationOperationResult();

        InternalMonitor.recordCount(InternalCounters.PROVISIONING_ALL_EXT_OPERATION_COUNT);

        ProvisioningContext ctx = ctxFactory.create(shadowCoordinates, task, result);
        boolean isSimulate = partition != null && partition.getStage() == ExecutionModeType.SIMULATE;

        PrismProperty<?> initialToken = getTokenFromTask(task);
        syncResult.setInitialToken(initialToken);
        if (initialToken == null) {
            // No sync token in task. We are going to simply fetch the current token value from the resource and exit right now.
            // (This is introduced in 4.0.1; it is different from the behaviour up to and including 4.0.) The rational is that
            // there's no point in trying to fetch changes after fetching the current token value. We defer that to next run
            // of the live sync task.
            //
            // We intentionally update the token even if we are in dry run mode. Otherwise we could never see any records
            // (without setting updateLiveSyncTokenInDryRun to true).
            fetchAndRememberCurrentToken(syncResult, isSimulate, ctx, result);
            return syncResult;
        }

        ErrorHandlingStrategyExecutor errorHandlingStrategyExecutor =
                new ErrorHandlingStrategyExecutor(task, prismContext, repositoryService);

        OldestTokenWatcher oldestTokenWatcher = new OldestTokenWatcher();

        ChangeProcessingCoordinator coordinator = new ChangeProcessingCoordinator(
                () -> ctx.canRun() && !syncResult.isHaltingErrorEncountered(),
                changeProcessor, task, partition);

        Holder<PrismProperty<?>> finalTokenHolder = new Holder<>();
        LiveSyncChangeListener changeHandler = new LiveSyncChangeListener() {
            @Override
            public boolean onChange(Change change, OperationResult opResult) {
                int sequentialNumber = oldestTokenWatcher.changeArrived(change.getToken());
                if (ctx.canRun()) {
                    ProcessChangeRequest request = new ProcessChangeRequest(change, ctx, isSimulate) {
                        /**
                         * This is a success reported by change processor.
                         *
                         * The change was successfully created and then successfully processed.
                         * It is hopefully the usual case.
                         */
                        @Override
                        public void onSuccessfullyProcessed() {
                            treatOverallSuccess(sequentialNumber);
                        }

                        /**
                         * This is a "soft" error reported by change processor - i.e. the one without an exception.
                         * The issue should be already recorded in the operation result. Our task is to stop or
                         * continue processing, depending on the error handling strategy.
                         *
                         * The change was successfully created but its processing failed.
                         *
                         * @param processingResult We SHOULD NOT write anything to this result.
                         */
                        @Override
                        public void onProcessingError(OperationResult processingResult) {
                            OperationResultStatus status = processingResult.getStatus();
                            LOGGER.error("An error ({}) occurred during processing of change #{} in live synchronization in {}: {}",
                                    status, sequentialNumber, task, change);
                            treatOverallError(sequentialNumber, RepoCommonUtils.getResultExceptionIfExists(processingResult), status,
                                    getShadowOidFromChange(change), opResult);
                        }

                        /**
                         * This is a "hard" error reported by change processor - i.e. the one with an exception.
                         * The issue should be already recorded in the operation result. Our task is to stop or
                         * continue processing, depending on the settings.
                         *
                         * The change was successfully created but its processing failed.
                         *
                         * @param processingResult We SHOULD NOT write anything to this result.
                         */
                        @Override
                        public void onProcessingError(Throwable t, OperationResult processingResult) {
                            OperationResultStatus status = processingResult.getStatus();
                            LoggingUtils.logUnexpectedException(LOGGER, "An exception (mapped to {}) occurred during processing "
                                    + "of change #{} in live synchronization in {}: {}", t, status, sequentialNumber, task, change);
                            treatOverallError(sequentialNumber, t, status, getShadowOidFromChange(change), opResult);
                        }
                    };
                    try {
                        coordinator.submit(request, opResult);
                    } catch (InterruptedException e) {
                        LOGGER.trace("Got InterruptedException, probably the coordinator task was suspended. Let's stop fetching changes.");
                        syncResult.setSuspendEncountered(true); // ok?
                        return false;
                    }
                }
                return ctx.canRun() && !syncResult.isHaltingErrorEncountered();
            }

            /**
             * This is a "hard" error reported in preparation stages of change processing. The change might be even null here
             * (in that case we hope at least token is present).
             *
             * So, the change could not be created; so no processing was started at all.
             * We interpret this as a fatal error.
             *
             * @param opResult This is regular operation result to be used for this operation reporting.
             */
            @Override
            public boolean onChangePreparationError(@Nullable PrismProperty<?> token, @Nullable Change change,
                    @NotNull Throwable t, @NotNull OperationResult opResult) {
                int sequentialNumber = oldestTokenWatcher.changeArrived(token);
                LoggingUtils.logUnexpectedException(LOGGER, "An exception (interpreted as fatal error) occurred during "
                                + "pre-processing of change #{} in live synchronization in {}: {}", t, sequentialNumber, task,
                        change != null ? "change " + change : "sync delta with token " + token);
                String shadowOid = getShadowOidFromChange(change);
                return treatOverallError(sequentialNumber, t, OperationResultStatus.FATAL_ERROR, shadowOid, opResult);
            }

            @Override
            public void onAllChangesFetched(PrismProperty<?> finalToken, OperationResult result) {
                LOGGER.trace("All changes were fetched; finalToken = {}", finalToken);
                finalTokenHolder.setValue(finalToken);
                syncResult.setAllChangesFetched(true);
            }

            private boolean treatOverallSuccess(int sequentialNumber) {
                oldestTokenWatcher.changeProcessed(sequentialNumber);
                syncResult.incrementChangesProcessed();
                if (task instanceof RunningTask) {
                    ((RunningTask) task).incrementProgressAndStoreStatsIfNeeded();
                }
                return ctx.canRun();
            }

            /**
             * @param opResult This is the operation result to be used for operation execution reporting.
             */
            private boolean treatOverallError(int sequentialNumber, @Nullable Throwable t, @NotNull OperationResultStatus status,
                    @Nullable String shadowOid, @NotNull OperationResult opResult) {
                syncResult.incrementErrors();
                Action action = errorHandlingStrategyExecutor.determineAction(t, status, shadowOid, opResult);
                switch (action) {
                    case CONTINUE:
                        LOGGER.info("LiveSync encountered an error that was NOT considered a halting one: so continuing processing. "
                                + "Task: {}", ctx.getTask());
                        return treatOverallSuccess(sequentialNumber);
                    case SUSPEND:
                        syncResult.setSuspendEncountered(true);
                    case STOP:
                        // We need to retry the failed change -- so we must not update the token.
                        // Moreover, we have to stop here, so that the changes will be applied in correct order.
                        syncResult.setHaltingErrorEncountered(true);
                        syncResult.setExceptionEncountered(t);
                        LOGGER.info("LiveSync encountered an error that was considered a {} one: so exiting now with the hope"
                                        + " that the error will be cleared on the next task run. Task: {}; processed changes: {}",
                                action == Action.STOP ? "halting" : "suspending", ctx.getTask(), syncResult.getChangesProcessed());
                        return false;
                    default:
                        throw new AssertionError(action);
                }
            }
        };

        try {
            resourceObjectConverter.fetchChanges(ctx, initialToken, changeHandler, result);
        } finally {
            coordinator.setAllItemsSubmitted();
        }

        if (task instanceof RunningTask) {
            taskManager.waitForTransientChildren((RunningTask) task, result);
            coordinator.updateOperationResult(result);
        }

        if (!ctx.canRun()) {
            LOGGER.info("LiveSync was suspended during processing. Task: {}; processed changes: {}", ctx.getTask(),
                    syncResult.getChangesProcessed());
            syncResult.setSuspendEncountered(true);
        }

        updateTokenValue(syncResult, oldestTokenWatcher.getOldestTokenProcessed(), finalTokenHolder.getValue(),
                isSimulate, ctx, task, result);
        task.flushPendingModifications(result);

        return syncResult;
    }

    private void updateTokenValue(SynchronizationOperationResult syncResult, PrismProperty<?> oldestTokenProcessed,
            PrismProperty<?> finalToken, boolean isSimulate, ProvisioningContext ctx,
            Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException,
            ExpressionEvaluationException, ConfigurationException, CommunicationException {

        LiveSyncCapabilityType capability = ctx.getEffectiveCapability(LiveSyncCapabilityType.class);
        boolean preciseTokenValue = capability != null && isTrue(capability.isPreciseTokenValue());
        boolean isDryRun = TaskUtil.isDryRun(task);
        boolean updateTokenInDryRun = TaskUtil.findExtensionItemValueInThisOrParent(task,
                SchemaConstants.MODEL_EXTENSION_UPDATE_LIVE_SYNC_TOKEN_IN_DRY_RUN, false);
        PrismProperty<?> initialToken = syncResult.getInitialToken();

        LOGGER.trace("oldestTokenProcessed = {}, synchronization result = {}", oldestTokenProcessed, syncResult);
        PrismProperty<?> tokenToSet;
        if (isSimulate) {
            tokenToSet = null;        // Token should not be updated during simulation.
        } else if (isDryRun && !updateTokenInDryRun) {
            tokenToSet = null;
        } else if (!syncResult.isHaltingErrorEncountered() && !syncResult.isSuspendEncountered() && syncResult.isAllChangesFetched()) {
            // Everything went OK. Everything was processed.
            tokenToSet = finalToken != null ? finalToken : oldestTokenProcessed;
            // Note that it is theoretically possible that tokenToSet is null here: it happens when no changes are fetched from
            // the resource and the connector returns null from .sync() method. But in this case nothing wrong happens: the
            // token in task will simply stay as it is. That's the correct behavior for such a case.
        } else if (preciseTokenValue) {
            // Something was wrong but we can continue on latest change.
            tokenToSet = oldestTokenProcessed;
            LOGGER.info("Capability of providing precise token values is present. Token in task is updated so the processing will "
                    + "continue where it was stopped. New token value is '{}' (initial value was '{}')",
                    SchemaDebugUtil.prettyPrint(tokenToSet), SchemaDebugUtil.prettyPrint(initialToken));
        } else {
            // Something was wrong and we must restart from the beginning.
            tokenToSet = null; // So we will not update the token.
            LOGGER.info("Capability of providing precise token values is NOT present. Token will not be updated so the "
                            + "processing will restart from the beginning at next task run. So token value stays as it was: '{}'",
                    SchemaDebugUtil.prettyPrint(initialToken));
        }

        if (tokenToSet != null) {
            LOGGER.trace("Setting token value of {}", SchemaDebugUtil.prettyPrintLazily(tokenToSet));
            task.setExtensionProperty(tokenToSet);
            syncResult.setTaskTokenUpdatedTo(tokenToSet);
        }
    }

    private String getShadowOidFromChange(Change change) {
        if (change == null) {
            return null;
        } else {
            return change.guessOid();
        }
    }

    private PrismProperty<?> getTokenFromTask(Task task) {
        PrismProperty<?> tokenProperty = task.getExtensionPropertyOrClone(SchemaConstants.SYNC_TOKEN);
        LOGGER.trace("Initial token from the task: {}", SchemaDebugUtil.prettyPrintLazily(tokenProperty));
        if (tokenProperty != null) {
            if (tokenProperty.getAnyRealValue() != null) {
                return tokenProperty;
            } else {
                LOGGER.warn("Sync token in task exists, but it is empty (null value). Ignoring it. Task: {}", task);
                LOGGER.trace("Empty sync token property:\n{}", tokenProperty.debugDumpLazily());
                return null;
            }
        } else {
            return null;
        }
    }

    private void fetchAndRememberCurrentToken(SynchronizationOperationResult syncResult, boolean isSimulate, ProvisioningContext ctx,
            OperationResult parentResult) throws ObjectNotFoundException, CommunicationException, SchemaException,
            ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException {
        Task task = ctx.getTask();
        PrismProperty<?> currentToken = resourceObjectConverter.fetchCurrentToken(ctx, parentResult);
        if (currentToken == null) {
            LOGGER.warn("No current token provided by resource: {}. Live sync will not proceed: {}",
                    ctx.getShadowCoordinates(), task);
        } else if (!isSimulate) {
            LOGGER.info("Setting initial live sync token ({}) in task: {}.", currentToken, task);
            task.setExtensionProperty(currentToken);
            task.flushPendingModifications(parentResult);
            syncResult.setTaskTokenUpdatedTo(currentToken);
        } else {
            LOGGER.info("We would set initial live sync token ({}) in task: {}; but not doing so because in simulation mode",
                    currentToken, task);
        }
    }
}
