/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.repo.api.ConflictWatcher;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConflictResolutionActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConflictResolutionType;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

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

    FocusConflictResolutionContext(@NotNull ConflictResolutionType resolutionPolicy) {
        this.resolutionPolicy = resolutionPolicy;
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
}
