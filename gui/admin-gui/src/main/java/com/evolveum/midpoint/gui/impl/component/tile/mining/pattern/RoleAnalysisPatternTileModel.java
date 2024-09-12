/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile.mining.pattern;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;

public class RoleAnalysisPatternTileModel<T extends Serializable> extends Tile<T> {

    String icon;
    String name;
    DetectedPattern pattern;
    String userCount;
    String roleCount;
    String processMode;
    double systemReductionPercentage;

    public RoleAnalysisPatternTileModel(String icon, String title) {
        super(icon, title);
    }

    public RoleAnalysisPatternTileModel(
            @NotNull DetectedPattern pattern,
            @NotNull String name,
            int totalRoleToUserAssignments) {
        this.icon = GuiStyleConstants.CLASS_DETECTED_PATTERN_ICON;
        this.name = name;
        this.pattern = pattern;
        this.userCount = String.valueOf(pattern.getUsers().size());
        this.roleCount = String.valueOf(pattern.getRoles().size());
        this.systemReductionPercentage = calculateSystemReductionPercentage(totalRoleToUserAssignments);
    }

    private double calculateSystemReductionPercentage(int totalRoleToUserAssignments) {
        Double metric = pattern.getMetric();
        if (metric == null) {
            return 0;
        }

        if (metric == 0 || totalRoleToUserAssignments == 0) {
            return 0;
        }

        double percentageCoverage = (metric / totalRoleToUserAssignments) * 100;
        BigDecimal bd = BigDecimal.valueOf(percentageCoverage);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public double getSystemReductionPercentage() {
        return systemReductionPercentage;
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

    public DetectedPattern getPattern() {
        return pattern;
    }

    public void setPattern(DetectedPattern pattern) {
        this.pattern = pattern;
    }

    public String getUserCount() {
        return userCount;
    }

    public String getRoleCount() {
        return roleCount;
    }

    public String getProcessMode() {
        return processMode;
    }

    public void setProcessMode(String processMode) {
        this.processMode = processMode;
    }

}
