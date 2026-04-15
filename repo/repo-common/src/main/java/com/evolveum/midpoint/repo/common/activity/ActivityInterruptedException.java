/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SeverityAwareException;

import org.jetbrains.annotations.NotNull;

/**
 * Exception thrown when an activity is interrupted (typically by a user suspending the task).
 */
@Experimental
public class ActivityInterruptedException extends Exception implements SeverityAwareException {

    @Override
    public @NotNull Severity getSeverity() {
        // It is not an error, but not a success either.
        return Severity.WARNING;
    }
}
