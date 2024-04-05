package com.evolveum.midpoint.gui.impl.page.admin.role.mining.model;

import com.evolveum.wicket.chartjs.ChartOptions;

//TODO remove after chartjs upgrade to 0.4
public class RoleAnalysisChartOptions extends ChartOptions {
    double barPercentage = 1;
    private String scales;

    public RoleAnalysisChartOptions() {
    }

    public double getBarPercentage() {
        return barPercentage;
    }

    public void setBarPercentage(double barPercentage) {
        this.barPercentage = barPercentage;
    }

    public String getScales() {
        return scales;
    }

    public void setScales(String scales) {
        this.scales = scales;
    }
}
