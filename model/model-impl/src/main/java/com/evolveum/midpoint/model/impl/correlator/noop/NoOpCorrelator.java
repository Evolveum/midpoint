/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.noop;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelationResult;
import com.evolveum.midpoint.model.impl.correlator.BaseCorrelator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * A correlator that does nothing: returns "no owner" in all cases.
 * Used as a replacement for not providing any filter before 4.5.
 */
class NoOpCorrelator extends BaseCorrelator {

    private static final Trace LOGGER = TraceManager.getTrace(NoOpCorrelator.class);

    @Override
    public @NotNull CorrelationResult correlateInternal(
            @NotNull CorrelationContext correlationContext,
            @NotNull OperationResult result) {
        return CorrelationResult.noOwner();
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }
}
