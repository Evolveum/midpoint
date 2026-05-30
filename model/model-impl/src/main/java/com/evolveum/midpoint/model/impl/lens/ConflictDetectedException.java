/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.util.exception.SeverityAwareException;

import org.jetbrains.annotations.NotNull;

/**
 * TODO better name
 *
 * An expected conflict was detected by repository, throwing {@link PreconditionViolationException}.
 * It is then converted to this one.
 *
 * Should occur on focus objects only. (For now.)
 */
public class ConflictDetectedException extends Exception implements SeverityAwareException {

    public ConflictDetectedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConflictDetectedException(Throwable cause) {
        super(cause);
    }

    @Override
    public @NotNull SeverityAwareException.Severity getSeverity() {
        // We mark the exception as "handled error" in order to prevent false errors being reported in OperationResult.
        //
        // It is OK to do so, because:
        //
        // 1. If it is (later) handled by repeating the operation, it is really a "handled error".
        // 2. Even if it causes the operation fail (e.g. if action=FAIL), the failure is represented by a separate
        // exception, with its own severity of FATAL_ERROR.
        return Severity.HANDLED_ERROR;
    }
}
