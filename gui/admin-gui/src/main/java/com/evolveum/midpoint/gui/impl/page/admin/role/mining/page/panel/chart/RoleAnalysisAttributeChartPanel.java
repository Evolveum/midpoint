/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.chart;

import static com.evolveum.midpoint.web.component.data.mining.RoleAnalysisCollapsableTablePanel.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.RoleAnalysisStackedAttributeChartModel;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.mining.objects.analysis.AttributeAnalysisStructure;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.RepeatingAttributeForm;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.RoleAnalysisAttributeChartModel;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.wicket.chartjs.ChartConfiguration;
import com.evolveum.wicket.chartjs.ChartJsPanel;

/**
 * Represents the role analysis attribute chart panel.
 * Used for displaying the role analysis cluster attribute chart.
 */
// TODO - this class is just fast experiment
public class RoleAnalysisAttributeChartPanel extends BasePanel<String> {

    private static final String ID_TOOL_FORM = "toolForm";
    private static final String ID_CONTAINER_CHART = "container";
    private static final String ID_CHART = "chart";
    private static final String ID_CARD_TITLE = "cardTitle";
    List<AttributeAnalysisStructure> attributeAnalysisStructureList;
    RoleAnalysisClusterType cluster;

    public RoleAnalysisAttributeChartPanel(String id, @NotNull List<AttributeAnalysisStructure> attributeAnalysisStructureList,
            @Nullable RoleAnalysisClusterType cluster) {
        super(id);
        this.cluster = cluster;
        this.attributeAnalysisStructureList = attributeAnalysisStructureList;
        this.attributeAnalysisStructureList.sort((model1, model2) -> Double.compare(model2.getDensity(), model1.getDensity()));
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initChartPart();

        if (cluster != null) {
            initAttributeStatisticsPanel(cluster);
        } else {
            initTargetedAttributeStatisticsPanel(attributeAnalysisStructureList);
        }
    }

    private void initChartPart() {
        WebMarkupContainer chartContainer = new WebMarkupContainer(ID_CONTAINER_CHART);
        chartContainer.setOutputMarkupId(true);
        add(chartContainer);

        Label cardTitle = new Label(ID_CARD_TITLE, getChartTitle());
        cardTitle.setOutputMarkupId(true);
        add(cardTitle);

        ChartJsPanel<ChartConfiguration> roleAnalysisChart =
                new ChartJsPanel<>(ID_CHART, new LoadableModel<>() {
                    @Override
                    protected ChartConfiguration load() {

                        if (getStackedNegativeValue() != null) {
                            return getRoleAnalysisStatisticsStacked().getObject();
                        }

                        return getRoleAnalysisStatistics().getObject();
                    }
                });

        roleAnalysisChart.setOutputMarkupId(true);
        roleAnalysisChart.setOutputMarkupPlaceholderTag(true);
        chartContainer.add(roleAnalysisChart);

        Form<?> toolForm = new MidpointForm<>(ID_TOOL_FORM);
        toolForm.setOutputMarkupId(true);
        add(toolForm);

    }

    public List<AttributeAnalysisStructure> getStackedNegativeValue() {
        return null;
    }

    public RoleAnalysisAttributeChartModel getRoleAnalysisStatistics() {
        return new RoleAnalysisAttributeChartModel(new LoadableDetachableModel<>() {
            @Override
            protected List<AttributeAnalysisStructure> load() {
                return attributeAnalysisStructureList;
            }

        }) {
            @Override
            public String getColor() {
                return RoleAnalysisAttributeChartPanel.this.getColor();
            }
        };
    }

    public RoleAnalysisStackedAttributeChartModel getRoleAnalysisStatisticsStacked() {
        return new RoleAnalysisStackedAttributeChartModel(new LoadableDetachableModel<>() {
            @Override
            protected List<AttributeAnalysisStructure> load() {
                return attributeAnalysisStructureList;
            }

        }, new LoadableDetachableModel<>() {
            @Override
            protected List<AttributeAnalysisStructure> load() {
                return getStackedNegativeValue();
            }

        }) {
            @Override
            public String getColor() {
                return RoleAnalysisAttributeChartPanel.this.getColor();
            }
        };
    }

    public StringResourceModel getChartTitle() {
        return createStringResource("PageRoleAnalysis.chart.title");
    }

    public String getColor() {
        return "#206F9D";
    }

