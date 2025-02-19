/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.aspects;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.explainOutlier;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.panel.OutlierPartitionPanel.PARAM_ANOMALY_OID;

import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.*;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;

import java.util.*;

@PanelType(name = "outlierOverView", defaultContainerPath = "empty")
@PanelInstance(identifier = "outlierOverView",
        applicableForType = RoleAnalysisOutlierType.class,
        defaultPanel = true,
        display = @PanelDisplay(
                label = "RoleAnalysis.overview.panel",
                icon = GuiStyleConstants.CLASS_LINE_CHART_ICON,
                order = 10))
public class RoleAnalysisOutlierAnalysisAspectsPanel extends AbstractObjectMainPanel<RoleAnalysisOutlierType, ObjectDetailsModels<RoleAnalysisOutlierType>> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_HEADER_ITEMS = "header-items";
    private static final String ID_ACCESS_PANEL = "accessPanel";

    private static final String DOT_CLASS = RoleAnalysisOutlierAnalysisAspectsPanel.class.getName() + ".";
    private static final String OP_INIT_ACCESS_ANOMALIES = DOT_CLASS + "initAccessAnomalies";
    private static final String OP_LOAD_DASHBOARD = DOT_CLASS + "loadDashboard";

    public RoleAnalysisOutlierAnalysisAspectsPanel(
            @NotNull String id,
            @NotNull ObjectDetailsModels<RoleAnalysisOutlierType> model,
            @NotNull ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        //TODO tbd
        getPageBase().getPageParameters().remove(PARAM_ANOMALY_OID);
    }

    protected void initLayout() {
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        initDashboard(container);

        initAccessPanel(container);

    }

    private void initAccessPanel(@NotNull WebMarkupContainer container) {
        RoleAnalysisViewAllPanel<?> accessPanel = new RoleAnalysisViewAllPanel<>(ID_ACCESS_PANEL,
                createStringResource("RoleAnalysis.aspect.overview.page.title.access.anomalies")) {
            @Contract(pure = true)
            @Override
            protected @NotNull String getIconCssClass() {
                return GuiStyleConstants.CLASS_ICON_ASSIGNMENTS;
            }

            @Contract(" -> new")
            @Override
            protected @NotNull IModel<String> getLinkModel() {
                return createStringResource(
                        "RoleAnalysis.aspect.overview.page.title.view.all.access.anomalies");
            }

            @Override
            protected void onLinkClick(AjaxRequestTarget target) {
                RoleAnalysisOutlierType outlier = getObjectDetailsModels().getObjectType();
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, outlier.getOid());
                parameters.add("panelId", "anomalyAccess");
                Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                        .getObjectDetailsPage(RoleAnalysisOutlierType.class);
                ((PageBase) getPage()).navigateToNext(detailsPageClass, parameters);
            }

            @Override
            protected @NotNull Component getPanelComponent(String id) {
                RoleAnalysisDetectedAnomalyTable detectedAnomalyTable = new RoleAnalysisDetectedAnomalyTable(id,
                        () -> {
                            Task task = getPageBase().createSimpleTask(OP_INIT_ACCESS_ANOMALIES);
                            OperationResult result = task.getResult();
                            return new AnomalyObjectDto(getPageBase().getRoleAnalysisService(),
                                    getObjectDetailsModels().getObjectType(), null, false, task, result);
                        });

                detectedAnomalyTable.setOutputMarkupId(true);
                detectedAnomalyTable.add(AttributeModifier.append("style", "min-height: 400px;"));
                return detectedAnomalyTable;
            }
        };

        accessPanel.setOutputMarkupId(true);
        container.add(accessPanel);
    }

    protected void initDashboard(
            @NotNull WebMarkupContainer container) {

        RepeatingView cardBodyComponent = new RepeatingView(ID_HEADER_ITEMS);
        cardBodyComponent.setOutputMarkupId(true);
        container.add(cardBodyComponent);

        RoleAnalysisOutlierDashboardPanel<?> characteristicHeader = new RoleAnalysisOutlierDashboardPanel<>(cardBodyComponent.newChildId(),
                createStringResource("RoleAnalysisOutlierAnalysisAspectsPanel.widget.characteristics")) {
            @Contract(pure = true)
            @Override
            protected @NotNull String getIconCssClass() {
                return "fa fa-lock";
            }

            @Override
            protected @NotNull Component getPanelComponent(String id) {
                Task task = getPageBase().createSimpleTask(OP_LOAD_DASHBOARD);
                OperationResult result = task.getResult();
                IconWithLabel iconWithLabel = new IconWithLabel(id, explainOutlier(getPageBase().getRoleAnalysisService(), getObjectDetailsModels().getObjectType(), false, task, result)) {
                    @Contract(pure = true)
                    @Override
                    protected @NotNull String getIconCssClass() {
                        return "fas fa-chart-line";
                    }

                    @Contract(pure = true)
                    @Override
                    protected @NotNull String getComponentCssStyle() {
                        return "font-size: 20px;";
                    }

                    @Override
                    protected @NotNull String getComponentCssClass() {
                        return super.getComponentCssClass() + " gap-2";
                    }
                };

                iconWithLabel.add(AttributeModifier.append("class", "badge p-3 my-auto justify-content-center flex-grow-1 flex-shrink-1 bg-transparent-red border border-danger text-danger text-wrap"));
                return iconWithLabel;
            }

            @Override
            protected @NotNull Component getFooterComponent(String id) {
                RepeatingView cardBodyComponent = new RepeatingView(id);
                cardBodyComponent.setOutputMarkupId(true);

                RoleAnalysisValueLabelPanel<?> anomaliesPanel = new RoleAnalysisValueLabelPanel<>(cardBodyComponent.newChildId()) {
                    @Contract(pure = true)
                    @Override
                    protected @NotNull Component getTitleComponent(String id) {
                        return new IconWithLabel(id,
                                createStringResource("RoleAnalysisOutlierAnalysisAspectsPanel.widget.characteristics.anomalies")) {
                            @Override
                            protected String getIconCssClass() {
                                return "fa fa-exclamation-triangle text-muted";
                            }

                            @Override
                            protected String getLabelComponentCssClass() {
                                return "text-muted";
                            }

                            @Override
                            protected String getComponentCssClass() {
                                return super.getComponentCssClass() + " gap-2";
                            }
                        };
                    }

                    @Contract(pure = true)
                    @Override
                    protected @NotNull Component getValueComponent(String id) {
                        Label label = new Label(id, RoleAnalysisOutlierAnalysisAspectsPanel.this::getAnomalyCount);
                        label.setOutputMarkupId(true);
                        label.add(AttributeModifier.append("class", "text-muted"));
                        return label;
                    }
                };
                anomaliesPanel.setOutputMarkupId(true);
                cardBodyComponent.add(anomaliesPanel);

                RoleAnalysisValueLabelPanel<?> partitionPanel = new RoleAnalysisValueLabelPanel<>(cardBodyComponent.newChildId()) {
                    @Contract(pure = true)
                    @Override
                    protected @NotNull Component getTitleComponent(String id) {
                        return new IconWithLabel(id,
                                createStringResource("RoleAnalysisOutlierAnalysisAspectsPanel.widget.characteristics.partitions")) {
                            @Override
                            protected String getIconCssClass() {
                                return GuiStyleConstants.CLASS_ROLE_ANALYSIS_SESSION_ICON + " text-muted";
                            }

                            @Override
                            protected String getLabelComponentCssClass() {
                                return "text-muted";
                            }

                            @Override
                            protected String getComponentCssClass() {
                                return super.getComponentCssClass() + " gap-2";
                            }
                        };
                    }

                    @Contract(pure = true)
                    @Override
                    protected @NotNull Component getValueComponent(String id) {
                        Label label = new Label(id, RoleAnalysisOutlierAnalysisAspectsPanel.this::getPartitionCount);
                        label.setOutputMarkupId(true);
                        label.add(AttributeModifier.append("class", "text-muted"));
                        return label;
                    }
                };
                partitionPanel.setOutputMarkupId(true);
                cardBodyComponent.add(partitionPanel);
                return cardBodyComponent;
            }
        };
        characteristicHeader.setOutputMarkupId(true);
        cardBodyComponent.add(characteristicHeader);

        RoleAnalysisOutlierDashboardPanel<?> accessHeader = new RoleAnalysisOutlierDashboardPanel<>(cardBodyComponent.newChildId(),
                createStringResource("RoleAnalysisOutlierAnalysisAspectsPanel.widget.access")) {
            @Contract(pure = true)
            @Override
            protected @NotNull String getIconCssClass() {
                return "fa fa-lock";
            }

            @Override
            protected boolean isFooterVisible() {
                return false;
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getPanelContainerCssClass() {
                return "p-0 h-100 border-top w-100";
            }

            @Override
            protected @NotNull Component getPanelComponent(String id) {
                return new RoleAnalysisAccessDistributionPanel(id, () -> {
                    PageBase pageBase = RoleAnalysisOutlierAnalysisAspectsPanel.this.getPageBase();
                    RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
                    Task simpleTask = pageBase.createSimpleTask("loadOutlierDetails");
                    OperationResult result = simpleTask.getResult();
                    return new AccessDistributionDto(
                            RoleAnalysisOutlierAnalysisAspectsPanel.this.getObjectDetailsModels().getObjectType(),
                            roleAnalysisService, result, simpleTask);
                });
            }


        };

        accessHeader.setOutputMarkupId(true);
        cardBodyComponent.add(accessHeader);
    }

    private int getPartitionCount() {
        List<RoleAnalysisOutlierPartitionType> outlierPartitions = getObjectDetailsModels().getObjectType().getPartition();
        return outlierPartitions.size();
    }

    private int getAnomalyCount() {
        Set<String> anomalySet = new HashSet<>();
        for (RoleAnalysisOutlierPartitionType outlierPartition : getObjectDetailsModels().getObjectType().getPartition()) {
            List<DetectedAnomalyResultType> detectedAnomalyResult = outlierPartition.getDetectedAnomalyResult();
            for (DetectedAnomalyResultType anomalyResult : detectedAnomalyResult) {
                anomalySet.add(anomalyResult.getTargetObjectRef().getOid());
            }
        }
        return anomalySet.size();
    }

}

