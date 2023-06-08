/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.spec;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.selector.eval.ClauseFilteringContext;
import com.evolveum.midpoint.schema.selector.eval.ClauseMatchingContext;
import com.evolveum.midpoint.schema.selector.eval.SubjectedEvaluationContext.Delegation;
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
    public boolean matches(@NotNull PrismValue value, @NotNull ClauseMatchingContext ctx)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        var realValue = value.getRealValueIfExists();
        if (realValue == null) {
            traceNotApplicable(ctx, "has no real value");
            return false;
        }
        var assignees = getAssignees(realValue, ctx);
        if (!assignees.isEmpty()) {
            ClauseMatchingContext nextCtx = ctx.next(Delegation.ASSIGNEE, "a", "assignee");
            for (PrismObject<? extends ObjectType> assignee : assignees) {
                assert assignee != null;
                if (selector.matches(assignee.getValue(), nextCtx)) {
                    traceApplicable(ctx, "assignee matches: %s", assignee);
                    return true;
                }
            }
        }
        traceNotApplicable(ctx, "no assignee matches (assignees=%s)", assignees);
        return false;
    }

    @NotNull
    private List<PrismObject<? extends ObjectType>> getAssignees(Object object, @NotNull ClauseMatchingContext ctx) {
        List<ObjectReferenceType> assigneeRefs;
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
    public boolean applyFilter(@NotNull ClauseFilteringContext ctx) {
        Class<?> type = ctx.getRestrictedType();
        if (CaseType.class.isAssignableFrom(type)) {
            addConjunct(
                    ctx,
                    PrismContext.get().queryFor(CaseType.class)
                            .exists(CaseType.F_WORK_ITEM)
                            .block()
                            .item(CaseWorkItemType.F_ASSIGNEE_REF)
                            .ref(ctx.getSelfOidsArray(Delegation.ASSIGNEE))
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
                            .ref(ctx.getSelfOidsArray(Delegation.ASSIGNEE))
                            .endBlock()
                            .buildFilter());
            return true;
        } else if (CaseWorkItemType.class.isAssignableFrom(type)) {
            addConjunct(
                    ctx,
                    PrismContext.get().queryFor(CaseWorkItemType.class)
                            .item(CaseWorkItemType.F_ASSIGNEE_REF)
                            .ref(ctx.getSelfOidsArray(Delegation.ASSIGNEE))
                            .buildFilter());
            return true;
        } else if (AccessCertificationWorkItemType.class.isAssignableFrom(type)) {
            addConjunct(
                    ctx,
                    PrismContext.get().queryFor(AccessCertificationWorkItemType.class)
                            .item(AccessCertificationWorkItemType.F_ASSIGNEE_REF)
                            .ref(ctx.getSelfOidsArray(Delegation.ASSIGNEE))
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
}
