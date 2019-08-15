/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.provisioning.impl.sync;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.provisioning.impl.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.ChangeHandler;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.LiveSyncCapabilityType;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.BooleanUtils.isNotFalse;
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

    /**
     * @param lastToken This is the value obtained either from the task (if exists) or current token fetched from the resource.
     *                  Note that these two cases are radically different.
     */
    @NotNull
    public SynchronizationOperationResult synchronize(ResourceShadowDiscriminator shadowCoordinates, PrismProperty<?> lastToken,
            Task task, TaskPartitionDefinitionType partition, OperationResult parentResult) throws ObjectNotFoundException,
            CommunicationException, GenericFrameworkException, SchemaException, ConfigurationException,
            SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException {

        SynchronizationOperationResult rv = new SynchronizationOperationResult();

        InternalMonitor.recordCount(InternalCounters.PROVISIONING_ALL_EXT_OPERATION_COUNT);

        ProvisioningContext ctx = ctxFactory.create(shadowCoordinates, task, parentResult);
        boolean isSimulate = partition != null && partition.getStage() == ExecutionModeType.SIMULATE;
        boolean retryLiveSyncErrors = isNotFalse(task.getExtensionPropertyRealValue(SchemaConstants.MODEL_EXTENSION_RETRY_LIVE_SYNC_ERRORS));

        LiveSyncCapabilityType capability = ctx.getEffectiveCapability(LiveSyncCapabilityType.class);
        boolean preciseTokenValue = capability != null && isTrue(capability.isPreciseTokenValue());

        OldestTokenWatcher oldestTokenWatcher = new OldestTokenWatcher();

        ChangeProcessingCoordinator coordinator = new ChangeProcessingCoordinator(
                () -> ctx.canRun() && !rv.isHaltingErrorEncountered(),
                changeProcessor, task, partition);

        ChangeHandler changeHandler = new ChangeHandler() {
            @Override
            public boolean handleChange(Change change, OperationResult result) {
                int sequentialNumber = oldestTokenWatcher.changeArrived(change.getToken());
                if (ctx.canRun()) {
                    ProcessChangeRequest request = new ProcessChangeRequest(change, ctx, isSimulate) {
                        /**
                         * This is a success reported by change processor. It is hopefully the usual case.
                         */
                        @Override
                        public void onSuccess() {
                            treatSuccess(sequentialNumber);
                        }

                        /**
                         * This is a "soft" error reported by change processor - i.e. the one without an exception.
                         * The issue should be already recorded in the operation result. Our task is to stop or
                         * continue processing, depending on the settings.
                         */
                        @Override
                        public void onError(OperationResult result) {
                            LOGGER.error("An error occurred during live synchronization in {}, when processing #{}: {}", task,
                                    sequentialNumber, change);
                            treatError(sequentialNumber);
                        }

                        /**
                         * This is a "hard" error reported by change processor - i.e. the one with an exception.
                         * The issue should be already recorded in the operation result. Our task is to stop or
                         * continue processing, depending on the settings.
                         */
                        @Override
                        public void onError(Throwable t, OperationResult result) {
                            LoggingUtils.logUnexpectedException(LOGGER, "An exception occurred during live synchronization in {},"
                                    + " when processing #{}: {}", t, task, sequentialNumber, change);
                            treatError(sequentialNumber);
                        }
                    };
                    try {
                        coordinator.submit(request, result);
                    } catch (InterruptedException e) {
                        LOGGER.trace("Got InterruptedException, probably the coordinator task was suspended. Let's stop fetching changes.");
                        rv.setSuspendEncountered(true);     // ok?
                        return false;
                    }
                }
                return ctx.canRun() && !rv.isHaltingErrorEncountered();
            }

            /**
             * This is a "hard" error reported in preparation stages of change processing. The change might be even null here
             * (in that case we hope at least token is present).
             */
            @Override
            public boolean handleError(@Nullable PrismProperty<?> token, @Nullable Change change,
                    @NotNull Throwable exception, @NotNull OperationResult result) {
                int sequentialNumber = oldestTokenWatcher.changeArrived(token);
                LoggingUtils
                        .logUnexpectedException(LOGGER, "An exception occurred during live synchronization in {}, "
                                + "as part of pre-processing #{}: {}", exception, task,
                                sequentialNumber, change != null ? "change " + change : "sync delta with token " + token);
                return treatError(sequentialNumber);
            }

            private boolean treatSuccess(int sequentialNumber) {
                oldestTokenWatcher.changeProcessed(sequentialNumber);
                rv.incrementChangesProcessed();
                if (task instanceof RunningTask) {
                    ((RunningTask) task).incrementProgressAndStoreStatsIfNeeded();
                }
                return ctx.canRun();
            }

            private boolean treatError(int sequentialNumber) {
                rv.incrementErrors();
                if (retryLiveSyncErrors) {
                    // We need to retry the failed change -- so we must not update the token.
                    // Moreover, we have to stop here, so that the changes will be applied in correct order.
                    rv.setHaltingErrorEncountered(true);
                    LOGGER.info("LiveSync encountered an error and 'retryLiveSyncErrors' is set to true: so exiting now with "
                                    + "the hope that the error will be cleared on the next task run. Task: {}; processed changes: {}",
                            ctx.getTask(), rv.getChangesProcessed());
                    return false;
                } else {
                    LOGGER.info("LiveSync encountered an error but 'retryLiveSyncErrors' is set to false: so continuing "
                                    + "as if nothing happened. Task: {}", ctx.getTask());
                    return treatSuccess(sequentialNumber);
                }
            }
        };

        try {
            resourceObjectConverter.fetchChanges(ctx, lastToken, changeHandler, parentResult);
        } finally {
            coordinator.setAllItemsSubmitted();
        }

        if (task instanceof RunningTask) {
            taskManager.waitForTransientChildren((RunningTask) task, parentResult);
            coordinator.updateOperationResult(parentResult);
        }

        if (!ctx.canRun()) {
            LOGGER.info("LiveSync was suspended during processing. Task: {}; processed changes: {}", ctx.getTask(),
                    rv.getChangesProcessed());
            rv.setSuspendEncountered(true);
        }

        PrismProperty<?> lastTokenProcessed = oldestTokenWatcher.getOldestTokenProcessed();
        if (rv.getChangesProcessed() == 0 && lastToken != null) {
            // Should we consider isSimulate here?
            LOGGER.trace("No changes to synchronize on {}", ctx.getResource());
            task.setExtensionProperty(lastToken);
        } else {
            boolean updateToken;
            if (isSimulate) {
                // Token should not be updated.
                updateToken = false;
            } else if (!rv.isHaltingErrorEncountered() && !rv.isSuspendEncountered()) {
                // Everything went OK.
                updateToken = true;
            } else if (preciseTokenValue) {
                // Something was wrong but we can continue on latest change.
                LOGGER.info("Capability of providing precise token values is present. Token is updated so the processing will "
                        + "continue where it was stopped. New token value is '{}'", lastTokenProcessed);
                updateToken = true;
            } else {
                // Something was wrong and we must restart from the beginning.
                LOGGER.info("Capability of providing precise token values is NOT present. Token will not be updated so the "
                                + "processing will restart from the beginning at next task run. So token value stays as it was: '{}'",
                        lastToken);
                // So we will not update the token.
                updateToken = false;
            }

            if (updateToken && lastTokenProcessed != null) {
                task.setExtensionProperty(lastTokenProcessed);
                rv.setTaskTokenUpdatedTo(lastTokenProcessed);
            }
        }
        task.flushPendingModifications(parentResult);
        return rv;
    }

    public PrismProperty<?> fetchCurrentToken(ResourceShadowDiscriminator shadowCoordinates,
            OperationResult parentResult) throws ObjectNotFoundException, CommunicationException,
            SchemaException, ConfigurationException, ExpressionEvaluationException {
        Validate.notNull(parentResult, "Operation result must not be null.");

        InternalMonitor.recordCount(InternalCounters.PROVISIONING_ALL_EXT_OPERATION_COUNT);

        ProvisioningContext ctx = ctxFactory.create(shadowCoordinates, null, parentResult);

        LOGGER.trace("Getting last token");
        PrismProperty<?> lastToken;
        try {
            lastToken = resourceObjectConverter.fetchCurrentToken(ctx, parentResult);
        } catch (ObjectNotFoundException | CommunicationException | SchemaException | ConfigurationException | ExpressionEvaluationException e) {
            parentResult.recordFatalError(e.getMessage(), e);
            throw e;
        }

        LOGGER.trace("Got last token: {}", SchemaDebugUtil.prettyPrint(lastToken));
        parentResult.recordSuccess();
        return lastToken;
    }
}
