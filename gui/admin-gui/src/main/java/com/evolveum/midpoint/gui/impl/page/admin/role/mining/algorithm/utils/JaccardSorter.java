/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils;

import java.util.*;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster.DataPoint;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningRoleTypeChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningUserTypeChunk;

public class JaccardSorter {

    public static List<DataPoint> sort(List<DataPoint> dataPoints) {

        List<DataPoint> sorted = new ArrayList<>();
        List<DataPoint> remaining = new ArrayList<>(dataPoints);

        remaining.sort(Comparator.comparingInt(set -> -set.getPoints().size()));

        while (!remaining.isEmpty()) {
            DataPoint current = remaining.remove(0);
            double maxSimilarity = 0;
            int insertIndex = -1;

            if (sorted.size() < 2) {
                if (sorted.isEmpty()) {
                    sorted.add(current);
                } else {
                    sorted.add(0, current);
                }
            } else {
                for (int i = 1; i < sorted.size(); i++) {
                    DataPoint previous = sorted.get(i - 1);
                    DataPoint next = sorted.get(i);
                    double similarity = jacquardSimilarity(current.getPoints(),
                            previous.getPoints());
                    double nextSimilarity = jacquardSimilarity(current.getPoints(),
                            next.getPoints());

                    if (Math.max(similarity, nextSimilarity) > maxSimilarity
                            && Math.min(similarity, nextSimilarity) >= jacquardSimilarity(
                            previous.getPoints(), next.getPoints())) {
                        maxSimilarity = Math.max(similarity, nextSimilarity);
                        insertIndex = i;
                    }
                }

                if (insertIndex == -1) {
                    if (jacquardSimilarity(current.getPoints(),
                            sorted.get(0).getPoints())
                            > jacquardSimilarity(sorted.get(0).getPoints(),
                            sorted.get(1).getPoints())) {
                        sorted.add(0, current);
                    } else {
                        sorted.add(current);
                    }
                } else {
                    sorted.add(insertIndex, current);
                }
            }
        }

        return sorted;
    }

    private static double jacquardSimilarity(List<String> set1, List<String> set2) {
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        return (double) intersection.size() / union.size();
    }


    public static List<MiningUserTypeChunk> sortByFrequencyUserType(List<MiningUserTypeChunk> dataPoints) {
        List<MiningUserTypeChunk> sorted = new ArrayList<>(dataPoints);
        sorted.sort(Comparator.comparingDouble(MiningUserTypeChunk::getFrequency).reversed());
        return sorted;
    }

    public static List<MiningRoleTypeChunk> sortByFrequencyRoleType(List<MiningRoleTypeChunk> dataPoints, String state) {
        List<MiningRoleTypeChunk> sorted = new ArrayList<>(dataPoints);
        sorted.sort(Comparator.comparingDouble(MiningRoleTypeChunk::getFrequency).reversed());
        return sorted;
    }
}
