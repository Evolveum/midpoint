/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.chart;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.chart.RoleAnalysisAttributeResultChartModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.chart.model.ChartType;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.RoleAnalysisSimpleModel;
import com.evolveum.wicket.chartjs.ChartConfiguration;
import com.evolveum.wicket.chartjs.ChartJsPanel;

public class RoleAnalysisAttributeResultChartPanel extends BasePanel<String> {

    private static final String ID_CONTAINER_CHART = "container";
    private static final String ID_CHART = "chart";
    ChartType chartType = ChartType.BAR;

    public RoleAnalysisAttributeResultChartPanel(String id) {
        super(id);
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
        return new RoleAnalysisAttributeResultChartModel(new LoadableDetachableModel<>() {
            @Override
            protected List<RoleAnalysisSimpleModel> load() {
                return prepareRoleAnalysisData();
            }
        }, chartType) {

            @Override
            public boolean isCompare() {
                return RoleAnalysisAttributeResultChartPanel.this.isCompare();
            }

            @Override
            public String getXAxisTitle() {
                return "Density";
            }

            @Override
            public String getYAxisTitle() {
                return "Analysed properties";
            }

            @Override
            public String getDatasetUserLabel() {
                return "Density";
            }

        };
    }

    protected String getChartContainerStyle(){
        return "height:20vh;";
    }
    public @NotNull List<RoleAnalysisSimpleModel> prepareRoleAnalysisData() {
        return new ArrayList<>();
    }

    public boolean isCompare() {
        return false;
    }

}
