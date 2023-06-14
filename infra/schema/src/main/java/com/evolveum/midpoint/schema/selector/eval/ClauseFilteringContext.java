/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.eval;

import com.evolveum.midpoint.schema.selector.eval.SubjectedEvaluationContext.DelegatorSelection;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.selector.spec.SelectorClause;
import com.evolveum.midpoint.schema.selector.spec.ValueSelector;

/**
 * Keeps everything needed to produce a filter from given clause.
 *
 * Most probably will be simplified in the future.
 */
@Experimental
public class ClauseFilteringContext extends ClauseMatchingContext {

    /** The object/value type we are searching for. */
    @NotNull private final Class<?> filterType;

    /** The type declared in the `type` selector (or {@link #filterType} if there's no such selector). */
    @NotNull private final Class<?> restrictedType;

    /** If we are adding the selector-generated filter to the original one, here it is. */
    @Nullable private final ObjectFilter originalFilter;

    /** TODO ... */
    private final boolean maySkipOnSearch;

    @Nullable private final ClauseApplicabilityPredicate clauseApplicabilityPredicate;

    @NotNull final FilterCollector filterCollector;

    public ClauseFilteringContext(
            @NotNull Class<?> filterType,
            @NotNull Class<?> restrictedType,
            @Nullable ObjectFilter originalFilter,
            boolean maySkipOnSearch,
            @Nullable ClauseApplicabilityPredicate clauseApplicabilityPredicate,
            @NotNull FilterCollector filterCollector,
            @Nullable ObjectFilterExpressionEvaluator filterEvaluator,
            @NotNull SelectorProcessingTracer tracer,
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
        this.filterType = filterType;
        this.restrictedType = restrictedType;
        this.originalFilter = originalFilter;
        this.maySkipOnSearch = maySkipOnSearch;
        this.clauseApplicabilityPredicate = clauseApplicabilityPredicate;
        this.filterCollector = filterCollector;
    }

    public @NotNull Class<?> getFilterType() {
        return filterType;
    }

    @NotNull
    public Class<?> getRestrictedType() {
        return restrictedType;
    }

    public void addConjunct(@NotNull SelectorClause clause, ObjectFilter conjunct) {
        filterCollector.addConjunct(clause, conjunct);
        traceConjunctAdded(clause, conjunct, null);
    }

    public void addConjunct(@NotNull SelectorClause clause, ObjectFilter conjunct, String message, Object... arguments) {
        filterCollector.addConjunct(clause, conjunct);
        traceConjunctAdded(clause, conjunct, message, arguments);
    }

    private void traceConjunctAdded(@NotNull SelectorClause clause, ObjectFilter conjunct, String message, Object... arguments) {
        if (tracer.isEnabled()) {
            tracer.trace(
                    new TraceEvent.ConjunctAdded(clause, conjunct, message, arguments, this));
        }
    }

    public void traceFilterProcessingStart(@NotNull ValueSelector selector) {
        if (tracer.isEnabled()) {
            tracer.trace(
                    new TraceEvent.FilterProcessingStarted(selector, this));
        }
    }

    public void traceFilterProcessingEnd(ValueSelector selector, boolean matched) {
        if (tracer.isEnabled()) {
            tracer.trace(
                    new TraceEvent.FilterProcessingFinished(selector, matched, this));
        }
    }

    public @NotNull FilterCollector getFilterCollector() {
        return filterCollector;
    }

    public @Nullable ObjectFilter getOriginalFilter() {
        return originalFilter;
    }

    public boolean maySkipOnSearch() {
        return maySkipOnSearch;
    }

    public @NotNull ClauseFilteringContext next(
            @NotNull Class<?> filterType,
            @NotNull FilterCollector filterCollector,
            @Nullable ObjectFilter originalFilter,
            @NotNull String idDelta,
            @NotNull String textDelta) {
        return new ClauseFilteringContext(
                filterType,
                filterType,
                originalFilter,
                maySkipOnSearch,
                clauseApplicabilityPredicate,
                filterCollector,
                filterEvaluator,
                tracer,
                orgTreeEvaluator,
                subjectedEvaluationContext,
                ownerResolver,
                objectResolver,
                description.child(idDelta, textDelta),
                delegatorSelection);
    }

    public boolean isClauseApplicable(SelectorClause clause) {
        return clauseApplicabilityPredicate == null || clauseApplicabilityPredicate.test(clause, this);
    }
}
