/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism;

import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.object.RoleAnalysisAttributeDefConvert;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.object.ExtensionProperties;

/**
 * A distance measure implementation for calculating the Jaccard distance/similarity between two sets of values.
 */
public class JaccardDistancesMeasure implements DistanceMeasure {
    private final int minIntersection;
    private final int minIntersectionAttributes;
    Set<RoleAnalysisAttributeDefConvert> attributesMatch;

    /**
     * Constructs a JaccardDistancesMeasure with the specified minimum intersection size for calculation.
     *
     * @param minIntersection The minimum intersection size required for Jaccard distance computation.
     */
    public JaccardDistancesMeasure(int minIntersection) {
        this.minIntersection = minIntersection;
        this.minIntersectionAttributes = 0;
    }

    public JaccardDistancesMeasure(int minIntersection,
            @NotNull Set<RoleAnalysisAttributeDefConvert> attributesMatch,
            int minIntersectionAttributes) {
        this.minIntersectionAttributes = minIntersectionAttributes;
        this.minIntersection = minIntersection;
        this.attributesMatch = attributesMatch;
    }

    /**
     * Computes the Jaccard distance between two sets of values.
     *
     * @param valueA The first set of values.
     * @param valueB The second set of values.
     * @return The computed Jaccard distance between the sets.
     */
    @Override
    public double computeBalancedDistance(
            @NotNull Set<String> valueA,
            @NotNull Set<String> valueB) {
        int intersectionCount = 0;
        int setBunique = 0;

        if (valueA.size() > valueB.size()) {
            for (String num : valueB) {
                if (valueA.contains(num)) {
                    intersectionCount++;
                } else {
                    setBunique++;
                }
            }

            if (intersectionCount < minIntersection) {
                return 1;
            }

            return 1 - (double) intersectionCount / (valueA.size() + setBunique);

        } else {

            for (String num : valueA) {
                if (valueB.contains(num)) {
                    intersectionCount++;
                } else {
                    setBunique++;
                }
            }

            if (intersectionCount < minIntersection) {
                return 1;
            }

            return 1 - (double) intersectionCount / (valueB.size() + setBunique);

        }

    }

    @Override
    public double computeMultiValueAttributes(
            @NotNull Set<String> valueA,
            @NotNull Set<String> valueB) {
        int intersectionCount = 0;
        int setBunique = 0;

        if (valueA.size() > valueB.size()) {
            for (String num : valueB) {
                if (valueA.contains(num)) {
                    intersectionCount++;
                } else {
                    setBunique++;
                }
            }

            if (intersectionCount < minIntersectionAttributes) {
                return 1;
            }

            return 1 - (double) intersectionCount / (valueA.size() + setBunique);

        } else {

            for (String num : valueA) {
                if (valueB.contains(num)) {
                    intersectionCount++;
                } else {
                    setBunique++;
                }
            }

            if (intersectionCount < minIntersectionAttributes) {
                return 1;
            }

            return 1 - (double) intersectionCount / (valueB.size() + setBunique);

        }

    }

    @Override
    public double computeRuleDistance(
            @NotNull ExtensionProperties valueA,
            @NotNull ExtensionProperties valueB,
            @NotNull Set<ClusterExplanation> explanation) {

        double weightSum = 0;

        ClusterExplanation clusterExplanation = new ClusterExplanation();

        Set<AttributeMatchExplanation> attributeMatchExplanations = new HashSet<>();
        for (RoleAnalysisAttributeDefConvert roleAnalysisAttributeDefConvert : attributesMatch) {

            boolean multiValue = roleAnalysisAttributeDefConvert.isMultiValue();
            if (!multiValue) {
                double weight = computeSingleValue(valueA, valueB, roleAnalysisAttributeDefConvert);
                if (weight > 0) {
                    AttributeMatchExplanation attributeMatchExplanation = new AttributeMatchExplanation(
                            roleAnalysisAttributeDefConvert.getAttributeDisplayValue().toString(),
                            valueA.getSingleValueForKey(roleAnalysisAttributeDefConvert));
                    attributeMatchExplanations.add(attributeMatchExplanation);
                    weightSum += weight;
                }
            } else {
                double weight = computeMultiValue(valueA, valueB, roleAnalysisAttributeDefConvert);
                if (weight > 0) {
                    AttributeMatchExplanation attributeMatchExplanation = new AttributeMatchExplanation(
                            roleAnalysisAttributeDefConvert.getAttributeDisplayValue().toString(),
                            "multiValue");
                    attributeMatchExplanations.add(attributeMatchExplanation);
                    weightSum += weight;
                }
            }
        }

        if (weightSum >= 1.0) {
            clusterExplanation.setAttributeExplanation(attributeMatchExplanations);
            explanation.add(clusterExplanation);
            return 0;
        }

        return 1;
    }

    @Override
    public double computeSimpleDistance(@NotNull Set<String> valueA, @NotNull Set<String> valueB) {
        int intersectionSize = 0;
        for (String element : valueA) {
            if (valueB.contains(element)) {
                intersectionSize++;
            }
        }

        if(intersectionSize < minIntersection) {
            return 1;
        }

        return 0;
    }

    private double computeSingleValue(
            @NotNull ExtensionProperties valueA,
            @NotNull ExtensionProperties valueB,
            @NotNull RoleAnalysisAttributeDefConvert roleAnalysisAttributeDefConvert) {
        String valuesForKeyA = valueA.getSingleValueForKey(roleAnalysisAttributeDefConvert);
        String valuesForKeyB = valueB.getSingleValueForKey(roleAnalysisAttributeDefConvert);

        if (valuesForKeyA != null && valuesForKeyB != null) {
            if (valuesForKeyA.equals(valuesForKeyB)) {
                return roleAnalysisAttributeDefConvert.getWeight();
            }
        }
        return 0;
    }

    private double computeMultiValue(
            @NotNull ExtensionProperties valueA,
            @NotNull ExtensionProperties valueB,
            @NotNull RoleAnalysisAttributeDefConvert roleAnalysisAttributeDefConvert) {
        Set<String> valuesForKeyA = valueA.getSetValuesForKeys(roleAnalysisAttributeDefConvert);
        Set<String> valuesForKeyB = valueB.getSetValuesForKeys(roleAnalysisAttributeDefConvert);
        double percentage = roleAnalysisAttributeDefConvert.getSimilarity();

        if (valuesForKeyA != null
                && valuesForKeyB != null
                && !valuesForKeyA.isEmpty()
                && !valuesForKeyB.isEmpty()) {
            double compute = computeMultiValueAttributes(valuesForKeyA, valuesForKeyB);

            if ((1 - compute) >= percentage) {
                return roleAnalysisAttributeDefConvert.getWeight();
            }
        }

        return 0;
    }

}
