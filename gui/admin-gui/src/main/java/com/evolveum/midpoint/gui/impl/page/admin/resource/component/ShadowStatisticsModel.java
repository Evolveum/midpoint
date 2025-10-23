/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;
import com.evolveum.wicket.chartjs.*;

public class ShadowStatisticsModel extends LoadableModel<ChartConfiguration> {

    @Override
    protected ChartConfiguration load() {
        return createChartConfiguration();
    }

    private ChartConfiguration createChartConfiguration() {
        BarChartConfiguration chart = new BarChartConfiguration();

        List<ShadowStatisticsDto> shadowStatistics = createShadowStatistics();

        ChartData chartData = new ChartData();
        chartData.addDataset(createDataset(shadowStatistics));

        for (ShadowStatisticsDto shadowStatisticsDto : shadowStatistics) {
            chartData.addLabel(createSituationLabel(shadowStatisticsDto.getSituation()));
        }

        chart.setData(chartData);
        chart.setOptions(createChartOptions());
        return chart;
    }

    private List<ShadowStatisticsDto> createShadowStatistics() {
        List<ShadowStatisticsDto> shadowStatisticsList =
                Arrays.stream(SynchronizationSituationType.values())
                        .map(situation -> new ShadowStatisticsDto(situation, createTotalsModel(prepareSynchroniationSituationFilter(situation))))
                        .collect(Collectors.toList());
        shadowStatisticsList.add(new ShadowStatisticsDto(null, createTotalsModel(prepareSynchroniationSituationFilter(null))));
        return shadowStatisticsList;
    }

    private ObjectFilter prepareSynchroniationSituationFilter(SynchronizationSituationType situation) {
        return PrismContext.get().queryFor(ShadowType.class)
                .item(ShadowType.F_SYNCHRONIZATION_SITUATION).eq(situation)
                .buildFilter();
    }

    private ChartDataset createDataset(List<ShadowStatisticsDto> shadowStatistics) {
        ChartDataset dataset = new ChartDataset();
        dataset.setLabel("Shadow statistics");

        for (ShadowStatisticsDto dto : shadowStatistics) {
            dataset.addData(dto.getCount());
            dataset.addBackgroudColor(dto.getColor());
        }

        return dataset;
    }

    private ChartOptions createChartOptions() {
        ChartOptions options = new ChartOptions();
        ChartPluginsOption plugins = new ChartPluginsOption();
        plugins.setLegend(createLegendOptions());
        options.setPlugins(plugins);
        options.setIndexAxis(IndexAxis.AXIS_Y.getValue());
        options.setResponsive(true);
        options.setMaintainAspectRatio(false);
        return options;
    }

    private ChartLegendOption createLegendOptions() {
        ChartLegendOption legend = new ChartLegendOption();
        legend.setPosition("right");
        ChartLegendLabel label = new ChartLegendLabel();
        label.setBoxWidth(15);
        legend.setLabels(label);
        return legend;
    }

    protected Integer createTotalsModel(ObjectFilter situationFilter) {
        return 0;
    }

    protected String createSituationLabel(SynchronizationSituationType situation) {
        return "";
    }

}
