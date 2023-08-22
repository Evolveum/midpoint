/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.eval;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.selector.spec.SelectorClause;
import com.evolveum.midpoint.schema.selector.spec.ValueSelector;

import com.evolveum.midpoint.schema.traces.details.ProcessingTracer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.schema.selector.eval.SubjectedEvaluationContext.*;

/**
 * Context for matching a value against a selector or clause.
 *
 * @see ValueSelector#matches(PrismValue, MatchingContext)
 * @see SelectorClause#matches(PrismValue, MatchingContext)
 */
public class MatchingContext extends SelectorProcessingContext {

    public MatchingContext(
            @Nullable ObjectFilterExpressionEvaluator filterEvaluator,
            @NotNull ProcessingTracer<? super SelectorTraceEvent> tracer,
            @NotNull OrgTreeEvaluator orgTreeEvaluator,
            @Nullable SubjectedEvaluationContext subjectedEvaluationContext,
            @Nullable OwnerResolver ownerResolver,
            @Nullable ObjectResolver objectResolver,
            @NotNull ClauseProcessingContextDescription description,
            @NotNull DelegatorSelection delegatorSelection) {
        super(
                filterEvaluator,
                tracer,
                orgTreeEvaluator,
                subjectedEvaluationContext,
                ownerResolver,
                objectResolver,
                description,
                delegatorSelection);
    }

    public @NotNull MatchingContext next(
            @NotNull DelegatorSelection delegatorSelection, @NotNull String idDelta, @NotNull String textDelta) {
        return new MatchingContext(
                filterEvaluator,
                tracer,
                orgTreeEvaluator,
                subjectedEvaluationContext,
                ownerResolver,
                objectResolver,
                description.child(idDelta, textDelta),
                delegatorSelection);
    }

    public @NotNull MatchingContext next(@NotNull String idDelta, @NotNull String textDelta) {
        return new MatchingContext(
                filterEvaluator,
                tracer,
                orgTreeEvaluator,
                subjectedEvaluationContext,
                ownerResolver,
                objectResolver,
                description.child(idDelta, textDelta),
                delegatorSelection);
    }
}
