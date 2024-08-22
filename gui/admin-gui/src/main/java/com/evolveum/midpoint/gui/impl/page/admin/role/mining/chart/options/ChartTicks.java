package com.evolveum.midpoint.gui.impl.page.admin.role.mining.chart.options;

public class ChartTicks {

    boolean display = true;

    public ChartTicks(boolean display) {
        this.display = display;
    }

    public boolean isDisplay() {
        return display;
    }

    public void setDisplay(boolean display) {
        this.display = display;
    }
}
