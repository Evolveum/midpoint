/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.spec;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectTypeIfPossible;

import com.evolveum.midpoint.schema.selector.eval.SubjectedEvaluationContext.DelegatorSelection;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.selector.eval.ClauseFilteringContext;
import com.evolveum.midpoint.schema.selector.eval.ClauseMatchingContext;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class RequesterClause extends SelectorClause {

    @NotNull private final ValueSelector selector;

    private RequesterClause(@NotNull ValueSelector selector) {
        this.selector = selector;
    }

    static RequesterClause of(@NotNull ValueSelector selector) {
        return new RequesterClause(selector);
    }

    @Override
    public @NotNull String getName() {
        return "requester";
    }

    @Override
    public boolean matches(@NotNull PrismValue value, @NotNull ClauseMatchingContext ctx)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        var object = asObjectTypeIfPossible(value);
        if (object == null) {
            traceNotApplicable(ctx, "not an object");
            return false;
        }
        PrismObject<? extends ObjectType> requestor = getRequestor(object, ctx);
        if (requestor == null) {
            traceNotApplicable(ctx, "no requestor");
            return false;
        }
        boolean matches =
                selector.matches(
                        requestor.getValue(),
                        ctx.next(DelegatorSelection.NO_DELEGATOR, "req", "requestor"));
        traceApplicability(ctx, matches, "requestor object (%s) matches: %s", requestor, matches);
        return matches;
    }

    private PrismObject<? extends ObjectType> getRequestor(ObjectType object, @NotNull ClauseMatchingContext ctx) {
        if (object instanceof CaseType) {
            return ctx.resolveReference(((CaseType) object).getRequestorRef(), object, "requestor");
        } else {
            return null;
        }
    }

    @Override
    public boolean applyFilter(@NotNull ClauseFilteringContext ctx) {
        if (CaseType.class.isAssignableFrom(ctx.getRestrictedType())) {
            addConjunct(ctx, createFilter(ctx));
            return true;
        } else {
            traceNotApplicable(ctx, "requester clause is applicable only for cases when searching");
            return false;
        }
    }

    private ObjectFilter createFilter(@NotNull ClauseFilteringContext ctx) {
        return PrismContext.get().queryFor(CaseType.class)
                .item(CaseType.F_REQUESTOR_REF)
                .ref(ctx.getSelfOidsArray(DelegatorSelection.NO_DELEGATOR))
                .buildFilter();
    }

    @Override
    void addDebugDumpContent(StringBuilder sb, int indent) {
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "selector", selector, indent + 1);
    }
}
