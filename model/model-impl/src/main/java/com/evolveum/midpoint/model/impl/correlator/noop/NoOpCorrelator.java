/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.noop;

import com.evolveum.midpoint.model.api.correlation.CorrelationPropertyDefinition;
import com.evolveum.midpoint.model.api.correlator.Confidence;
import com.evolveum.midpoint.model.api.correlator.CorrelationResult;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NoOpCorrelatorType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.correlation.CorrelationContext;
import com.evolveum.midpoint.model.impl.correlator.BaseCorrelator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.Collection;
import java.util.List;

/**
 * A correlator that does nothing: returns "no owner" in all cases.
 * Used as a replacement for not providing any filter before 4.5.
 */
public class NoOpCorrelator extends BaseCorrelator<NoOpCorrelatorType> {

    private static final Trace LOGGER = TraceManager.getTrace(NoOpCorrelator.class);

    NoOpCorrelator(@NotNull CorrelatorContext<NoOpCorrelatorType> correlatorContext, ModelBeans beans) {
        super(LOGGER, "no-op", correlatorContext, beans);
    }

    @Override
    public @NotNull CorrelationResult correlateInternal(
            @NotNull CorrelationContext correlationContext,
            @NotNull OperationResult result) {
        return CorrelationResult.empty();
    }

    @Override
    protected @NotNull Confidence checkCandidateOwnerInternal(
            @NotNull CorrelationContext correlationContext,
            @NotNull FocusType candidateOwner,
            @NotNull OperationResult result) {
        return Confidence.zero();
    }

    @Override
    public @NotNull Collection<CorrelationPropertyDefinition> getCorrelationPropertiesDefinitions(
            PrismObjectDefinition<? extends FocusType> focusDefinition, @NotNull Task task, @NotNull OperationResult result) {
        return List.of();
    }
}
