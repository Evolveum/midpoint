/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl.activities;

import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributeTupleStatisticsType;

import org.jetbrains.annotations.VisibleForTesting;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAttributeDefinition;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributeStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowObjectClassStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.namespace.QName;

/**
 * Computes statistics for shadow objects.
 * Does not need to care about the timestamp and the coverage.
 */
@VisibleForTesting
public class StatisticsComputer {

    /**
     * Maximum number of value occurrences to be retained per attribute.
     * TODO: Make this configurable.
     */
    private static final int TOP_N_LIMIT = 30;

    /**
     * Cardinality limit for cross-table (pairwise attribute) statistics.
     *
     * If the number of unique value pairs for a particular attribute exceeds this limit,
     * the attribute is excluded from pairwise statistics.
     * TODO: Make this configurable.
     */
    private static final int VALUE_COUNT_PAIR_HARD_LIMIT = 10;

    /**
     * Percentage-based cardinality limit for cross-table statistics.
     * If the unique value count for an attribute exceeds this percentage of the total sample size,
     * the attribute is excluded from pairwise statistics.
     * TODO: Make this configurable.
     */
    private static final double VALUE_COUNT_PAIR_PERCENTAGE_LIMIT = 0.05;

    /**
     * Attribute value counts for each attribute (by attribute name).
     */
    private final Map<String, Map<String, Integer>> valueCounts = new HashMap<>();

    /**
     * Stores attribute values for each shadow, for use in cross-table (pairwise) statistics.
     *
     * Maps attribute QName to a list of observed values (one per processed shadow).
     */
    Map<QName, LinkedList<Object>> shadowStorage = new HashMap<>();

    /**
     * JAXB statistics object being built.
     */
    private final ShadowObjectClassStatisticsType statistics = new ShadowObjectClassStatisticsType();

    /**
     * Attribute definitions for the relevant object class.
     */
    private final Collection<? extends ShadowAttributeDefinition<?, ?, ?, ?>> attributeDefinitions;

    /**
     * Constructs a new statistics computer for the given object class definition.
     *
     * @param objectClassDef Resource object class definition.
     */
    public StatisticsComputer(ResourceObjectClassDefinition objectClassDef) {
        this.attributeDefinitions = objectClassDef.getAttributeDefinitions();
        initializeAttributeStatistics();
        initializeValueCounts();
        initializeShadowStorage();
    }

    /**
     * Initializes attribute statistics objects for each attribute definition.
     */
    private void initializeAttributeStatistics() {
        for (ShadowAttributeDefinition<?, ?, ?, ?> attrDef : attributeDefinitions) {
            getOrCreateAttributeStatistics(attrDef.getItemName());
        }
    }

    /**
     * Initializes the value counts map for each attribute.
     */
    private void initializeValueCounts() {
        for (ShadowAttributeDefinition<?, ?, ?, ?> attrDef : attributeDefinitions) {
            valueCounts.put(attrDef.getItemName().toString(), new HashMap<>());
        }
    }

    private void initializeShadowStorage() {
        for (ShadowAttributeDefinition<?, ?, ?, ?> attrDef : attributeDefinitions) {
            shadowStorage.put(attrDef.getItemName(), new LinkedList<Object>());
        }
    }

    /**
     * Processes the given shadow object, updating statistics.
     *
     * @param shadow Shadow object to process.
     */
    public void process(ShadowType shadow) {
        statistics.setSize(statistics.getSize() + 1);
        updateMissingValueCounts(shadow);
        updateValueCounts(shadow);
        updateShadowStorage(shadow);
    }

    /**
     * Performs post-processing: retains only the top N value counts and
     * converts internal maps to JAXB-compatible structures.
     */
    public void postProcessStatistics() {
        retainTopNValueCounts();
        populateJaxbAttributeStatistics();
        removeAttributesWithLargeCardinalityFromShadowStorage();
        computeValueCountsofValuePairs();
    }

    /**
     * Returns the statistics object.
     *
     * @return Shadow object class statistics.
     */
    public ShadowObjectClassStatisticsType getStatistics() {
        return statistics;
    }

    /**
     * Gets or creates attribute statistics for the given attribute name.
     *
     * @param attrName Attribute item name.
     * @return Attribute statistics object.
     */
    private ShadowAttributeStatisticsType getOrCreateAttributeStatistics(ItemName attrName) {
        for (ShadowAttributeStatisticsType stats : statistics.getAttribute()) {
            if (attrName.equals(stats.getRef())) {
                return stats;
            }
        }
        ShadowAttributeStatisticsType newStats = new ShadowAttributeStatisticsType().ref(attrName);
        statistics.getAttribute().add(newStats);
        return newStats;
    }

    /**
     * Increments missing value counts for attributes not present in the shadow.
     */
    private void updateMissingValueCounts(ShadowType shadow) {
        for (ShadowAttributeStatisticsType stats : statistics.getAttribute()) {
            if (ShadowUtil.getAttributeValues(shadow, stats.getRef()).isEmpty()) {
                stats.setMissingValueCount(stats.getMissingValueCount() + 1);
            }
        }
    }

    /**
     * Updates value occurrence counts for each attribute in the given shadow.
     *
     * For each attribute with exactly one value, increments the occurrence count for that value,
     * and updates the unique value count if this is the first occurrence.
     *
     * @param shadow Shadow object to analyze.
     */
    private void updateValueCounts(ShadowType shadow) {
        for (ShadowAttributeStatisticsType stats : statistics.getAttribute()) {
            List<?> attributeValues = ShadowUtil.getAttributeValues(shadow, stats.getRef());

            if (attributeValues.size() != 1) {
                continue;
            }

            String attributeKey = stats.getRef().toString();
            Map<String, Integer> attributeValueCounts = valueCounts.get(attributeKey);

            String valueKey = String.valueOf(attributeValues.get(0));
            int newCount = attributeValueCounts.merge(valueKey, 1, Integer::sum);

            if (newCount == 1) {
                stats.setUniqueValueCount(stats.getUniqueValueCount() + 1);
            }
        }
    }

