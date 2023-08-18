/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.SORT;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.SimilarityUtils.jacquardSimilarity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.sorter.JaccardSorter;

public class MiningOperationChunk implements Serializable {

    String state = "START";
    List<MiningUserTypeChunk> miningUserTypeChunks;

    List<MiningRoleTypeChunk> miningRoleTypeChunks;

    public MiningOperationChunk(List<MiningUserTypeChunk> miningUserTypeChunks, List<MiningRoleTypeChunk> miningRoleTypeChunks) {
        resetState();
        resetList();
        this.miningUserTypeChunks = miningUserTypeChunks;
        this.miningRoleTypeChunks = miningRoleTypeChunks;
    }

    public List<MiningUserTypeChunk> getMiningUserTypeChunks(SORT sort) {
        resetState();

        if (sort.equals(SORT.JACCARD)) {
            return sortUserTypeChunks(miningUserTypeChunks);
        } else if (sort.equals(SORT.FREQUENCY)) {
            return JaccardSorter.sortByFrequencyUserType(miningUserTypeChunks);
        } else {
            return miningUserTypeChunks;
        }
    }

    public List<MiningRoleTypeChunk> getMiningRoleTypeChunks(SORT sort) {
        resetState();

        if (sort.equals(SORT.JACCARD)) {
            return sortRoleTypeChunks(miningRoleTypeChunks);
        } else if (sort.equals(SORT.FREQUENCY)) {
            return JaccardSorter.sortByFrequencyRoleType(miningRoleTypeChunks);
        } else {
            return miningRoleTypeChunks;
        }

    }

    private List<MiningUserTypeChunk> sortUserTypeChunks(List<MiningUserTypeChunk> dataPoints) {

        List<MiningUserTypeChunk> sorted = new ArrayList<>();
        List<MiningUserTypeChunk> remaining = new ArrayList<>(dataPoints);

        remaining.sort(Comparator.comparingInt(set -> -set.getRoles().size()));

        int counter = 0;
        int size = dataPoints.size();
        while (!remaining.isEmpty()) {
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

            state = counter + "/" + size + " (4/4 sorting)";
            System.out.println(state);
            counter++;
        }
        return sorted;
    }

    private List<MiningRoleTypeChunk> sortRoleTypeChunks(List<MiningRoleTypeChunk> dataPoints) {

        List<MiningRoleTypeChunk> sorted = new ArrayList<>();
        List<MiningRoleTypeChunk> remaining = new ArrayList<>(dataPoints);

        remaining.sort(Comparator.comparingInt(set -> -set.getUsers().size()));

        int counter = 0;
        int size = dataPoints.size();
        while (!remaining.isEmpty()) {
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

            state = counter + "/" + size + " (4/4 sorting)";
            System.out.println(state);
            counter++;
        }

        return sorted;
    }

    private void resetList() {
        miningUserTypeChunks = new ArrayList<>();
        miningRoleTypeChunks = new ArrayList<>();
    }

    private void resetState() {
        state = "START";
    }

    public String getState() {
        return state;
    }

}
