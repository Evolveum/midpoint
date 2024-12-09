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
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.ConflictWatcher;
import com.evolveum.midpoint.repo.api.RepositoryService;
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

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * Resolves conflicts occurring during clockwork processing (multiple threads modifying the same focus).
 */
@Component
public class ClockworkConflictResolver {

    private static final Trace LOGGER = TraceManager.getTrace(ClockworkConflictResolver.class);

    @Autowired private Clockwork clockwork;
    @Autowired private ContextFactory contextFactory;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    private static final int DEFAULT_MAX_CONFLICT_RESOLUTION_ATTEMPTS = 1; // synchronize with common-core-3.xsd
    private static final int DEFAULT_CONFLICT_RESOLUTION_DELAY_UNIT = 5000; // synchronize with common-core-3.xsd
    private static final int MAX_PRECONDITION_CONFLICT_RESOLUTION_ATTEMPTS = 3;

    static class Context {
        private boolean focusConflictPresent;
        private boolean conflictExceptionPresent;
        private ConflictResolutionType resolutionPolicy;

        void recordConflictException() {
            focusConflictPresent = true;
            conflictExceptionPresent = true;
        }
    }

    <F extends ObjectType> void createConflictWatcherOnStart(LensContext<F> context) {
        if (context.getFocusContext() != null && context.getFocusContext().getOid() != null) {
            context.createAndRegisterFocusConflictWatcher(context.getFocusContext().getOid(), repositoryService);
        }
    }

    public <O extends ObjectType> void createConflictWatcherAfterFocusAddition(LensContext<O> context, String oid, String expectedVersion) {
        // The watcher can already exist; if the OID was pre-existing in the object.
        if (context.getFocusConflictWatcher() == null) {
            ConflictWatcher watcher = context.createAndRegisterFocusConflictWatcher(oid, repositoryService);
            watcher.setExpectedVersion(expectedVersion);
        }
    }

    <O extends ObjectType> void unregisterConflictWatcher(LensContext<O> context) {
        context.unregisterConflictWatcher(repositoryService);
    }

    <F extends ObjectType> void detectFocusConflicts(LensContext<F> context, Context resolutionContext, OperationResult result) {
        resolutionContext.resolutionPolicy = ModelImplUtils.getConflictResolution(context);
        ConflictWatcher watcher = context.getFocusConflictWatcher();
        if (watcher != null
                && resolutionContext.resolutionPolicy != null
                && resolutionContext.resolutionPolicy.getAction() != ConflictResolutionActionType.NONE
                && repositoryService.hasConflict(watcher, result)) {
            LOGGER.debug("Found modify-modify conflict on {}", watcher);
            resolutionContext.focusConflictPresent = true;
        } else {
            resolutionContext.focusConflictPresent = false;
        }
    }

