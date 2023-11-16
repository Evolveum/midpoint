/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism;

import java.util.Set;

import org.jetbrains.annotations.NotNull;

/**
 * A distance measure implementation for calculating the Jaccard distance/similarity between two sets of values.
 */
public class JaccardDistancesMeasure implements DistanceMeasure {
    private final int minIntersection;

    /**
     * Constructs a JaccardDistancesMeasure with the specified minimum intersection size for calculation.
     *
     * @param minIntersection The minimum intersection size required for Jaccard distance computation.
     */
    public JaccardDistancesMeasure(int minIntersection) {
        this.minIntersection = minIntersection;
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

}
