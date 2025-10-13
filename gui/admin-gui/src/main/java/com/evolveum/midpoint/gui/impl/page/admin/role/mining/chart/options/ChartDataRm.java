/*
 * Copyright (c) 2021 Evolveum
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.chart.options;

import java.io.Serializable;

import com.evolveum.wicket.chartjs.ChartDataset;

public class ChartDataRm extends ChartDataset implements Serializable {

    public String getBorderRadius() {
        return borderRadius;
    }

    public void setBorderRadius(String borderRadius) {
        this.borderRadius = borderRadius;
    }

    String borderRadius;
}
