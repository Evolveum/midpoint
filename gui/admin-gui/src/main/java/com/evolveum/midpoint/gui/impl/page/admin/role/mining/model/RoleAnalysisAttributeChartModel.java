/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.model;

import java.util.List;

import com.evolveum.midpoint.common.mining.objects.analysis.AttributeAnalysisStructure;

import org.apache.wicket.model.LoadableDetachableModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.wicket.chartjs.*;

import org.jetbrains.annotations.NotNull;

/**
 * The model for the role analysis attribute chart.
 * Used for loading the data for the role analysis attribute chart.
 * Used in role analysis cluster chart.
 */
// TODO - this class is just fast experiment
public class RoleAnalysisAttributeChartModel extends LoadableModel<ChartConfiguration> {

    LoadableDetachableModel<List<AttributeAnalysisStructure>> roleAnalysisModels;

    public RoleAnalysisAttributeChartModel(LoadableDetachableModel<List<AttributeAnalysisStructure>> roleAnalysisModel) {
        this.roleAnalysisModels = roleAnalysisModel;

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

    private @NotNull RoleAnalysisChartOptions createChartOptions() {
        RoleAnalysisChartOptions options = new RoleAnalysisChartOptions();
        options.setLegend(createLegendOptions());
        options.setIndexAxis(IndexAxis.AXIS_X.getValue());
        ChartAnimationOption chartAnimationOption = new ChartAnimationOption();
        chartAnimationOption.setDuration(0);
        options.setAnimation(chartAnimationOption);
        if (roleAnalysisModels.getObject().size() < 10) {
            options.setBarPercentage(0.4);
        }
        return options;
    }

    private @NotNull ChartData createDataset() {
        ChartData chartData = new ChartData();

        ChartDataset datasetAttributeDensity = new ChartDataset();
        datasetAttributeDensity.setLabel("Density");
        datasetAttributeDensity.addBackgroudColor(getColor());
        datasetAttributeDensity.setBorderWidth(1);

        List<AttributeAnalysisStructure> object = roleAnalysisModels.getObject();
        for (AttributeAnalysisStructure roleAnalysisModel : object) {
            double density = roleAnalysisModel.getDensity();
            datasetAttributeDensity.addData(density);
            chartData.addLabel(roleAnalysisModel.getItemPath());
        }
        chartData.addDataset(datasetAttributeDensity);
        return chartData;
    }

    private @NotNull ChartLegendOption createLegendOptions() {
        ChartLegendOption legend = new ChartLegendOption();
        legend.setDisplay(false);
        ChartLegendLabel label = new ChartLegendLabel();
        label.setBoxWidth(15);
        legend.setLabels(label);
        return legend;
    }

    public String getColor(){
        return "#206F9D";
    }

}
