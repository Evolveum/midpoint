/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.spec;

import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.schema.selector.eval.MatchingContext;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.selector.eval.FilteringContext;
import com.evolveum.midpoint.schema.selector.eval.SubjectedEvaluationContext.DelegatorSelection;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.getOidsFromRefs;

public class CandidateAssigneeClause extends SelectorClause {

    @NotNull private final ValueSelector selector;

    private CandidateAssigneeClause(@NotNull ValueSelector selector) {
        this.selector = selector;
    }

    static CandidateAssigneeClause of(@NotNull ValueSelector selector) {
        return new CandidateAssigneeClause(selector);
    }

    @Override
    public @NotNull String getName() {
        return "assignee";
    }

    @Override
    public boolean matches(@NotNull PrismValue value, @NotNull MatchingContext ctx)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        var realValue = value.getRealValueIfExists();
        if (realValue == null) {
            traceNotApplicable(ctx, "has no real value");
            return false;
        }
        Set<String> candidateOids = getOidsFromRefs(getCandidateRefs(realValue));
        if (!candidateOids.isEmpty()) {
            if (!selector.isPureSelf()) {
                // Currently, we support only "self" selector clause here
                throw new UnsupportedOperationException("Unsupported non-self candidateAssignee clause");
            }
            Set<String> myOids = ctx.getSelfPlusRolesOids(
                    AssigneeClause.getDelegatorSelectionMode(realValue));
            var match = Sets.intersection(candidateOids, myOids);
            if (!match.isEmpty()) {
                traceApplicable(ctx, "candidate assignee(s) match: %s", match);
                return true;
            }
        }
        traceNotApplicable(ctx, "no candidate assignee matches (candidate assignees: %s)", candidateOids);
        return false;
    }

    @NotNull
    private List<ObjectReferenceType> getCandidateRefs(Object object) {
        if (object instanceof CaseType aCase) {
            return CaseTypeUtil.getAllCandidateAssignees(aCase);
        } else if (object instanceof AccessCertificationCaseType aCase) {
            return CertCampaignTypeUtil.getAllCandidateAssignees(aCase);
        } else if (object instanceof AbstractWorkItemType workItem) {
            return workItem.getCandidateRef();
        } else {
            return List.of();
        }
    }

    @Override
    public boolean toFilter(@NotNull FilteringContext ctx) {
        Class<?> type = ctx.getRestrictedType();
        if (CaseType.class.isAssignableFrom(type)) {
            addConjunct(
                    ctx,
                    PrismContext.get().queryFor(CaseType.class)
                            .exists(CaseType.F_WORK_ITEM)
                            .block()
                            .item(CaseWorkItemType.F_CANDIDATE_REF)
                            .ref(ctx.getSelfPlusRolesOidsArray(DelegatorSelection.CASE_MANAGEMENT))
                            .endBlock()
                            .buildFilter());
            return true;
        } else if (AccessCertificationCaseType.class.isAssignableFrom(type)) {
            addConjunct(
                    ctx,
                    PrismContext.get().queryFor(AccessCertificationCaseType.class)
                            .exists(CaseType.F_WORK_ITEM)
                            .block()
                            .item(AccessCertificationWorkItemType.F_CANDIDATE_REF)
                            .ref(ctx.getSelfPlusRolesOidsArray(DelegatorSelection.ACCESS_CERTIFICATION))
                            .endBlock()
                            .buildFilter());
            return true;
        } else if (CaseWorkItemType.class.isAssignableFrom(type)) {
            addConjunct(
                    ctx,
                    PrismContext.get().queryFor(CaseWorkItemType.class)
                            .item(CaseWorkItemType.F_CANDIDATE_REF)
                            .ref(ctx.getSelfPlusRolesOidsArray(DelegatorSelection.CASE_MANAGEMENT))
                            .buildFilter());
            return true;
        } else if (AccessCertificationWorkItemType.class.isAssignableFrom(type)) {
            addConjunct(
                    ctx,
                    PrismContext.get().queryFor(AccessCertificationWorkItemType.class)
                            .item(AccessCertificationWorkItemType.F_CANDIDATE_REF)
                            .ref(ctx.getSelfPlusRolesOidsArray(DelegatorSelection.ACCESS_CERTIFICATION))
                            .buildFilter());
            return true;
        } else {
            traceNotApplicable(ctx, "when searching, this clause applies only to cases and work items");
            return false;
        }
    }

    @Override
    void addDebugDumpContent(StringBuilder sb, int indent) {
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "selector", selector, indent + 1);
    }

    @Override
    public String toString() {
        return "CandidateAssigneeClause{" +
                "selector=" + selector +
                "}";
    }
}
