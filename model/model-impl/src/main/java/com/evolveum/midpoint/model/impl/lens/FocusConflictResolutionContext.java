/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.api.ConflictWatcher;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConflictResolutionActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConflictResolutionType;

/**
 * Stores runtime information needed for conflict detection and resolution. Created fresh for each execution attempt.
 *
 * See {@link ClockworkConflictResolver} for details.
 */
class FocusConflictResolutionContext {

    /** Actual resolution policy. The action is never {@code null} or {@link ConflictResolutionActionType#NONE}. */
    @NotNull final ConflictResolutionType resolutionPolicy;

    /** Was a conflict detected by {@link PreconditionViolationException} (ex ante)? */
    private boolean conflictDetectedByPreconditionException;

    /** Was a conflict detected by {@link ConflictWatcher} (ex post)? */
    private boolean conflictDetectedByWatcher;

    /** Watches for concurrent focus object modifications. See {@link ClockworkConflictResolver}. */
    private transient ConflictWatcher focusConflictWatcher;

    /**
     * A copy of the original {@link LensContext} to support {@link ConflictResolutionActionType#RESTART} action.
     *
     * The idea is that we want to repeat the operation. Hence, the easiest way is to use a copy (clone) of the original
     * {@link LensContext} and related structures ({@link LensFocusContext}, {@link LensProjectionContext}s).
     *
     * Notes:
     *
     * - It is a "simple copy", excluding structures internal to the clockwork. The problem is that these are extremely
     * complex structures, and we are not quite certain what is internal to the clockwork and what is not. Maybe we need
     * something like "Initial lens context" - a well-defined starting point for the clockwork.
     *
     * - Obviously, it is present only for {@link ConflictResolutionActionType#RESTART} action. There's no need for it
     * for other actions.
     *
     * @see LensContext#simpleCopy()
     */
    private final LensContext<?> contextCopy;

    FocusConflictResolutionContext(@NotNull ConflictResolutionType resolutionPolicy, @Nullable LensContext<?> contextCopy) {
        this.resolutionPolicy = resolutionPolicy;
        this.contextCopy = contextCopy;
    }

    void recordConflictException() {
        conflictDetectedByPreconditionException = true;
    }

    ConflictWatcher getFocusConflictWatcher() {
        return focusConflictWatcher;
    }

    ConflictWatcher createAndRegisterFocusConflictWatcher(@NotNull String oid, RepositoryService repositoryService) {
        if (focusConflictWatcher != null) {
            throw new IllegalStateException("Focus conflict watcher defined twice");
        }
        return focusConflictWatcher = repositoryService.createAndRegisterConflictWatcher(oid);
    }

    void unregisterConflictWatcher(RepositoryService repositoryService) {
        if (focusConflictWatcher != null) {
            repositoryService.unregisterConflictWatcher(focusConflictWatcher);
            focusConflictWatcher = null;
        }
    }

    public @NotNull ConflictResolutionActionType getAction() {
        return Objects.requireNonNullElse(resolutionPolicy.getAction(), ConflictResolutionActionType.NONE);
    }

    void setFocusConflictDetectedByWatcher() {
        conflictDetectedByWatcher = true;
    }

    boolean wasConflictDetected() {
        return conflictDetectedByWatcher || conflictDetectedByPreconditionException;
    }

    LensContext<?> getContextCopy() {
        return contextCopy;
    }
}
