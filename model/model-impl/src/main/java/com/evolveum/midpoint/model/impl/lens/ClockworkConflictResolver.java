/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.ConflictWatcher;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.VersionPrecondition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.WAITING;
import static com.evolveum.midpoint.model.api.ProgressInformation.StateType.EXITING;

import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import static java.util.Objects.requireNonNullElse;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

/**
 * Resolves conflicts occurring during clockwork processing when multiple threads modify the same focus object concurrently.
 *
 * For the feature description, see https://docs.evolveum.com/midpoint/reference/concepts/clockwork/conflict-resolution-howto/.
 *
 * It is an optimistic locking strategy. The conflict detection is based on comparing object versions. There are two mechanisms
 * employed:
 *
 * - precondition-based detection using {@link VersionPrecondition} objects
 * - watching for conflicts using {@link ConflictWatcher} objects
 *
 * The former is able to prevent unwanted (concurrent) modifications, because it halts the operation as soon as a conflict
 * is detected, i.e. before the actual focus modification takes place.
 *
 * The latter is used to detect that a conflict happened so we can later act upon it.
 *
 * For some retry actions (namely: recompute, reconcile) we use only the watching approach.
 *
 * See also the docs in the related configuration element ({@link ConflictResolutionType}).
 */
@Component
public class ClockworkConflictResolver {

    private static final Trace LOGGER = TraceManager.getTrace(ClockworkConflictResolver.class);

    @Autowired private Clockwork clockwork;
    @Autowired private ContextFactory contextFactory;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    private static final int DEFAULT_MAX_CONFLICT_RESOLUTION_ATTEMPTS = 1; // synchronize with common-core-3.xsd
    private static final int DEFAULT_CONFLICT_RESOLUTION_DELAY_UNIT = 5000; // synchronize with common-core-3.xsd

    //region Precondition-based approach

    public <O extends ObjectType> boolean shouldCreatePrecondition(LensContext<O> context) {
        var conflictContext = context.getFocusConflictResolutionContext();
        if (conflictContext == null) {
            return false; // No need to deal with conflicts at all
        }
        ConflictResolutionActionType action = conflictContext.getAction();
        if (action == ConflictResolutionActionType.NONE
                || action == ConflictResolutionActionType.LOG
                || action == ConflictResolutionActionType.RECOMPUTE
                || action == ConflictResolutionActionType.RECONCILE) {
            // For RECOMPUTE and RECONCILE we want the operation to be executed in full. Only after it's done,
            // we'll evaluate if there was a conflict and if so, we'll do the recomputation.
            LOGGER.debug("Not creating modification precondition because conflict resolution action is {}", action);
            return false;
        }
        String reason = getNonEligibilityReason(context);
        if (reason != null) {
            LOGGER.debug("Not creating modification precondition because: {}", reason);
            return false;
        }
        return true;
    }

    //endregion

    //region Watcher-based approach

    <F extends ObjectType> void createConflictWatcherOnStart(LensContext<F> lensContext) {
        var conflictContext = lensContext.getFocusConflictResolutionContext();
        if (conflictContext != null // If null, there's no need to do anything with conflict watching
                && lensContext.getFocusContext() != null
                && lensContext.getFocusContext().getOid() != null) {
            conflictContext.createAndRegisterFocusConflictWatcher(lensContext.getFocusContext().getOid(), repositoryService);
        }
    }

    public <O extends ObjectType> void createConflictWatcherAfterFocusAddition(
            LensContext<O> context, String focusOid, String initialFocusVersion) {
        var conflictContext = context.getFocusConflictResolutionContext();
        if (conflictContext == null) {
            return; // No need to do anything with conflict watching
        }
        // The watcher may already exist; e.g. if the OID was pre-existing in the object.
        if (conflictContext.getFocusConflictWatcher() == null) {
            ConflictWatcher watcher = conflictContext.createAndRegisterFocusConflictWatcher(focusOid, repositoryService);
            watcher.setExpectedVersion(initialFocusVersion);
        }
    }

    <O extends ObjectType> void unregisterConflictWatcher(LensContext<O> context) {
        var conflictContext = context.getFocusConflictResolutionContext();
        if (conflictContext == null) {
            return; // No need to do anything with conflict watching
        }
        conflictContext.unregisterConflictWatcher(repositoryService);
    }

    <F extends ObjectType> void detectFocusConflictsUsingWatcher(LensContext<F> lensContext, OperationResult result) {
        var conflictContext = lensContext.getFocusConflictResolutionContext();
        if (conflictContext == null) {
            return;
        }
        assert conflictContext.getAction() != ConflictResolutionActionType.NONE;
        var watcher = conflictContext.getFocusConflictWatcher();
        if (watcher != null && repositoryService.hasConflict(watcher, result)) {
            LOGGER.debug("Found modify-modify conflict on {}", watcher);
            conflictContext.setFocusConflictDetectedByWatcher();
        }
    }

    //endregion

    //region Resolving detected conflicts

