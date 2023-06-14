/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.spec;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.PrismValueCollectionsUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.FilterCreationUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.selector.eval.FilteringContext;
import com.evolveum.midpoint.schema.selector.eval.MatchingContext;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleRelationObjectSpecificationType;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectTypeIfPossible;

public class RoleRelationClause extends SelectorClause {

    /** Immutable. */
    @NotNull private final RoleRelationObjectSpecificationType bean;

    private RoleRelationClause(@NotNull RoleRelationObjectSpecificationType bean) {
        this.bean = bean;
        bean.freeze();
    }

    static RoleRelationClause of(@NotNull RoleRelationObjectSpecificationType bean) {
        return new RoleRelationClause(bean);
    }

    @Override
    public @NotNull String getName() {
        return "roleRelation";
    }

    @Override
    public boolean matches(@NotNull PrismValue value, @NotNull MatchingContext ctx)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        var object = asObjectTypeIfPossible(value);
        if (object == null) {
            traceNotApplicable(ctx, "not an object");
            return false;
        }
        var principalFocus = ctx.getPrincipalFocus();
        if (principalFocus != null) {
            for (ObjectReferenceType subjectRoleMembershipRef : principalFocus.getRoleMembershipRef()) {
                if (matchesRoleRelation(object, subjectRoleMembershipRef)) {
                    traceApplicable(ctx, "subject role matches: %s", subjectRoleMembershipRef.getOid());
                    return true;
                }
            }
        }
        traceNotApplicable(ctx, "none of the subject roles match");
        return false;
    }

    private boolean matchesRoleRelation(ObjectType object, ObjectReferenceType subjectRoleMembershipRef) {
        PrismContext prismContext = PrismContext.get();
        if (!prismContext.relationMatches(
                bean.getSubjectRelation(), subjectRoleMembershipRef.getRelation())) {
            return false;
        }
        if (BooleanUtils.isTrue(bean.isIncludeReferenceRole())
                && subjectRoleMembershipRef.getOid().equals(object.getOid())) {
            return true;
        }
        if (!BooleanUtils.isFalse(bean.isIncludeMembers())) {
            if (!(object instanceof FocusType)) {
                return false;
            }
            for (ObjectReferenceType objectRoleMembershipRef : ((FocusType) object).getRoleMembershipRef()) {
                if (!subjectRoleMembershipRef.getOid().equals(objectRoleMembershipRef.getOid())) {
                    continue;
                }
                if (!prismContext.relationMatches(
                        bean.getObjectRelation(), objectRoleMembershipRef.getRelation())) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean toFilter(@NotNull FilteringContext ctx) {
        ObjectFilter filter = processRoleRelationFilter(ctx);
        ObjectFilter conjunct;
        if (filter != null) {
            conjunct = filter;
        } else {
            if (ctx.maySkipOnSearch()) {
                traceNotApplicable(ctx, "filter is not efficient and maySkipOnSearch is set");
                return false;
            } else {
                conjunct = FilterCreationUtil.createNone();
            }
        }
        addConjunct(ctx, conjunct);
        return true; // TODO false if none?
    }

    /**
     * Very rudimentary and experimental implementation.
     */
    private ObjectFilter processRoleRelationFilter(@NotNull FilteringContext ctx) {

        if (BooleanUtils.isTrue(bean.isIncludeReferenceRole())) {
            // This could mean that we will need to add filters for all roles in
            // subject's roleMembershipRef. There may be thousands of these.
            if (!ctx.maySkipOnSearch()) {
                throw new UnsupportedOperationException("Inefficient roleRelation search (includeReferenceRole=true) is not supported yet");
            }
        }

        if (!BooleanUtils.isFalse(bean.isIncludeMembers())) {
            List<PrismReferenceValue> queryRoleRefs = getRoleOidsFromFilter(ctx.getOriginalFilter());
            if (queryRoleRefs == null || queryRoleRefs.isEmpty()) {
                // Cannot find specific role OID in original query. This could mean that we
                // will need to add filters for all roles in subject's roleMembershipRef.
                // There may be thousands of these.
                if (!ctx.maySkipOnSearch()) {
                    throw new UnsupportedOperationException("Inefficient roleRelation search (includeMembers=true without role in the original query) is not supported yet");
                }
            } else {
                List<QName> subjectRelation = bean.getSubjectRelation();
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

    @Override
    void addDebugDumpContent(StringBuilder sb, int indent) {
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "specification", bean, indent + 1);
    }
}
