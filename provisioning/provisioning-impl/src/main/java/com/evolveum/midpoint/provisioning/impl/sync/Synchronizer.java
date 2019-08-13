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
import com.evolveum.midpoint.provisioning.ucf.api.ChangeListener;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncUpdateListeningActivityInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.LiveSyncCapabilityType;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * Provides interface between Live Synchronization and Async Update and the change processing engine.
 */
@Component
public class Synchronizer {

    private static final Trace LOGGER = TraceManager.getTrace(Synchronizer.class);

    @Autowired private ProvisioningContextFactory ctxFactory;
    @Autowired private ResourceObjectConverter resourceObjectConverter;
    @Autowired private ChangeProcessor changeProcessor;

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
        boolean retryLiveSyncErrors = !Boolean.FALSE.equals(task.getExtensionPropertyRealValue(SchemaConstants.MODEL_EXTENSION_RETRY_LIVE_SYNC_ERRORS));

        Holder<PrismProperty<?>> lastTokenSeen = new Holder<>(lastToken);

        LiveSyncCapabilityType capability = ctx.getEffectiveCapability(LiveSyncCapabilityType.class);
        boolean preciseTokenValue = capability != null && isTrue(capability.isPreciseTokenValue());

        ChangeHandler changeHandler = new ChangeHandler() {
            @Override
            public boolean handleChange(Change change, OperationResult result) {
                if (!ctx.canRun()) {
                    return false;
                } else if (change.isTokenOnly()) {
                    LOGGER.trace("Found token-only change: {}", change);
                    lastTokenSeen.setValue(change.getToken());
                    return true;
                } else {
                    try {
                        if (changeProcessor.processChange(ctx, isSimulate, change, task, partition, result)) {
                            return handleOk(change);
                        } else {
                            // we hope the error is already recorded in the result
                            return handleError(change);
                        }
                    } catch (Throwable t) {
                        return handleError(change, t, result);
                    }
                }
            }

            private boolean handleOk(Change change) {
                lastTokenSeen.setValue(change.getToken());
                rv.incrementChangesProcessed();
                if (task instanceof RunningTask) {
                    ((RunningTask) task).incrementProgressAndStoreStatsIfNeeded();
                }
                return ctx.canRun();
            }

            @Override
            public boolean handleError(@Nullable Change change, @NotNull Throwable exception, @NotNull OperationResult result) {
                LoggingUtils
                        .logUnexpectedException(LOGGER, "Exception during processing object as part of live synchronization in {}",
                                exception, task);
                result.recordFatalError(exception);
                return handleError(change);
            }

            private boolean handleError(Change change) {
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
                    return handleOk(change);
                }
            }
        };

        resourceObjectConverter.fetchChanges(ctx, lastToken, changeHandler, parentResult);

        if (!ctx.canRun()) {
            LOGGER.info("LiveSync was suspended during processing. Task: {}; processed changes: {}", ctx.getTask(),
                    rv.getChangesProcessed());
            rv.setSuspendEncountered(true);
        }

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
                        + "continue where it was stopped. New token value is '{}'", lastTokenSeen);
                updateToken = true;
            } else {
                // Something was wrong and we must restart from the beginning.
                LOGGER.info("Capability of providing precise token values is NOT present. Token will not be updated so the "
                                + "processing will restart from the beginning at next task run. So token value stays as it was: '{}'",
                        lastToken);
                // So we will not update the token.
                updateToken = false;
            }

            if (updateToken) {
                task.setExtensionProperty(lastTokenSeen.getValue());
                rv.setTaskTokenUpdatedTo(lastTokenSeen.getValue());
            }
        }
        task.flushPendingModifications(parentResult);
        return rv;
    }

    public String startListeningForAsyncUpdates(ResourceShadowDiscriminator shadowCoordinates, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            ExpressionEvaluationException {
        InternalMonitor.recordCount(InternalCounters.PROVISIONING_ALL_EXT_OPERATION_COUNT);

        ProvisioningContext ctx = ctxFactory.create(shadowCoordinates, task, parentResult);
        ChangeListener listener = change -> {
            try {
                boolean success = changeProcessor.processChange(ctx, false, change, task, null, parentResult);
                if (task instanceof RunningTask) {
                    ((RunningTask) task).incrementProgressAndStoreStatsIfNeeded();
                }
                return success;
            } catch (Throwable t) {
                throw new SystemException("Couldn't process async update: " + t.getMessage(), t);
            }
        };
        return resourceObjectConverter.startListeningForAsyncUpdates(ctx, listener, parentResult);
    }

    @SuppressWarnings("unused")
    public void stopListeningForAsyncUpdates(String listeningActivityHandle, Task task, OperationResult parentResult) {
        InternalMonitor.recordCount(InternalCounters.PROVISIONING_ALL_EXT_OPERATION_COUNT);
        resourceObjectConverter.stopListeningForAsyncUpdates(listeningActivityHandle, parentResult);
    }

    @SuppressWarnings("unused")
    public AsyncUpdateListeningActivityInformationType getAsyncUpdatesListeningActivityInformation(
            @NotNull String listeningActivityHandle, Task task, OperationResult parentResult) {
        return resourceObjectConverter.getAsyncUpdatesListeningActivityInformation(listeningActivityHandle, parentResult);
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
