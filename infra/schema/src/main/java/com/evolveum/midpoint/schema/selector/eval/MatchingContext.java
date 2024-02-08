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

import java.util.Objects;

import static com.evolveum.midpoint.schema.selector.eval.SubjectedEvaluationContext.*;

/**
 * Context for matching a value against a selector or clause.
 *
 * @see ValueSelector#matches(PrismValue, MatchingContext)
 * @see SelectorClause#matches(PrismValue, MatchingContext)
 */
public class MatchingContext extends SelectorProcessingContext {

    /**
     * Do we have the full information about the object being matched?
     *
     * @see SelectorClause#requiresFullInformation()
     */
    private final boolean fullInformationAvailable;

    public MatchingContext(
            @Nullable ObjectFilterExpressionEvaluator filterEvaluator,
            @NotNull ProcessingTracer<? super SelectorTraceEvent> tracer,
            @NotNull OrgTreeEvaluator orgTreeEvaluator,
            @Nullable SubjectedEvaluationContext subjectedEvaluationContext,
            @Nullable OwnerResolver ownerResolver,
            @Nullable ObjectResolver objectResolver,
            @NotNull ClauseProcessingContextDescription description,
            @NotNull DelegatorSelection delegatorSelection,
            boolean fullInformationAvailable) {
        super(
                filterEvaluator,
                tracer,
                orgTreeEvaluator,
                subjectedEvaluationContext,
                ownerResolver,
                objectResolver,
                description,
                delegatorSelection);
        this.fullInformationAvailable = fullInformationAvailable;
    }

    public @NotNull MatchingContext next(
            @NotNull DelegatorSelection delegatorSelection,
            @NotNull String idDelta,
            @NotNull String textDelta,
            boolean fullInformationAvailable) {
        return new MatchingContext(
                filterEvaluator,
                tracer,
                orgTreeEvaluator,
                subjectedEvaluationContext,
                ownerResolver,
                objectResolver,
                description.child(idDelta, textDelta),
                delegatorSelection,
                fullInformationAvailable);
    }

    public @NotNull MatchingContext next(
            @NotNull String idDelta, @NotNull String textDelta, Boolean fullInformationAvailable) {
        return new MatchingContext(
                filterEvaluator,
                tracer,
                orgTreeEvaluator,
                subjectedEvaluationContext,
                ownerResolver,
                objectResolver,
                description.child(idDelta, textDelta),
                delegatorSelection,
                Objects.requireNonNullElse(fullInformationAvailable, this.fullInformationAvailable));
    }

    public boolean isFullInformationAvailable() {
        return fullInformationAvailable;
    }
}
