/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy.evaluator;

import com.evolveum.midpoint.repo.common.activity.policy.ActivityPolicyRuleEvaluationContext;
import com.evolveum.midpoint.repo.common.activity.policy.EvaluatedPolicyRule;
import com.evolveum.midpoint.repo.common.activity.policy.ThresholdValueType;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.xml.ns._public.common.common_3.NumericThresholdPolicyConstraintType;

@Component
public class ExecutionAttemptsConstraintEvaluator
        extends NumericConstraintEvaluator<NumericThresholdPolicyConstraintType> {

    @Override
    public Integer getValue(ActivityPolicyRuleEvaluationContext context) {
        return context.getActivityRun().getActivityState().getExecutionAttempt();
    }

    @Override
    protected void updateRuleThresholdTypeAndValue(EvaluatedPolicyRule rule, NumericThresholdPolicyConstraintType constraint, Integer value) {
        if (!constraint.asPrismContainerValue().isEmpty()) {
            return;
        }

        rule.setThresholdValueType(ThresholdValueType.INTEGER, value);
    }

    @Override
    protected boolean shouldTriggerOnEmptyConstraint(NumericThresholdPolicyConstraintType constraint, Integer value) {
        return value != null && value > 1;
    }

    @Override
    protected LocalizableMessage createEvaluatorName() {
        return new SingleLocalizableMessage("RestartActivityConstraintEvaluator.name");
    }
}
