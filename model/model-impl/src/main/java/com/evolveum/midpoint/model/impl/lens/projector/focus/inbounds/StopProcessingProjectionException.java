/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds;

import com.evolveum.midpoint.util.exception.CommonException;

import com.evolveum.midpoint.util.exception.SeverityAwareException;
import org.jetbrains.annotations.NotNull;

/**
 * Indicates we should stop evaluating inbounds for given projection.
 *
 * Currently thrown when the full shadow cannot be loaded, or when the projection context is broken.
 * (So it has no use in pre-inbounds.)
 */
public class StopProcessingProjectionException extends Exception implements SeverityAwareException {

    @Override
    public @NotNull SeverityAwareException.Severity getSeverity() {
        // Does not necessarily indicate an error.
        return SeverityAwareException.Severity.NOT_APPLICABLE;
    }
}
