/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl.activities;

import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributeValueCountType;

import org.jetbrains.annotations.VisibleForTesting;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAttributeDefinition;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributeStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowObjectClassStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

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
     * Attribute value counts for each attribute (by attribute name).
     */
    private final Map<String, Map<String, Integer>> valueCounts = new HashMap<>();

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

    /**
     * Processes the given shadow object, updating statistics.
     *
     * @param shadow Shadow object to process.
     */
    public void process(ShadowType shadow) {
        statistics.setSize(statistics.getSize() + 1);
        updateMissingValueCounts(shadow);
        updateValueCounts(shadow);
    }

    /**
     * Performs post-processing: retains only the top N value counts and
     * converts internal maps to JAXB-compatible structures.
     */
    public void postProcessStatistics() {
        retainTopNValueCounts();
        populateJaxbAttributeStatistics();
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
     * Updates value occurrence counts for each attribute in the shadow.
     */
    private void updateValueCounts(ShadowType shadow) {
        for (ShadowAttributeStatisticsType stats : statistics.getAttribute()) {
            Collection<?> values = ShadowUtil.getAttributeValues(shadow, stats.getRef());
            String attrKey = stats.getRef().toString();
            Map<String, Integer> attrValueCounts = valueCounts.get(attrKey);
            for (Object value : values) {
                var x = attrValueCounts.merge(String.valueOf(value), 1, Integer::sum);
                if (x == 1) {
                    stats.setUniqueValueCount(stats.getUniqueValueCount() + 1);
                }
            }
        }
    }

    /**
     * Retains only the top N most frequent values for each attribute.
     */
    private void retainTopNValueCounts() {
        for (ShadowAttributeStatisticsType stats : statistics.getAttribute()) {
            String attrKey = stats.getRef().toString();
            Map<String, Integer> counts = valueCounts.get(attrKey);
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

    /**
     * Populates JAXB attribute statistics objects with value count data.
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
}
