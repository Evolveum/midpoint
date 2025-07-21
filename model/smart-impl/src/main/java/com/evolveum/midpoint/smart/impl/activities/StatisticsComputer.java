/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl.activities;

import java.util.Collection;

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
     * Upper limit for the maximum allowed number of values in the statistics.
     * This number will never be exceeded.
     * See {@link #MAX_VALUE_COUNT_PERCENTAGE}.
     *
     * TODO make this configurable (eventually).
     */
    private static final int MAX_VALUE_COUNT_UPPER_LIMIT = 30;

    /**
     * Lower limit for the maximum allowed number of values in the statistics.
     * We always allow at least this number of values.
     * See {@link #MAX_VALUE_COUNT_PERCENTAGE}.
     *
     * TODO make this configurable (eventually).
     */
    private static final int MAX_VALUE_COUNT_LOWER_LIMIT = 5;

    /**
     * Limit for the number of values in the statistics, related to the total number of objects with non-null values.
     * For example, if there are 200 such records in total, and this limit is set to 5% (0.05), we allow only 10 values.
     *
     * The effective limit is also bound by {@link #MAX_VALUE_COUNT_UPPER_LIMIT} and {@link #MAX_VALUE_COUNT_LOWER_LIMIT}:
     *
     * - If the computed limit is greater than {@link #MAX_VALUE_COUNT_UPPER_LIMIT}, we use that instead.
     * - If the computed limit is lower than {@link #MAX_VALUE_COUNT_LOWER_LIMIT}, we use that instead.
     *
     * TODO make this configurable (eventually).
     */
    private static final float MAX_VALUE_COUNT_PERCENTAGE = 0.25f;

    /** Statistics being computed. */
    private final ShadowObjectClassStatisticsType statistics;

    /** Definitions of attributes that are expected to be present in the shadows. Ignoring auxiliary OCs for now. */
    private final Collection<? extends ShadowAttributeDefinition<?, ?, ?, ?>> attributeDefinitions;

    public StatisticsComputer(ResourceObjectClassDefinition objectClassDef) {
        statistics = new ShadowObjectClassStatisticsType();
        attributeDefinitions = objectClassDef.getAttributeDefinitions();
        createAttributeStatistics();
    }

    private void createAttributeStatistics() {
        for (var attributeDefinition : attributeDefinitions) {
            findOrCreateAttributeStatistics(attributeDefinition.getItemName());
        }
    }

    /** Adds the shadow to the statistics. */
    public void process(ShadowType shadow) {
        statistics.setSize(statistics.getSize() + 1);
        addMissingValueCounts(shadow);
        addValueCounts(shadow);
    }

    private void addMissingValueCounts(ShadowType shadow) {
        for (var attributeStatistics : statistics.getAttribute()) {
            var attrName = attributeStatistics.getRef();
            if (ShadowUtil.getAttributeValues(shadow, attrName).isEmpty()) {
                attributeStatistics.setMissingValueCount(attributeStatistics.getMissingValueCount() + 1);
            }
        }
    }

    /**
     * Updates the value counts for the attributes in the shadow.
     * For each attribute, it counts the number of occurrences of each value.
     * If the number of distinct values exceeds the limit defined by {@link #MAX_VALUE_COUNT_UPPER_LIMIT},
     * statistics for that attribute will be removed, and will not be collected any further.
     */
    private void addValueCounts(ShadowType shadow) {
        for (var attributeStatistics : statistics.getAttribute()) {
            var attrValues = ShadowUtil.getAttributeValues(shadow, attributeStatistics.getRef());

            attributeValuesLoop:
            for (var attrValue : attrValues) {
                for (var valueCountPair : attributeStatistics.getValueCount()) {
                    if (valueCountPair.getValue().equals(attrValue)) {
                        valueCountPair.setCount(valueCountPair.getCount() + 1);
                        continue attributeValuesLoop;
                    }
                }
                attributeStatistics.beginValueCount()
                        .count(1)
                        .value(String.valueOf(attrValue));
                attributeStatistics.setUniqueValueCount(attributeStatistics.getUniqueValueCount() + 1);
            }
        }

        statistics.getAttribute().removeIf(attr -> attr.getUniqueValueCount() > MAX_VALUE_COUNT_UPPER_LIMIT);
    }

    /**
     * Now, when we already know the number of objects with non-null values in the attributes statistics,
     * we can remove the statistics for attributes that have too many values, as described in {@link #MAX_VALUE_COUNT_PERCENTAGE}.
     */
    private void removeLargeValueCounts() {
        statistics.getAttribute().removeIf(attr -> attr.getUniqueValueCount() > MAX_VALUE_COUNT_PERCENTAGE * statistics.getSize());
    }

    private ShadowAttributeStatisticsType findOrCreateAttributeStatistics(ItemName attrName) {
        for (var attributeStatistics : statistics.getAttribute()) {
            if (attrName.equals(attributeStatistics.getRef())) {
                return attributeStatistics;
            }
        }
        var newAttrStatistics = new ShadowAttributeStatisticsType().ref(attrName);
        statistics.getAttribute().add(newAttrStatistics);
        return newAttrStatistics;
    }

    public void postProcessStatistics() {
        removeLargeValueCounts();
    }

    public ShadowObjectClassStatisticsType getStatistics() {
        return statistics;
    }
}
