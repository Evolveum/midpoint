/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.sync;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

import com.evolveum.midpoint.util.logging.LoggingUtils;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.provisioning.api.LiveSyncEvent;
import com.evolveum.midpoint.provisioning.api.LiveSyncEventHandler;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.provisioning.impl.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.impl.ShadowCache;
import com.evolveum.midpoint.provisioning.impl.adoption.AdoptedLiveSyncChange;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectLiveSyncChangeListener;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.UcfFetchChangesResult;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.LiveSyncCapabilityType;

/**
 * Implements Live synchronization functionality.
 *
 * Although this class exists outside of {@link ShadowCache}, the related functionality is embedded
 * in {@link AdoptedLiveSyncChange#preprocess(OperationResult)} method.
 *
 * Responsibilities:
 *
 * 1. Converts ROC changes into pre-processed adopted changes, embeds them to {@link LiveSyncEvent} instances and emits them out.
 * 2. Manages the token value in the task, based on the acknowledgements.
 * 3. Keeps the control until all events are acknowledged.
 */
@Component
public class LiveSynchronizer {

    private static final Trace LOGGER = TraceManager.getTrace(LiveSynchronizer.class);

    @Autowired private ProvisioningContextFactory ctxFactory;
    @Autowired private ResourceObjectConverter resourceObjectConverter;
    @Autowired private ChangeProcessingBeans beans;

    @NotNull
    public SynchronizationOperationResult synchronize(ResourceShadowDiscriminator shadowCoordinates,
            Task task, TaskPartitionDefinitionType partition, LiveSyncEventHandler handler, OperationResult gResult)
            throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SchemaException,
            ConfigurationException, SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException {

        LiveSyncCtx ctx = new LiveSyncCtx(shadowCoordinates, task, partition, gResult);

        InternalMonitor.recordCount(InternalCounters.PROVISIONING_ALL_EXT_OPERATION_COUNT);

        setupInitialToken(ctx);
        if (!ctx.hasInitialToken()) {
            fetchAndRememberCurrentToken(ctx, gResult); // see comment in the called method
            return ctx.syncResult;
        }

        EventsAcknowledgeGate acknowledgeGate = new EventsAcknowledgeGate();

        ResourceObjectLiveSyncChangeListener listener = (resourceObjectChange, lResult) -> {

            int sequentialNumber = ctx.oldestTokenWatcher.changeArrived(resourceObjectChange.getToken());

            AdoptedLiveSyncChange change = new AdoptedLiveSyncChange(resourceObjectChange, ctx.simulate, beans) {
                @Override
                public void acknowledge(boolean release, OperationResult aResult) {
                    acknowledgeGate.acknowledgeIssuedEvent();
                    if (release) {
                        ctx.oldestTokenWatcher.changeProcessed(sequentialNumber);
                    }
                }
            };
            change.preprocess(lResult);

            LiveSyncEvent event = new LiveSyncEventImpl(change);

            acknowledgeGate.registerIssuedEvent();
            try {
                return handler.handle(event, lResult);
            } catch (Throwable t) {
                // We assume the event was not acknowledged yet. Note that serious handler should never throw an exception!
                LoggingUtils.logUnexpectedException(LOGGER, "Got unexpected exception while handling a live sync event", t);
                acknowledgeGate.acknowledgeIssuedEvent();
                return false;
            }
        };

        UcfFetchChangesResult fetchChangesResult;
        try {
            fetchChangesResult = resourceObjectConverter.fetchChanges(ctx.context, ctx.getInitialToken(), listener, gResult);
        } finally {
            handler.allEventsSubmitted();
        }

        if (fetchChangesResult.isAllChangesFetched()) {
            ctx.syncResult.setAllChangesFetched(true);
            ctx.finalToken = fetchChangesResult.getFinalToken();
        }

        acknowledgeGate.waitForIssuedEventsAcknowledge();

        updateTokenValue(ctx);
        task.flushPendingModifications(gResult);

        return ctx.syncResult;
    }

    private void setupInitialToken(LiveSyncCtx ctx) {
        PrismProperty<?> initialToken = getTokenFromTask(ctx.task);
        ctx.syncResult.setInitialToken(initialToken);
    }

