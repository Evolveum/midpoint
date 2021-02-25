/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import static com.evolveum.midpoint.util.MiscUtil.or0;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.QualifiedItemProcessingOutcomeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StructuredTaskProgressType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartProgressType;

/**
 * This is "live" structured task progress information.
 *
 * Thread safety: Must be thread safe.
 *
 * 1. Updates are invoked in the context of the thread executing the task.
 * 2. But queries are invoked either from this thread, or from some observer (task manager or GUI thread).
 *
 */
public class StructuredTaskProgress {

    private static final Trace LOGGER = TraceManager.getTrace(StructuredTaskProgress.class);

    /** Current value */
    @NotNull private final StructuredTaskProgressType value = new StructuredTaskProgressType();

    @NotNull
    private final PrismContext prismContext;

    public StructuredTaskProgress(@NotNull PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    public StructuredTaskProgress(StructuredTaskProgressType value, @NotNull PrismContext prismContext) {
        this.prismContext = prismContext;
        if (value != null) {
            addTo(this.value, value);
        }
    }

    /** Returns a current value of this statistics. It is copied because of thread safety issues. */
    public synchronized StructuredTaskProgressType getValueCopy() {
        return value.clone();
    }

    /**
     * Sets the part information. Should be called when part processing starts.
     */
    public synchronized void setPartInformation(String partUri, Integer partNumber, Integer expectedParts) {
        value.setCurrentPartUri(partUri);
        value.setCurrentPartNumber(partNumber);
        value.setExpectedParts(expectedParts);
    }

    /**
     * Increments the progress.
     */
    public synchronized void increment(String partUri, QualifiedItemProcessingOutcomeType outcome) {
        TaskPartProgressType part =
                findOrCreateMatchingPart(value.getPart(), partUri, prismContext);
        int count = OutcomeKeyedCounterTypeUtil.incrementCounter(part.getOpen(), outcome, prismContext);
        LOGGER.info("Incremented structured progress to {}. Part uri = {}, outcome = {}", count, partUri, outcome);
    }

    /**
     * Moves "open" counters to "closed" state.
     */
    public synchronized void updateStructuredProgressOnWorkBucketCompletion() {
        LOGGER.info("Updating structured progress on work bucket completion. Part URI: {}", value.getCurrentPartUri()); // todo trace
        Optional<TaskPartProgressType> partOptional = findMatchingPart(value.getPart(), value.getCurrentPartUri());
        if (partOptional.isPresent()) {
            TaskPartProgressType part = partOptional.get();
            OutcomeKeyedCounterTypeUtil.addCounters(part.getClosed(), part.getOpen());
            part.getOpen().clear();
        } else {
            LOGGER.info("Didn't update structured progress for part {} as there are no records present for that part",
                    value.getCurrentPartUri()); // todo trace
        }
    }

    /** Updates specified summary with given delta. */
    public static void addTo(@NotNull StructuredTaskProgressType sum, @NotNull StructuredTaskProgressType delta) {
        addMatchingParts(sum.getPart(), delta.getPart());
    }

    /** Looks for matching parts (created if necessary) and adds them. */
    private static void addMatchingParts(List<TaskPartProgressType> sumParts, List<TaskPartProgressType> deltaParts) {
        for (TaskPartProgressType deltaPart : deltaParts) {
            TaskPartProgressType matchingPart = findOrCreateMatchingPart(sumParts, deltaPart.getPartUri(), null);
            addPartInformation(matchingPart, deltaPart);
        }
    }

    private static TaskPartProgressType findOrCreateMatchingPart(
            List<TaskPartProgressType> parts, String partUri, PrismContext prismContext) {
        return findMatchingPart(parts, partUri)
                .orElseGet(
                        () -> OutcomeKeyedCounterTypeUtil.add(parts, new TaskPartProgressType(prismContext).partUri(partUri)));
    }

    private static Optional<TaskPartProgressType> findMatchingPart(
            List<TaskPartProgressType> parts, String partUri) {
        return parts.stream()
                .filter(item -> Objects.equals(item.getPartUri(), partUri))
                .findFirst();
    }

    /** Adds two "part information" */
    private static void addPartInformation(@NotNull TaskPartProgressType sum, @NotNull TaskPartProgressType delta) {
        OutcomeKeyedCounterTypeUtil.addCounters(sum.getClosed(), delta.getClosed());
        OutcomeKeyedCounterTypeUtil.addCounters(sum.getOpen(), delta.getOpen());
    }

    public static String format(StructuredTaskProgressType source) {
        return format(source, null);
    }

    /** Formats the information. */
    public static String format(StructuredTaskProgressType source, AbstractStatisticsPrinter.Options options) {
        StructuredTaskProgressType information = source != null ? source : new StructuredTaskProgressType();
        return new StructuredTaskProgressPrinter(information, options).print();
    }
}
