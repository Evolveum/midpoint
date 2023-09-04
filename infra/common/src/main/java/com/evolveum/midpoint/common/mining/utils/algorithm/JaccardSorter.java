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

import com.evolveum.midpoint.common.mining.objects.handler.Handler;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;

public class JaccardSorter {

    public static @NotNull List<MiningUserTypeChunk> frequencyUserBasedSort(@NotNull List<MiningUserTypeChunk> dataPoints) {
        List<MiningUserTypeChunk> sorted = new ArrayList<>(dataPoints);
        sorted.sort(Comparator.comparingDouble(MiningUserTypeChunk::getFrequency).reversed());
        return sorted;
    }

    public static @NotNull List<MiningRoleTypeChunk> frequencyRoleBasedSort(@NotNull List<MiningRoleTypeChunk> dataPoints) {
        List<MiningRoleTypeChunk> sorted = new ArrayList<>(dataPoints);
        sorted.sort(Comparator.comparingDouble(MiningRoleTypeChunk::getFrequency).reversed());
        return sorted;
    }

    public static List<MiningUserTypeChunk> jaccardUserBasedSorter(List<MiningUserTypeChunk> dataPoints) {

        Handler handler = new Handler("Jaccard Sort", 1);
        handler.enterNewStep("Sorting");
        handler.setActive(true);

        List<MiningUserTypeChunk> sorted = new ArrayList<>();
        List<MiningUserTypeChunk> remaining = new ArrayList<>(dataPoints);

        remaining.sort(Comparator.comparingInt(set -> -set.getRoles().size()));

        int size = dataPoints.size();
        handler.setOperationCountToProcess(size);
        while (!remaining.isEmpty()) {
            handler.iterateActualStatus();

            MiningUserTypeChunk current = remaining.remove(0);
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
                    MiningUserTypeChunk previous = sorted.get(i - 1);
                    MiningUserTypeChunk next = sorted.get(i);
                    List<String> currentRoles = current.getRoles();
                    double similarity = jacquardSimilarity(currentRoles,
                            previous.getRoles());
                    double nextSimilarity = jacquardSimilarity(currentRoles,
                            next.getRoles());

                    if (Math.max(similarity, nextSimilarity) > maxSimilarity
                            && Math.min(similarity, nextSimilarity) >= jacquardSimilarity(
                            previous.getRoles(), next.getRoles())) {
                        maxSimilarity = Math.max(similarity, nextSimilarity);
                        insertIndex = i;
                    }
                }

                if (insertIndex == -1) {
                    if (jacquardSimilarity(current.getRoles(),
                            sorted.get(0).getRoles())
                            > jacquardSimilarity(sorted.get(0).getRoles(),
                            sorted.get(1).getRoles())) {
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

    public static List<MiningRoleTypeChunk> jaccardRoleBasedSorter(List<MiningRoleTypeChunk> dataPoints) {

        Handler handler = new Handler("Jaccard Sort", 1);
        handler.enterNewStep("Sorting");
        handler.setActive(true);

        List<MiningRoleTypeChunk> sorted = new ArrayList<>();
        List<MiningRoleTypeChunk> remaining = new ArrayList<>(dataPoints);

        remaining.sort(Comparator.comparingInt(set -> -set.getUsers().size()));

        int size = dataPoints.size();
        handler.setOperationCountToProcess(size);
        while (!remaining.isEmpty()) {
            handler.iterateActualStatus();

            MiningRoleTypeChunk current = remaining.remove(0);
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
                    MiningRoleTypeChunk previous = sorted.get(i - 1);
                    MiningRoleTypeChunk next = sorted.get(i);
                    double similarity = jacquardSimilarity(current.getUsers(),
                            previous.getUsers());
                    double nextSimilarity = jacquardSimilarity(current.getUsers(),
                            next.getUsers());

                    if (Math.max(similarity, nextSimilarity) > maxSimilarity
                            && Math.min(similarity, nextSimilarity) >= jacquardSimilarity(
                            previous.getUsers(), next.getUsers())) {
                        maxSimilarity = Math.max(similarity, nextSimilarity);
                        insertIndex = i;
                    }
                }

                if (insertIndex == -1) {
                    if (jacquardSimilarity(current.getUsers(),
                            sorted.get(0).getUsers())
                            > jacquardSimilarity(sorted.get(0).getUsers(),
                            sorted.get(1).getUsers())) {
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
