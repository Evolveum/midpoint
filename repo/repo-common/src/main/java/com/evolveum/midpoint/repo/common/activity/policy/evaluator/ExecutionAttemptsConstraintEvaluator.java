/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy.evaluator;

import com.evolveum.midpoint.repo.common.activity.policy.ActivityPolicyRuleEvaluationContext;
import com.evolveum.midpoint.repo.common.activity.policy.DataNeed;
import com.evolveum.midpoint.repo.common.activity.policy.EvaluatedPolicyRule;
import com.evolveum.midpoint.repo.common.activity.policy.ThresholdValueType;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;

import jakarta.xml.bind.JAXBElement;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.xml.ns._public.common.common_3.NumericThresholdPolicyConstraintType;

import java.util.Set;

@Component
public class ExecutionAttemptsConstraintEvaluator
        extends NumericConstraintEvaluator<NumericThresholdPolicyConstraintType> {

    @Override
    public Integer getLocalValue(ActivityPolicyRuleEvaluationContext context) {
        return context.getActivityRun().getActivityState().getExecutionAttempt();
    }

    @Override
    protected @Nullable Integer getPreexistingValue(ActivityPolicyRuleEvaluationContext context) {
        return context.getPreexistingExecutionAttemptNumber();
    }

    @Override
    protected void updateRuleThresholdTypeAndValues(
            EvaluatedPolicyRule rule, NumericThresholdPolicyConstraintType constraint, Integer localValue, Integer totalValue) {
        if (constraint.asPrismContainerValue().isEmpty()) {
            rule.setThresholdTypeAndValues(ThresholdValueType.INTEGER, localValue, totalValue);
        }
    }

    @Override
    protected boolean shouldTriggerOnEmptyConstraint(NumericThresholdPolicyConstraintType constraint, Integer value) {
        return value != null && value > 1;
    }

    @Override
    protected LocalizableMessage createEvaluatorName() {
        return new SingleLocalizableMessage("RestartActivityConstraintEvaluator.name");
    }

    @Override
    public Set<DataNeed> getDataNeeds(JAXBElement<NumericThresholdPolicyConstraintType> constraint) {
        return Set.of(DataNeed.EXECUTION_ATTEMPTS);
    }
}
