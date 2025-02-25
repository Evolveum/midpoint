/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.List;
import javax.xml.datatype.Duration;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DurationThresholdPolicyConstraintType;

public abstract class EvaluatedDurationThresholdConstraintEvaluator<C extends DurationThresholdPolicyConstraintType>
        implements ActivityPolicyConstraintEvaluator<C, DurationThresholdPolicyTrigger<C>> {

    @Override
    public List<DurationThresholdPolicyTrigger<C>> evaluate(
            C constraint,
            ActivityPolicyRuleEvaluationContext context,
            OperationResult result) {

        Duration value = getDurationValue(context);

        // todo probably handle "below", however it's probably out of scope currently

        Duration exceeds = constraint.getExceeds();
        if (exceeds != null && value.isLongerThan(exceeds)) {
            // todo probably check whether we previously hit this one? or maybe sometime later (most probably)
            LocalizableMessage message = createMessage(constraint.getName(), value, exceeds);
            LocalizableMessage shortMessage = createShortMessage(constraint.getName());

            return List.of(createTrigger(constraint, message, shortMessage));
        }

        return List.of();
    }

    protected DurationThresholdPolicyTrigger<C> createTrigger(C constraint, LocalizableMessage message, LocalizableMessage shortMessage) {
        return new DurationThresholdPolicyTrigger(constraint, message, shortMessage);
    }

    /**
     * Duration value to be tested against defined constraint
     */
    protected abstract Duration getDurationValue(ActivityPolicyRuleEvaluationContext context);

    private LocalizableMessage createMessage(String constraintName, Duration taskExecutionTime, Duration exceeds) {
        return new SingleLocalizableMessage(
                "ActivityExecutionConstraintEvaluator.exceedsMessage",
                new Object[] { taskExecutionTime, exceeds, constraintName },
                "Task execution time is %s, which exceeds the threshold of constraint %s (%s)"
                        .formatted(taskExecutionTime, constraintName, exceeds));
    }

    private LocalizableMessage createShortMessage(String constraintName) {
        return new SingleLocalizableMessage(
                "ActivityExecutionConstraintEvaluator.exceedsShortMessage",
                new Object[] { constraintName },
                "Task execution exceeded for constraint %s"
                        .formatted(constraintName));
    }
}
