package com.evolveum.midpoint.gui.impl.page.admin.role.mining.chart;/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.path.ItemPath;

import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.analysis.AttributeAnalysisStructure;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.wicket.chartjs.*;

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
        ChartData chartData = generateDataset();
        chart.setData(chartData);
        chart.setOptions(createChartOptions());
        return chart;
    }

    private @NotNull ChartOptions createChartOptions() {
        ChartOptions options = new ChartOptions();
        options.setLegend(createLegendOptions());
        options.setIndexAxis(IndexAxis.AXIS_X.getValue());
        options.setResponsive(true);
        options.setMaintainAspectRatio(false);

        ChartAnimationOption chartAnimationOption = new ChartAnimationOption();
        chartAnimationOption.setDuration(0);
        options.setAnimation(chartAnimationOption);
        if (roleAnalysisModels.getObject().size() < 10) {
            options.setBarPercentage(0.8);
        }
        return options;
    }

    public ChartData generateDataset() {
        return createDataset();
    }

    private @NotNull ChartData createDataset() {
        ChartData chartData = new ChartData();

        ChartDataset datasetAttributeDensity = new ChartDataset();
        datasetAttributeDensity.setLabel("Density");
        datasetAttributeDensity.addBackgroudColor(getColor());
        datasetAttributeDensity.setBorderWidth(1);

        ChartDataset datasetUsers = new ChartDataset();
        datasetUsers.setLabel("Users attribute");
        datasetUsers.addBackgroudColor("Red");

        ChartDataset datasetRoles = new ChartDataset();
        datasetRoles.setLabel("Roles attribute");
        datasetRoles.addBackgroudColor("Green");

        List<AttributeAnalysisStructure> objects = roleAnalysisModels.getObject();
        Map<ItemPath, List<AttributeAnalysisStructure>> itemPathMap = objects.stream()
                .collect(Collectors.groupingBy(AttributeAnalysisStructure::getItemPath));

        Map<ItemPath, List<AttributeAnalysisStructure>> sortedItemPathMap = itemPathMap.entrySet().stream()
                .sorted((entry1, entry2) -> {
                    double totalDensity1 = entry1.getValue().stream()
                            .mapToDouble(AttributeAnalysisStructure::getDensity)
                            .sum();

                    double totalDensity2 = entry2.getValue().stream()
                            .mapToDouble(AttributeAnalysisStructure::getDensity)
                            .sum();

                    double averageDensity1 = totalDensity1 / 2;
                    double averageDensity2 = totalDensity2 / 2;

                    return Double.compare(averageDensity2, averageDensity1);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        for (Map.Entry<ItemPath, List<AttributeAnalysisStructure>> entry : sortedItemPathMap.entrySet()) {
            ItemPath itemPath = entry.getKey();
            List<AttributeAnalysisStructure> filteredObjects = entry.getValue();

            if (filteredObjects.size() == 2) {
                AttributeAnalysisStructure userAttribute = null;
                AttributeAnalysisStructure roleAttribute = null;

                for (AttributeAnalysisStructure obj : filteredObjects) {
                    if (obj.getComplexType() == UserType.COMPLEX_TYPE) {
                        userAttribute = obj;
                    } else {
                        roleAttribute = obj;
                    }
                }

                if (userAttribute != null) {
                    datasetUsers.addData(userAttribute.getDensity());
                }
                if (roleAttribute != null) {
                    datasetRoles.addData(roleAttribute.getDensity());
                }
            } else if (filteredObjects.size() == 1) {
                AttributeAnalysisStructure attributeStructure = filteredObjects.get(0);
                if (attributeStructure.getComplexType() == UserType.COMPLEX_TYPE) {
                    datasetUsers.addData(attributeStructure.getDensity());
                    datasetRoles.addData(0.0);
                } else {
                    datasetRoles.addData(attributeStructure.getDensity());
                    datasetUsers.addData(0.0);
                }
            }

            chartData.addLabel(itemPath.toString());
        }

        chartData.addDataset(datasetRoles);
        chartData.addDataset(datasetUsers);

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

    public String getColor() {
        return "#206F9D";
    }

}
