/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.utils.algorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningBaseTypeChunk;
import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;

public class JaccardSorter {

    /**
     * The `JaccardSorter` class provides methods to sort `MiningUserTypeChunk` and `MiningRoleTypeChunk` collections
     * based on Jaccard similarity and frequency.
     */
    public static <T extends MiningBaseTypeChunk> @NotNull List<T> frequencyBasedSort(@NotNull List<T> dataPoints) {
        return dataPoints.stream()
                .sorted(Comparator.comparingDouble(MiningBaseTypeChunk::getFrequencyValue).reversed())
                .collect(Collectors.toList());
    }

    public static <T extends MiningBaseTypeChunk> List<T> jaccardSorter(@NotNull List<T> dataPoints) {

        RoleAnalysisProgressIncrement handler = new RoleAnalysisProgressIncrement("Jaccard Sort", 1);
        handler.enterNewStep("Sorting");
        handler.setActive(true);

        List<T> sorted = new ArrayList<>();
        List<T> remaining = new ArrayList<>(dataPoints);

        remaining.sort(Comparator.comparingInt(set -> -set.getProperties().size()));

        int size = dataPoints.size();
        handler.setOperationCountToProcess(size);
        while (!remaining.isEmpty()) {
            handler.iterateActualStatus();

            T current = remaining.remove(0);
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
                    T previous = sorted.get(i - 1);
                    T next = sorted.get(i);
                    List<String> currentRoles = current.getProperties();
                    double similarity = jacquardSimilarity(currentRoles,
                            previous.getProperties());
                    double nextSimilarity = jacquardSimilarity(currentRoles,
                            next.getProperties());

                    if (Math.max(similarity, nextSimilarity) > maxSimilarity
                            && Math.min(similarity, nextSimilarity) >= jacquardSimilarity(
                            previous.getProperties(), next.getProperties())) {
                        maxSimilarity = Math.max(similarity, nextSimilarity);
                        insertIndex = i;
                    }
                }

                if (insertIndex == -1) {
                    if (jacquardSimilarity(current.getProperties(),
                            sorted.get(0).getProperties())
                            > jacquardSimilarity(sorted.get(0).getProperties(),
                            sorted.get(1).getProperties())) {
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

    public static double jacquardSimilarity(List<String> set1, List<String> set2) {

        int intersectionCount = 0;
        int setBunique = 0;

        if (set1.size() > set2.size()) {
            for (String num : set2) {
                if (set1.contains(num)) {
                    intersectionCount++;
                } else {
                    setBunique++;
                }
            }

            return (double) intersectionCount / (set1.size() + setBunique);

        } else {

            for (String num : set1) {
                if (set2.contains(num)) {
                    intersectionCount++;
                } else {
                    setBunique++;
                }
            }

            return (double) intersectionCount / (set2.size() + setBunique);

        }
    }

}
