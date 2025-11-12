/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy.evaluator;

import java.util.List;

import jakarta.xml.bind.JAXBElement;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.common.activity.policy.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NumericThresholdPolicyConstraintType;

public abstract class NumericConstraintEvaluator<C extends NumericThresholdPolicyConstraintType>
        implements ActivityPolicyConstraintEvaluator<C, NumericConstraintTrigger<C>> {

    private static final Trace LOGGER = TraceManager.getTrace(NumericConstraintEvaluator.class);

    private static final String DEFAULT_CONSTRAINT_EVALUATOR_NAME = "Numeric value";

    @Override
    public List<NumericConstraintTrigger<C>> evaluate(
            JAXBElement<C> element, ActivityPolicyRuleEvaluationContext context, OperationResult result) {
        C constraint = element.getValue();

        LocalizableMessage constraintName = ActivityPolicyUtils.getConstraintName(element);
        String defaultConstraintName = ActivityPolicyUtils.getDefaultConstraintName(element);

        Integer localValue = getLocalValue(context);
        Integer totalValue = ComputationUtil.add(localValue, getPreexistingValue(context));

        if (totalValue == null) {
            if (shouldTriggerOnNullValue()) {
                LOGGER.trace("Triggering on empty value for constraint {}", defaultConstraintName);
                LocalizableMessage message = createEmptyMessage(null);
                return List.of(createTrigger(constraint, message, message));
            } else {
                LOGGER.trace("No numeric value to evaluate for constraint {}", defaultConstraintName);
                return List.of();
            }
        }

        Integer below = constraint.getBelow();
        if (below != null && totalValue < below) {
            LOGGER.trace("Numeric value {} is below the threshold {} of constraint {}, creating trigger",
                    totalValue, below, defaultConstraintName);

            LocalizableMessage message = createMessage(
                    constraintName, defaultConstraintName, totalValue, constraint.getBelow(), EvaluatorUtils.ThresholdType.BELOW);
            LocalizableMessage shortMessage = createShortMessage(
                    constraintName, defaultConstraintName, EvaluatorUtils.ThresholdType.BELOW);

            return List.of(createTrigger(constraint, message, shortMessage));
        }

        Integer exceeds = constraint.getExceeds();
        if (exceeds != null && totalValue > exceeds) {
            LOGGER.trace("Numeric value {} exceeds the threshold {} of constraint {}, creating trigger",
                    totalValue, exceeds, constraintName);

            LocalizableMessage message = createMessage(
                    constraintName, defaultConstraintName, totalValue, constraint.getExceeds(), EvaluatorUtils.ThresholdType.EXCEEDS);
            LocalizableMessage shortMessage = createShortMessage(
                    constraintName, defaultConstraintName, EvaluatorUtils.ThresholdType.EXCEEDS);

            return List.of(createTrigger(constraint, message, shortMessage));
        }

        if (below == null && exceeds == null) {
            LOGGER.trace("No below/exceeds thresholds defined for constraint {}", constraintName);

            if (shouldTriggerOnEmptyConstraint(constraint, totalValue)) {
                LOGGER.trace("Triggering on empty constraint {}", constraintName);

                LocalizableMessage message = createEmptyMessage(totalValue);

                return List.of(createTrigger(constraint, message, message));
            }
        }

        return List.of();
    }

    protected boolean shouldTriggerOnNullValue() {
        return false;
    }

    protected boolean shouldTriggerOnEmptyConstraint(C constraint, Integer value) {
        return false;
    }

    private NumericConstraintTrigger<C> createTrigger(C constraint, LocalizableMessage message, LocalizableMessage shortMessage) {
        return new NumericConstraintTrigger<>(constraint, message, shortMessage);
    }

    public abstract Integer getLocalValue(ActivityPolicyRuleEvaluationContext context);

    protected abstract @Nullable Integer getPreexistingValue(ActivityPolicyRuleEvaluationContext context);

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
            LocalizableMessage constraintName,
            String defaultConstraintName,
            int realValue,
            int threshold,
            EvaluatorUtils.ThresholdType type) {

        String key = type == EvaluatorUtils.ThresholdType.EXCEEDS ?
                "NumericConstraintEvaluator.exceedsMessage" :
                "NumericConstraintEvaluator.belowMessage";

        LocalizableMessage evaluatorName = createEvaluatorName();
        String evaluatorNameDefault = createDefaultEvaluatorName();

        String message = EvaluatorUtils.createDefaultMessage(
                evaluatorNameDefault, defaultConstraintName, Integer.toString(realValue), Integer.toString(threshold), type);

        return new SingleLocalizableMessage(key, new Object[] { evaluatorName, constraintName, realValue, threshold }, message);
    }

    protected LocalizableMessage createShortMessage(
            LocalizableMessage constraintName, String defaultConstraintName, EvaluatorUtils.ThresholdType type) {
        String key = type == EvaluatorUtils.ThresholdType.EXCEEDS ?
                "NumericConstraintEvaluator.exceedsShortMessage" :
                "NumericConstraintEvaluator.belowShortMessage";

        LocalizableMessage evaluatorName = createEvaluatorName();
        String evaluatorNameDefault = createDefaultEvaluatorName();

        String message = EvaluatorUtils.createDefaultShortMessage(evaluatorNameDefault, defaultConstraintName, type);

        return new SingleLocalizableMessage(key, new Object[] { evaluatorName, constraintName }, message);
    }
}
