/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl.clauses;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.util.MiscUtil.configCheck;

/**
 * Evaluates "orgRef" object selector clause.
 *
 * Note that applicability checking si provided by the repository service.
 */
public class OrgRef extends AbstractSelectorClauseEvaluation {

    @NotNull private final ObjectReferenceType selectorOrgRef;

    public OrgRef(@NotNull ObjectReferenceType selectorOrgRef, @NotNull ClauseEvaluationContext ctx) {
        super(ctx);
        this.selectorOrgRef = selectorOrgRef;
    }

    public void applyFilter() throws ConfigurationException {
        String oid = selectorOrgRef.getOid();
        configCheck(oid != null, "Null OID in orgRef filter"); // TODO error location
        ObjectFilter increment = PrismContext.get().queryFor(ObjectType.class)
                .isChildOf(oid)
                .buildFilter();
        fCtx.addConjunction(increment);
        LOGGER.trace("      applying org filter {}", increment);
    }
}
