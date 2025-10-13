/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
