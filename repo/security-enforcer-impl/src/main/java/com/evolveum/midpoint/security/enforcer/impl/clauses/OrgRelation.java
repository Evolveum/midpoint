/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl.clauses;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.FilterCreationUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Objects;

/**
 * Evaluates "orgRelation" object selector clause.
 */
public class OrgRelation extends AbstractSelectorClauseEvaluation {

    @NotNull
    private final OrgRelationObjectSpecificationType selectorOrgRelation;

    public OrgRelation(
            @NotNull OrgRelationObjectSpecificationType selectorOrgRelation, @NotNull ClauseEvaluationContext ctx) {
        super(ctx);
        this.selectorOrgRelation = selectorOrgRelation;
    }

    public boolean isApplicable(PrismObject<? extends ObjectType> object) throws SchemaException {
        boolean match = false;
        var principalFocus = ctx.getPrincipalFocus();
        if (principalFocus != null) {
            for (ObjectReferenceType subjectParentOrgRef : principalFocus.getParentOrgRef()) {
                if (matchesOrgRelation(object, subjectParentOrgRef)) {
                    LOGGER.trace("    org applicable for {}, object OID {} because subject org {} matches",
                            ctx.getDesc(), object.getOid(), subjectParentOrgRef.getOid());
                    match = true;
                    break;
                }
            }
        }
        if (!match) {
            LOGGER.trace("    org not applicable for {}, object OID {} because none of the subject orgs matches",
                    ctx.getDesc(), object.getOid());
        }
        return match;
    }

    private boolean matchesOrgRelation(
            PrismObject<? extends ObjectType> object, ObjectReferenceType subjectParentOrgRef)
            throws SchemaException {
        if (!PrismContext.get().relationMatches(selectorOrgRelation.getSubjectRelation(), subjectParentOrgRef.getRelation())) {
            return false;
        }
        if (BooleanUtils.isTrue(selectorOrgRelation.isIncludeReferenceOrg())
                && subjectParentOrgRef.getOid().equals(object.getOid())) {
            return true;
        }
        OrgScopeType scope = Objects.requireNonNullElse(selectorOrgRelation.getScope(), OrgScopeType.ALL_DESCENDANTS);
        switch (scope) {
            case ALL_DESCENDANTS:
                return ctx.getRepositoryService().isDescendant(object, subjectParentOrgRef.getOid());
            case DIRECT_DESCENDANTS:
                return hasParentOrgRef(object, subjectParentOrgRef.getOid());
            case ALL_ANCESTORS:
                return ctx.getRepositoryService().isAncestor(object, subjectParentOrgRef.getOid());
            default:
                throw new UnsupportedOperationException("Unknown orgRelation scope " + scope);
        }
    }

    private boolean hasParentOrgRef(PrismObject<? extends ObjectType> object, String oid) {
        List<ObjectReferenceType> objParentOrgRefs = object.asObjectable().getParentOrgRef();
        for (ObjectReferenceType objParentOrgRef : objParentOrgRefs) {
            if (oid.equals(objParentOrgRef.getOid())) {
                return true;
            }
        }
        return false;
    }

    public void applyFilter() {
        ObjectFilter increment = null;
        QName subjectRelation = selectorOrgRelation.getSubjectRelation();
        FocusType principalFocus = ctx.getPrincipalFocus();
        if (principalFocus != null) {
            for (ObjectReferenceType subjectParentOrgRef : principalFocus.getParentOrgRef()) {
                if (PrismContext.get().relationMatches(subjectRelation, subjectParentOrgRef.getRelation())) {
                    S_FilterEntryOrEmpty q = PrismContext.get().queryFor(ObjectType.class);
                    S_FilterExit q2;
                    OrgScopeType scope = selectorOrgRelation.getScope();
                    if (scope == null || scope == OrgScopeType.ALL_DESCENDANTS) {
                        q2 = q.isChildOf(subjectParentOrgRef.getOid());
                    } else if (scope == OrgScopeType.DIRECT_DESCENDANTS) {
                        q2 = q.isDirectChildOf(subjectParentOrgRef.getOid());
                    } else if (scope == OrgScopeType.ALL_ANCESTORS) {
                        q2 = q.isParentOf(subjectParentOrgRef.getOid());
                    } else {
                        throw new UnsupportedOperationException("Unknown orgRelation scope " + scope);
                    }
                    if (Boolean.TRUE.equals(selectorOrgRelation.isIncludeReferenceOrg())) {
                        q2 = q2.or().id(subjectParentOrgRef.getOid());
                    }
                    increment = ObjectQueryUtil.filterOr(increment, q2.buildFilter());
                }
            }
        }
        if (increment == null) {
            increment = FilterCreationUtil.createNone();
        }
        fCtx.addConjunction(increment);
        LOGGER.trace("      applying orgRelation filter {}", increment);
    }
}
