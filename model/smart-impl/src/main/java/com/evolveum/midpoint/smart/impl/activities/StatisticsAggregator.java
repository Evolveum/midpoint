/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.smart.impl.activities;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributeStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowObjectClassStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowValuePatternType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Function;

class StatisticsAggregator<K> {

    private final Function<ItemPathType, K> refResolver;
    private final StatisticsPatternDetector patternDetector = new StatisticsPatternDetector();

    /** Per-attribute incremental aggregation state. Attribute order is kept separately in attributeOrder. */
    private final Map<K, ItemAggregation> aggregations = new ConcurrentHashMap<>();

    /** Item processing order matching the resource object class definition. */
    private final List<K> itemOrder = new ArrayList<>();

    /** Number of processed shadows. */
    private final AtomicInteger size = new AtomicInteger();

    /** JAXB statistics object being built. */
    private final ShadowObjectClassStatisticsType statistics = new ShadowObjectClassStatisticsType();

    StatisticsAggregator(@NotNull Function<ItemPathType, K> refResolver) {
        this.refResolver = refResolver;
        statistics.setSize(0);
    }

    void registerItem(K key, ItemPathType ref, boolean dnAttribute) {
        ItemAggregation previous = aggregations.putIfAbsent(key, new ItemAggregation(dnAttribute));
        if (previous == null) {
            itemOrder.add(key);
            statistics.getAttribute().add(new ShadowAttributeStatisticsType().ref(ref));
        }
    }

    List<K> getItemOrder() {
        return itemOrder;
    }

    void incrementSize() {
        size.incrementAndGet();
    }

    void markMissing(K key) {
        ItemAggregation aggregation = aggregations.get(key);
        if (aggregation != null) {
            aggregation.missingCount.incrementAndGet();
        }
    }

    /**
     * Aggregates values from one item into the running counts.
     */
    void aggregateValues(K key, List<?> values) {
        ItemAggregation aggregation = aggregations.get(key);
        if (aggregation == null) {
            return;
        }

        if (values == null || values.isEmpty()) {
            aggregation.missingCount.incrementAndGet();
            return;
        }

        if (values.size() > 1) {
            return;
        }

        Object rawValue = values.get(0);
        if (rawValue == null) {
            aggregation.missingCount.incrementAndGet();
            return;
        }

        aggregateStringValue(key, String.valueOf(rawValue).trim(), aggregation.isDnAttribute);
    }

    boolean isDnAttribute(@NotNull QName attrName) {
        return patternDetector.isDnAttribute(attrName);
    }

    void aggregateStringValue(K key, String value) {
        ItemAggregation aggregation = aggregations.get(key);
        if (aggregation == null) {
            return;
        }

        aggregateStringValue(key, value, aggregation.isDnAttribute);
    }

    private void aggregateStringValue(K key, String value, boolean dnAttribute) {
        ItemAggregation aggregation = aggregations.get(key);
        if (aggregation == null) {
            return;
        }

        if (value == null || value.isBlank()) {
            aggregation.missingCount.incrementAndGet();
            return;
        }

        aggregation.valueCounts.merge(value, 1, Integer::sum);

        if (dnAttribute) {
            patternDetector.aggregateDnSuffix(value, aggregation.dnSuffixCounts);
        } else {
            patternDetector.aggregateTokenPatterns(
                    value,
                    aggregation.firstTokenCounts,
                    aggregation.lastTokenCounts);
        }
    }

    synchronized void postProcessStatistics() {
        postProcessStatistics((stats, statistics) -> false);
    }

    /**
     * Performs post-processing: converts aggregated counts into JAXB-compatible structures.
     * All values and patterns are emitted without any filtering or top-N limits.
     */
    synchronized void postProcessStatistics(
            BiPredicate<ShadowAttributeStatisticsType, ShadowObjectClassStatisticsType> removePredicate) {

        statistics.setSize(size.get());

        for (var iterator = statistics.getAttribute().iterator(); iterator.hasNext(); ) {
            ShadowAttributeStatisticsType stats = iterator.next();

            K key = refResolver.apply(stats.getRef());
            ItemAggregation aggregation = aggregations.get(key);

            if (aggregation == null) {
                iterator.remove();
                continue;
            }

            stats.setMissingValueCount(aggregation.missingCount.get());
            stats.setUniqueValueCount(aggregation.valueCounts.size());

            emitAllValueCounts(aggregation.valueCounts, stats);

            if (aggregation.isDnAttribute) {
                emitAllPatterns(aggregation.dnSuffixCounts, ShadowValuePatternType.DN_SUFFIX, stats);
            } else {
                emitAllPatterns(aggregation.firstTokenCounts, ShadowValuePatternType.FIRST_TOKEN, stats);
                emitAllPatterns(aggregation.lastTokenCounts, ShadowValuePatternType.LAST_TOKEN, stats);
            }

            if (removePredicate.test(stats, statistics)) {
                iterator.remove();
            }
        }
    }

    ShadowObjectClassStatisticsType getStatistics() {
        return statistics;
    }

    /**
     * Emits all value counts into the statistics, sorted by count descending.
     */
    private void emitAllValueCounts(
            Map<String, Integer> valueCounts,
            ShadowAttributeStatisticsType stats) {

        valueCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> stats.beginValueCount()
                        .value(entry.getKey())
                        .count(entry.getValue()));
    }

    /**
     * Emits all pattern counts of a given type into the statistics, sorted by count descending.
     */
    private void emitAllPatterns(
            Map<String, Integer> counts,
            ShadowValuePatternType type,
            ShadowAttributeStatisticsType stats) {

        counts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> stats.beginValuePatternCount()
                        .value(entry.getKey())
                        .type(type)
                        .count(entry.getValue()));
    }

    private static class ItemAggregation {

        private final AtomicInteger missingCount = new AtomicInteger();

        private final ConcurrentMap<String, Integer> valueCounts = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, Integer> dnSuffixCounts = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, Integer> firstTokenCounts = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, Integer> lastTokenCounts = new ConcurrentHashMap<>();

        private final boolean isDnAttribute;

        private ItemAggregation(boolean isDnAttribute) {
            this.isDnAttribute = isDnAttribute;
        }
    }
}
