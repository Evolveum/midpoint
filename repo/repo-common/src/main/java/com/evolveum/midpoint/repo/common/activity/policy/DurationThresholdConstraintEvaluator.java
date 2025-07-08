/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.GregorianCalendar;
import java.util.List;
import javax.xml.datatype.Duration;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DurationThresholdPolicyConstraintType;

import org.jetbrains.annotations.Nullable;

public abstract class DurationThresholdConstraintEvaluator<C extends DurationThresholdPolicyConstraintType>
        implements ActivityPolicyConstraintEvaluator<C, DurationThresholdPolicyTrigger<C>> {

    private static final Trace LOGGER = TraceManager.getTrace(DurationThresholdConstraintEvaluator.class);

    public enum ThresholdType {
        BELOW, EXCEEDS
    }

    @Override
    public List<DurationThresholdPolicyTrigger<C>> evaluate(
            C constraint,
            ActivityPolicyRuleEvaluationContext context,
            OperationResult result) {

        Long value = getDurationValue(context);
        if (value == null) {
            LOGGER.trace("No duration value to evaluate for constraint {}", constraint.getName());
            return List.of();
        }

        LOGGER.trace("Evaluating duration constraint {} against value {}", constraint.getName(), value);

        Long below = durationToMillis(constraint.getBelow(), context.getActivityRun().getStartTimestampRequired());
        if (below != null && context.getActivityRun().getActivityState().isComplete() && value < below) {
            LOGGER.trace("Duration value {} is below the threshold of constraint {}, creating trigger", value, constraint.getName());

            LocalizableMessage message = createMessage(constraint.getName(), value, constraint.getBelow(), ThresholdType.BELOW);
            LocalizableMessage shortMessage = createShortMessage(constraint.getName(), ThresholdType.BELOW);

            return List.of(createTrigger(constraint, message, shortMessage));
        }

        Long exceeds = durationToMillis(constraint.getExceeds(), context.getActivityRun().getStartTimestampRequired());

        if (exceeds != null && value > exceeds) {
            LOGGER.trace("Duration value {}ms exceeds the threshold of constraint {}, creating trigger", value, constraint.getName());

            LocalizableMessage message = createMessage(constraint.getName(), value, constraint.getExceeds(), ThresholdType.EXCEEDS);
            LocalizableMessage shortMessage = createShortMessage(constraint.getName(), ThresholdType.EXCEEDS);

            return List.of(createTrigger(constraint, message, shortMessage));
        }

        return List.of();
    }

    private Long durationToMillis(Duration duration, long startTimestamp) {
        if (duration == null) {
            return null;
        }

        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(startTimestamp);
        duration.addTo(calendar);

        return calendar.getTimeInMillis() - startTimestamp;
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
    protected abstract Long getDurationValue(ActivityPolicyRuleEvaluationContext context);

    protected LocalizableMessage createMessage(String constraintName, Long realValue, Duration threshold, ThresholdType type) {
        String key = type == ThresholdType.EXCEEDS ?
                "EvaluatedDurationThresholdConstraintEvaluator.exceedsMessage" :
                "EvaluatedDurationThresholdConstraintEvaluator.belowMessage";

        String message = createDefaultMessage(constraintName, realValue, threshold, type);

        return new SingleLocalizableMessage(key, new Object[] { constraintName, realValue, threshold }, message);
    }

    protected LocalizableMessage createShortMessage(String constraintName, ThresholdType type) {
        String key = type == ThresholdType.EXCEEDS ?
                "EvaluatedDurationThresholdConstraintEvaluator.exceedsShortMessage" :
                "EvaluatedDurationThresholdConstraintEvaluator.belowShortMessage";

        String message = createDefaultShortMessage(constraintName, type);

        return new SingleLocalizableMessage(key, new Object[] { constraintName }, message);
    }

    protected String createDefaultMessage(String constraintName, Long realValue, Duration threshold, ThresholdType type) {
        String msg = type == ThresholdType.EXCEEDS ?
                "Measured duration is %s ms, which exceeds the threshold of constraint %s (%s)" :
                "Measured duration is %s ms, which is below the threshold of constraint %s (%s)";

        return msg.formatted(realValue, constraintName, threshold);
    }

    protected String createDefaultShortMessage(String constraintName, ThresholdType type) {
        String msg = type == ThresholdType.EXCEEDS ?
                "Measured duration exceeded for constraint %s" :
                "Measured duration is below the threshold for constraint %s";

        return msg.formatted(constraintName);
    }
}
