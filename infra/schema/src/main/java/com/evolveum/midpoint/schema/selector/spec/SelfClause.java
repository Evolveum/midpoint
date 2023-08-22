/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.spec;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.selector.eval.FilteringContext;
import com.evolveum.midpoint.schema.selector.eval.MatchingContext;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

public class SelfClause extends SelectorClause {

    @Override
    public @NotNull String getName() {
        return "self";
    }

    @Override
    public boolean matches(@NotNull PrismValue value, @NotNull MatchingContext ctx)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        var object = ObjectTypeUtil.asObjectTypeIfPossible(value);
        if (object == null) {
            traceNotApplicable(ctx, "Not an object");
            return false;
        }
        String principalOid = ctx.getPrincipalOid();
        if (principalOid == null) {
            traceNotApplicable(ctx, "no principal OID");
            return false;
        } else {
            String objectOid = object.getOid();
            if (principalOid.equals(objectOid)) {
                traceApplicable(ctx, "match on principal OID (%s)", objectOid);
                return true;
            } else if (ctx.getSelfOids().contains(objectOid)) {
                traceApplicable(ctx, "match on other 'self OID' (%s)", objectOid);
                return true;
            } else {
                traceNotApplicable(
                        ctx, "self OIDs: %s, object OID %s", ctx.getSelfOids(), objectOid);
                return false;
            }
        }
    }

    @Override
    public boolean toFilter(@NotNull FilteringContext ctx) {
        if (!ObjectType.class.isAssignableFrom(ctx.getRestrictedType())) {
            traceNotApplicable(ctx, "not an object type");
            return false;
        }

        String principalOid = ctx.getPrincipalOid();
        if (principalOid == null) {
            traceNotApplicable(ctx, "no principal");
            return false;
        }

        // TODO other self OIDs?

        addConjunct(ctx, PrismContext.get().queryFactory().createInOid(principalOid));
        return true;
    }

    @Override
    void addDebugDumpContent(StringBuilder sb, int indent) {
        // nothing to do here
    }

    @Override
    public String toString() {
        return "SelfClause{}";
    }
}
