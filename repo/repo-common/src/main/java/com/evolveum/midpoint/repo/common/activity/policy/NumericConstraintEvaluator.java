/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.List;

import jakarta.xml.bind.JAXBElement;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NumericThresholdPolicyConstraintType;

public abstract class NumericConstraintEvaluator<C extends NumericThresholdPolicyConstraintType>
        implements ActivityPolicyConstraintEvaluator<C, NumericConstraintTrigger<C>> {

    private static final Trace LOGGER = TraceManager.getTrace(NumericConstraintEvaluator.class);

    public enum ThresholdType {
        BELOW, EXCEEDS
    }

    // todo improve messages [viliam]
    // todo figure out how to merge this with DurationThresholdConstraintEvaluator [viliam]
    @Override
    public List<NumericConstraintTrigger<C>> evaluate(JAXBElement<C> element, ActivityPolicyRuleEvaluationContext context, OperationResult result) {
        C constraint = element.getValue();

        Integer value = getValue(context);
        if (value == null) {
            LOGGER.trace("No numeric value to evaluate for constraint {}", constraint.getName());
            return List.of();
        }

        Integer below = constraint.getBelow();
        if (below != null && value < below) {
            LOGGER.trace("Numeric value {} is below the threshold of constraint {}, creating trigger", value, constraint.getName());

            LocalizableMessage message = new SingleLocalizableMessage("NumericConstraintEvaluator.below");

            return List.of(createTrigger(constraint, message, message));
        }

        Integer exceeds = constraint.getExceeds();
        if (exceeds != null && value > exceeds) {
            LOGGER.trace("Numeric value {} exceeds the threshold of constraint {}, creating trigger", value, constraint.getName());

            LocalizableMessage message = new SingleLocalizableMessage("NumericConstraintEvaluator.exceeds");

            return List.of(createTrigger(constraint, message, message));
        }

        if (below == null && exceeds == null) {
            LOGGER.trace("No below/exceeds thresholds defined for constraint {}", constraint.getName());

            if (shouldTriggerOnEmptyConstraint(constraint, value)) {
                LOGGER.trace("Triggering on empty constraint {}", constraint.getName());

                LocalizableMessage message = new SingleLocalizableMessage("NumericConstraintEvaluator.empty");

                return List.of(createTrigger(constraint, message, message));
            }
        }

        return List.of();
    }

    protected abstract boolean shouldTriggerOnEmptyConstraint(C constraint, Integer value);

    private NumericConstraintTrigger<C> createTrigger(C constraint, LocalizableMessage message, LocalizableMessage shortMessage) {
        return new NumericConstraintTrigger<>(constraint, message, shortMessage);
    }

    public abstract Integer getValue(ActivityPolicyRuleEvaluationContext context);
}
