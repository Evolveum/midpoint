/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy.evaluator;

import java.util.Date;
import java.util.List;
import javax.xml.datatype.Duration;

import com.evolveum.midpoint.repo.common.activity.policy.*;
import jakarta.xml.bind.JAXBElement;
import org.jetbrains.annotations.Nullable;

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

        Duration value = getDurationValue(context);
        updateRuleThresholdTypeAndValue(context.getRule(), constraint, value);

        if (value == null) {
            if (shouldTriggerOnNullValue(value)) {
                LOGGER.trace("Triggering on empty value for constraint {}", constraint.getName());

                LocalizableMessage message = createEmptyMessage(null);

                return List.of(createTrigger(constraint, message, message));
            }

            LOGGER.trace("No duration value to evaluate for constraint {}", constraint.getName());
            return List.of();
        }

        LOGGER.trace("Evaluating duration constraint {} against value {}", constraint.getName(), value);

        final Date now = new Date();    // arbitrary point in time, used for duration comparison

        Long valueMs = durationToMillis(value, now);

        Long belowMs = durationToMillis(constraint.getBelow(), now);
        if (belowMs != null && valueMs < belowMs) {
            LOGGER.trace("Duration value {} is below the threshold of constraint {}, creating trigger", value, constraint.getName());

            LocalizableMessage message = createMessage(constraint.getName(), value, valueMs, constraint.getBelow(), EvaluatorUtils.ThresholdType.BELOW);
            LocalizableMessage shortMessage = createShortMessage(constraint.getName(), EvaluatorUtils.ThresholdType.BELOW);

            return List.of(createTrigger(constraint, message, shortMessage));
        }

        Long exceedsMs = durationToMillis(constraint.getExceeds(), now);

        if (exceedsMs != null && valueMs > exceedsMs) {
            LOGGER.trace("Duration value {}ms exceeds the threshold of constraint {}, creating trigger", value, constraint.getName());

            LocalizableMessage message = createMessage(constraint.getName(), value, valueMs, constraint.getExceeds(), EvaluatorUtils.ThresholdType.EXCEEDS);
            LocalizableMessage shortMessage = createShortMessage(constraint.getName(), EvaluatorUtils.ThresholdType.EXCEEDS);

            return List.of(createTrigger(constraint, message, shortMessage));
        }

        if (belowMs == null && exceedsMs == null) {
            LOGGER.trace("No below/exceeds thresholds defined for constraint {}", constraint.getName());

            if (shouldTriggerOnEmptyConstraint(constraint, value)) {
                LOGGER.trace("Triggering on empty constraint {}", constraint.getName());

                LocalizableMessage message = createEmptyMessage(value);

                return List.of(createTrigger(constraint, message, message));
            }
        }

        return List.of();
    }

    private Long durationToMillis(Duration duration, Date offset) {
        return duration != null ? duration.getTimeInMillis(offset) : null;
    }

    protected void updateRuleThresholdTypeAndValue(EvaluatedPolicyRule rule, C constraint, Duration value) {
        if (!constraint.asPrismContainerValue().isEmpty()) {
            return;
        }

        rule.setThresholdValueType(ThresholdValueType.DURATION, value);
    }

    protected boolean shouldTriggerOnNullValue(Duration value) {
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
    @Nullable
    protected abstract Duration getDurationValue(ActivityPolicyRuleEvaluationContext context);

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
