/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NumericThresholdPolicyConstraintType;

@Component
public class RestartActivityConstraintEvaluator
        extends NumericConstraintEvaluator<NumericThresholdPolicyConstraintType> {

    @Override
    public Integer getValue(ActivityPolicyRuleEvaluationContext context) {
        AbstractActivityRun<?, ?, ?> activityRun = context.getActivityRun();
        Integer executionAttempt = activityRun.getActivityState().getExecutionAttempt();
        if (executionAttempt == null) {
            executionAttempt = 1;
        }

        if (executionAttempt <= 0) {
            throw new IllegalStateException("Execution attempt must be greater than 0, but was: " + executionAttempt);
        }

        // The first execution is just normal execution (zero restarts happened)
        return executionAttempt - 1;
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
}
