/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.util.exception.SeverityAwareException;

import org.jetbrains.annotations.NotNull;

/**
 * Signals that we should stop the Projector because the focus is gone, and it was deleted by an inner clockwork run
 * (i.e., during a discovery process).
 *
 * The current behavior is that we simply exit the current (outer) clockwork execution ASAP, signalling the situation as
 * a warning.
 */
public class FocusGoneException extends ClockworkAbortedException implements SeverityAwareException {

    @Override
    public String getMessage() {
        return "Operation aborted: focus is gone";
    }

    @Override
    public @NotNull Severity getSeverity() {
        return Severity.WARNING;
    }

}
