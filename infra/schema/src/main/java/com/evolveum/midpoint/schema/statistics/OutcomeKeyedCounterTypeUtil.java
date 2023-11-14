/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemProcessingOutcomeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OutcomeKeyedCounterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.QualifiedItemProcessingOutcomeType;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static com.evolveum.midpoint.util.MiscUtil.or0;

public class OutcomeKeyedCounterTypeUtil {

    /** Adds two lists of counters: finds matching pairs and adds them. */
    public static void addCounters(List<OutcomeKeyedCounterType> sumCounters, List<OutcomeKeyedCounterType> deltaCounters) {
        for (OutcomeKeyedCounterType deltaCounter : deltaCounters) {
            OutcomeKeyedCounterType matchingCounter = findOrCreateCounter(sumCounters, deltaCounter.getOutcome());
            addMatchingCounters(matchingCounter, deltaCounter);
        }
    }

    /** Adds two matching counters. */
    private static void addMatchingCounters(OutcomeKeyedCounterType sum, OutcomeKeyedCounterType delta) {
        sum.setCount(or0(sum.getCount()) + or0(delta.getCount()));
    }

    /** Finds a counter, creating _and adding to the list_ if necessary. */
    private static OutcomeKeyedCounterType findOrCreateCounter(
            List<OutcomeKeyedCounterType> counters, QualifiedItemProcessingOutcomeType outcome) {
        return counters.stream()
                .filter(counter -> Objects.equals(counter.getOutcome(), outcome))
                .findFirst()
                .orElseGet(
                        () -> add(counters, new OutcomeKeyedCounterType().outcome(outcome.clone())));
    }

    /** Like {@link List#add(Object)} but returns the value. */
    private static <T> T add(List<T> list, T value) {
        list.add(value);
        return value;
    }

    /** Increments counter corresponding to given outcome. */
    public static int incrementCounter(List<OutcomeKeyedCounterType> counters, QualifiedItemProcessingOutcomeType outcome) {
        OutcomeKeyedCounterType counter = findOrCreateCounter(counters, outcome);
        counter.setCount(or0(counter.getCount()) + 1);
        return counter.getCount();
    }

    public static int getSuccessCount(List<? extends OutcomeKeyedCounterType> counters) {
        return getCount(counters, OutcomeKeyedCounterTypeUtil::isSuccess);
    }

    public static int getFailureCount(List<? extends OutcomeKeyedCounterType> counters) {
        return getCount(counters, OutcomeKeyedCounterTypeUtil::isFailure);
    }

    public static int getSkipCount(List<? extends OutcomeKeyedCounterType> counters) {
        return getCount(counters, OutcomeKeyedCounterTypeUtil::isSkip);
    }

    public static int getCount(List<? extends OutcomeKeyedCounterType> counters, Predicate<OutcomeKeyedCounterType> filter) {
        return counters.stream()
                .filter(filter)
                .mapToInt(c -> or0(c.getCount()))
                .sum();
    }

    public static boolean isSuccess(OutcomeKeyedCounterType counter) {
        return getOutcome(counter) == ItemProcessingOutcomeType.SUCCESS;
    }

    public static boolean isFailure(OutcomeKeyedCounterType counter) {
        return getOutcome(counter) == ItemProcessingOutcomeType.FAILURE;
    }

    public static boolean isSkip(OutcomeKeyedCounterType counter) {
        return getOutcome(counter) == ItemProcessingOutcomeType.SKIP;
    }

    public static ItemProcessingOutcomeType getOutcome(OutcomeKeyedCounterType counter) {
        if (counter != null && counter.getOutcome() != null) {
            return counter.getOutcome().getOutcome();
        } else {
            return null;
        }
    }

    static String getOutcomeQualifierUri(OutcomeKeyedCounterType counter) {
        if (counter != null && counter.getOutcome() != null) {
            return counter.getOutcome().getQualifierUri();
        } else {
            return null;
        }
    }

    static Comparator<? super OutcomeKeyedCounterType> createOutcomeKeyedCounterComparator() {
        return (o1, o2) ->
                ComparisonChain.start()
                        .compare(getOutcome(o1), getOutcome(o2), Ordering.natural().nullsLast())
                        .compare(getOutcomeQualifierUri(o1), getOutcomeQualifierUri(o2), Ordering.natural().nullsLast())
                        .result();
    }

    public static Predicate<OutcomeKeyedCounterType> getCounterFilter(ItemProcessingOutcomeType outcome) {
        return switch (outcome) {
            case SUCCESS -> OutcomeKeyedCounterTypeUtil::isSuccess;
            case FAILURE -> OutcomeKeyedCounterTypeUtil::isFailure;
            case SKIP -> OutcomeKeyedCounterTypeUtil::isSkip;
        };
    }
}
