/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism;

import java.util.Set;


public class JaccardDistancesMeasure implements DistanceMeasure {
    private final int minIntersection;

    public JaccardDistancesMeasure(int minIntersection) {
        this.minIntersection = minIntersection;
    }

    @Override
    public double compute(Set<String> valueA, Set<String> valueB) {
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
