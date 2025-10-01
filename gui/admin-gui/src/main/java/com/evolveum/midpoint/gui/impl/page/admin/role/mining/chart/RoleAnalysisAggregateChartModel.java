package com.evolveum.midpoint.gui.impl.page.admin.role.mining.chart;/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

import java.util.List;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.chart.options.ChartTicks;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.chart.options.UniqueChartScaleAxisOption;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.RoleAnalysisModel;

import org.apache.wicket.model.LoadableDetachableModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.wicket.chartjs.*;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.chart.ChartType.SCATTER;

/**
 * The RoleAnalysisAggregateChartModel class is a LoadableModel that generates aggregate
 * charts for role analysis based on provided data.
 */
public class RoleAnalysisAggregateChartModel extends LoadableModel<ChartConfiguration> {

    LoadableDetachableModel<List<RoleAnalysisModel>> roleAnalysisModels;
    ChartType chartType;
    PageBase pageBase;
    boolean isUserMode;

    public RoleAnalysisAggregateChartModel(
            PageBase pageBase,
            @NotNull LoadableDetachableModel<List<RoleAnalysisModel>> roleAnalysisModel,
            @NotNull ChartType chartType,
            boolean isUserMode) {
        this.roleAnalysisModels = roleAnalysisModel;
        this.chartType = chartType;
        this.pageBase = pageBase;
        this.isUserMode = isUserMode;
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
        datasetUsers.setLabel(getDatasetUserLabel() + " / " + getDatasetRoleLabel());
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
        ChartPluginsOption plugins = new ChartPluginsOption();
        plugins.setLegend(createLegendOptions());
        options.setPlugins(plugins);
        options.setIndexAxis(IndexAxis.AXIS_X.getValue());
        options.setResponsive(true);
        options.setMaintainAspectRatio(false);

        ChartInteractionOption interaction = new ChartInteractionOption();
        interaction.setMode("index");
        interaction.setIntersect(false);
        options.setInteraction(interaction);

        UniqueChartScaleAxisOption chartScaleXAxisOption = buildUniqueChartScaleAxisOption();

        ChartScaleOption scales = buildChartScaleOptions(chartScaleXAxisOption);
        options.setScales(scales);

        return options;
    }

    private @NotNull UniqueChartScaleAxisOption buildUniqueChartScaleAxisOption() {
        UniqueChartScaleAxisOption chartScaleXAxisOption = new UniqueChartScaleAxisOption();
        ChartTitleOption chartTitleXOption =
                new ChartTitleOption();
        if (chartType.equals(ChartType.SCATTER)) {
            chartTitleXOption.setDisplay(true);
            chartTitleXOption.setText(getXAxisTitle());
        } else {
            chartTitleXOption.setDisplay(false);
            chartScaleXAxisOption.setTicks(new ChartTicks(false));
        }

        chartScaleXAxisOption.setTitle(chartTitleXOption);
        return chartScaleXAxisOption;
    }

    private @NotNull ChartScaleOption buildChartScaleOptions(UniqueChartScaleAxisOption chartScaleXAxisOption) {
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
        return scales;
    }

    private @NotNull ChartLegendOption createLegendOptions() {
        ChartLegendOption legend = new ChartLegendOption();
        legend.setDisplay(false);
        ChartLegendLabel label = new ChartLegendLabel();
        label.setBoxWidth(15);
        legend.setLabels(label);
        return legend;
    }

    private String getXAxisTitle() {
        if (chartType.equals(SCATTER)) {
            return buildTitle("PageRoleAnalysis.chart.dataset.user.roleMode.label");
        }

        String localizationKey = "PageRoleAnalysis.chart.xAxis.title";
        return buildTitle(localizationKey);
    }

    private String getYAxisTitle() {
        if (chartType.equals(SCATTER)) {
            return buildTitle("PageRoleAnalysis.chart.dataset.role.roleMode.label");
        }

        String localizationKey = "PageRoleAnalysis.chart.yAxis.title";
        return buildTitle(localizationKey);
    }

    private String getDatasetUserLabel() {
        String localizationKey = isUserMode
                ? "PageRoleAnalysis.chart.dataset.user.userMode.label"
                : "PageRoleAnalysis.chart.dataset.user.roleMode.label";
        return buildTitle(localizationKey);
    }

    private String getDatasetRoleLabel() {
        String localizationKey = isUserMode
                ? "PageRoleAnalysis.chart.dataset.role.userMode.label"
                : "PageRoleAnalysis.chart.dataset.role.roleMode.label";
        return buildTitle(localizationKey);
    }

    private String buildTitle(String localizationKey) {
        return pageBase.createStringResource(localizationKey).getString();
    }

}
