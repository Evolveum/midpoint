/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.lens.EvaluatedPolicyRuleImpl;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Evaluation context for object-based policy rule.
 */
public class ObjectPolicyRuleEvaluationContext<O extends ObjectType>
        extends PolicyRuleEvaluationContext<O>
        implements Cloneable {

    ObjectPolicyRuleEvaluationContext(
            @NotNull EvaluatedPolicyRuleImpl policyRule,
            LensElementContext<O> elementContext,
            Task task) {
        this(policyRule, elementContext, task, ObjectState.AFTER);
    }

    private ObjectPolicyRuleEvaluationContext(
            @NotNull EvaluatedPolicyRuleImpl policyRule,
            LensElementContext<O> elementContext,
            Task task,
            ObjectState state) {
        super(policyRule, elementContext, task, state);
    }

    @Override
    public PolicyRuleEvaluationContext<O> cloneWithStateConstraints(ObjectState state) {
        return new ObjectPolicyRuleEvaluationContext<>(policyRule, elementContext, task, state);
    }

    @Override
    public String getShortDescription() {
        return ObjectTypeUtil.toShortString(elementContext.getObjectAny()) + " / " + state;
    }

    @SuppressWarnings({ "MethodDoesntCallSuperMethod" })
    @Override
    public ObjectPolicyRuleEvaluationContext<O> clone() {
        return new ObjectPolicyRuleEvaluationContext<>(policyRule, elementContext, task);
    }

    @Override
    public String toString() {
        return "ObjectPolicyRuleEvaluationContext{" + getShortDescription() + ")";
    }
}
