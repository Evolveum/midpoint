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
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Evaluation context for a policy rule.
 */
public abstract class PolicyRuleEvaluationContext<AH extends AssignmentHolderType> implements Cloneable {

    @NotNull public final EvaluatedPolicyRule policyRule;
    @NotNull public final LensContext<AH> lensContext;
    @NotNull public final LensFocusContext<AH> focusContext;
    @NotNull public final Task task;
    @NotNull public final ObjectState state;
    @NotNull final RulesEvaluationContext globalCtx;

    protected PolicyRuleEvaluationContext(@NotNull EvaluatedPolicyRule policyRule, @NotNull LensContext<AH> context,
            @NotNull Task task, @NotNull RulesEvaluationContext globalCtx, @NotNull ObjectState state) {
        this.policyRule = policyRule;
        this.lensContext = context;
        this.focusContext = context.getFocusContext();
        this.task = task;
        this.globalCtx = globalCtx;
        if (focusContext == null) {
            throw new IllegalStateException("No focus context");
        }
        this.state = state;
    }

    public abstract PolicyRuleEvaluationContext<AH> cloneWithStateConstraints(ObjectState state);

    public abstract void triggerRule(Collection<EvaluatedPolicyRuleTrigger<?>> triggers);

    public PrismObject<AH> getObject() {
        if (state == ObjectState.BEFORE) {
            return focusContext.getObjectOld();
        } else {
            return focusContext.getObjectNew();
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
}
