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
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskProgressCounterType;

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
        TaskProgressCounterType counter = findOrCreateCounter(part.getOpen(), outcome, prismContext);
        counter.setCount(or0(counter.getCount()) + 1);
        LOGGER.info("Incremented structured progress to {}. Part uri = {}, outcome = {}", counter.getCount(), partUri, outcome);
    }

    /**
     * Moves "open" counters to "closed" state.
     */
    public synchronized void updateStructuredProgressOnWorkBucketCompletion() {
        LOGGER.info("Updating structured progress on work bucket completion. Part URI: {}", value.getCurrentPartUri()); // todo trace
        Optional<TaskPartProgressType> partOptional = findMatchingPart(value.getPart(), value.getCurrentPartUri());
        if (partOptional.isPresent()) {
            TaskPartProgressType part = partOptional.get();
            addCounters(part.getClosed(), part.getOpen());
            part.getOpen().clear();
        } else {
            LOGGER.info("Didn't update structured progress for part {} as there are no records present for that part",
                    value.getCurrentPartUri()); // todo trace
        }
    }

    /** Finds progress counter, creating _and adding to the list_ if necessary. */
    private static TaskProgressCounterType findOrCreateCounter(List<TaskProgressCounterType> counters,
            QualifiedItemProcessingOutcomeType outcome, PrismContext prismContext) {
        return counters.stream()
                .filter(counter -> Objects.equals(counter.getOutcome(), outcome))
                .findFirst()
                .orElseGet(
                        () -> add(counters, new TaskProgressCounterType(prismContext).outcome(outcome.clone())));
    }

    /** Like {@link List#add(Object)} but returns the value. */
    private static <T> T add(List<T> list, T value) {
        list.add(value);
        return value;
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
                        () -> add(parts, new TaskPartProgressType(prismContext).partUri(partUri)));
    }

    private static Optional<TaskPartProgressType> findMatchingPart(
            List<TaskPartProgressType> parts, String partUri) {
        return parts.stream()
                .filter(item -> Objects.equals(item.getPartUri(), partUri))
                .findFirst();
    }

    /** Adds two "part information" */
    private static void addPartInformation(@NotNull TaskPartProgressType sum, @NotNull TaskPartProgressType delta) {
        addCounters(sum.getClosed(), delta.getClosed());
        addCounters(sum.getOpen(), delta.getOpen());
    }

    /** Adds progress counters. */
    private static void addCounters(List<TaskProgressCounterType> sumCounters, List<TaskProgressCounterType> deltaCounters) {
        for (TaskProgressCounterType deltaCounter : deltaCounters) {
            TaskProgressCounterType matchingCounter =
                    findOrCreateCounter(sumCounters, deltaCounter.getOutcome(), null);
            addMatchingCounters(matchingCounter, deltaCounter);
        }
    }

    /** Adds matching processed item sets: adds counters and determines the latest `lastItem`. */
    private static void addMatchingCounters(TaskProgressCounterType sum, TaskProgressCounterType delta) {
        sum.setCount(or0(sum.getCount()) + or0(delta.getCount()));
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
