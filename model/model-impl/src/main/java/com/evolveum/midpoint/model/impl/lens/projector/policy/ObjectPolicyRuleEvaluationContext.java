/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Evaluation context for object-based policy rule.
 */
public class ObjectPolicyRuleEvaluationContext<AH extends AssignmentHolderType> extends PolicyRuleEvaluationContext<AH> {

    ObjectPolicyRuleEvaluationContext(@NotNull EvaluatedPolicyRule policyRule, RulesEvaluationContext globalCtx,
            LensContext<AH> context, Task task) {
        this(policyRule, globalCtx, context, task, ObjectState.AFTER);
    }

    private ObjectPolicyRuleEvaluationContext(@NotNull EvaluatedPolicyRule policyRule, RulesEvaluationContext globalCtx,
            LensContext<AH> context, Task task,
            ObjectState state) {
        super(policyRule, context, task, globalCtx, state);
    }

    @Override
    public PolicyRuleEvaluationContext<AH> cloneWithStateConstraints(ObjectState state) {
        return new ObjectPolicyRuleEvaluationContext<>(policyRule, globalCtx, lensContext, task, state);
    }

    @Override
    public void triggerRule(Collection<EvaluatedPolicyRuleTrigger<?>> triggers) {
        focusContext.triggerRule(policyRule, triggers);
    }

    @Override
    public String getShortDescription() {
        return ObjectTypeUtil.toShortString(focusContext.getObjectAny()) + " / " + state;
    }

    @SuppressWarnings({ "MethodDoesntCallSuperMethod" })
    @Override
    public ObjectPolicyRuleEvaluationContext<AH> clone() {
        return new ObjectPolicyRuleEvaluationContext<>(policyRule, globalCtx, lensContext, task);
    }

    @Override
    public String toString() {
        return "ObjectPolicyRuleEvaluationContext{" + getShortDescription() + ")";
    }
}
