/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Evaluation context for object-based policy rule.
 */
public class ObjectPolicyRuleEvaluationContext<O extends ObjectType>
        extends PolicyRuleEvaluationContext<O>
        implements Cloneable {

    ObjectPolicyRuleEvaluationContext(
            @NotNull EvaluatedPolicyRule policyRule,
            RulesEvaluationContext globalCtx,
            LensElementContext<O> elementContext,
            Task task) {
        this(policyRule, globalCtx, elementContext, task, ObjectState.AFTER);
    }

    private ObjectPolicyRuleEvaluationContext(
            @NotNull EvaluatedPolicyRule policyRule,
            RulesEvaluationContext globalCtx,
            LensElementContext<O> elementContext,
            Task task,
            ObjectState state) {
        super(policyRule, elementContext, task, globalCtx, state);
    }

    @Override
    public PolicyRuleEvaluationContext<O> cloneWithStateConstraints(ObjectState state) {
        return new ObjectPolicyRuleEvaluationContext<>(policyRule, globalCtx, elementContext, task, state);
    }

    @Override
    public void triggerRule(Collection<EvaluatedPolicyRuleTrigger<?>> triggers) {
        elementContext.triggerRule(policyRule, triggers);
    }

    @Override
    public String getShortDescription() {
        return ObjectTypeUtil.toShortString(elementContext.getObjectAny()) + " / " + state;
    }

    @SuppressWarnings({ "MethodDoesntCallSuperMethod" })
    @Override
    public ObjectPolicyRuleEvaluationContext<O> clone() {
        return new ObjectPolicyRuleEvaluationContext<>(policyRule, globalCtx, elementContext, task);
    }

    @Override
    public String toString() {
        return "ObjectPolicyRuleEvaluationContext{" + getShortDescription() + ")";
    }
}
