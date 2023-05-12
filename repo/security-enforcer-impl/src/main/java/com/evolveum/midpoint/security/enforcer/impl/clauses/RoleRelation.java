/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl.clauses;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValueCollectionsUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.FilterCreationUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleRelationObjectSpecificationType;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * Evaluates "roleRelation" object selector clause.
 *
 * See {@link OrgRelation}.
 */
public class RoleRelation extends AbstractSelectorClauseEvaluation {

    @NotNull
    private final RoleRelationObjectSpecificationType selectorRoleRelation;

    public RoleRelation(
            @NotNull RoleRelationObjectSpecificationType selectorRoleRelation, @NotNull ClauseEvaluationContext ctx) {
        super(ctx);
        this.selectorRoleRelation = selectorRoleRelation;
    }

    public boolean isApplicable(@NotNull PrismObject<? extends ObjectType> object) {
        boolean match = false;
        var principalFocus = ctx.getPrincipalFocus();
        if (principalFocus != null) {
            for (ObjectReferenceType subjectRoleMembershipRef : principalFocus.getRoleMembershipRef()) {
                if (matchesRoleRelation(object, subjectRoleMembershipRef)) {
                    LOGGER.trace("    applicable for {}, object OID {} because subject role relation {} matches",
                            ctx.getDesc(), object.getOid(), subjectRoleMembershipRef.getOid());
                    match = true;
                    break;
                }
            }
        }
        if (!match) {
            LOGGER.trace("    not applicable for {}, object OID {} because none of the subject roles matches",
                    ctx.getDesc(), object.getOid());
        }
        return match;
    }

    private boolean matchesRoleRelation(
            PrismObject<? extends ObjectType> object,
            ObjectReferenceType subjectRoleMembershipRef) {
        PrismContext prismContext = PrismContext.get();
        if (!prismContext.relationMatches(
                selectorRoleRelation.getSubjectRelation(), subjectRoleMembershipRef.getRelation())) {
            return false;
        }
        if (BooleanUtils.isTrue(selectorRoleRelation.isIncludeReferenceRole())
                && subjectRoleMembershipRef.getOid().equals(object.getOid())) {
            return true;
        }
        if (!BooleanUtils.isFalse(selectorRoleRelation.isIncludeMembers())) {
            if (!object.canRepresent(FocusType.class)) {
                return false;
            }
            for (ObjectReferenceType objectRoleMembershipRef : ((FocusType) object.asObjectable()).getRoleMembershipRef()) {
                if (!subjectRoleMembershipRef.getOid().equals(objectRoleMembershipRef.getOid())) {
                    continue;
                }
                if (!prismContext.relationMatches(
                        selectorRoleRelation.getObjectRelation(), objectRoleMembershipRef.getRelation())) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    public boolean applyFilter() {
        ObjectFilter filter = processRoleRelationFilter();
        ObjectFilter increment;
        if (filter != null) {
            increment = filter;
        } else {
            if (fCtx.maySkipOnSearch()) {
                LOGGER.trace("      not applying roleRelation filter because it is not efficient and maySkipOnSearch is set");
                return false;
            } else {
                increment = FilterCreationUtil.createNone();
            }
        }
        fCtx.addConjunction(increment);
        LOGGER.trace("      applying roleRelation filter {}", increment);
        return true;
    }

    /**
     * Very rudimentary and experimental implementation.
     */
    private ObjectFilter processRoleRelationFilter() {

        if (BooleanUtils.isTrue(selectorRoleRelation.isIncludeReferenceRole())) {
            // This could mean that we will need to add filters for all roles in
            // subject's roleMembershipRef. There may be thousands of these.
            if (!fCtx.maySkipOnSearch()) {
                throw new UnsupportedOperationException("Inefficient roleRelation search (includeReferenceRole=true) is not supported yet");
            }
        }

        if (!BooleanUtils.isFalse(selectorRoleRelation.isIncludeMembers())) {
            List<PrismReferenceValue> queryRoleRefs = getRoleOidsFromFilter(fCtx.getOriginalFilter());
            if (queryRoleRefs == null || queryRoleRefs.isEmpty()) {
                // Cannot find specific role OID in original query. This could mean that we
                // will need to add filters for all roles in subject's roleMembershipRef.
                // There may be thousands of these.
                if (!fCtx.maySkipOnSearch()) {
                    throw new UnsupportedOperationException("Inefficient roleRelation search (includeMembers=true without role in the original query) is not supported yet");
                }
            } else {
                List<QName> subjectRelation = selectorRoleRelation.getSubjectRelation();
                boolean isRoleOidOk = false;
                for (ObjectReferenceType subjectRoleMembershipRef : ctx.getPrincipalFocus().getRoleMembershipRef()) {
                    if (!PrismContext.get().relationMatches(subjectRelation, subjectRoleMembershipRef.getRelation())) {
                        continue;
                    }
                    if (!PrismValueCollectionsUtil.containsOid(queryRoleRefs, subjectRoleMembershipRef.getOid())) {
                        continue;
                    }
                    isRoleOidOk = true;
                    break;
                }
                if (isRoleOidOk) {
                    // There is already a good filter in the origFilter
                    // TODO: mind the objectRelation
                    return FilterCreationUtil.createAll();
                } else {
                    return FilterCreationUtil.createNone();
                }
            }
        }

        return null;
    }

    private List<PrismReferenceValue> getRoleOidsFromFilter(ObjectFilter origFilter) {
        if (origFilter == null) {
            return null;
        }
        if (origFilter instanceof RefFilter) {
            ItemPath path = ((RefFilter) origFilter).getPath();
            if (path.equivalent(SchemaConstants.PATH_ROLE_MEMBERSHIP_REF)) {
                return ((RefFilter) origFilter).getValues();
            }
        }
        if (origFilter instanceof AndFilter) {
            for (ObjectFilter condition : ((AndFilter) origFilter).getConditions()) {
                List<PrismReferenceValue> refs = getRoleOidsFromFilter(condition);
                if (refs != null && !refs.isEmpty()) {
                    return refs;
                }
            }
        }
        return null;
    }
}
