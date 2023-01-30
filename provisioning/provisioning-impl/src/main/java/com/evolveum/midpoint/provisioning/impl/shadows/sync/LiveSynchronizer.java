/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.sync;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

import com.evolveum.midpoint.provisioning.api.LiveSyncOptions;
import com.evolveum.midpoint.provisioning.api.LiveSyncTokenStorage;
import com.evolveum.midpoint.provisioning.api.LiveSyncToken;
import com.evolveum.midpoint.provisioning.impl.TokenUtil;
import com.evolveum.midpoint.schema.ResourceOperationCoordinates;
import com.evolveum.midpoint.util.logging.LoggingUtils;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionModeType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.api.LiveSyncEvent;
import com.evolveum.midpoint.provisioning.api.LiveSyncEventHandler;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowsFacade;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowedLiveSyncChange;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectLiveSyncChangeListener;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.UcfFetchChangesResult;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.LiveSyncCapabilityType;

/**
 * Implements Live synchronization functionality.
 *
 * Although this class exists outside of {@link ShadowsFacade}, the related functionality is embedded
 * in {@link ShadowedLiveSyncChange#initializeInternal(Task, OperationResult)} method.
 *
 * Responsibilities:
 *
 * 1. Converts ROC changes into pre-processed shadowed changes, embeds them to {@link LiveSyncEvent} instances and emits them out.
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
    public SynchronizationOperationResult synchronize(
            ResourceOperationCoordinates coordinates,
            LiveSyncOptions options,
            LiveSyncTokenStorage tokenStorage,
            LiveSyncEventHandler handler,
            Task task,
            OperationResult gResult)
            throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SchemaException,
            ConfigurationException, SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException {

        LiveSyncCtx ctx = new LiveSyncCtx(coordinates, task, options, tokenStorage, gResult);

        InternalMonitor.recordCount(InternalCounters.PROVISIONING_ALL_EXT_OPERATION_COUNT);

        setupInitialToken(ctx);
        if (!ctx.hasInitialToken()) {
            fetchAndRememberCurrentToken(ctx, gResult); // see comment in the called method
            return ctx.syncResult;
        }

        IndividualEventsAcknowledgeGate<LiveSyncEvent> acknowledgeGate = new IndividualEventsAcknowledgeGate<>();

        ResourceObjectLiveSyncChangeListener listener = (resourceObjectChange, lResult) -> {

            int sequentialNumber = ctx.oldestTokenWatcher.changeArrived(resourceObjectChange.getToken());

            ShadowedLiveSyncChange change = new ShadowedLiveSyncChange(resourceObjectChange, beans);
            change.initialize(task, lResult);

            LiveSyncEvent event = new LiveSyncEventImpl(change) {
                @Override
                public void acknowledge(boolean release, OperationResult aResult) {
                    LOGGER.trace("Acknowledgement (release={}) sent for {}", release, this);
                    if (release) {
                        ctx.oldestTokenWatcher.changeProcessed(sequentialNumber);
                    }
                    acknowledgeGate.acknowledgeIssuedEvent(this);
                }
            };

            acknowledgeGate.registerIssuedEvent(event);
            try {
                return handler.handle(event, lResult);
            } catch (Throwable t) {
                // We assume the event was not acknowledged yet. Note that serious handler should never throw an exception!
                LoggingUtils.logUnexpectedException(LOGGER, "Got unexpected exception while handling a live sync event", t);
                acknowledgeGate.acknowledgeIssuedEvent(event);
                return false;
            }
        };

        UcfFetchChangesResult fetchChangesResult;
        try {
            fetchChangesResult = resourceObjectConverter.fetchChanges(ctx.context, ctx.getInitialToken(), ctx.getBatchSize(),
                    listener, gResult);
        } finally {
            handler.allEventsSubmitted(gResult);
        }

        if (fetchChangesResult.isAllChangesFetched()) {
            ctx.syncResult.setAllChangesFetched();
            ctx.finalToken = TokenUtil.fromUcf(fetchChangesResult.getFinalToken());
        }

        acknowledgeGate.waitForIssuedEventsAcknowledge(gResult);

        if (ctx.oldestTokenWatcher.isEverythingProcessed()) {
            ctx.syncResult.setAllFetchedChangesProcessed();
        }

        updateTokenValue(ctx, gResult);

        return ctx.syncResult;
    }

    private void setupInitialToken(LiveSyncCtx ctx) {
        ctx.syncResult.setInitialToken(
                ctx.tokenStorage.getToken());
    }

    private void updateTokenValue(LiveSyncCtx ctx, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

        LiveSyncCapabilityType capability = ctx.context.getCapability(LiveSyncCapabilityType.class); // TODO only if enabled?
        boolean preciseTokenValue = capability != null && isTrue(capability.isPreciseTokenValue());
        boolean updateTokenInDryRun = ctx.isUpdateLiveSyncTokenInDryRun();
        boolean updateTokenInPreviewMode = ctx.isUpdateLiveSyncTokenInPreviewMode();
        LiveSyncToken initialToken = ctx.getInitialToken();

        LiveSyncToken oldestTokenProcessed = ctx.oldestTokenWatcher.getOldestTokenProcessed();
        LOGGER.trace("oldestTokenProcessed = {}, synchronization result = {}", oldestTokenProcessed, ctx.syncResult);
        LiveSyncToken tokenToSet;
        if (ctx.isPreview() && !updateTokenInPreviewMode) {
            LOGGER.trace("Preview mode with updateTokenInPreviewMode=false -> token will not be updated.");
            tokenToSet = null;
        } else if (ctx.isDryRun() && !updateTokenInDryRun) {
            LOGGER.trace("Dry run mode with updateTokenInDryRun=false -> token will not be updated.");
            tokenToSet = null;
        } else if (ctx.canRun() && ctx.syncResult.isAllChangesFetched() && ctx.syncResult.isAllFetchedChangesProcessed()) {
            tokenToSet = ctx.finalToken != null ? ctx.finalToken : oldestTokenProcessed;
            LOGGER.trace("All changes fetched and processed (positively acknowledged). "
                    + "Task is not suspended. Updating token to {}", tokenToSet);
            // Note that it is theoretically possible that tokenToSet is null here: it happens when no changes are fetched from
            // the resource and the connector returns null from .sync() method. But in this case nothing wrong happens: the
            // token in task will simply stay as it is. That's the correct behavior for such a case.
        } else if (preciseTokenValue) {
            LOGGER.trace("Processing is not complete but we can count on precise token values.");
            tokenToSet = oldestTokenProcessed;
            LOGGER.info("Capability of providing precise token values is present. Token in task is updated so the processing will "
                    + "continue where it was stopped. New token value is '{}' (initial value was '{}')",
                    SchemaDebugUtil.prettyPrint(tokenToSet), SchemaDebugUtil.prettyPrint(initialToken));
        } else {
            LOGGER.trace("Processing is not complete and we cannot count on precise token values. So we'll not update the token");
            tokenToSet = null;
            LOGGER.info("Capability of providing precise token values is NOT present. Token will not be updated so the "
                            + "processing will restart from the beginning at next task run. So token value stays as it was: '{}'",
                    SchemaDebugUtil.prettyPrint(initialToken));
        }

        if (tokenToSet != null) {
            LOGGER.trace("Setting token value of {}", SchemaDebugUtil.prettyPrintLazily(tokenToSet));
            ctx.tokenStorage.setToken(tokenToSet, result);
            ctx.syncResult.setTokenUpdatedTo(tokenToSet);
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
        LiveSyncToken currentToken = resourceObjectConverter.fetchCurrentToken(ctx.context, result);
        if (currentToken == null) {
            LOGGER.warn("No current token provided by resource: {}. Live sync will not proceed: {}", ctx.context, ctx.task);
        } else if (ctx.isPreview() && !ctx.isUpdateLiveSyncTokenInPreviewMode()) { // TODO what about dry run?
            LOGGER.debug(
                    "We would set initial live sync token ({}) in task: {}; but not doing so because in simulation (preview) mode",
                    currentToken, ctx.task);
        } else {
            LOGGER.debug("Setting initial live sync token ({}) in task: {}.", currentToken, ctx.task);
            ctx.tokenStorage.setToken(currentToken, result);
            ctx.syncResult.setTokenUpdatedTo(currentToken);
        }
    }

    private class LiveSyncCtx {
        @NotNull private final SynchronizationOperationResult syncResult;
        @NotNull private final ProvisioningContext context;
        @NotNull private final Task task; // redundant to simplify access
        @NotNull private final LiveSyncOptions options;
        @NotNull private final LiveSyncTokenStorage tokenStorage;
        @NotNull private final OldestTokenWatcher oldestTokenWatcher;
        private LiveSyncToken finalToken; // TODO what exactly is this for? Be sure to set it only when all changes were processed

        private LiveSyncCtx(
                @NotNull ResourceOperationCoordinates coordinates,
                @NotNull Task task,
                LiveSyncOptions options,
                @NotNull LiveSyncTokenStorage tokenStorage,
                OperationResult result)
                throws ObjectNotFoundException, SchemaException, ConfigurationException,
                ExpressionEvaluationException {
            this.syncResult = new SynchronizationOperationResult();
            this.context = ctxFactory.createForBulkOperation(coordinates, task, result);
            this.task = task;
            this.options = options != null ? options : new LiveSyncOptions();
            this.tokenStorage = tokenStorage;
            this.oldestTokenWatcher = new OldestTokenWatcher();
        }

        LiveSyncToken getInitialToken() {
            return syncResult.getInitialToken();
        }

        private boolean hasInitialToken() {
            return getInitialToken() != null;
        }

        private boolean isPreview() {
            return options.getExecutionMode() == ExecutionModeType.PREVIEW;
        }

        private boolean isDryRun() {
            return options.getExecutionMode() == ExecutionModeType.DRY_RUN;
        }

        Integer getBatchSize() {
            return options.getBatchSize();
        }

        boolean isUpdateLiveSyncTokenInDryRun() {
            return options.isUpdateLiveSyncTokenInDryRun();
        }

        boolean isUpdateLiveSyncTokenInPreviewMode() {
            return options.isUpdateLiveSyncTokenInPreviewMode();
        }

        public boolean canRun() {
            return context.canRun();
        }
    }
}
