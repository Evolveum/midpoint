/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy.evaluator;

import java.util.Set;

import jakarta.xml.bind.JAXBElement;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.activity.policy.ActivityPolicyRuleEvaluationContext;
import com.evolveum.midpoint.repo.common.activity.policy.DataNeed;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NumericThresholdPolicyConstraintType;

@Component
public class ExecutionAttemptsConstraintEvaluator
        extends NumericConstraintEvaluator<NumericThresholdPolicyConstraintType> {

    @Override
    public Integer getLocalValue(ActivityPolicyRuleEvaluationContext context) {
        // We consider only the preexisting value, i.e., the number of attempts of an activity where the rule is defined.
        return null;
    }

    @Override
    protected @Nullable Integer getPreexistingValue(ActivityPolicyRuleEvaluationContext context) {
        return context.getPreexistingExecutionAttemptNumber();
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