    // When null is returned, the process should be repeated
    <F extends ObjectType> HookOperationMode resolveFocusConflictIfPresent(LensContext<F> context, Context resolutionContext,
            HookOperationMode finalMode, Task task, OperationResult result) throws CommunicationException,
            ObjectNotFoundException, ObjectAlreadyExistsException, PolicyViolationException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        if (resolutionContext.focusConflictPresent) {
            assert finalMode == HookOperationMode.FOREGROUND;
            return resolveFocusConflict(context, resolutionContext, task, result);
        } else {
            if (context.getConflictResolutionAttemptNumber() > 0) {
                LOGGER.info("Resolved update conflict on attempt number {}", context.getConflictResolutionAttemptNumber());
            }
            return finalMode;
        }
    }

    private <F extends ObjectType> HookOperationMode resolveFocusConflict(LensContext<F> context,
            Context resolutionContext, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, ConfigurationException,
            CommunicationException, SecurityViolationException, PolicyViolationException, ObjectAlreadyExistsException {
        ConflictResolutionType resolutionPolicy = resolutionContext.resolutionPolicy;
        if (resolutionPolicy == null || resolutionPolicy.getAction() == ConflictResolutionActionType.NONE) {
            if (resolutionContext.conflictExceptionPresent) {
                throw new SystemException("Conflict exception present but resolution policy is null/NONE");
            }
            return HookOperationMode.FOREGROUND;
        }
        PrismObject<F> focusObject = context.getFocusContext() != null ? context.getFocusContext().getObjectAny() : null;
        ModelExecuteOptions options = ModelExecuteOptions.create();
        switch (resolutionPolicy.getAction()) {
            case FAIL:
                throw new SystemException("Conflict detected while updating " + focusObject);
            case LOG:
                LOGGER.warn("Conflict detected while updating {}", focusObject);
                return HookOperationMode.FOREGROUND;
            case ERROR: // TODO what to do with this?
            case RESTART: // TODO what to do with this?
            case RECOMPUTE:
                break;
            case RECONCILE:
                options.reconcile();
                break;
            case NONE:
                throw new AssertionError("Already treated");
            default:
                throw new IllegalStateException("Unsupported conflict resolution action: " + resolutionPolicy.getAction());
        }

        // so, recompute is the action
        LOGGER.debug("CONFLICT: Conflict detected while updating {}, recomputing (options={})", focusObject, options);

        String nonEligibilityReason = getNonEligibilityReason(context);
        if (nonEligibilityReason != null) {
            if (!nonEligibilityReason.isEmpty()) {
                LOGGER.warn("Not eligible for conflict resolution by repetition: {}", nonEligibilityReason);
            }
            return HookOperationMode.FOREGROUND;
        }

        ConflictResolutionType focusConflictResolution = new ConflictResolutionType();
        focusConflictResolution.setAction(ConflictResolutionActionType.ERROR);
        options.focusConflictResolution(focusConflictResolution);

        int preconditionAttempts = 0;
        while (true) {

            int attemptOld = context.getConflictResolutionAttemptNumber();
            int attemptNew = attemptOld + 1;
            boolean shouldExecuteAttempt = shouldExecuteAttempt(resolutionPolicy, attemptNew);
            if (!shouldExecuteAttempt) {
                LOGGER.warn("CONFLICT: Couldn't resolve conflict even after {} resolution attempt(s), giving up.", attemptOld);
                return HookOperationMode.FOREGROUND;
            }

            delay(context, resolutionPolicy, attemptNew + preconditionAttempts);

            Class<F> focusClass = context.getFocusContext().getObjectTypeClass();
            String oid = context.getFocusContext().getOid();

            // Not using read-only here because we are loading the focus (that will be worked with)
            PrismObject<F> focus = repositoryService.getObject(focusClass, oid, null, result);
            LensContext<FocusType> contextNew = contextFactory.createRecomputeContext(focus, options, task, result);
            contextNew.setProgressListeners(new ArrayList<>(emptyIfNull(context.getProgressListeners())));
            contextNew.setConflictResolutionAttemptNumber(attemptNew);

            LOGGER.debug("CONFLICT: Recomputing {} as reaction to conflict (options={}, attempts={},{}, readVersion={})",
                    context.getFocusContext().getHumanReadableName(), options, attemptNew, preconditionAttempts, contextNew.getFocusContext().getObjectReadVersion());

            ClockworkConflictResolver.Context conflictResolutionContext = new ClockworkConflictResolver.Context();

            // this is a recursion; but limited to max attempts which should not be a large number
            HookOperationMode hookOperationMode =
                    clockwork.runWithConflictDetection(contextNew, conflictResolutionContext, task, result);

            if (!conflictResolutionContext.focusConflictPresent) {
                LOGGER.debug("CONFLICT: Clean recompute of {} achieved (options={}, attempts={},{})",
                        context.getFocusContext().getHumanReadableName(), options, attemptNew, preconditionAttempts);
                return hookOperationMode;
            }

            // Actually, we could stop distinguish precondition-based and "normal" retries...
            if (conflictResolutionContext.conflictExceptionPresent) {
                preconditionAttempts++;
                LOGGER.debug("CONFLICT: Recompute precondition failed (attempt {}, precondition attempt {}), trying again", attemptNew, preconditionAttempts);
                if (preconditionAttempts < MAX_PRECONDITION_CONFLICT_RESOLUTION_ATTEMPTS) {
                    continue;
                }
                LOGGER.warn("CONFLICT: Couldn't resolve conflict even after {} resolution attempt(s) and {} precondition attempts, giving up.",
                        attemptOld, preconditionAttempts);
                return HookOperationMode.FOREGROUND;
            }
        }
    }

    private String getNonEligibilityReason(LensContext<?> context) {
        if (context.getFocusContext() == null) {
            return "No focus context, not possible to resolve conflict by focus recomputation"; // should really never occur
        }
        String oid = context.getFocusContext().getOid();
        if (oid == null) {
            return "No focus OID, not possible to resolve conflict by focus recomputation"; // should really never occur
        }
        Class<?> focusClass = context.getFocusContext().getObjectTypeClass();
        if (TaskType.class.isAssignableFrom(focusClass)) {
            return ""; // this is actually quite expected, so don't bother anyone with that
        }
        if (!FocusType.class.isAssignableFrom(focusClass)) {
            return String.format("Focus is not of FocusType (it is %s); not possible to resolve conflict by focus recomputation", focusClass.getName());
        }
        return null;
    }

    private boolean shouldExecuteAttempt(@NotNull ConflictResolutionType resolutionPolicy, int attempt) {
        int maxAttempts = defaultIfNull(resolutionPolicy.getMaxAttempts(), DEFAULT_MAX_CONFLICT_RESOLUTION_ATTEMPTS);
        return attempt <= maxAttempts;
    }

    private <F extends ObjectType> void delay(LensContext<F> context, @NotNull ConflictResolutionType resolutionPolicy, int attempt) {
        long delayRange = defaultIfNull(resolutionPolicy.getDelayUnit(), DEFAULT_CONFLICT_RESOLUTION_DELAY_UNIT) * (1L << attempt);
        long delay = (long) (Math.random() * delayRange);
        String message = "CONFLICT: Waiting "+delay+" milliseconds before starting conflict resolution (delay exponent: "+attempt+")";
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

    public <O extends ObjectType> boolean shouldCreatePrecondition(LensContext<O> context, ConflictResolutionType conflictResolution) {
        if (conflictResolution == null) {
            // can occur e.g. for projections, TODO clean up!
            return false;
        }
        ConflictResolutionActionType action = conflictResolution.getAction();
        if (action == null || action == ConflictResolutionActionType.NONE || action == ConflictResolutionActionType.LOG) {
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
}
