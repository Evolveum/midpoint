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

// todo add localization keys to midpoint.properties
public abstract class NumericConstraintEvaluator<C extends NumericThresholdPolicyConstraintType>
        implements ActivityPolicyConstraintEvaluator<C, NumericConstraintTrigger<C>> {

    private static final Trace LOGGER = TraceManager.getTrace(NumericConstraintEvaluator.class);

    private static final String DEFAULT_CONSTRAINT_EVALUATOR_NAME = "Numeric value";

    @Override
    public List<NumericConstraintTrigger<C>> evaluate(JAXBElement<C> element, ActivityPolicyRuleEvaluationContext context, OperationResult result) {
        C constraint = element.getValue();

        Integer value = getValue(context);
        updateRuleThresholdTypeAndValue(context.getRule(), constraint, value);

        if (value == null) {
            if (shouldTriggerOnNullValue(value)) {
                LOGGER.trace("Triggering on empty value for constraint {}", constraint.getName());

                LocalizableMessage message = createEmptyMessage(null);

                return List.of(createTrigger(constraint, message, message));
            }

            LOGGER.trace("No numeric value to evaluate for constraint {}", constraint.getName());
            return List.of();
        }

        Integer below = constraint.getBelow();
        if (below != null && value < below) {
            LOGGER.trace("Numeric value {} is below the threshold of constraint {}, creating trigger", value, constraint.getName());

            LocalizableMessage message = createMessage(constraint.getName(), value, constraint.getBelow(), EvaluatorUtils.ThresholdType.BELOW);
            LocalizableMessage shortMessage = createShortMessage(constraint.getName(), EvaluatorUtils.ThresholdType.BELOW);

            return List.of(createTrigger(constraint, message, shortMessage));
        }

        Integer exceeds = constraint.getExceeds();
        if (exceeds != null && value > exceeds) {
            LOGGER.trace("Numeric value {} exceeds the threshold of constraint {}, creating trigger", value, constraint.getName());

            LocalizableMessage message = createMessage(constraint.getName(), value, constraint.getExceeds(), EvaluatorUtils.ThresholdType.EXCEEDS);
            LocalizableMessage shortMessage = createShortMessage(constraint.getName(), EvaluatorUtils.ThresholdType.EXCEEDS);

            return List.of(createTrigger(constraint, message, shortMessage));
        }

        if (below == null && exceeds == null) {
            LOGGER.trace("No below/exceeds thresholds defined for constraint {}", constraint.getName());

            if (shouldTriggerOnEmptyConstraint(constraint, value)) {
                LOGGER.trace("Triggering on empty constraint {}", constraint.getName());

                LocalizableMessage message = createEmptyMessage(value);

                return List.of(createTrigger(constraint, message, message));
            }
        }

        return List.of();
    }

    protected void updateRuleThresholdTypeAndValue(EvaluatedPolicyRule rule, C constraint, Integer value) {
    }

    protected boolean shouldTriggerOnNullValue(Integer value) {
        return false;
    }

    protected boolean shouldTriggerOnEmptyConstraint(C constraint, Integer value) {
        return false;
    }

    private NumericConstraintTrigger<C> createTrigger(C constraint, LocalizableMessage message, LocalizableMessage shortMessage) {
        return new NumericConstraintTrigger<>(constraint, message, shortMessage);
    }

    public abstract Integer getValue(ActivityPolicyRuleEvaluationContext context);

    protected LocalizableMessage createEvaluatorName() {
        return new SingleLocalizableMessage("DurationConstraintEvaluator.name", new String[0], "Measured duration");
    }

    private String createDefaultEvaluatorName() {
        LocalizableMessage nameMsg = createEvaluatorName();
        return nameMsg != null && nameMsg.getFallbackMessage() != null ?
                nameMsg.getFallbackMessage() : DEFAULT_CONSTRAINT_EVALUATOR_NAME;
    }

    private LocalizableMessage createEmptyMessage(Integer realValue) {
        Object value = realValue != null ? realValue : "";

        LocalizableMessage evaluatorName = createEvaluatorName();
        String evaluatorNameDefault = createDefaultEvaluatorName();

        String defaultMessage = "%s unlimited constraint triggered with '%s'".formatted(evaluatorNameDefault, value);

        return new SingleLocalizableMessage(
                "NumericConstraintEvaluator.empty", new Object[] { evaluatorName, value }, defaultMessage);
    }

    protected LocalizableMessage createMessage(
            String constraintName, int realValue, int threshold, EvaluatorUtils.ThresholdType type) {

        String key = type == EvaluatorUtils.ThresholdType.EXCEEDS ?
                "NumericConstraintEvaluator.exceedsMessage" :
                "NumericConstraintEvaluator.belowMessage";

        LocalizableMessage evaluatorName = createEvaluatorName();
        String evaluatorNameDefault = createDefaultEvaluatorName();

        String message = EvaluatorUtils.createDefaultMessage(
                evaluatorNameDefault, constraintName, Integer.toString(realValue), Integer.toString(threshold), type);

        return new SingleLocalizableMessage(key, new Object[] { evaluatorName, constraintName, realValue, threshold }, message);
    }

    protected LocalizableMessage createShortMessage(String constraintName, EvaluatorUtils.ThresholdType type) {
        String key = type == EvaluatorUtils.ThresholdType.EXCEEDS ?
                "NumericConstraintEvaluator.exceedsShortMessage" :
                "NumericConstraintEvaluator.belowShortMessage";

        LocalizableMessage evaluatorName = createEvaluatorName();
        String evaluatorNameDefault = createDefaultEvaluatorName();

        String message = EvaluatorUtils.createDefaultShortMessage(evaluatorNameDefault, constraintName, type);

        return new SingleLocalizableMessage(key, new Object[] { evaluatorName, constraintName }, message);
    }
}
