/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.spec;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.query.FilterCreationUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.schema.selector.eval.ClauseFilteringContext;
import com.evolveum.midpoint.schema.selector.eval.ClauseMatchingContext;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Objects;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectTypeIfPossible;

public class OrgRelationClause extends SelectorClause {

    /** Immutable. */
    @NotNull private final OrgRelationObjectSpecificationType bean;

    /** Immutable. */
    private OrgRelationClause(@NotNull OrgRelationObjectSpecificationType bean) {
        this.bean = bean;
        bean.freeze();
    }

    static OrgRelationClause of(@NotNull OrgRelationObjectSpecificationType bean) {
        return new OrgRelationClause(bean);
    }

    @Override
    public @NotNull String getName() {
        return "orgRelation";
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
        var principalFocus = ctx.getPrincipalFocus();
        if (principalFocus != null) {
            for (ObjectReferenceType subjectParentOrgRef : principalFocus.getParentOrgRef()) {
                if (matchesOrgRelation(object, subjectParentOrgRef, ctx)) {
                    traceApplicable(ctx, "subject org matches: %s", subjectParentOrgRef.getOid());
                    return true;
                }
            }
        }
        traceNotApplicable(ctx, "none of the subject orgs match");
        return false;
    }

    private boolean matchesOrgRelation(
            ObjectType object, ObjectReferenceType subjectParentOrgRef, @NotNull ClauseMatchingContext ctx)
            throws SchemaException {
        if (!PrismContext.get().relationMatches(bean.getSubjectRelation(), subjectParentOrgRef.getRelation())) {
            return false;
        }
        if (BooleanUtils.isTrue(bean.isIncludeReferenceOrg())
                && subjectParentOrgRef.getOid().equals(object.getOid())) {
            return true;
        }
        OrgScopeType scope = Objects.requireNonNullElse(bean.getScope(), OrgScopeType.ALL_DESCENDANTS);
        switch (scope) {
            case ALL_DESCENDANTS:
                return ctx.orgTreeEvaluator.isDescendant(object.asPrismObject(), subjectParentOrgRef.getOid());
            case DIRECT_DESCENDANTS:
                return hasParentOrgRef(object.asPrismObject(), subjectParentOrgRef.getOid());
            case ALL_ANCESTORS:
                return ctx.orgTreeEvaluator.isAncestor(object.asPrismObject(), subjectParentOrgRef.getOid());
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

    @Override
    public boolean applyFilter(@NotNull ClauseFilteringContext ctx) throws SchemaException {
        ObjectFilter conjunct = null;
        QName subjectRelation = bean.getSubjectRelation();
        FocusType principalFocus = ctx.getPrincipalFocus();
        if (principalFocus != null) {
            for (ObjectReferenceType subjectParentOrgRef : principalFocus.getParentOrgRef()) {
                if (PrismContext.get().relationMatches(subjectRelation, subjectParentOrgRef.getRelation())) {
                    S_FilterEntryOrEmpty q = PrismContext.get().queryFor(ObjectType.class);
                    S_FilterExit q2;
                    OrgScopeType scope = bean.getScope();
                    if (scope == null || scope == OrgScopeType.ALL_DESCENDANTS) {
                        q2 = q.isChildOf(subjectParentOrgRef.getOid());
                    } else if (scope == OrgScopeType.DIRECT_DESCENDANTS) {
                        q2 = q.isDirectChildOf(subjectParentOrgRef.getOid());
                    } else if (scope == OrgScopeType.ALL_ANCESTORS) {
                        q2 = q.isParentOf(subjectParentOrgRef.getOid());
                    } else {
                        throw new UnsupportedOperationException("Unknown orgRelation scope " + scope);
                    }
                    if (Boolean.TRUE.equals(bean.isIncludeReferenceOrg())) {
                        q2 = q2.or().id(subjectParentOrgRef.getOid());
                    }
                    conjunct = ObjectQueryUtil.filterOr(conjunct, q2.buildFilter());
                }
            }
        }
        if (conjunct == null) {
            conjunct = FilterCreationUtil.createNone();
        }
        addConjunct(ctx, conjunct);
        return true; // Todo false if "none" ?
    }

    @Override
    void addDebugDumpContent(StringBuilder sb, int indent) {
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "specification", bean, indent + 1);
    }
}
