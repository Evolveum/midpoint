/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds;

import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;

import static com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluator.EvaluationContext.empty;
import static com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluator.EvaluationContext.forProjectionContext;

/**
 * Everything needed to evaluate already prepared inbound mapping.
 *
 * @param <V> type of mapping output value
 * @param <D> type of mapping output value definition (property, container, ...)
 */
public class MappingEvaluationRequest<V extends PrismValue, D extends ItemDefinition<?>> {

    /** Mapping to be evaluated. */
    @NotNull private final MappingImpl<V, D> mapping;

    /** True if the source (shadow) is being deleted. */
    private final boolean sourceIsBeingDeleted;

    /** The context to be fed to the mapping evaluator. */
    @NotNull private final MappingEvaluator.EvaluationContext<V, D> evaluationContext;

    public MappingEvaluationRequest(
            @NotNull MappingImpl<V, D> mapping,
            boolean sourceIsBeingDeleted,
            @Nullable LensProjectionContext projectionContext) {
        this.mapping = mapping;
        this.sourceIsBeingDeleted = sourceIsBeingDeleted;
        this.evaluationContext = projectionContext != null ? forProjectionContext(projectionContext) : empty();
    }

    public @NotNull MappingImpl<V, D> getMapping() {
        return mapping;
    }

    @NotNull MappingEvaluator.EvaluationContext<V, D> getEvaluationContext() {
        return evaluationContext;
    }

    boolean isSourceBeingDeleted() {
        return sourceIsBeingDeleted;
    }

    @Override
    public String toString() {
        return mapping.toString();
    }
}
