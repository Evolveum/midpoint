/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile.mining.outlier;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierPartitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierType;

public class RoleAnalysisOutlierTileModel<T extends Serializable> extends Tile<T> {

    String icon;
    String name;
    String processMode;
    RoleAnalysisOutlierType outlier;
    RoleAnalysisOutlierPartitionType partition;

    public RoleAnalysisOutlierTileModel(String icon, String title) {
        super(icon, title);
    }

    public RoleAnalysisOutlierTileModel(
            @Nullable RoleAnalysisOutlierPartitionType partition,
            @NotNull RoleAnalysisOutlierType outlier,
            @NotNull String processMode) {
        this.partition = partition;
        this.icon = GuiStyleConstants.CLASS_ICON_OUTLIER;
        this.processMode = processMode;
        this.outlier = outlier;
        this.name = outlier.getName().getOrig();
    }

    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProcessMode() {
        return processMode;
    }

    public void setProcessMode(String processMode) {
        this.processMode = processMode;
    }

    public RoleAnalysisOutlierType getOutlier() {
        return outlier;
    }

    public void setOutlier(RoleAnalysisOutlierType outlier) {
        this.outlier = outlier;
    }

    public RoleAnalysisOutlierPartitionType getPartition() {
        return partition;
    }
}
