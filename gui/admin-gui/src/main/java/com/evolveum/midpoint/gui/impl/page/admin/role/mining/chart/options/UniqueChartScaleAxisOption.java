package com.evolveum.midpoint.gui.impl.page.admin.role.mining.chart.options;

import com.evolveum.wicket.chartjs.ChartScaleAxisOption;

public class UniqueChartScaleAxisOption extends ChartScaleAxisOption {

    ChartTicks ticks;

    public ChartTicks getTicks() {
        return ticks;
    }

    public void setTicks(ChartTicks ticks) {
        this.ticks = ticks;
    }
}
