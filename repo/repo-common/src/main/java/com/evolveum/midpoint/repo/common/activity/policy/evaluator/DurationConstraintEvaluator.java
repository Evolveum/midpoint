/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy.evaluator;

import java.util.List;
import javax.xml.datatype.Duration;

import jakarta.xml.bind.JAXBElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.common.activity.policy.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DurationThresholdPolicyConstraintType;

public abstract class DurationConstraintEvaluator<C extends DurationThresholdPolicyConstraintType>
        implements ActivityPolicyConstraintEvaluator<C, DurationThresholdPolicyTrigger<C>> {

    private static final Trace LOGGER = TraceManager.getTrace(DurationConstraintEvaluator.class);

    private static final String DEFAULT_CONSTRAINT_EVALUATOR_NAME = "Measured duration";

    @Override
    public List<DurationThresholdPolicyTrigger<C>> evaluate(
            JAXBElement<C> element,
            ActivityPolicyRuleEvaluationContext context,
            OperationResult result) {

        C constraint = element.getValue();

        Duration localValue = getLocalValue(context);
        Duration totalValue = add(localValue, getPreexistingValue(context));

        if (totalValue == null) {
            if (shouldTriggerOnNullValue()) {
                LOGGER.trace("Triggering on empty value for constraint {}", constraint.getName());

                LocalizableMessage message = createEmptyMessage(null);

                return List.of(createTrigger(constraint, message, message));
            }

            LOGGER.trace("No duration value to evaluate for constraint {}", constraint.getName());
            return List.of();
        }

        Duration lowerLimit = constraint.getBelow();
        Duration upperLimit = constraint.getExceeds();
        LOGGER.trace("Evaluating duration constraint {} (lower: {}, upper: {}) against value {}",
                constraint.getName(), lowerLimit, upperLimit, totalValue);

        Long totalValueMillis = ComputationUtil.durationToMillis(totalValue);
        if (lowerLimit != null && compare(totalValue, lowerLimit) < 0) {
            LOGGER.trace("Duration value {} is below the threshold of constraint {}, creating trigger", totalValue, constraint.getName());

            LocalizableMessage message = createMessage(constraint.getName(), totalValue, totalValueMillis, lowerLimit, EvaluatorUtils.ThresholdType.BELOW);
            LocalizableMessage shortMessage = createShortMessage(constraint.getName(), EvaluatorUtils.ThresholdType.BELOW);

            return List.of(createTrigger(constraint, message, shortMessage));
        }

        if (upperLimit != null && compare(totalValue, upperLimit) > 0) {
            LOGGER.trace("Duration value {} exceeds the threshold of constraint {}, creating trigger", totalValue, constraint.getName());

            LocalizableMessage message = createMessage(constraint.getName(), totalValue, totalValueMillis, upperLimit, EvaluatorUtils.ThresholdType.EXCEEDS);
            LocalizableMessage shortMessage = createShortMessage(constraint.getName(), EvaluatorUtils.ThresholdType.EXCEEDS);

            return List.of(createTrigger(constraint, message, shortMessage));
        }

        if (lowerLimit == null && upperLimit == null) {
            LOGGER.trace("No below/exceeds thresholds defined for constraint {}", constraint.getName());

            if (shouldTriggerOnEmptyConstraint(constraint, totalValue)) {
                LOGGER.trace("Triggering on empty constraint {}", constraint.getName());

                LocalizableMessage message = createEmptyMessage(totalValue);

                return List.of(createTrigger(constraint, message, message));
            }
        }

        return List.of();
    }

    private int compare(@NotNull Duration value, @NotNull Duration limit) {
        // It is strange but value.compare(limit) does not work as expected. For example, PT10S is not greater than PT10.5S.
        // Moreover, we'd need to treat INDETERMINATE result as well - by defaulting to millis comparison as done here.
        return ComputationUtil.durationToMillis(value).compareTo(ComputationUtil.durationToMillis(limit));
    }

    protected boolean shouldTriggerOnNullValue() {
        return false;
    }

    protected boolean shouldTriggerOnEmptyConstraint(C constraint, Duration value) {
        return value != null;
    }

    protected DurationThresholdPolicyTrigger<C> createTrigger(
            C constraint, LocalizableMessage message, LocalizableMessage shortMessage) {
        return new DurationThresholdPolicyTrigger<>(constraint, message, shortMessage);
    }

    /**
     * Duration value to be tested against defined constraint.
     * If value is null, constraint evaluation will be skipped.
     */
    protected abstract @Nullable Duration getLocalValue(ActivityPolicyRuleEvaluationContext context);

    protected abstract @Nullable Duration getPreexistingValue(ActivityPolicyRuleEvaluationContext context);

    private Duration add(@Nullable Duration localValue, @Nullable Duration preexistingValue) {
        if (localValue == null) {
            return preexistingValue;
        }
        if (preexistingValue == null) {
            return localValue;
        }
        return localValue.add(preexistingValue);
    }

    protected LocalizableMessage createEvaluatorName() {
        return new SingleLocalizableMessage("DurationConstraintEvaluator.name", new String[0], "Measured duration");
    }

    private String createDefaultEvaluatorName() {
        LocalizableMessage nameMsg = createEvaluatorName();
        return nameMsg != null && nameMsg.getFallbackMessage() != null ?
                nameMsg.getFallbackMessage() : DEFAULT_CONSTRAINT_EVALUATOR_NAME;
    }

    private LocalizableMessage createEmptyMessage(Duration realValue) {
        Object value = realValue != null ? realValue : "";

        LocalizableMessage evaluatorName = createEvaluatorName();
        String evaluatorNameDefault = createDefaultEvaluatorName();

        String defaultMessage = "%s unlimited constraint triggered with '%s'".formatted(evaluatorNameDefault, value);

        return new SingleLocalizableMessage(
                "DurationThresholdConstraintEvaluator.empty", new Object[] { evaluatorName, value }, defaultMessage);
    }

    protected LocalizableMessage createMessage(
            String constraintName, Duration realValue, long realValueMs, Duration threshold, EvaluatorUtils.ThresholdType type) {

        String key = type == EvaluatorUtils.ThresholdType.EXCEEDS ?
                "DurationConstraintEvaluator.exceedsMessage" :
                "DurationConstraintEvaluator.belowMessage";

        LocalizableMessage evaluatorName = createEvaluatorName();
        String evaluatorNameDefault = createDefaultEvaluatorName();

        String formattedValue = realValueMs + "ms";
        String formattedThreshold = threshold.toString();

        String message = EvaluatorUtils.createDefaultMessage(evaluatorNameDefault, constraintName, formattedValue, formattedThreshold, type);

        return new SingleLocalizableMessage(key, new Object[] { evaluatorName, constraintName, realValue, threshold }, message);
    }

    protected LocalizableMessage createShortMessage(String constraintName, EvaluatorUtils.ThresholdType type) {
        String key = type == EvaluatorUtils.ThresholdType.EXCEEDS ?
                "DurationConstraintEvaluator.exceedsShortMessage" :
                "DurationConstraintEvaluator.belowShortMessage";

        LocalizableMessage evaluatorName = createEvaluatorName();
        String evaluatorNameDefault = createDefaultEvaluatorName();

        String message = EvaluatorUtils.createDefaultShortMessage(evaluatorNameDefault, constraintName, type);

        return new SingleLocalizableMessage(key, new Object[] { evaluatorName, constraintName }, message);
    }
}
