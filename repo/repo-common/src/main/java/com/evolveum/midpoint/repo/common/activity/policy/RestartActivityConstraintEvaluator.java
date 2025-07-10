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
        return activityRun.getActivityState().getExecutionAttempt();
    }

    @Override
    protected boolean shouldTriggerOnEmptyConstraint(NumericThresholdPolicyConstraintType constraint, Integer value) {
        return value != null && value > 1;
    }
}
