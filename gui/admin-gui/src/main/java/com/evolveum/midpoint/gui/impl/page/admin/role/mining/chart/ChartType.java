/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.chart.model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;

import org.jetbrains.annotations.NotNull;

public enum ChartType {

    LINE(GuiStyleConstants.CLASS_LINE_CHART_ICON),
    BAR(GuiStyleConstants.CLASS_BAR_CHART_ICON),
    SCATTER(GuiStyleConstants.CLASS_CIRCLE_FULL);

    private final String chartIcon;

    ChartType(String chartIcon) {
        this.chartIcon = chartIcon;
    }

    public String getChartIcon() {
        return chartIcon;
    }

    public static ChartType getNextChartType(@NotNull ChartType currentType) {
        return switch (currentType) {
            case LINE -> BAR;
            case BAR -> SCATTER;
            case SCATTER -> LINE;
        };
    }


}

