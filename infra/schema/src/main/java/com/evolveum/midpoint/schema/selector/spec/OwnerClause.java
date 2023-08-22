/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.spec;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectTypeIfPossible;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.schema.selector.eval.FilteringContext;
import com.evolveum.midpoint.schema.selector.eval.MatchingContext;
import com.evolveum.midpoint.schema.selector.eval.OwnerResolver;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

public class OwnerClause extends SelectorClause {

    @NotNull private final ValueSelector selector;

    private OwnerClause(@NotNull ValueSelector selector) {
        this.selector = selector;
    }

    static OwnerClause of(@NotNull ValueSelector selector) {
        return new OwnerClause(selector);
    }

    @Override
    public @NotNull String getName() {
        return "owner";
    }

    @Override
    public boolean matches(@NotNull PrismValue value, @NotNull MatchingContext ctx)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        var object = asObjectTypeIfPossible(value);
        if (object == null) {
            traceNotApplicable(ctx, "Not an object");
            return false;
        }
        OwnerResolver ownerResolver = ctx.ownerResolver;
        if (ownerResolver == null) {
            traceNotApplicable(ctx, "no owner resolver");
            return false;
        }
        PrismObject<? extends FocusType> owner = ownerResolver.resolveOwner(object.asPrismObject());
        if (owner == null) {
            traceNotApplicable(ctx, "no owner");
            return false;
        }
        boolean matches =
                selector.matches(owner.getValue(), ctx.next("o", "owner"));
        traceApplicability(ctx, matches, "owner (%s) matches: %s", owner, matches);
        return matches;
    }

    @Override
    public boolean toFilter(@NotNull FilteringContext ctx) {
        // TODO: MID-3899
        // TODO what if owner is specified not as "self" ?
        if (TaskType.class.isAssignableFrom(ctx.getRestrictedType())) {
            FocusType principal = ctx.getPrincipalFocus();
            if (principal != null) {
                addConjunct(ctx, applyOwnerFilterOwnerRef(principal));
                return true;
            } else {
                traceNotApplicable(ctx, "no principal");
                return false;
            }
        } else {
            traceNotApplicable(ctx, "applicability when searching is limited to TaskType objects");
            return false;
        }
    }

    // TODO review this legacy code
    private ObjectFilter applyOwnerFilterOwnerRef(FocusType principalFocus) {
        S_FilterExit builder = PrismContext.get().queryFor(TaskType.class)
                .item(TaskType.F_OWNER_REF).ref(principalFocus.getOid());
        // TODO don't understand this code
        for (ObjectReferenceType subjectParentOrgRef : principalFocus.getParentOrgRef()) {
            if (PrismContext.get().isDefaultRelation(subjectParentOrgRef.getRelation())) {
                builder = builder.or().item(TaskType.F_OWNER_REF).ref(subjectParentOrgRef.getOid());
            }
        }
        return builder.buildFilter();
    }

    @Override
    void addDebugDumpContent(StringBuilder sb, int indent) {
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "selector", selector, indent + 1);
    }

    @Override
    public String toString() {
        return "OwnerClause{" +
                "selector=" + selector +
                "}";
    }
}