    private void updateTokenValue(LiveSyncCtx ctx) throws SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, ConfigurationException, CommunicationException {

        LiveSyncCapabilityType capability = ctx.context.getEffectiveCapability(LiveSyncCapabilityType.class);
        boolean preciseTokenValue = capability != null && isTrue(capability.isPreciseTokenValue());
        boolean isDryRun = TaskUtil.isDryRun(ctx.task);
        boolean updateTokenInDryRun = TaskUtil.findExtensionItemValueInThisOrParent(ctx.task,
                SchemaConstants.MODEL_EXTENSION_UPDATE_LIVE_SYNC_TOKEN_IN_DRY_RUN, false);
        PrismProperty<?> initialToken = ctx.getInitialToken();

        PrismProperty<?> oldestTokenProcessed = ctx.oldestTokenWatcher.getOldestTokenProcessed();
        LOGGER.trace("oldestTokenProcessed = {}, synchronization result = {}", oldestTokenProcessed, ctx.syncResult);
        PrismProperty<?> tokenToSet;
        if (ctx.simulate) {
            tokenToSet = null;        // Token should not be updated during simulation.
        } else if (isDryRun && !updateTokenInDryRun) {
            tokenToSet = null;
        } else if (!ctx.syncResult.isHaltingErrorEncountered() && !ctx.syncResult.isSuspendEncountered() && ctx.syncResult.isAllChangesFetched()) {
            // Everything went OK. Everything was processed.
            tokenToSet = ctx.finalToken != null ? ctx.finalToken : oldestTokenProcessed;
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
            ctx.task.setExtensionProperty(tokenToSet);
            ctx.syncResult.setTaskTokenUpdatedTo(tokenToSet);
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

    /**
     * No sync token in task. We are going to simply fetch the current token value from the resource and skip any processing.
     * (This is introduced in 4.0.1; it is different from the behaviour up to and including 4.0.) The rational is that
     * there's no point in trying to fetch changes after fetching the current token value. We defer that to next run
     * of the live sync task.
     *
     * We intentionally update the token even if we are in dry run mode. Otherwise we could never see any records
     * (without setting updateLiveSyncTokenInDryRun to true).
     */
    private void fetchAndRememberCurrentToken(LiveSyncCtx ctx, OperationResult result) throws ObjectNotFoundException,
            CommunicationException, SchemaException, ConfigurationException, ExpressionEvaluationException,
            ObjectAlreadyExistsException {
        PrismProperty<?> currentToken = resourceObjectConverter.fetchCurrentToken(ctx.context, result);
        if (currentToken == null) {
            LOGGER.warn("No current token provided by resource: {}. Live sync will not proceed: {}",
                    ctx.context.getShadowCoordinates(), ctx.task);
        } else if (!ctx.simulate) {
            LOGGER.info("Setting initial live sync token ({}) in task: {}.", currentToken, ctx.task);
            ctx.task.setExtensionProperty(currentToken);
            ctx.task.flushPendingModifications(result);
            ctx.syncResult.setTaskTokenUpdatedTo(currentToken);
        } else {
            LOGGER.info("We would set initial live sync token ({}) in task: {}; but not doing so because in simulation mode",
                    currentToken, ctx.task);
        }
    }

    private class LiveSyncCtx {
        private final SynchronizationOperationResult syncResult;
        private final ProvisioningContext context;
        private final Task task; // redundant to simplify access
        private final boolean simulate;
        private final OldestTokenWatcher oldestTokenWatcher;
        private PrismProperty<?> finalToken; // TODO what exactly is this for? Be sure to set it only when all changes were processed

        private LiveSyncCtx(ResourceShadowDiscriminator shadowCoordinates, Task task, TaskPartitionDefinitionType partition, OperationResult result)
                throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
            this.syncResult = new SynchronizationOperationResult();
            this.context = ctxFactory.create(shadowCoordinates, task, result);
            this.task = task;
            this.simulate = partition != null && partition.getStage() == ExecutionModeType.SIMULATE;
            this.oldestTokenWatcher = new OldestTokenWatcher();
        }

        public PrismProperty<?> getInitialToken() {
            return syncResult.getInitialToken();
        }

        private boolean hasInitialToken() {
            return getInitialToken() != null;
        }

    }
}
