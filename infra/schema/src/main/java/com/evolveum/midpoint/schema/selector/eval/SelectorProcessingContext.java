/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.eval;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.selector.eval.SubjectedEvaluationContext.DelegatorSelection;
import com.evolveum.midpoint.schema.selector.spec.SelectorClause;
import com.evolveum.midpoint.schema.selector.spec.ValueSelector;
import com.evolveum.midpoint.schema.traces.details.ProcessingTracer;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.evolveum.midpoint.schema.selector.eval.SelectorTraceEvent.*;

/**
 * Keeps everything needed to evaluate whether a clause matches given value or how is clause translated to a filter.
 *
 * Most probably will be simplified in the future.
 */
@Experimental
public abstract class SelectorProcessingContext {

    /** If provided, resolves expressions in filters. */
    @Nullable public final ObjectFilterExpressionEvaluator filterEvaluator;

    /**
     * Traces evaluation of selectors and clauses, typically into standard log.
     *
     * Mainly used for troubleshooting of selectors and their clauses; especially important for
     * https://docs.evolveum.com/midpoint/reference/diag/troubleshooting/authorizations.
     */
    @NotNull public final ProcessingTracer<? super SelectorTraceEvent> tracer;

    /** Evaluates organization tree questions (is descendant, is ancestor). Usually it is the repository itself. */
    @NotNull public final OrgTreeEvaluator orgTreeEvaluator;

    /** Provides information on the subject, i.e. the "self". */
    @Nullable final SubjectedEvaluationContext subjectedEvaluationContext;

    /** Resolves the object owners, mainly for the `owner` clause. */
    @Nullable public final OwnerResolver ownerResolver;

    /**
     * Resolves the object references to full objects when needed.
     *
     * @see #resolveReference(ObjectReferenceType, Object, String)
     */
    @Nullable final ObjectResolver objectResolver;

    /** Description of the processing context, mainly for tracing and error reporting. */
    @NotNull final ClauseProcessingContextDescription description;

    /**
     * Interpretation of `self` clause for the current evaluation.
     *
     * @see MatchingContext#child(DelegatorSelection, String, String)
     * @see #getSelfOids()
     */
    @NotNull final DelegatorSelection delegatorSelection;

    public SelectorProcessingContext(
            @Nullable ObjectFilterExpressionEvaluator filterEvaluator,
            @NotNull ProcessingTracer<? super SelectorTraceEvent> tracer,
            @NotNull OrgTreeEvaluator orgTreeEvaluator,
            @Nullable SubjectedEvaluationContext subjectedEvaluationContext,
            @Nullable OwnerResolver ownerResolver,
            @Nullable ObjectResolver objectResolver,
            @NotNull ClauseProcessingContextDescription description,
            @NotNull DelegatorSelection delegatorSelection) {
        this.filterEvaluator = filterEvaluator;
        this.tracer = tracer;
        this.orgTreeEvaluator = orgTreeEvaluator;
        this.subjectedEvaluationContext = subjectedEvaluationContext;
        this.ownerResolver = ownerResolver;
        this.objectResolver = objectResolver;
        this.description = description;
        this.delegatorSelection = delegatorSelection;
    }

    public @Nullable String getPrincipalOid() {
        return subjectedEvaluationContext != null ? subjectedEvaluationContext.getPrincipalOid() : null;
    }

    public @Nullable FocusType getPrincipalFocus() {
        return subjectedEvaluationContext != null ? subjectedEvaluationContext.getPrincipalFocus() : null;
    }

    public @NotNull Set<String> getSelfOids() {
        return subjectedEvaluationContext != null ? subjectedEvaluationContext.getSelfOids(delegatorSelection) : Set.of();
    }

    public @NotNull Set<String> getSelfPlusRolesOids(@NotNull DelegatorSelection delegatorSelectionMode) {
        return subjectedEvaluationContext != null ?
                subjectedEvaluationContext.getSelfPlusRolesOids(delegatorSelectionMode) : Set.of();
    }

    public @NotNull String[] getSelfOidsArray(DelegatorSelection delegation) {
        Collection<String> selfOids =
                subjectedEvaluationContext != null ? subjectedEvaluationContext.getSelfOids(delegation) : List.of();
        return selfOids.toArray(new String[0]);
    }

    public @NotNull String[] getSelfPlusRolesOidsArray(DelegatorSelection delegatorSelectionMode) {
        Collection<String> oids = getSelfPlusRolesOids(delegatorSelectionMode);
        return oids.toArray(new String[0]);
    }

    /**
     * Resolves reference to full object.
     *
     * TODO Note that this is not necessary in some cases (e.g. when comparing only with `self` clause).
     *  So we should do this more lazily. See MID-8899.
     */
    public PrismObject<? extends ObjectType> resolveReference(
            ObjectReferenceType ref, Object context, String referenceName) {
        if (objectResolver != null) {
            return objectResolver.resolveReference(ref, context, referenceName);
        } else {
            // TODO better message
            throw new UnsupportedOperationException("Object resolver is not available; " + ref + " cannot be resolved");
        }
    }

    //region Tracing
    public void traceMatchingStart(ValueSelector selector, @NotNull PrismValue value) {
        if (tracer.isEnabled()) {
            tracer.trace(
                    new MatchingStarted(selector, value, this));
        }
    }

    public void traceMatchingEnd(ValueSelector selector, @NotNull PrismValue value, boolean match) {
        if (tracer.isEnabled()) {
            tracer.trace(
                    new MatchingFinished(selector, value, match, this));
        }
    }

    public void traceClauseNotApplicable(@NotNull SelectorClause clause, @NotNull String message, Object... arguments) {
        if (tracer.isEnabled()) {
            tracer.trace(
                    new ClauseApplicability(clause, false, this, message, arguments));
        }
    }

    public void traceClauseApplicable(@NotNull SelectorClause clause, @NotNull String message, Object... arguments) {
        if (tracer.isEnabled()) {
            tracer.trace(
                    new ClauseApplicability(clause, true, this, message, arguments));
        }
    }
    //endregion
}
