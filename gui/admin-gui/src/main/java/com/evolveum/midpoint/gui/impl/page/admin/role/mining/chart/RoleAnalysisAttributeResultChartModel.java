/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.chart;

import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.chart.options.ChartDataRm;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.RoleAnalysisSimpleModel;
import com.evolveum.wicket.chartjs.*;

/**
 * The RoleAnalysisAggregateChartModel class is a LoadableModel that generates aggregate
 * charts for role analysis based on provided data.
 */
public class RoleAnalysisAttributeResultChartModel extends LoadableModel<ChartConfiguration> {

    IModel<List<RoleAnalysisSimpleModel>> roleAnalysisModels;
    ChartType chartType;

    public RoleAnalysisAttributeResultChartModel(
            @NotNull IModel<List<RoleAnalysisSimpleModel>> roleAnalysisModel,
            @NotNull ChartType chartType) {
        this.roleAnalysisModels = roleAnalysisModel;
        this.chartType = chartType;
    }

    @Override
    protected ChartConfiguration load() {
        return createChartConfiguration();
    }

    private @NotNull ChartConfiguration createChartConfiguration() {
        BarChartConfiguration chart = new BarChartConfiguration();
        ChartData chartData = createDataset();
        chart.setData(chartData);
        chart.setOptions(createChartOptions());
        return chart;
    }

    private @NotNull ChartData createDataset() {
        ChartData chartData = new ChartData();

        if (isCompare()) {
            ChartDataRm dataset = new ChartDataRm();
            dataset.setLabel(getDatasetUserLabel());
            dataset.addBackgroudColor("#206f9d");
            dataset.setBorderRadius("10");
            dataset.setStack("stack0");

            ChartDataRm datasetCompared = new ChartDataRm();
            datasetCompared.setLabel("Compared user part");
            datasetCompared.addBackgroudColor("red");
            datasetCompared.setBorderRadius("10");
            datasetCompared.setStack("stack0");

            List<RoleAnalysisSimpleModel> object = roleAnalysisModels.getObject();
            for (RoleAnalysisSimpleModel roleAnalysisModel : object) {
                dataset.addData(roleAnalysisModel.getDensity());
                datasetCompared.addData(-roleAnalysisModel.getComparedPercentagePart());
                chartData.addLabel(roleAnalysisModel.getDescription());
            }
            chartData.addDataset(dataset);
            chartData.addDataset(datasetCompared);
            return chartData;
        }

        ChartDataRm datasetUsers = new ChartDataRm();
        datasetUsers.setLabel(getDatasetUserLabel());
        datasetUsers.addBackgroudColor("#206f9d");
        datasetUsers.setBorderRadius("10");

        List<RoleAnalysisSimpleModel> object = roleAnalysisModels.getObject();
        for (RoleAnalysisSimpleModel roleAnalysisModel : object) {
            datasetUsers.addData(roleAnalysisModel.getDensity());
            chartData.addLabel(roleAnalysisModel.getDescription());
        }
        chartData.addDataset(datasetUsers);

        return chartData;
    }

    private @NotNull ChartOptions createChartOptions() {
        ChartOptions options = new ChartOptions();
        options.setBarPercentage(0.8);
        ChartPluginsOption plugins = new ChartPluginsOption();
        plugins.setLegend(createLegendOptions());
        options.setPlugins(plugins);
        options.setIndexAxis(IndexAxis.AXIS_Y.getValue());
        options.setResponsive(true);
        options.setMaintainAspectRatio(false);

        ChartInteractionOption interaction = new ChartInteractionOption();
        interaction.setMode("index");
        interaction.setIntersect(false);
        options.setInteraction(interaction);

        ChartScaleAxisOption chartScaleXAxisOption = new ChartScaleAxisOption();
        chartScaleXAxisOption.setStacked(isCompare());

        chartScaleXAxisOption.setDisplay(true);

        ChartTitleOption chartTitleXOption =
                new ChartTitleOption();
        chartTitleXOption.setDisplay(true);
        chartTitleXOption.setText(getXAxisTitle());

        chartScaleXAxisOption.setTitle(chartTitleXOption);

        ChartScaleAxisOption chartScaleYAxisOption = new ChartScaleAxisOption();
        chartScaleYAxisOption.setStacked(isCompare());

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

    public boolean isCompare() {
        return false;
    }

}
