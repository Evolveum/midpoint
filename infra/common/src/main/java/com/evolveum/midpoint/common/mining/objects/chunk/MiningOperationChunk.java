/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.objects.chunk;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.common.mining.utils.algorithm.JaccardSorter;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;

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
    RoleAnalysisSortMode sortModeUserChunk = RoleAnalysisSortMode.NONE;
    RoleAnalysisSortMode sortModeRoleChunk = RoleAnalysisSortMode.NONE;

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

    public List<MiningUserTypeChunk> getMiningUserTypeChunks(RoleAnalysisSortMode roleAnalysisSortMode) {
        this.sortModeUserChunk = roleAnalysisSortMode;
        if (roleAnalysisSortMode.equals(RoleAnalysisSortMode.JACCARD)) {
            this.miningUserTypeChunks = JaccardSorter.jaccardSorter(miningUserTypeChunks);
        } else if (roleAnalysisSortMode.equals(RoleAnalysisSortMode.FREQUENCY)) {
            this.miningUserTypeChunks = JaccardSorter.frequencyBasedSort(miningUserTypeChunks);
        }
        return miningUserTypeChunks;
    }

    public List<MiningRoleTypeChunk> getMiningRoleTypeChunks(RoleAnalysisSortMode roleAnalysisSortMode) {
        this.sortModeRoleChunk = roleAnalysisSortMode;
        if (roleAnalysisSortMode.equals(RoleAnalysisSortMode.JACCARD)) {
            this.miningRoleTypeChunks = JaccardSorter.jaccardSorter(miningRoleTypeChunks);
        } else if (roleAnalysisSortMode.equals(RoleAnalysisSortMode.FREQUENCY)) {
            this.miningRoleTypeChunks = JaccardSorter.frequencyBasedSort(miningRoleTypeChunks);
        }
        return miningRoleTypeChunks;
    }

    private void resetList() {
        miningUserTypeChunks = new ArrayList<>();
        miningRoleTypeChunks = new ArrayList<>();
    }

    public RoleAnalysisSortMode getSortModeUserChunk() {
        return sortModeUserChunk;
    }

    public RoleAnalysisSortMode getSortModeRoleChunk() {
        return sortModeRoleChunk;
    }

}
