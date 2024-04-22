/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.chart.options;

import com.evolveum.wicket.chartjs.ChartDataset;

//TODO move to chartjs then remove
public class RoleAnalysisChartDataSet extends ChartDataset {

    private String stack;

    public RoleAnalysisChartDataSet() {
    }

    public String getStack() {
        return stack;
    }

    public void setStack(String stack) {
        this.stack = stack;
    }

}