    public void initAttributeStatisticsPanel(@NotNull RoleAnalysisClusterType cluster) {

        RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResult = null;
        RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = null;

        AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();
        if (clusterStatistics != null) {
            roleAttributeAnalysisResult = clusterStatistics.getRoleAttributeAnalysisResult();
            userAttributeAnalysisResult = clusterStatistics.getUserAttributeAnalysisResult();
        }

        WebMarkupContainer webMarkupContainerUser = new WebMarkupContainer(ID_FIRST_COLLAPSABLE_CONTAINER);
        webMarkupContainerUser.setOutputMarkupId(true);
        add(webMarkupContainerUser);

        if (userAttributeAnalysisResult != null) {
            RepeatingAttributeForm repeatingAttributeForm = new RepeatingAttributeForm(
                    ID_COLLAPSABLE_CONTENT, userAttributeAnalysisResult, new HashSet<>(), RoleAnalysisProcessModeType.USER) {
                @Override
                protected boolean isTableSupported() {
                    return false;
                }

                @Override
                protected Set<String> getPathToMark() {
                    return getUserPathToMark();
                }

                @Override
                public boolean isHide() {
                    return true;
                }
            };
            repeatingAttributeForm.setOutputMarkupId(true);
            webMarkupContainerUser.add(repeatingAttributeForm);
        } else {
            Label label = new Label(ID_COLLAPSABLE_CONTENT, "No data available");
            label.setOutputMarkupId(true);
            webMarkupContainerUser.add(label);
        }

        WebMarkupContainer webMarkupContainerRole = new WebMarkupContainer(ID_SECOND_COLLAPSABLE_CONTAINER);
        webMarkupContainerRole.setOutputMarkupId(true);
        add(webMarkupContainerRole);

        if (roleAttributeAnalysisResult != null) {
            RepeatingAttributeForm repeatingAttributeForm = new RepeatingAttributeForm(
                    ID_COLLAPSABLE_CONTENT, roleAttributeAnalysisResult, new HashSet<>(), RoleAnalysisProcessModeType.ROLE) {
                @Override
                protected boolean isTableSupported() {
                    return false;
                }

                @Override
                protected Set<String> getPathToMark() {
                    return getRolePathToMark();
                }

                @Override
                public boolean isHide() {
                    return true;
                }
            };
            repeatingAttributeForm.setOutputMarkupId(true);
            webMarkupContainerRole.add(repeatingAttributeForm);
        } else {
            Label label = new Label(ID_COLLAPSABLE_CONTENT, "No data available");
            label.setOutputMarkupId(true);
            webMarkupContainerRole.add(label);
        }
    }

    public void initTargetedAttributeStatisticsPanel(@NotNull List<AttributeAnalysisStructure> attributeAnalysisStructureList) {

        RoleAnalysisAttributeAnalysisResult userAnalysis = new RoleAnalysisAttributeAnalysisResult();
        for (AttributeAnalysisStructure attributeAnalysisStructure : attributeAnalysisStructureList) {
            double density = attributeAnalysisStructure.getDensity();
            if (density == 0) {
                continue;
            }
            RoleAnalysisAttributeAnalysis roleAnalysisAttributeAnalysis = new RoleAnalysisAttributeAnalysis();
            roleAnalysisAttributeAnalysis.setDensity(density);
            roleAnalysisAttributeAnalysis.setItemPath(attributeAnalysisStructure.getItemPath());
            roleAnalysisAttributeAnalysis.setIsMultiValue(attributeAnalysisStructure.isMultiValue());
            roleAnalysisAttributeAnalysis.setDescription(attributeAnalysisStructure.getDescription());
            List<RoleAnalysisAttributeStatistics> attributeStatistics = attributeAnalysisStructure.getAttributeStatistics();
            for (RoleAnalysisAttributeStatistics attributeStatistic : attributeStatistics) {
                roleAnalysisAttributeAnalysis.getAttributeStatistics().add(attributeStatistic.clone());
            }

            userAnalysis.getAttributeAnalysis().add(roleAnalysisAttributeAnalysis.clone());
        }

        WebMarkupContainer webMarkupContainerUser = new WebMarkupContainer(ID_FIRST_COLLAPSABLE_CONTAINER);
        webMarkupContainerUser.setOutputMarkupId(true);
        add(webMarkupContainerUser);

        RepeatingAttributeForm repeatingAttributeForm = new RepeatingAttributeForm(
                ID_COLLAPSABLE_CONTENT, userAnalysis, new HashSet<>(), getProcessMode()) {
            @Override
            protected boolean isTableSupported() {
                return false;
            }

            @Override
            protected Set<String> getPathToMark() {
                if (getProcessMode().equals(RoleAnalysisProcessModeType.ROLE)) {
                    return getRolePathToMark();
                } else {
                    return getUserPathToMark();
                }
            }

            @Override
            public boolean isHide() {
                return true;
            }
        };
        repeatingAttributeForm.setOutputMarkupId(true);
        webMarkupContainerUser.add(repeatingAttributeForm);

        WebMarkupContainer webMarkupContainerRole = new WebMarkupContainer(ID_SECOND_COLLAPSABLE_CONTAINER);
        webMarkupContainerRole.setOutputMarkupId(true);
        add(webMarkupContainerRole);

        WebMarkupContainer label = new WebMarkupContainer(ID_COLLAPSABLE_CONTENT);
        label.setOutputMarkupId(true);
        webMarkupContainerRole.add(label);

    }

    protected Set<String> getRolePathToMark() {
        return null;
    }

    protected Set<String> getUserPathToMark() {
        return null;
    }

    protected RoleAnalysisProcessModeType getProcessMode() {
        return RoleAnalysisProcessModeType.USER;
    }

}
