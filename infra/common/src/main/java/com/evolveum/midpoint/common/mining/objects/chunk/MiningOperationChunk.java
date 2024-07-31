/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.objects.chunk;

import java.io.Serializable;
import java.util.*;
import java.util.stream.IntStream;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.utils.values.FrequencyItem;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisObjectStatus;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.utils.algorithm.JaccardSorter;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;

import org.springframework.scheduling.config.Task;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidAssignment;
import static com.evolveum.midpoint.common.mining.utils.algorithm.JaccardSorter.jacquardSimilarity;

/**
 * <p>
 * The `MiningOperationChunk` class represents a chunk of data used in the role analysis process. It contains two lists:
 * - `miningUserTypeChunks` for user data
 * - `miningRoleTypeChunks` for role data
 * </p>
 * <p>
 * This class provides methods to retrieve these lists and sort them based on the specified `RoleAnalysisSortMode`.
 * Sorting is performed by chunk, so the lists are sorted independently of each other.
 * </p>
 */
public class MiningOperationChunk implements Serializable {

    private List<MiningUserTypeChunk> miningUserTypeChunks;
    private List<MiningRoleTypeChunk> miningRoleTypeChunks;

    private RoleAnalysisSortMode sortMode;
    private RoleAnalysisProcessModeType processMode;
    private double minFrequency;
    private double maxFrequency;



//    RoleAnalysisSortMode sortModeUserChunk = RoleAnalysisSortMode.NONE;
//    RoleAnalysisSortMode sortModeRoleChunk = RoleAnalysisSortMode.NONE;

    public MiningOperationChunk(List<MiningUserTypeChunk> miningUserTypeChunks, List<MiningRoleTypeChunk> miningRoleTypeChunks) {
        resetList();
        this.miningUserTypeChunks = miningUserTypeChunks;
        this.miningRoleTypeChunks = miningRoleTypeChunks;
    }

    public List<MiningUserTypeChunk> getSimpleMiningUserTypeChunks() {
        return miningUserTypeChunks;
    }

    public List<MiningRoleTypeChunk> getSimpleMiningRoleTypeChunks() {
        return miningRoleTypeChunks;
    }

    public List<MiningUserTypeChunk> getMiningUserTypeChunks(@NotNull RoleAnalysisSortMode roleAnalysisSortMode) {
//        this.sortModeUserChunk = roleAnalysisSortMode;
        if (roleAnalysisSortMode.equals(RoleAnalysisSortMode.JACCARD)) {
            this.miningUserTypeChunks = JaccardSorter.jaccardSorter(miningUserTypeChunks);
        } else if (roleAnalysisSortMode.equals(RoleAnalysisSortMode.FREQUENCY)) {
            this.miningUserTypeChunks = JaccardSorter.frequencyBasedSort(miningUserTypeChunks);
        } else if (roleAnalysisSortMode.equals(RoleAnalysisSortMode.INCLUDES)) {
            this.miningUserTypeChunks = sortByIncludeStatusMiningUserTypeChunks();
        }
        return miningUserTypeChunks;
    }

    public List<MiningRoleTypeChunk> getMiningRoleTypeChunks(@NotNull RoleAnalysisSortMode roleAnalysisSortMode) {
//        this.sortModeRoleChunk = roleAnalysisSortMode;
        if (roleAnalysisSortMode.equals(RoleAnalysisSortMode.JACCARD)) {
            this.miningRoleTypeChunks = JaccardSorter.jaccardSorter(miningRoleTypeChunks);
        } else if (roleAnalysisSortMode.equals(RoleAnalysisSortMode.FREQUENCY)) {
            this.miningRoleTypeChunks = JaccardSorter.frequencyBasedSort(miningRoleTypeChunks);
        }else if (roleAnalysisSortMode.equals(RoleAnalysisSortMode.INCLUDES)) {
            this.miningRoleTypeChunks = sortByStatusIncludeMiningRoleTypeChunks();
        }
        return miningRoleTypeChunks;
    }

    public List<MiningUserTypeChunk> getMiningUserTypeChunks() {
//        this.sortModeUserChunk = roleAnalysisSortMode;
//        if (sortMode.equals(RoleAnalysisSortMode.JACCARD)) {
//            this.miningUserTypeChunks = JaccardSorter.jaccardSorter(miningUserTypeChunks);
//        } else if (sortMode.equals(RoleAnalysisSortMode.FREQUENCY)) {
//            this.miningUserTypeChunks = JaccardSorter.frequencyBasedSort(miningUserTypeChunks);
//        } else if (sortMode.equals(RoleAnalysisSortMode.INCLUDES)) {
//            this.miningUserTypeChunks = sortByIncludeStatusMiningUserTypeChunks();
//        }
        return miningUserTypeChunks;
    }

