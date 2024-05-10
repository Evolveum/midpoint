package com.evolveum.midpoint.gui.impl.page.admin.role.mining.chart;/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */


import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.chart.model.ChartType;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.RoleAnalysisModel;

import org.apache.wicket.model.LoadableDetachableModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.wicket.chartjs.*;

import org.jetbrains.annotations.NotNull;

/**
 * The RoleAnalysisAggregateChartModel class is a LoadableModel that generates aggregate
 * charts for role analysis based on provided data.
 */
public class RoleAnalysisAggregateChartModel extends LoadableModel<ChartConfiguration> {

    LoadableDetachableModel<List<RoleAnalysisModel>> roleAnalysisModels;
   ChartType chartType;

    public RoleAnalysisAggregateChartModel(
            @NotNull LoadableDetachableModel<List<RoleAnalysisModel>> roleAnalysisModel,
            @NotNull ChartType chartType) {
        this.roleAnalysisModels = roleAnalysisModel;
        this.chartType = chartType;
    }

    @Override
    protected ChartConfiguration load() {
        return createChartConfiguration();
    }

    private @NotNull ChartConfiguration createChartConfiguration() {

        if (chartType.equals(ChartType.LINE)) {
            LineChartConfiguration chart = new LineChartConfiguration();
            ChartData chartData = createDataset();
            chart.setData(chartData);
            chart.setOptions(createChartOptions());
            return chart;
        } else if (chartType.equals(ChartType.BAR)) {
            BarChartConfiguration chart = new BarChartConfiguration();
            ChartData chartData = createDataset();
            chart.setData(chartData);
            chart.setOptions(createChartOptions());
            return chart;
        } else {
            ScatterChartConfiguration chart = new ScatterChartConfiguration();
            ChartData chartData = createScatterDataset();
            chart.setData(chartData);
            chart.setOptions(createChartOptions());
            return chart;
        }

    }

    private @NotNull ChartData createScatterDataset() {
        ChartData chartData = new ChartData();

        ChartDataset datasetUsers = new ChartDataset();
        datasetUsers.setLabel("Users / Roles occupation");
        datasetUsers.addBackgroudColor("Red");
        datasetUsers.setBorderWidth(1);

        List<RoleAnalysisModel> object = roleAnalysisModels.getObject();
        for (RoleAnalysisModel roleAnalysisModel : object) {
            int rolesCount = roleAnalysisModel.getRolesCount();
            int usersCount = roleAnalysisModel.getUsersCount();
            ScatterDataPoint scatterDataPoint = new ScatterDataPoint(usersCount, rolesCount);
            datasetUsers.addData(scatterDataPoint);
            datasetUsers.addData(roleAnalysisModel.getUsersCount());

        }

        chartData.addDataset(datasetUsers);

        return chartData;
    }

    private @NotNull ChartData createDataset() {
        ChartData chartData = new ChartData();

        ChartDataset datasetUsers = new ChartDataset();
        datasetUsers.setLabel(getDatasetUserLabel());
        datasetUsers.addBackgroudColor("Red");

        ChartDataset datasetRoles = new ChartDataset();
        datasetRoles.setLabel(getDatasetRoleLabel());
        datasetRoles.addBackgroudColor("Green");

        if (chartType.equals(ChartType.LINE)) {
            datasetUsers.addBorderColor("Red");
            datasetUsers.setBorderWidth(1);
            datasetRoles.addBorderColor("Green");
            datasetRoles.setBorderWidth(1);
        }

        List<RoleAnalysisModel> object = roleAnalysisModels.getObject();
        for (RoleAnalysisModel roleAnalysisModel : object) {
            int rolesCount = roleAnalysisModel.getRolesCount();
            datasetUsers.addData(roleAnalysisModel.getUsersCount());
            datasetRoles.addData(rolesCount);
            chartData.addLabel(String.valueOf(rolesCount));
        }

        chartData.addDataset(datasetRoles);
        chartData.addDataset(datasetUsers);

        return chartData;
    }

    private @NotNull ChartOptions createChartOptions() {
        ChartOptions options = new ChartOptions();
        options.setLegend(createLegendOptions());
        options.setIndexAxis(IndexAxis.AXIS_X.getValue());
        options.setResponsive(true);
        options.setMaintainAspectRatio(false);

        ChartInteractionOption interaction = new ChartInteractionOption();
        interaction.setMode("index");
        interaction.setIntersect(false);
        options.setInteraction(interaction);

        ChartScaleAxisOption chartScaleXAxisOption = new ChartScaleAxisOption();
        chartScaleXAxisOption.setDisplay(true);
        ChartTitleOption chartTitleXOption =
                new ChartTitleOption();
        chartTitleXOption.setDisplay(true);
        chartTitleXOption.setText(getXAxisTitle());

        chartScaleXAxisOption.setTitle(chartTitleXOption);

        ChartScaleAxisOption chartScaleYAxisOption = new ChartScaleAxisOption();
        chartScaleYAxisOption.setDisplay(true);
        ChartTitleOption chartTitleYOption =
                new ChartTitleOption();
        chartTitleYOption.setDisplay(true);
        chartTitleYOption.setText(getYAxisTitle());
        chartScaleYAxisOption.setTitle(chartTitleYOption);

        ChartScaleOption scales = new ChartScaleOption();
        scales.setX(chartScaleXAxisOption);
        scales.setY(chartScaleYAxisOption);
        options.setScales(scales);

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

    public String getXAxisTitle() {
        return "Roles occupation";
    }

    public String getYAxisTitle() {
        return "Users occupation";
    }

    public String getDatasetUserLabel() {
        return "Users";
    }

    public String getDatasetRoleLabel() {
        return "Roles";
    }

}
