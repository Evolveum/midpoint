package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.widgets.component;

import com.evolveum.wicket.chartjs.ChartOptions;

public class DonuthChartOptions extends ChartOptions {

    String cutout = "80%";

    public DonuthChartOptions() {
        super();
    }

    public String getCutout() {
        return cutout;
    }

    public void setCutout(String cutout) {
        this.cutout = cutout;
    }
}
