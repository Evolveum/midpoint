/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.SORT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.sorter.JaccardSorter;

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

    public List<MiningUserTypeChunk> getMiningUserTypeChunks(SORT sort) {

        if (sort.equals(SORT.JACCARD)) {
            this.miningUserTypeChunks = JaccardSorter.jaccardUserBasedSorter(miningUserTypeChunks);
        } else if (sort.equals(SORT.FREQUENCY)) {
            this.miningUserTypeChunks = JaccardSorter.frequencyUserBasedSort(miningUserTypeChunks);
        }
        return miningUserTypeChunks;
    }

    public List<MiningRoleTypeChunk> getMiningRoleTypeChunks(SORT sort) {

        if (sort.equals(SORT.JACCARD)) {
            this.miningRoleTypeChunks = JaccardSorter.jaccardRoleBasedSorter(miningRoleTypeChunks);
        } else if (sort.equals(SORT.FREQUENCY)) {
            this.miningRoleTypeChunks = JaccardSorter.frequencyRoleBasedSort(miningRoleTypeChunks);
        }
        return miningRoleTypeChunks;
    }


    private void resetList() {
        miningUserTypeChunks = new ArrayList<>();
        miningRoleTypeChunks = new ArrayList<>();
    }

}
