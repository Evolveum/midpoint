/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.model;

import java.util.List;

import org.apache.commons.math3.util.MathArrays;
import org.apache.wicket.model.LoadableDetachableModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.wicket.chartjs.*;

public class RoleAnalysisAggregateChartModel extends LoadableModel<ChartConfiguration> {

    LoadableDetachableModel<List<RoleAnalysisModel>> roleAnalysisModels;

    public RoleAnalysisAggregateChartModel(LoadableDetachableModel<List<RoleAnalysisModel>> roleAnalysisModel) {
        this.roleAnalysisModels = roleAnalysisModel;

    }

    @Override
    protected ChartConfiguration load() {
        return createChartConfiguration();
    }

    private ChartConfiguration createChartConfiguration() {
        BarChartConfiguration chart = new BarChartConfiguration();

        ChartData chartData = createDataset();
        chart.setData(chartData);
        chart.setOptions(createChartOptions());
        return chart;
    }

    private ChartData createDataset() {
        ChartData chartData = new ChartData();

        ChartDataset datasetUsers = new ChartDataset();
        datasetUsers.setLabel("Users");
        datasetUsers.addBackgroudColor("Red");

        ChartDataset datasetRoles = new ChartDataset();
        datasetRoles.setLabel("Roles");
        datasetRoles.addBackgroudColor("Green");

        List<RoleAnalysisModel> object = roleAnalysisModels.getObject();
        for (RoleAnalysisModel roleAnalysisModel : object) {
            int rolesCount = roleAnalysisModel.getRolesCount();
            datasetUsers.addData(roleAnalysisModel.getUsersCount());
            datasetRoles.addData(rolesCount);
            chartData.addLabel("Roles: " + rolesCount);
        }

        chartData.addDataset(datasetRoles);
        chartData.addDataset(datasetUsers);

        return chartData;
    }

    private ChartOptions createChartOptions() {
        ChartOptions options = new ChartOptions();
        options.setLegend(createLegendOptions());
        options.setIndexAxis(IndexAxis.AXIS_X.getValue());
        return options;
    }

    private ChartLegendOption createLegendOptions() {
        ChartLegendOption legend = new ChartLegendOption();
        legend.setDisplay(false);
        ChartLegendLabel label = new ChartLegendLabel();
        label.setBoxWidth(15);
        legend.setLabels(label);
        return legend;
    }

}
