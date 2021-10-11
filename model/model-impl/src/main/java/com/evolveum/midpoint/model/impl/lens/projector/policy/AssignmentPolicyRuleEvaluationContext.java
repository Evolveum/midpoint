/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.impl.lens.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class AssignmentPolicyRuleEvaluationContext<AH extends AssignmentHolderType> extends PolicyRuleEvaluationContext<AH> {

    @NotNull public final EvaluatedAssignmentImpl<AH> evaluatedAssignment;
    public final boolean inPlus;
    public final boolean inZero;
    public final boolean inMinus;
    public final boolean isDirect;
    public final DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple;

    AssignmentPolicyRuleEvaluationContext(@NotNull EvaluatedPolicyRule policyRule,
            @NotNull EvaluatedAssignmentImpl<AH> evaluatedAssignment, boolean inPlus, boolean inZero,
            boolean inMinus, boolean isDirect, LensContext<AH> context,
            DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple, Task task,
            RulesEvaluationContext globalCtx) {
        this(policyRule, evaluatedAssignment, inPlus, inZero, inMinus, isDirect, context, evaluatedAssignmentTriple,
                task, ObjectState.AFTER, globalCtx);
    }

    private AssignmentPolicyRuleEvaluationContext(@NotNull EvaluatedPolicyRule policyRule,
            @NotNull EvaluatedAssignmentImpl<AH> evaluatedAssignment, boolean inPlus, boolean inZero,
            boolean inMinus, boolean isDirect, LensContext<AH> context,
            DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple, Task task, ObjectState state,
            RulesEvaluationContext globalCtx) {
        super(policyRule, context, task, globalCtx, state);
        this.evaluatedAssignment = evaluatedAssignment;
        this.inPlus = inPlus;
        this.inZero = inZero;
        this.inMinus = inMinus;
        this.isDirect = isDirect;
        this.evaluatedAssignmentTriple = evaluatedAssignmentTriple;
    }

    @Override
    public AssignmentPolicyRuleEvaluationContext<AH> cloneWithStateConstraints(ObjectState state) {
        return new AssignmentPolicyRuleEvaluationContext<>(policyRule, evaluatedAssignment, inPlus, inZero, inMinus, isDirect, lensContext, evaluatedAssignmentTriple, task, state,
                globalCtx);
    }

    @Override
    public void triggerRule(Collection<EvaluatedPolicyRuleTrigger<?>> triggers) {
        evaluatedAssignment.triggerRule(policyRule, triggers);
    }

    @Override
    public boolean isApplicableToState() {
        return super.isApplicableToState() && isAssignmentApplicable();
    }

    private boolean isAssignmentApplicable() {
        if (state == ObjectState.BEFORE) {
            return inMinus || inZero;
        } else {
            return inZero || inPlus;
        }
    }

    @Override
    public String getShortDescription() {
        return evaluatedAssignment.getTarget() + " (" +
                (inPlus ? "+":"") +
                (inMinus ? "-":"") +
                (inZero ? "0":"") +
                ") " +
                (isDirect ? "directly":"indirectly") +
                " in " + ObjectTypeUtil.toShortString(focusContext.getObjectAny()) + " / " + state;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public AssignmentPolicyRuleEvaluationContext<AH> clone() {
        return new AssignmentPolicyRuleEvaluationContext<>(policyRule, evaluatedAssignment, inPlus, inZero, inMinus,
                isDirect, lensContext, evaluatedAssignmentTriple, task, globalCtx);
    }

    @Override
    public String toString() {
        return "AssignmentPolicyRuleEvaluationContext{" + getShortDescription() + "}";
    }
}
