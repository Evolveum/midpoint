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

import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.object.AttributeMatch;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.object.ExtensionProperties;

/**
 * A distance measure implementation for calculating the Jaccard distance/similarity between two sets of values.
 */
public class JaccardDistancesMeasure implements DistanceMeasure {
    private final int minIntersection;
    Set<AttributeMatch> attributesMatch;

    /**
     * Constructs a JaccardDistancesMeasure with the specified minimum intersection size for calculation.
     *
     * @param minIntersection The minimum intersection size required for Jaccard distance computation.
     */
    public JaccardDistancesMeasure(int minIntersection) {
        this.minIntersection = minIntersection;
    }

    public JaccardDistancesMeasure(int minIntersection, Set<AttributeMatch> attributesMatch) {
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
    public double compute(@NotNull Set<String> valueA, @NotNull Set<String> valueB) {
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
    public double compute(ExtensionProperties valueA, ExtensionProperties valueB, Set<ClusterExplanation> explanation) {

        double weightSum = 0;

        ClusterExplanation clusterExplanation = new ClusterExplanation();

        Set<AttributeMatchExplanation> attributeMatchExplanations = new HashSet<>();
        for (AttributeMatch attributeMatch : attributesMatch) {

            boolean multiValue = attributeMatch.isMultiValue();
            if (!multiValue) {
                double weight = computeSingleValue(valueA, valueB, attributeMatch);
                if (weight > 0) {
                    AttributeMatchExplanation attributeMatchExplanation = new AttributeMatchExplanation(
                            attributeMatch.getKey().toString(),
                            valueA.getSingleValueForKey(attributeMatch));
                    attributeMatchExplanations.add(attributeMatchExplanation);
                    weightSum += weight;
                }
            } else {
                double weight = computeMultiValue(valueA, valueB, attributeMatch);
                if (weight > 0) {
                    AttributeMatchExplanation attributeMatchExplanation = new AttributeMatchExplanation(
                            attributeMatch.getKey().toString(),
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

    private double calculateConfidence(double weightSum) {
        return weightSum / attributesMatch.size();
    }

    private double computeSingleValue(ExtensionProperties valueA, ExtensionProperties valueB, AttributeMatch attributeMatch) {
        String valuesForKeyA = valueA.getSingleValueForKey(attributeMatch);
        String valuesForKeyB = valueB.getSingleValueForKey(attributeMatch);

        if (valuesForKeyA != null && valuesForKeyB != null) {
            if (valuesForKeyA.equals(valuesForKeyB)) {
                return attributeMatch.getWeight();
            }
        }
        return 0;
    }

    private double computeMultiValue(ExtensionProperties valueA, ExtensionProperties valueB, AttributeMatch attributeMatch) {
        Set<String> valuesForKeyA = valueA.getSetValuesForKeys(attributeMatch);
        Set<String> valuesForKeyB = valueB.getSetValuesForKeys(attributeMatch);
        double percentage = attributeMatch.getSimilarity();

        if (valuesForKeyA != null
                && valuesForKeyB != null
                && !valuesForKeyA.isEmpty()
                && !valuesForKeyB.isEmpty()) {
            double compute = compute(valuesForKeyA, valuesForKeyB);

            if ((1 - compute) >= percentage) {
                return attributeMatch.getWeight();
            }
        }

        return 0;
    }

}
