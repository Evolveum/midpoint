/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.impl.lens.EvaluatedPolicyRuleImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Evaluation context for a policy rule. Common for assignment- and object-based rules.
 *
 * Should be quite short-living, only during policy rule evaluation.
 */
public abstract class PolicyRuleEvaluationContext<O extends ObjectType> {

    @NotNull public final EvaluatedPolicyRuleImpl policyRule;
    @NotNull public final LensContext<?> lensContext;
    @NotNull public final LensElementContext<O> elementContext;
    @NotNull public final Task task;
    @NotNull public final ObjectState state;

    protected PolicyRuleEvaluationContext(
            @NotNull EvaluatedPolicyRuleImpl policyRule,
            @NotNull LensElementContext<O> elementContext,
            @NotNull Task task,
            @NotNull ObjectState state) {
        this.policyRule = policyRule;
        this.lensContext = elementContext.getLensContext();
        this.elementContext = elementContext;
        this.task = task;
        this.state = state;
    }

    public abstract PolicyRuleEvaluationContext<O> cloneWithStateConstraints(ObjectState state);

    void triggerRuleIfNoExceptions(Collection<EvaluatedPolicyRuleTrigger<?>> triggers) {
        if (!hasPolicyRuleExceptions(policyRule, triggers)) {
            policyRule.trigger(triggers);
        }
    }

    boolean hasPolicyRuleExceptions(
            @NotNull EvaluatedPolicyRuleImpl policyRule, @NotNull Collection<EvaluatedPolicyRuleTrigger<?>> triggers) {
        return false; // FIXME not currently implemented for objects (only for assignments)
    }

    public PrismObject<O> getObject() {
        if (state == ObjectState.BEFORE) {
            return elementContext.getObjectOld();
        } else {
            return elementContext.getObjectNew();
        }
    }

    public PrismObjectDefinition<O> getObjectDefinition() {
        PrismObject<O> object = getObject();
        if (object != null && object.getDefinition() != null) {
            return object.getDefinition();
        } else {
            return elementContext.getObjectDefinition();
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isApplicableToState() {
        return getObject() != null;
    }

    public abstract String getShortDescription();

    public LensFocusContext<?> getFocusContext() {
        return elementContext instanceof LensFocusContext<?> ? (LensFocusContext<?>) elementContext : null;
    }
}
