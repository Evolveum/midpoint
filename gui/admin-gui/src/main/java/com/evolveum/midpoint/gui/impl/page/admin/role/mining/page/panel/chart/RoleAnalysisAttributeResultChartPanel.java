/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.chart;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.chart.ChartType;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisAttributesDto;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.chart.RoleAnalysisAttributeResultChartModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.RoleAnalysisSimpleModel;
import com.evolveum.wicket.chartjs.ChartConfiguration;
import com.evolveum.wicket.chartjs.ChartJsPanel;

public class RoleAnalysisAttributeResultChartPanel extends BasePanel<RoleAnalysisAttributesDto> {

    private static final String ID_CONTAINER_CHART = "container";
    private static final String ID_CHART = "chart";
    ChartType chartType = ChartType.SCATTER;

    public RoleAnalysisAttributeResultChartPanel(String id, IModel<RoleAnalysisAttributesDto> chartModel) {
        super(id, chartModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initChartPart();
    }

    private void initChartPart() {

        WebMarkupContainer chartContainer = new WebMarkupContainer(ID_CONTAINER_CHART);
        chartContainer.add(AttributeAppender.replace("style", getChartContainerStyle()));
        chartContainer.setOutputMarkupId(true);
        add(chartContainer);

        ChartJsPanel<ChartConfiguration> roleAnalysisChart =
                new ChartJsPanel<>(ID_CHART, new LoadableModel<>() {
                    @Override
                    protected ChartConfiguration load() {
                        return getRoleAnalysisStatistics().getObject();
                    }
                });

        roleAnalysisChart.setOutputMarkupId(true);
        roleAnalysisChart.setOutputMarkupPlaceholderTag(true);
        chartContainer.add(roleAnalysisChart);

    }

    private RoleAnalysisAttributeResultChartModel getRoleAnalysisStatistics() {
        return new RoleAnalysisAttributeResultChartModel(new PropertyModel<>(getModel(), RoleAnalysisAttributesDto.F_CHART_MODEL), chartType) {

            @Override
            public boolean isCompare() {
                return RoleAnalysisAttributeResultChartPanel.this.getModelObject().isCompared();
            }

            @Override
            public String getXAxisTitle() {
                return "Density";
            } //TODO localization

            @Override
            public String getYAxisTitle() {
                return "Analysed properties";
            } //TODO localization

            @Override
            public String getDatasetUserLabel() {
                return "Density";
            } //TODO localization

        };
    }

    protected String getChartContainerStyle(){
        return null;
    }
}
