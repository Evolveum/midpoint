/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.impl.lens.EvaluatedPolicyRuleImpl;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.projector.AssignmentOrigin;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class AssignmentPolicyRuleEvaluationContext<AH extends AssignmentHolderType>
        extends PolicyRuleEvaluationContext<AH>
        implements Cloneable {

    @NotNull public final EvaluatedAssignmentImpl<AH> evaluatedAssignment;
    public final boolean isAdded;
    public final boolean isKept;
    public final boolean isDeleted;
    public final DeltaSetTriple<? extends EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple;
    @NotNull private final LensFocusContext<AH> focusContext;

    AssignmentPolicyRuleEvaluationContext(
            @NotNull EvaluatedPolicyRuleImpl policyRule,
            @NotNull EvaluatedAssignmentImpl<AH> evaluatedAssignment,
            @NotNull LensFocusContext<AH> focusContext,
            DeltaSetTriple<? extends EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple,
            Task task) {
        this(policyRule, evaluatedAssignment, focusContext, evaluatedAssignmentTriple, task, ObjectState.AFTER);
    }

    private AssignmentPolicyRuleEvaluationContext(
            @NotNull EvaluatedPolicyRuleImpl policyRule,
            @NotNull EvaluatedAssignmentImpl<AH> evaluatedAssignment,
            @NotNull LensFocusContext<AH> focusContext,
            DeltaSetTriple<? extends EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple,
            Task task,
            ObjectState state) {
        super(policyRule, focusContext, task, state);
        this.evaluatedAssignment = evaluatedAssignment;
        AssignmentOrigin origin = evaluatedAssignment.getOrigin();
        this.isAdded = origin.isBeingAdded();
        this.isKept = origin.isBeingKept();
        this.isDeleted = origin.isBeingDeleted();
        this.evaluatedAssignmentTriple = evaluatedAssignmentTriple;
        this.focusContext = focusContext;
    }

    @Override
    public AssignmentPolicyRuleEvaluationContext<AH> cloneWithStateConstraints(ObjectState state) {
        return new AssignmentPolicyRuleEvaluationContext<>(
                policyRule, evaluatedAssignment, focusContext, evaluatedAssignmentTriple, task, state);
    }

    @Override
    boolean hasPolicyRuleExceptions(
            @NotNull EvaluatedPolicyRuleImpl policyRule, @NotNull Collection<EvaluatedPolicyRuleTrigger<?>> triggers) {
        return evaluatedAssignment.hasPolicyRuleException(policyRule, triggers);
    }

    @Override
    public boolean isApplicableToState() {
        return super.isApplicableToState() && isAssignmentApplicable();
    }

    private boolean isAssignmentApplicable() {
        if (state == ObjectState.BEFORE) {
            return isDeleted || isKept;
        } else {
            return isKept || isAdded;
        }
    }

    public boolean isDirect() {
        return policyRule.getTargetType() == EvaluatedPolicyRule.TargetType.DIRECT_ASSIGNMENT_TARGET;
    }

    @Override
    public String getShortDescription() {
        return evaluatedAssignment.getTarget() + " (" +
                (isAdded ? "+":"") +
                (isDeleted ? "-":"") +
                (isKept ? "0":"") +
                ") " +
                (isDirect() ? "directly":"indirectly") +
                " in " + ObjectTypeUtil.toShortString(elementContext.getObjectAny()) + " / " + state;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public AssignmentPolicyRuleEvaluationContext<AH> clone() {
        return new AssignmentPolicyRuleEvaluationContext<>(
                policyRule, evaluatedAssignment, focusContext, evaluatedAssignmentTriple, task);
    }

    @Override
    public String toString() {
        return "AssignmentPolicyRuleEvaluationContext{" + getShortDescription() + "}";
    }

    public @NotNull LensFocusContext<AH> getFocusContext() {
        return focusContext;
    }
}