    public List<MiningRoleTypeChunk> getMiningRoleTypeChunks() {
//        if (sortMode.equals(RoleAnalysisSortMode.JACCARD)) {
//            this.miningRoleTypeChunks = JaccardSorter.jaccardSorter(miningRoleTypeChunks);
//        } else if (sortMode.equals(RoleAnalysisSortMode.FREQUENCY)) {
//            this.miningRoleTypeChunks = JaccardSorter.frequencyBasedSort(miningRoleTypeChunks);
//        }else if (sortMode.equals(RoleAnalysisSortMode.INCLUDES)) {
//            this.miningRoleTypeChunks = sortByStatusIncludeMiningRoleTypeChunks();
//        }
        return miningRoleTypeChunks;
    }

    //TODO check it. it should be executed only when pattern or candidate role is selected.
    public List<MiningRoleTypeChunk> sortByStatusIncludeMiningRoleTypeChunks() {
        RoleAnalysisOperationMode thisStatusFirst = RoleAnalysisOperationMode.INCLUDE;
        this.miningRoleTypeChunks.sort((chunk1, chunk2) -> {
            if (chunk1.getStatus() == thisStatusFirst && chunk2.getStatus() != thisStatusFirst) {
                return -1;
            } else if (chunk1.getStatus() != thisStatusFirst && chunk2.getStatus() == thisStatusFirst) {
                return 1;
            } else {
                return chunk1.getStatus().compareTo(chunk2.getStatus());
            }
        });

        Comparator<MiningRoleTypeChunk> jaccardComparator = (chunk1, chunk2) -> {
            List<String> propertiesA = chunk1.getProperties();
            List<String> propertiesB = chunk2.getProperties();
            double similarity1 = jacquardSimilarity(propertiesA, propertiesB);
            double similarity2 = jacquardSimilarity(propertiesA, propertiesB);
            return Double.compare(similarity2, similarity1);
        };


        miningRoleTypeChunks.subList(0, Collections.frequency(miningRoleTypeChunks, thisStatusFirst))
                .sort(jaccardComparator);

        return miningRoleTypeChunks;
    }

    //TODO check it. it should be executed only when pattern or candidate role is selected.
    public List<MiningUserTypeChunk> sortByIncludeStatusMiningUserTypeChunks() {
        RoleAnalysisOperationMode thisStatusFirst = RoleAnalysisOperationMode.INCLUDE;
        this.miningUserTypeChunks.sort((chunk1, chunk2) -> {
            if (chunk1.getStatus() == thisStatusFirst && chunk2.getStatus() != thisStatusFirst) {
                return -1;
            } else if (chunk1.getStatus() != thisStatusFirst && chunk2.getStatus() == thisStatusFirst) {
                return 1;
            } else {
                return chunk1.getStatus().compareTo(chunk2.getStatus());
            }
        });


        Comparator<MiningUserTypeChunk> jaccardComparator = (chunk1, chunk2) -> {
            List<String> propertiesA = chunk1.getProperties();
            List<String> propertiesB = chunk2.getProperties();
            double similarity1 = jacquardSimilarity(propertiesA, propertiesB);
            double similarity2 = jacquardSimilarity(propertiesA, propertiesB);
            return Double.compare(similarity2, similarity1);
        };

        miningUserTypeChunks.subList(0, Collections.frequency(miningUserTypeChunks, thisStatusFirst))
                .sort(jaccardComparator);

        return miningUserTypeChunks;
    }

    private void resetList() {
        miningUserTypeChunks = new ArrayList<>();
        miningRoleTypeChunks = new ArrayList<>();
    }

    public void setSortMode(RoleAnalysisSortMode sortMode) {
        this.sortMode = sortMode;
    }

    public void setProcessMode(RoleAnalysisProcessModeType processMode) {
        this.processMode = processMode;
    }

//    public RoleAnalysisSortMode getSortModeUserChunk() {
//        return sortModeUserChunk;
//    }
//
//    public RoleAnalysisSortMode getSortModeRoleChunk() {
//        return sortModeRoleChunk;
//    }

    public <T extends MiningBaseTypeChunk> List<T> getMainMiningChunk() {
        if (RoleAnalysisProcessModeType.ROLE == processMode) {
            return (List<T>) miningRoleTypeChunks;
        }
        return (List<T>) miningUserTypeChunks;
    }

