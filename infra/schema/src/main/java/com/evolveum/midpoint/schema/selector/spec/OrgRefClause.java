/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.spec;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.selector.eval.ClauseFilteringContext;
import com.evolveum.midpoint.schema.selector.eval.ClauseMatchingContext;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import static com.evolveum.midpoint.util.MiscUtil.*;

public class OrgRefClause extends SelectorClause {

    @NotNull private final String orgOid;

    private OrgRefClause(@NotNull String orgOid) {
        this.orgOid = orgOid;
    }

    static OrgRefClause of(@NotNull ObjectReferenceType orgRef) throws ConfigurationException {
        return new OrgRefClause(
                configNonNull(orgRef.getOid(), "No OID in orgRef clause: %s", orgRef));
    }

    public @NotNull String getOrgOid() {
        return orgOid;
    }

    @Override
    public @NotNull String getName() {
        return "orgRef";
    }

    @Override
    public boolean matches(@NotNull PrismValue value, @NotNull ClauseMatchingContext ctx)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        Object realValue = value.getRealValueIfExists();
        ObjectType objectBean = realValue instanceof ObjectType ? ((ObjectType) realValue) : null;
        if (objectBean != null) {
            if (ctx.orgTreeEvaluator.isDescendant(objectBean.asPrismObject(), orgOid)) {
                return true;
            }
            traceNotApplicable(ctx, "object OID {} (org={})", objectBean.getOid(), orgOid);
        } else {
            if (ctx.tracer.isEnabled()) {
                traceNotApplicable(ctx, "non-prism object %s (org=%s)", getDiagInfo(value), orgOid);
            }
        }
        return false;
    }

    @Override
    public boolean applyFilter(@NotNull ClauseFilteringContext ctx) throws SchemaException {
        addConjunct(
                ctx,
                PrismContext.get().queryFor(ObjectType.class)
                        .isChildOf(orgOid)
                        .buildFilter());
        return true;
    }

    @Override
    void addDebugDumpContent(StringBuilder sb, int indent) {
        sb.append("org: ").append(orgOid);
    }
}
