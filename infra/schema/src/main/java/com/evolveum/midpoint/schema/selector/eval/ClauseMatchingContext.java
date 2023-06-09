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
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/** Keeps everything needed to evaluate whether a clause matches given value. */
public class ClauseMatchingContext {

    @Nullable public final ObjectFilterExpressionEvaluator filterEvaluator;
    @NotNull public final MatchingTracer tracer;
    @NotNull public final OrgTreeEvaluator orgTreeEvaluator;
    @Nullable final SubjectedEvaluationContext subjectedEvaluationContext;
    @Nullable public final OwnerResolver ownerResolver;
    @Nullable final ObjectResolver objectResolver;
    @NotNull final ClauseProcessingContextDescription description;
    @NotNull final DelegatorSelection delegatorSelection;

    public ClauseMatchingContext(
            @Nullable ObjectFilterExpressionEvaluator filterEvaluator,
            @NotNull MatchingTracer tracer,
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

    public @NotNull ClauseMatchingContext next(
            @NotNull DelegatorSelection delegatorSelection, @NotNull String idDelta, @NotNull String textDelta) {
        return new ClauseMatchingContext(
                filterEvaluator,
                tracer,
                orgTreeEvaluator,
                subjectedEvaluationContext,
                ownerResolver,
                objectResolver,
                description.child(idDelta, textDelta),
                delegatorSelection);
    }

    public @NotNull ClauseMatchingContext next(@NotNull String idDelta, @NotNull String textDelta) {
        return new ClauseMatchingContext(
                filterEvaluator,
                tracer,
                orgTreeEvaluator,
                subjectedEvaluationContext,
                ownerResolver,
                objectResolver,
                description.child(idDelta, textDelta),
                delegatorSelection);
    }

    public PrismObject<? extends ObjectType> resolveReference(
            ObjectReferenceType ref, Object context, String referenceName) {
        if (objectResolver != null) {
            return objectResolver.resolveReference(ref, context, referenceName);
        } else {
            // TODO better message
            throw new UnsupportedOperationException("Object resolver is not available; " + ref + " cannot be resolved");
        }
    }

    public void traceMatchingStart(ValueSelector selector, @NotNull PrismValue value) {
        if (tracer.isEnabled()) {
            tracer.trace(
                    new TraceEvent.MatchingStarted(selector, value, this));
        }
    }

    public void traceMatchingEnd(ValueSelector selector, @NotNull PrismValue value, boolean match) {
        if (tracer.isEnabled()) {
            tracer.trace(
                    new TraceEvent.MatchingFinished(selector, value, match, this));
        }
    }

    public void traceClauseNotApplicable(@NotNull SelectorClause clause, @NotNull String message, Object... arguments) {
        if (tracer.isEnabled()) {
            tracer.trace(
                    new TraceEvent.ClauseApplicability(clause, false, this, message, arguments));

        }
    }

    public void traceClauseApplicable(@NotNull SelectorClause clause, @NotNull String message, Object... arguments) {
        if (tracer.isEnabled()) {
            tracer.trace(
                    new TraceEvent.ClauseApplicability(clause, true, this, message, arguments));

        }
    }
}
