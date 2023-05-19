/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl.clauses;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.query.FilterCreationUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.prism.Referencable.getOid;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectTypeIfPossible;
import static com.evolveum.midpoint.util.MiscUtil.getDiagInfo;

/**
 * Evaluates "tenant" object selector clause.
 */
public class Tenant extends AbstractSelectorClauseEvaluation {

    @NotNull
    private final TenantSelectorType tenantSelector;

    public Tenant(
            @NotNull TenantSelectorType tenantSelector,
            @NotNull ClauseEvaluationContext ctx) {
        super(ctx);
        this.tenantSelector = tenantSelector;
    }

    public boolean isApplicable(PrismValue value) {
        var object = asObjectTypeIfPossible(value);
        if (object == null) {
            throw new UnsupportedOperationException("'tenant' spec is not applicable to sub-object values: " + getDiagInfo(value));
        }

        if (BooleanUtils.isTrue(tenantSelector.isSameAsSubject())) {
            FocusType principalFocus = ctx.getPrincipalFocus();
            ObjectReferenceType subjectTenantRef = principalFocus != null ? principalFocus.getTenantRef() : null;
            if (subjectTenantRef == null || subjectTenantRef.getOid() == null) {
                LOGGER.trace("    tenant object spec not applicable for {}, object OID {} because subject does not have tenantRef",
                        ctx.getDesc(), object.getOid());
                return false;
            }
            ObjectReferenceType objectTenantRef = object.getTenantRef();
            if (objectTenantRef == null || objectTenantRef.getOid() == null) {
                LOGGER.trace("    tenant object spec not applicable for {}, object OID {} because object does not have tenantRef",
                        ctx.getDesc(), object.getOid());
                return false;
            }
            if (!subjectTenantRef.getOid().equals(objectTenantRef.getOid())) {
                LOGGER.trace("    tenant object spec not applicable for {}, object OID {} because of tenant mismatch",
                        ctx.getDesc(), object.getOid());
                return false;
            }
            if (!BooleanUtils.isTrue(tenantSelector.isIncludeTenantOrg())) {
                if (object instanceof OrgType) {
                    if (BooleanUtils.isTrue(((OrgType) object).isTenant())) {
                        LOGGER.trace("    tenant object spec not applicable for {}, object OID {} because it is a tenant org and it is not included",
                                ctx.getDesc(), object.getOid());
                        return false;
                    }
                }
            }
        } else {
            LOGGER.trace("    tenant object spec not applicable for {}, object OID {} because there is a strange tenant specification in authorization",
                    ctx.getDesc(), object.getOid());
            return false;
        }
        return true;
    }

    public void applyFilter() {
        fCtx.addConjunction(
                computeIncrement());
    }

    private @NotNull ObjectFilter computeIncrement() {
        if (!Boolean.TRUE.equals(tenantSelector.isSameAsSubject())) {
            LOGGER.trace("    tenant authorization empty (none filter)");
            return FilterCreationUtil.createNone();
        }

        var subjectTenantOid = getOid(ctx.getPrincipalFocus().getTenantRef());
        if (subjectTenantOid == null) {
            LOGGER.trace("    subject tenant empty (none filter)");
            return FilterCreationUtil.createNone();
        }

        ObjectFilter increment;
        var builder = PrismContext.get().queryFor(ObjectType.class)
                .item(ObjectType.F_TENANT_REF).ref(subjectTenantOid);
        if (Boolean.TRUE.equals(tenantSelector.isIncludeTenantOrg())) {
            increment = builder.buildFilter();
        } else {
            increment = builder.and().not()
                    .type(OrgType.class)
                    .item(OrgType.F_TENANT).eq(true)
                    .buildFilter();
        }
        LOGGER.trace("    applying tenant filter {}", increment);
        return increment;
    }
}