    public  <T extends MiningBaseTypeChunk>  List<T> getAdditionalMiningChunk() {
        if (RoleAnalysisProcessModeType.ROLE == processMode) {
            return (List<T>) miningUserTypeChunks;
        }
        return (List<T>) miningRoleTypeChunks;
    }

    public void setMinFrequency(double minFrequency) {
        this.minFrequency = minFrequency;
    }

    public void setMaxFrequency(double maxFrequency) {
        this.maxFrequency = maxFrequency;
    }

    public double getMinFrequency() {
        return minFrequency;
    }

    public double getMaxFrequency() {
        return maxFrequency;
    }

    public RoleAnalysisSortMode getSortMode() {
        if (sortMode == null) {
            sortMode = RoleAnalysisSortMode.NONE;
        }
        return sortMode;
    }

    public RoleAnalysisProcessModeType getProcessMode() {
        return processMode;
    }


//    @Override
    public void updateChunkWithPatterns(MiningOperationChunk basicChunk, List<DetectedPattern> detectedPatterns, Task task, OperationResult result) {
        List<MiningRoleTypeChunk> miningRoleTypeChunks = basicChunk.getMiningRoleTypeChunks();//basicChunk.getMiningRoleTypeChunks(option.getSortMode());
        List<MiningUserTypeChunk> miningUserTypeChunks = basicChunk.getMiningUserTypeChunks();

        List<List<String>> detectedPatternsRoles = new ArrayList<>();
        List<List<String>> detectedPatternsUsers = new ArrayList<>();
        List<String> candidateRolesIds = new ArrayList<>();

        for (DetectedPattern detectedPattern : detectedPatterns) {
            detectedPatternsRoles.add(new ArrayList<>(detectedPattern.getRoles()));
            detectedPatternsUsers.add(new ArrayList<>(detectedPattern.getUsers()));
            candidateRolesIds.add(detectedPattern.getIdentifier());
        }

        for (MiningRoleTypeChunk role : miningRoleTypeChunks) {
            FrequencyItem frequencyItem = role.getFrequencyItem();
            double frequency = frequencyItem.getFrequency();

            for (int i = 0; i < detectedPatternsRoles.size(); i++) {
                List<String> detectedPatternsRole = detectedPatternsRoles.get(i);
                List<String> chunkRoles = role.getRoles();
                if (new HashSet<>(detectedPatternsRole).containsAll(chunkRoles)) {
                    RoleAnalysisObjectStatus objectStatus = role.getObjectStatus();
                    objectStatus.setRoleAnalysisOperationMode(RoleAnalysisOperationMode.INCLUDE);
                    objectStatus.addContainerId(candidateRolesIds.get(i));
                    detectedPatternsRole.removeAll(chunkRoles);
                } else if (basicChunk.getMinFrequency() > frequency && frequency < basicChunk.getMaxFrequency() && !role.getStatus().isInclude()) {
                    role.setStatus(RoleAnalysisOperationMode.DISABLE);
                } else if (!role.getStatus().isInclude()) {
                    role.setStatus(RoleAnalysisOperationMode.EXCLUDE);
                }
            }
        }

        for (MiningUserTypeChunk user : miningUserTypeChunks) {
            for (int i = 0; i < detectedPatternsUsers.size(); i++) {
                List<String> detectedPatternsUser = detectedPatternsUsers.get(i);
                List<String> chunkUsers = user.getUsers();
                if (new HashSet<>(detectedPatternsUser).containsAll(chunkUsers)) {
                    RoleAnalysisObjectStatus objectStatus = user.getObjectStatus();
                    objectStatus.setRoleAnalysisOperationMode(RoleAnalysisOperationMode.INCLUDE);
                    objectStatus.addContainerId(candidateRolesIds.get(i));
                    detectedPatternsUser.removeAll(chunkUsers);
                } else if (!user.getStatus().isInclude()) {
                    user.setStatus(RoleAnalysisOperationMode.EXCLUDE);
                }
            }
        }

        int size = detectedPatternsUsers.size();

//        IntStream.range(0, size).forEach(i -> {
//            List<String> detectedPatternRoles = detectedPatternsRoles.get(i);
//            List<String> detectedPatternUsers = detectedPatternsUsers.get(i);
//            String candidateRoleId = candidateRolesIds.get(i);
//            addAdditionalObject(candidateRoleId, detectedPatternUsers, detectedPatternRoles, miningUserTypeChunks,
//                    miningRoleTypeChunks,
//                    task,
//                    result);
//        });
    }

}
