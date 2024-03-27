/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.objects.chunk;

import java.io.Serializable;

import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChunkMode;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

public class DisplayValueOption implements Serializable {
    ItemPath roleItemValuePath;
    ItemPath userItemValuePath;
    RoleAnalysisSortMode sortMode;
    RoleAnalysisProcessModeType processMode;
    RoleAnalysisChunkMode chunkMode;

    public DisplayValueOption() {
    }

    public DisplayValueOption(ItemPath roleItemValuePath, ItemPath userItemValuePath, RoleAnalysisSortMode sortMode) {
        this.roleItemValuePath = roleItemValuePath;
        this.userItemValuePath = userItemValuePath;
        this.sortMode = sortMode;
    }

    public ItemPath getRoleItemValuePath() {
        return roleItemValuePath;
    }

    public void setRoleItemValuePath(ItemPath roleItemValuePath) {
        this.roleItemValuePath = roleItemValuePath;
    }

    public ItemPath getUserItemValuePath() {
        return userItemValuePath;
    }

    public void setUserItemValuePath(ItemPath userItemValuePath) {
        this.userItemValuePath = userItemValuePath;
    }

    public RoleAnalysisSortMode getSortMode() {
        return sortMode;
    }

    public void setSortMode(RoleAnalysisSortMode sortMode) {
        this.sortMode = sortMode;
    }

    public RoleAnalysisProcessModeType getProcessMode() {
        return processMode;
    }

    public void setProcessMode(RoleAnalysisProcessModeType processMode) {
        this.processMode = processMode;
    }

    public RoleAnalysisChunkMode getChunkMode() {
        return chunkMode;
    }

    public void setChunkMode(RoleAnalysisChunkMode chunkMode) {
        this.chunkMode = chunkMode;
    }

}
