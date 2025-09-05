/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.spec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.evolveum.midpoint.schema.selector.eval.MatchingContext;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.selector.eval.FilteringContext;
import com.evolveum.midpoint.schema.selector.eval.SubjectedEvaluationContext.DelegatorSelection;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class AssigneeClause extends SelectorClause {

    @NotNull private final ValueSelector selector;

    private AssigneeClause(@NotNull ValueSelector selector) {
        this.selector = selector;
    }

    static AssigneeClause of(@NotNull ValueSelector selector) {
        return new AssigneeClause(selector);
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
        //self assignee clause doesn't require resolving of the assignee referenced objects
        //fixing #10811, #10812
        if (selector.isPureSelf()) {
            String[] selfOids = ctx.getSelfOidsArray(getDelegatorSelectionMode(realValue));
            List<ObjectReferenceType> assigneeRefs = getAssigneesWithoutResolving(realValue);
            if (selfOids.length > 0 && !assigneeRefs.isEmpty()) {
                List<String> assigneesList = Arrays.asList(selfOids);
                for (ObjectReferenceType assignee : assigneeRefs) {
                    if (assignee.getOid() != null && assigneesList.contains(assignee.getOid())) {
                        return true;
                    }
                }
                return false;
            }
        }

        var assignees = getAssignees(realValue, ctx);
        if (!assignees.isEmpty()) {
            // The assignees are always known "in full", as they are fetched from the repository via objectResolver.
            var childCtx = ctx.next(getDelegatorSelectionMode(realValue), "a", "assignee", true);
            for (PrismObject<? extends ObjectType> assignee : assignees) {
                assert assignee != null;
                if (selector.matches(assignee.getValue(), childCtx)) {
                    traceApplicable(ctx, "assignee matches: %s", assignee);
                    return true;
                }
            }
        }
        traceNotApplicable(ctx, "no assignee matches (assignees=%s)", assignees);
        return false;
    }



    static @NotNull DelegatorSelection getDelegatorSelectionMode(Object object) {
        if (object instanceof CaseType
                || object instanceof CaseWorkItemType) {
            return DelegatorSelection.CASE_MANAGEMENT;
        } else if (object instanceof AccessCertificationCaseType
                || object instanceof AccessCertificationWorkItemType) {
            return DelegatorSelection.ACCESS_CERTIFICATION;
        } else {
            return DelegatorSelection.NO_DELEGATOR;
        }
    }

    @NotNull
    private List<ObjectReferenceType> getAssigneesWithoutResolving(Object object) {
        if (object instanceof CaseType aCase) {
            return CaseTypeUtil.getAllAssignees(aCase);
        } else if (object instanceof AccessCertificationCaseType aCase) {
            return CertCampaignTypeUtil.getAllAssignees(aCase);
        } else if (object instanceof AbstractWorkItemType workItem) {
            return workItem.getAssigneeRef();
        }
        return List.of();
    }

    @NotNull
    private List<PrismObject<? extends ObjectType>> getAssignees(Object object, @NotNull MatchingContext ctx) {
        List<ObjectReferenceType> assigneeRefs = getAssigneesWithoutResolving(object);
        if (object instanceof CaseType aCase) {
            assigneeRefs = CaseTypeUtil.getAllAssignees(aCase);
        } else if (object instanceof AccessCertificationCaseType aCase) {
            assigneeRefs = CertCampaignTypeUtil.getAllAssignees(aCase);
        } else if (object instanceof AbstractWorkItemType workItem) {
            assigneeRefs = workItem.getAssigneeRef();
        } else {
            assigneeRefs = List.of();
        }

        List<PrismObject<? extends ObjectType>> assignees = new ArrayList<>();
        for (ObjectReferenceType assigneeRef : assigneeRefs) {
            CollectionUtils.addIgnoreNull(
                    assignees,
                    ctx.resolveReference(assigneeRef, object, "assignee"));
        }
        return assignees;
    }

    @Override
    public boolean toFilter(@NotNull FilteringContext ctx) {

        // Currently, we support only the "self" assignee clause, not even empty one.
        // (The interpretation of even such empty clause should be thought out -> does it require any assignee, or not?)
        //
        // The current solution is to use old-school "ref" filter, with all the identities known in the particular context.
        // The support for arbitrary clause is probably implementable with "ref" filter with "target" clause.
        //
        // See MID-8898.

        if (!selector.isPureSelf()) {
            throw new UnsupportedOperationException("Only 'self' selector is supported for 'assignee' clause when searching");
        }

        Class<?> type = ctx.getRestrictedType();
        if (CaseType.class.isAssignableFrom(type)) {
            addConjunct(
                    ctx,
                    PrismContext.get().queryFor(CaseType.class)
                            .exists(CaseType.F_WORK_ITEM)
                            .block()
                            .item(CaseWorkItemType.F_ASSIGNEE_REF)
                            .ref(ctx.getSelfOidsArray(DelegatorSelection.CASE_MANAGEMENT))
                            .endBlock()
                            .buildFilter());
            return true;
        } else if (AccessCertificationCaseType.class.isAssignableFrom(type)) {
            addConjunct(
                    ctx,
                    PrismContext.get().queryFor(AccessCertificationCaseType.class)
                            .exists(CaseType.F_WORK_ITEM)
                            .block()
                            .item(AccessCertificationWorkItemType.F_ASSIGNEE_REF)
                            .ref(ctx.getSelfOidsArray(DelegatorSelection.ACCESS_CERTIFICATION))
                            .endBlock()
                            .buildFilter());
            return true;
        } else if (CaseWorkItemType.class.isAssignableFrom(type)) {
            addConjunct(
                    ctx,
                    PrismContext.get().queryFor(CaseWorkItemType.class)
                            .item(CaseWorkItemType.F_ASSIGNEE_REF)
                            .ref(ctx.getSelfOidsArray(DelegatorSelection.CASE_MANAGEMENT))
                            .buildFilter());
            return true;
        } else if (AccessCertificationWorkItemType.class.isAssignableFrom(type)) {
            addConjunct(
                    ctx,
                    PrismContext.get().queryFor(AccessCertificationWorkItemType.class)
                            .item(AccessCertificationWorkItemType.F_ASSIGNEE_REF)
                            .ref(ctx.getSelfOidsArray(DelegatorSelection.ACCESS_CERTIFICATION))
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
        return "AssigneeClause{selector=" + selector + "}";
    }
}
