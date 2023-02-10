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
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Evaluation context for a policy rule.
 */
public abstract class PolicyRuleEvaluationContext<O extends ObjectType> {

    @NotNull public final EvaluatedPolicyRule policyRule;
    @NotNull public final LensContext<?> lensContext;
    @NotNull public final LensElementContext<O> elementContext;
    @NotNull public final Task task;
    @NotNull public final ObjectState state;
    @NotNull final RulesEvaluationContext globalCtx;

    protected PolicyRuleEvaluationContext(
            @NotNull EvaluatedPolicyRule policyRule,
            @NotNull LensElementContext<O> elementContext,
            @NotNull Task task,
            @NotNull RulesEvaluationContext globalCtx,
            @NotNull ObjectState state) {
        this.policyRule = policyRule;
        this.lensContext = elementContext.getLensContext();
        this.elementContext = elementContext;
        this.task = task;
        this.globalCtx = globalCtx;
        this.state = state;
    }

    public abstract PolicyRuleEvaluationContext<O> cloneWithStateConstraints(ObjectState state);

    public abstract void triggerRule(Collection<EvaluatedPolicyRuleTrigger<?>> triggers);

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

    public void record() {
        globalCtx.rulesToRecord.add(policyRule);
    }

    public LensFocusContext<?> getFocusContext() {
        return elementContext instanceof LensFocusContext<?> ? (LensFocusContext<?>) elementContext : null;
    }
}
