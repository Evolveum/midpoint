/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelationResult;
import com.evolveum.midpoint.model.api.correlator.Correlator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;

public abstract class BaseCorrelator implements Correlator {

    private static final String OP_CORRELATE_SUFFIX = ".correlate";

    @Override
    public @NotNull CorrelationResult correlate(
            @NotNull CorrelationContext correlationContext, @NotNull OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        OperationResult result = parentResult.subresult(getClass().getName() + OP_CORRELATE_SUFFIX)
                .build();
        try {
            getLogger().trace("Correlating:\n{}", correlationContext.debugDumpLazily(1));

            CorrelationResult correlationResult = correlateInternal(correlationContext, result);

            getLogger().trace("Result:\n{}", correlationResult.debugDumpLazily(1));

            result.addArbitraryObjectAsReturn("correlationResult", correlationResult);

            return correlationResult;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    protected abstract @NotNull CorrelationResult correlateInternal(
            @NotNull CorrelationContext correlationContext, @NotNull OperationResult result)
            throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException;

    protected abstract Trace getLogger();
}
