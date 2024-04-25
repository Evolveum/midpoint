/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.chart.options;

import com.evolveum.wicket.chartjs.ChartOptions;

//TODO move to chartjs then remove
public class RoleAnalysisChartOptions extends ChartOptions {
    double barPercentage = 1;
    private ChartScaleOption scales;
    private ChartInteractionOption interaction;

    public RoleAnalysisChartOptions() {
    }

    public double getBarPercentage() {
        return barPercentage;
    }

    public void setBarPercentage(double barPercentage) {
        this.barPercentage = barPercentage;
    }

    public ChartScaleOption getScales() {
        return scales;
    }

    public void setScales(ChartScaleOption scales) {
        this.scales = scales;
    }

    public void setInteraction(ChartInteractionOption interaction) {
        this.interaction = interaction;
    }

    public ChartInteractionOption getInteraction() {
        return interaction;
    }
}
