/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.objects.chunk;

import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.common.mining.utils.algorithm.JaccardSorter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MiningOperationChunk implements Serializable {

    List<MiningUserTypeChunk> miningUserTypeChunks;

    List<MiningRoleTypeChunk> miningRoleTypeChunks;

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

        if (roleAnalysisSortMode.equals(RoleAnalysisSortMode.JACCARD)) {
            this.miningUserTypeChunks = JaccardSorter.jaccardUserBasedSorter(miningUserTypeChunks);
        } else if (roleAnalysisSortMode.equals(RoleAnalysisSortMode.FREQUENCY)) {
            this.miningUserTypeChunks = JaccardSorter.frequencyUserBasedSort(miningUserTypeChunks);
        }
        return miningUserTypeChunks;
    }

    public List<MiningRoleTypeChunk> getMiningRoleTypeChunks(RoleAnalysisSortMode roleAnalysisSortMode) {

        if (roleAnalysisSortMode.equals(RoleAnalysisSortMode.JACCARD)) {
            this.miningRoleTypeChunks = JaccardSorter.jaccardRoleBasedSorter(miningRoleTypeChunks);
        } else if (roleAnalysisSortMode.equals(RoleAnalysisSortMode.FREQUENCY)) {
            this.miningRoleTypeChunks = JaccardSorter.frequencyRoleBasedSort(miningRoleTypeChunks);
        }
        return miningRoleTypeChunks;
    }

    private void resetList() {
        miningUserTypeChunks = new ArrayList<>();
        miningRoleTypeChunks = new ArrayList<>();
    }

}
