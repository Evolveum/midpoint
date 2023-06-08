/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.spec;

import static com.evolveum.midpoint.prism.Referencable.getOid;
import static com.evolveum.midpoint.prism.query.FilterCreationUtil.createNone;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectTypeIfPossible;

import com.evolveum.midpoint.schema.selector.eval.ClauseFilteringContext;
import com.evolveum.midpoint.schema.selector.eval.ClauseMatchingContext;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class TenantClause extends SelectorClause {

    /** Immutable. */
    @NotNull private final TenantSelectorType bean;

    private TenantClause(@NotNull TenantSelectorType bean) {
        this.bean = bean;
        bean.freeze();
    }

    static TenantClause of(@NotNull TenantSelectorType bean) {
        return new TenantClause(bean);
    }

    @Override
    public @NotNull String getName() {
        return "tenant";
    }

    @Override
    public boolean matches(@NotNull PrismValue value, @NotNull ClauseMatchingContext ctx) {
        var object = asObjectTypeIfPossible(value);
        if (object == null) {
            traceNotApplicable(ctx, "not an object");
            return false;
        }

        if (BooleanUtils.isTrue(bean.isSameAsSubject())) {
            FocusType principalFocus = ctx.getPrincipalFocus();
            ObjectReferenceType subjectTenantRef = principalFocus != null ? principalFocus.getTenantRef() : null;
            if (subjectTenantRef == null || subjectTenantRef.getOid() == null) {
                traceNotApplicable(ctx, "subject does not have tenantRef");
                return false;
            }
            ObjectReferenceType objectTenantRef = object.getTenantRef();
            if (objectTenantRef == null || objectTenantRef.getOid() == null) {
                traceNotApplicable(ctx, "object does not have tenantRef");
                return false;
            }
            if (!subjectTenantRef.getOid().equals(objectTenantRef.getOid())) {
                traceNotApplicable(ctx, "tenant mismatch");
                return false;
            }
            if (!BooleanUtils.isTrue(bean.isIncludeTenantOrg())) {
                if (object instanceof OrgType) {
                    if (BooleanUtils.isTrue(((OrgType) object).isTenant())) {
                        traceNotApplicable(ctx, "object is a tenant org and it is not included");
                        return false;
                    }
                }
            }
            return true;
        } else {
            traceNotApplicable(ctx, "unsupported tenant specification");
            return false;
        }
    }

    @Override
    public boolean applyFilter(@NotNull ClauseFilteringContext ctx) {
        if (!Boolean.TRUE.equals(bean.isSameAsSubject())) {
            addConjunct(ctx, createNone(), "tenant authorization empty");
            return false;
        }

        var subjectTenantOid = getOid(ctx.getPrincipalFocus().getTenantRef());
        if (subjectTenantOid == null) {
            addConjunct(ctx, createNone(), "subject tenant empty");
            return false;
        }

        ObjectFilter conjunct;
        var builder = PrismContext.get().queryFor(ObjectType.class)
                .item(ObjectType.F_TENANT_REF).ref(subjectTenantOid);
        if (Boolean.TRUE.equals(bean.isIncludeTenantOrg())) {
            conjunct = builder.buildFilter();
        } else {
            conjunct = builder.and().not()
                    .type(OrgType.class)
                    .item(OrgType.F_TENANT).eq(true)
                    .buildFilter();
        }
        addConjunct(ctx, conjunct);
        return true;
    }

    @Override
    void addDebugDumpContent(StringBuilder sb, int indent) {
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "specification", bean, indent + 1);
    }
}
