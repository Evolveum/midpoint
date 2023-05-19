/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl.clauses;

import static com.evolveum.midpoint.util.MiscUtil.getDiagInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Evaluates "assignee" object selector clause.
 */
public class Assignee extends AbstractSelectorClauseEvaluation {

    /** TODO why is this used only for "is applicable" checking? */
    @NotNull private final SubjectedObjectSelectorType assigneeSelector;

    public Assignee(@NotNull SubjectedObjectSelectorType assigneeSelector, @NotNull ClauseEvaluationContext ctx) {
        super(ctx);
        this.assigneeSelector = assigneeSelector;
    }

    public <O extends ObjectType> boolean isApplicable(PrismValue value)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        var realValue = value.getRealValueIfExists();
        if (realValue == null) {
            return false; // TODO log?
        }
        List<PrismObject<? extends ObjectType>> assignees = getAssignees(realValue);
        if (assignees.isEmpty()) {
            LOGGER.trace("    assignee spec not applicable for {}, object OID {} because it has no assignees",
                    ctx.getDesc(), getDiagInfo(realValue)); // TODO diag
            return false;
        }
        Collection<String> relevantDelegators = ctx.getDelegatorsForAssignee();
        boolean applicable = false;
        for (PrismObject<? extends ObjectType> assignee : assignees) {
            if (ctx.isSelectorApplicable(
                    assigneeSelector, assignee, relevantDelegators, "assignee of " + ctx.getDesc())) {
                applicable = true;
                break;
            }
        }
        if (!applicable) {
            LOGGER.trace("    assignee spec not applicable for {}, object OID {} because none of the assignees match (assignees={})",
                    ctx.getDesc(), getDiagInfo(realValue), assignees); // TODO diag
        }
        return applicable;
    }

    @NotNull
    private List<PrismObject<? extends ObjectType>> getAssignees(Object object) {
        List<ObjectReferenceType> assigneeRefs;
        if (object instanceof CaseType) {
            CaseType aCase = (CaseType) object;
            assigneeRefs = CaseTypeUtil.getAllCurrentAssignees(aCase);
        } else if (object instanceof AbstractWorkItemType) {
            assigneeRefs = ((AbstractWorkItemType) object).getAssigneeRef();
        } else {
            // TODO e.g. cert case
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

    public boolean applyFilter() {
        if (CaseType.class.isAssignableFrom(fCtx.getRefinedType())) {
            var increment = PrismContext.get().queryFor(CaseType.class)
                    .exists(CaseType.F_WORK_ITEM)
                    .block()
                    .item(CaseWorkItemType.F_CLOSE_TIMESTAMP).isNull()
                    .and().item(CaseWorkItemType.F_ASSIGNEE_REF)
                    .ref(ctx.getSelfAndOtherOids(ctx.getDelegatorsForAssignee()))
                    .endBlock()
                    .buildFilter();
            LOGGER.trace("  applying assignee filter {}", increment);
            fCtx.addConjunction(increment);
            return true;
        } else {
            LOGGER.trace("      Authorization not applicable for object because it has assignee specification (this is not applicable for search for objects other than CaseType)");
            return false;
        }
    }
}
