/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.noop;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelationResult;
import com.evolveum.midpoint.model.api.correlator.Correlator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * A correlator that does nothing: returns "no owner" in all cases.
 * Used as a replacement for not providing any filter before 4.5.
 */
class NoOpCorrelator implements Correlator {

    private static final Trace LOGGER = TraceManager.getTrace(NoOpCorrelator.class);

    @Override
    public CorrelationResult correlate(
            @NotNull CorrelationContext correlationContext,
            @NotNull Task task,
            @NotNull OperationResult result) {

        LOGGER.debug("Returning no owner.");
        return CorrelationResult.noOwner();
    }

    @Override
    public void resolve(
            @NotNull CaseType aCase,
            @NotNull String outcomeUri,
            @NotNull Task task,
            @NotNull OperationResult result) {
        // This correlator should never create any correlation cases.
        throw new IllegalStateException("The resolve() method should not be called for this correlator");
    }
}