    /**
     * Updates the shadow storage with attribute values from the provided shadow object.
     * If the number of value-count pairs for a particular key exceeds the allowed limit,
     * the entry is removed from the storage.
     */
    private void updateShadowStorage(ShadowType shadow) {
        Iterator<Map.Entry<QName, LinkedList<Object>>> iter = shadowStorage.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<QName, LinkedList<Object>> entry = iter.next();
            QName key = entry.getKey();
            LinkedList<Object> list = entry.getValue();

            List<?> values = ShadowUtil.getAttributeValues(shadow, key);
            list.add(values.size() != 1 ? null : values.get(0));
            if (valueCounts.get(key.toString()).size() > VALUE_COUNT_PAIR_HARD_LIMIT) {
                iter.remove();
            }
        }
    }

    /**
     * Retains only the top N most frequent values for each attribute in {@code valueCounts}.
     *
     * For each attribute, if all values appear only once, clears its value counts.
     * Otherwise, keeps only the top {@code TOP_N_LIMIT} values with the highest counts,
     * discarding the rest.
     */
    private void retainTopNValueCounts() {
        for (ShadowAttributeStatisticsType stats : statistics.getAttribute()) {
            String attrKey = stats.getRef().toString();
            Map<String, Integer> counts = valueCounts.get(attrKey);

            int maxCount = counts.values().stream().max(Integer::compareTo).orElse(0);

            if (maxCount == 1) {
                valueCounts.put(attrKey, new LinkedHashMap<>());
            } else {
                Map<String, Integer> topN = counts.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                        .limit(TOP_N_LIMIT)
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (a, b) -> a,
                                LinkedHashMap::new
                        ));
                valueCounts.put(attrKey, topN);
            }
        }
    }

    /**
     * Populates JAXB attribute statistics objects with value count data.
     *
     * For each attribute, adds value/count pairs to the corresponding
     * {@link ShadowAttributeStatisticsType} in the statistics object.
     */
    private void populateJaxbAttributeStatistics() {
        for (ShadowAttributeStatisticsType stats : statistics.getAttribute()) {
            String attrKey = stats.getRef().toString();
            Map<String, Integer> counts = valueCounts.get(attrKey);
            if (counts == null) {
                continue;
            }
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                stats.beginValueCount()
                        .value(entry.getKey())
                        .count(entry.getValue());
            }
        }
    }

    /**
     * Removes entries from {@code shadowStorage} for attributes whose unique value count
     * exceeds a specified percentage threshold of the total statistics size.
     *
     * The threshold is calculated as {@code statisticsSize * VALUE_COUNT_PAIR_PERCENTAGE_LIMIT}.
     * Any attribute in {@code statistics} with a unique value count above this threshold
     * will have its corresponding entry (by reference) removed from {@code shadowStorage}.
     */
    private void removeAttributesWithLargeCardinalityFromShadowStorage() {
        int statisticsSize = statistics.getSize();
        double cardinalityThreshold = statisticsSize * VALUE_COUNT_PAIR_PERCENTAGE_LIMIT;

        for (ShadowAttributeStatisticsType attribute : statistics.getAttribute()) {
            Object ref = attribute.getRef();

            if (shadowStorage.containsKey(ref) && attribute.getUniqueValueCount() > cardinalityThreshold) {
                shadowStorage.remove(ref);
            }
        }
    }

    /**
     * Computes and records the occurrence counts of all value pairs across
     * pairs of lists stored in {@code shadowStorage}. For every unique pair of
     * keys in {@code shadowStorage}, this method iterates over the corresponding
     * lists in parallel, counts how often each pair of non-null values appears
     * at the same position, and records these statistics using the {@code statistics} object.
     */
    private void computeValueCountsofValuePairs() {
        List<Object> indices = new ArrayList<>(shadowStorage.keySet());

        for (int x = 0; x < indices.size() - 1; x++) {
            for (int y = x + 1; y < indices.size(); y++) {
                List<Object> list1 = shadowStorage.get(indices.get(x));
                List<Object> list2 = shadowStorage.get(indices.get(y));
                Map<String, Map<String, Integer>> pairCounts = new HashMap<>();

                int minSize = Math.min(list1.size(), list2.size());
                for (int i = 0; i < minSize; i++) {
                    Object s1 = list1.get(i);
                    Object s2 = list2.get(i);
                    if (s1 != null && s2 != null) {
                        pairCounts.computeIfAbsent(s1.toString(), k -> new HashMap<>())
                                .merge(s2.toString(), 1, Integer::sum);
                    }
                }

                if (!pairCounts.isEmpty()) {
                    ShadowAttributeTupleStatisticsType tuple = statistics.beginAttributeTuple()
                            .ref((QName) indices.get(x))
                            .ref((QName) indices.get(y));
                    for (Map.Entry<String, Map<String, Integer>> entry1 : pairCounts.entrySet()) {
                        for (Map.Entry<String, Integer> entry2 : entry1.getValue().entrySet()) {
                            tuple
                                    .beginTupleCount()
                                    .value(entry1.getKey())
                                    .value(entry2.getKey())
                                    .count(entry2.getValue());
                        }
                    }
                }
            }
        }
    }
}
