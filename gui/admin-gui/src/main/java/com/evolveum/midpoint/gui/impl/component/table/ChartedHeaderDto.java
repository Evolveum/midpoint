/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.table;

import com.evolveum.wicket.chartjs.ChartConfiguration;

import java.io.Serializable;

public class ChartedHeaderDto<T extends ChartConfiguration> implements Serializable {

    public static final String F_CHART_CONFIGURATION = "chartConfiguration";
    public static final String F_CHART_TITLE = "chartTitle";
    public static final String F_CHART_VALUE = "chartValue";

    private T chartConfiguration;
    private String chartTitle;
    private String chartValue;

    public ChartedHeaderDto(T chartModel, String chartTitle, String chartValue) {
        this.chartConfiguration = chartModel;
        this.chartTitle = chartTitle;
        this.chartValue = chartValue;
    }

    public T getChartConfiguration() {
        return chartConfiguration;
    }

}