    /**
     * Resolves focus modification conflict if present.
     *
     * The resolution is usually done by repeating the operation until it succeeds.
     */
    <F extends ObjectType> @NotNull HookOperationMode resolveFocusConflictIfPresent(
            LensContext<F> context, HookOperationMode finalMode, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, PolicyViolationException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        var conflictContext = context.getFocusConflictResolutionContext();
        if (conflictContext == null || !conflictContext.wasConflictDetected()) {
            return finalMode;
        }
        assert finalMode == HookOperationMode.FOREGROUND; // Not quite clear why is this check here!

        ConflictResolutionType resolutionPolicy = conflictContext.resolutionPolicy;
        ConflictResolutionActionType action = conflictContext.getAction();
        assert resolutionPolicy != null && action != ConflictResolutionActionType.NONE;

        PrismObject<F> focusObject = context.getFocusContext() != null ? context.getFocusContext().getObjectAny() : null;
        boolean restart; // true = restart the operation, false = recompute/reconcile the object
        ModelExecuteOptions options = ModelExecuteOptions.create();
        switch (action) {
            case FAIL:
                throw new SystemException("Conflict detected while updating " + focusObject);
            case LOG:
                LOGGER.warn("Conflict detected while updating {}", focusObject);
                return finalMode;
            case NONE:
                throw new AssertionError("Should not be here");
            case ERROR:
                // TODO here we should throw an exception that could be caught by the client so it could repeat the operation
            case RECOMPUTE:
                restart = false;
                break;
            case RECONCILE:
                restart = false;
                options.reconcile();
                break;
            case RESTART:
                restart = true;
                break;
            default:
                throw new IllegalStateException("Unsupported conflict resolution action: " + action);
        }

        LOGGER.debug("FOCUS UPDATE CONFLICT: Conflict detected while updating {}, {} (options={})",
                focusObject, restart ? "restarting the operation" : "recomputing the object", options);

        // TODO reconsider this "eligibility check"
        String nonEligibilityReason = getNonEligibilityReason(context);
        if (nonEligibilityReason != null) {
            if (!nonEligibilityReason.isEmpty()) {
                LOGGER.warn("Not eligible for conflict resolution: {}", nonEligibilityReason);
            }
            return HookOperationMode.FOREGROUND;
        }

        // For restart: capture the original primary delta and channel before they are gone
        ObjectDelta<F> originalPrimaryDelta = restart && context.getFocusContext() != null
                ? context.getFocusContext().getPrimaryDelta()
                : null;
        String originalChannel = context.getChannel();

        int maxAttempts = requireNonNullElse(resolutionPolicy.getMaxAttempts(), DEFAULT_MAX_CONFLICT_RESOLUTION_ATTEMPTS);
        for (int retryAttempt = 1; retryAttempt <= maxAttempts; retryAttempt++) {

            delay(context, resolutionPolicy, retryAttempt);

            Class<F> focusClass = context.getFocusContext().getObjectTypeClass();
            String oid = context.getFocusContext().getOid();

            // Not using read-only here because we are loading the focus (that will be worked with)
            PrismObject<F> focus = repositoryService.getObject(focusClass, oid, null, result);
            LensContext<FocusType> newContext;
            if (restart) {
                //noinspection unchecked
                newContext = (LensContext<FocusType>) contextFactory.createRestartContext(
                        focus, originalPrimaryDelta, originalChannel, options, task);
            } else {
                newContext = contextFactory.createRecomputeContext(focus, options, task, result);
            }
            newContext.setProgressListeners(new ArrayList<>(emptyIfNull(context.getProgressListeners())));

            LOGGER.debug("FOCUS UPDATE CONFLICT: Repeating the operation on {} as a reaction to conflict "
                            + "(options={}, retry attempt={}, readVersion={})",
                    context.getFocusContext().getHumanReadableName(),
                    options,
                    retryAttempt,
                    newContext.getFocusContext().getObjectReadVersion());

            HookOperationMode hookOperationMode = clockwork.runWithConflictDetection(newContext, task, result);

            var newConflictContext = newContext.getFocusConflictResolutionContext();
            if (newConflictContext != null && newConflictContext.wasConflictDetected()) {
                LOGGER.debug("FOCUS UPDATE CONFLICT: Retry failed (attempt {}), will try again", retryAttempt);
            } else {
                LOGGER.debug("FOCUS UPDATE CONFLICT: Clean retry of {} achieved (options={}, retry attempt={})",
                        context.getFocusContext().getHumanReadableName(), options, retryAttempt);
                return hookOperationMode;
            }
        }

        LOGGER.warn("CONFLICT: Couldn't resolve conflict even after {} resolution attempt(s), giving up.", maxAttempts);
        return HookOperationMode.FOREGROUND;
    }

    // TODO reconsider this method
    private String getNonEligibilityReason(LensContext<?> context) {
        var focusContext = stateNonNull(context.getFocusContext(), "No focus context but focus conflict detected?");
        String oid = focusContext.getOid();
        if (oid == null) {
            return "No focus OID"; // should really never occur
        }
        Class<?> focusClass = context.getFocusContext().getObjectTypeClass();
        if (!FocusType.class.isAssignableFrom(focusClass)) {
            return String.format("Focus conflict detection is supported only for FocusType, not for %s", focusClass.getName());
        }
        return null;
    }

    private <F extends ObjectType> void delay(LensContext<F> context, @NotNull ConflictResolutionType resolutionPolicy, int attempt) {
        int delayUnit = requireNonNullElse(resolutionPolicy.getDelayUnit(), DEFAULT_CONFLICT_RESOLUTION_DELAY_UNIT);
        long delayRange = delayUnit * (1L << attempt);
        long delay = (long) (Math.random() * delayRange);
        String message = "CONFLICT: Waiting " + delay + " milliseconds before starting conflict resolution (delay exponent: " + attempt + ")";
        // TODO convey information about waiting time after some GUI mechanism for displaying it is available
        //  (showing text messages is currently really ugly)
        context.reportProgress(new ProgressInformation(WAITING, EXITING));
        LOGGER.debug(message);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            // ignore
        }
        context.reportProgress(new ProgressInformation(WAITING, EXITING));
    }
    //endregion
}
