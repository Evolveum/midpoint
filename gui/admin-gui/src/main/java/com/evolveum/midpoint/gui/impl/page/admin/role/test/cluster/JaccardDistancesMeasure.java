/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.test.cluster;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.ml.distance.DistanceMeasure;

import java.util.HashSet;
import java.util.Set;

public class JaccardDistancesMeasure implements DistanceMeasure {
    int minIntersection;

    public JaccardDistancesMeasure(int minIntersection) {
        this.minIntersection = minIntersection;
    }

    //Elapsed time: 21.599 seconds
    @Override
    public double compute(double[] a, double[] b) throws DimensionMismatchException {
        int intersectionCount = 0;
        Set<Double> setA = new HashSet<>();
        int setBunique = 0;

        for (double num : a) {
            setA.add(num);
        }
        for (double num : b) {
            if (setA.contains(num)) {
                intersectionCount++;
            } else {
                setBunique++;
            }
        }

        if (intersectionCount < minIntersection) {
            return 1;
        }

        return 1 - (double) intersectionCount / (setA.size() + setBunique);
    }

    //Elapsed time: 109.211 seconds.
//    @Override
//    public double compute(double[] setA, double[] setB) throws DimensionMismatchException {
//        Set<Double> elementsA = new HashSet<>();
//        Set<Double> elementsB = new HashSet<>();
//
//        for (double v : setA) {
//            elementsA.add(v);
//        }
//
//        for (double v : setB) {
//            elementsB.add(v);
//        }
//
//        Set<Double> union = new HashSet<>(elementsA);
//        union.addAll(elementsB);
//
//        Set<Double> intersection = new HashSet<>(elementsA);
//        intersection.retainAll(elementsB);
//
//        int size = intersection.size();
//        if (size < minIntersection) {
//            return 1;
//        }
//
//        return 1 - ((double) size / union.size());
//    }
}
