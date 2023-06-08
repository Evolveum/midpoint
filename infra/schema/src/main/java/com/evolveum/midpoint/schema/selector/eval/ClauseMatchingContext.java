/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.eval;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.selector.eval.SubjectedEvaluationContext.Delegation;
import com.evolveum.midpoint.schema.selector.spec.SelectorClause;
import com.evolveum.midpoint.schema.selector.spec.ValueSelector;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/** Keeps everything needed to evaluate whether a clause matches given value. */
public class ClauseMatchingContext {

    @Nullable public final ObjectFilterExpressionEvaluator filterEvaluator;
    @NotNull public final MatchingTracer tracer;
    @NotNull public final OrgTreeEvaluator orgTreeEvaluator;
    @Nullable final SubjectedEvaluationContext subjectedEvaluationContext;
    @Nullable public final OwnerResolver ownerResolver;
    @Nullable final ObjectResolver objectResolver;
    @NotNull final ClauseProcessingContextDescription description;
    @Nullable final Delegation delegation; // TODO

    public ClauseMatchingContext(
            @Nullable ObjectFilterExpressionEvaluator filterEvaluator,
            @NotNull MatchingTracer tracer,
            @NotNull OrgTreeEvaluator orgTreeEvaluator,
            @Nullable SubjectedEvaluationContext subjectedEvaluationContext,
            @Nullable OwnerResolver ownerResolver,
            @Nullable ObjectResolver objectResolver,
            @NotNull ClauseProcessingContextDescription description,
            @Nullable Delegation delegation) {
        this.filterEvaluator = filterEvaluator;
        this.tracer = tracer;
        this.orgTreeEvaluator = orgTreeEvaluator;
        this.subjectedEvaluationContext = subjectedEvaluationContext;
        this.ownerResolver = ownerResolver;
        this.objectResolver = objectResolver;
        this.description = description;
        this.delegation = delegation;
    }

    public @Nullable String getPrincipalOid() {
        return subjectedEvaluationContext != null ? subjectedEvaluationContext.getPrincipalOid() : null;
    }

    public @Nullable FocusType getPrincipalFocus() {
        return subjectedEvaluationContext != null ? subjectedEvaluationContext.getPrincipalFocus() : null;
    }

    public @NotNull Collection<String> getSelfOids() {
        return subjectedEvaluationContext != null ? subjectedEvaluationContext.getSelfOids(delegation) : List.of();
    }

    public @NotNull String[] getSelfOidsArray(Delegation delegation) {
        return getSelfOids(delegation).toArray(new String[0]);
    }

    private @NotNull Collection<String> getSelfOids(Delegation delegation) {
        return subjectedEvaluationContext != null ? subjectedEvaluationContext.getSelfOids(delegation) : List.of();
    }

    public @NotNull ClauseMatchingContext next(
            @Nullable Delegation delegation, @NotNull String idDelta, @NotNull String textDelta) {
        return new ClauseMatchingContext(
                filterEvaluator,
                tracer,
                orgTreeEvaluator,
                subjectedEvaluationContext,
                ownerResolver,
                objectResolver,
                description.child(idDelta, textDelta),
                delegation);
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
                delegation);
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
