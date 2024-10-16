/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.panel;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.panel.OutlierPartitionPanel.PARAM_ANOMALY_OID;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisWidgetsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.WidgetItemModel;

import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.AnomalyTableCategory;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.RoleAnalysisDetectedAnomalyTable;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.RoleAnalysisTabbedPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

@PanelType(name = "anomalyAccess")
@PanelInstance(
        identifier = "anomalyAccess",
        applicableForType = RoleAnalysisOutlierType.class,
        display = @PanelDisplay(
                label = "RoleAnalysisOutlierType.outlierPanel.access",
                icon = "fa fa-warning",
                order = 30
        )
)
public class RoleAnalysisOutlierAnomalyPanel extends AbstractObjectMainPanel<RoleAnalysisOutlierType, ObjectDetailsModels<RoleAnalysisOutlierType>> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_HEADER_ITEMS = "header-items";
    private static final String ID_PANEL = "panelId";

    public RoleAnalysisOutlierAnomalyPanel(String id, ObjectDetailsModels<RoleAnalysisOutlierType> model,
            ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        //TODO tbd
        getPageBase().getPageParameters().remove(PARAM_ANOMALY_OID);
    }

    @Override
    protected void initLayout() {
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        initHeaderLayout(container);
        initPanelLayout(container);
    }

    private void initPanelLayout(@NotNull WebMarkupContainer container) {

        ObjectDetailsModels<RoleAnalysisOutlierType> objectDetailsModels = getObjectDetailsModels();

        List<ITab> tabs = createPartitionTabs(objectDetailsModels.getObjectType());
        RoleAnalysisTabbedPanel<ITab> tabPanel = new RoleAnalysisTabbedPanel<>(ID_PANEL, tabs, null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected WebMarkupContainer newLink(String linkId, final int index) {
                return new AjaxSubmitLink(linkId) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected void onError(AjaxRequestTarget target) {
                        super.onError(target);
                        target.add(getPageBase().getFeedbackPanel());
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        super.onSubmit(target);

                        setSelectedTab(index);
                        if (target != null) {
                            target.add(findParent(TabbedPanel.class));
                            target.add(getPageBase().getFeedbackPanel());
                        }
                    }

                };
            }
        };
        tabPanel.setOutputMarkupId(true);
        tabPanel.setOutputMarkupPlaceholderTag(true);
        tabPanel.add(AttributeAppender.append("class", "ml-2 mr-2 m-0"));
        container.add(tabPanel);
    }

    protected List<ITab> createPartitionTabs(RoleAnalysisOutlierType outlier) {
        List<ITab> tabs = new ArrayList<>();

        tabs.add(new PanelTab(getPageBase().createStringResource("RoleAnalysisOutlierAnomalyPanel.all.access.anomaly..tab.title"), new VisibleEnableBehaviour()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                RoleAnalysisDetectedAnomalyTable detectedAnomalyTable = new RoleAnalysisDetectedAnomalyTable(panelId,
                        outlier, null, AnomalyTableCategory.OUTLIER_ANOMALY);
                detectedAnomalyTable.setOutputMarkupId(true);
                return detectedAnomalyTable;
            }
        });

        List<RoleAnalysisOutlierPartitionType> outlierPartitions = outlier.getPartition();

        Task task = getPageBase().createSimpleTask("resolveSessionName");
        for (RoleAnalysisOutlierPartitionType partition : outlierPartitions) {
            ObjectReferenceType targetSessionRef = partition.getTargetSessionRef();
            PolyStringType targetName = partition.getTargetSessionRef().getTargetName();
            String partitionName;
            if(targetName == null) {
                partitionName = WebModelServiceUtils.resolveReferenceName(targetSessionRef, getPageBase(), task, task.getResult());
            }else {
                partitionName = targetName.toString();
            }

            tabs.add(new PanelTab(Model.of(partitionName + " partition anomalies"), new VisibleEnableBehaviour()) {

                @Serial private static final long serialVersionUID = 1L;

                @Override
                public WebMarkupContainer createPanel(String panelId) {
                    RoleAnalysisDetectedAnomalyTable detectedAnomalyTable = new RoleAnalysisDetectedAnomalyTable(panelId,
                            outlier, partition, AnomalyTableCategory.PARTITION_ANOMALY);
                    detectedAnomalyTable.setOutputMarkupId(true);
                    return detectedAnomalyTable;
                }
            });
        }

        return tabs;
    }

    private void initHeaderLayout(@NotNull WebMarkupContainer container) {
        RoleAnalysisWidgetsPanel components = new RoleAnalysisWidgetsPanel(ID_HEADER_ITEMS, loadDetailsModel());
        components.setOutputMarkupId(true);
        container.add(components);
    }


    private @NotNull IModel<List<WidgetItemModel>> loadDetailsModel() {
        RoleAnalysisOutlierType outlier = getObjectDetailsModels().getObjectType();
        List<RoleAnalysisOutlierPartitionType> outlierPartitions = outlier.getPartition();
        Set<String> anomalies = new HashSet<>();
        for (RoleAnalysisOutlierPartitionType outlierPartition : outlierPartitions) {
            List<DetectedAnomalyResult> detectedAnomalyResult = outlierPartition.getDetectedAnomalyResult();
            detectedAnomalyResult.forEach(anomaly -> anomalies.add(anomaly.getTargetObjectRef().getOid()));
        }

        List<WidgetItemModel> detailsModel = List.of(
                new WidgetItemModel(createStringResource(""),
                        Model.of("")) {
                    @Override
                    public Component createValueComponent(String id) {
                        Label label = new Label(id, anomalies.size());
                        label.add(AttributeAppender.append("class", " h4"));
                        return label;
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        return new LabelWithHelpPanel(id, createStringResource("RoleAnalysisOutlierType.anomalyCount")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisOutlierType.anomalyCount.help");
                            }
                        };
                    }
                },

                new WidgetItemModel(createStringResource(""),
                        Model.of("")) {
                    @Override
                    public Component createValueComponent(String id) {
                        Double anomalyObjectsConfidence = outlier.getAnomalyObjectsConfidence();
                        if (anomalyObjectsConfidence == null) {
                            anomalyObjectsConfidence = 0.0;
                        }
                        BigDecimal bd = new BigDecimal(anomalyObjectsConfidence);
                        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
                        double confidence = bd.doubleValue();

                        Label label = new Label(id, confidence + "%");
                        label.add(AttributeAppender.append("class", " h4"));
                        return label;
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        return new LabelWithHelpPanel(id,
                                createStringResource("RoleAnalysisOutlierType.anomalyAverageConfidence")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisOutlierType.anomalyAverageConfidence.help");
                            }
                        };
                    }
                }

//                new WidgetItemModel(createStringResource(""),
//                        Model.of("Sort")) {
//                    @Override
//                    public Component createValueComponent(String id) {
//                        Label label = new Label(id, "0 (todo)");
//                        label.add(AttributeAppender.append("class", " h4"));
//                        return label;
//                    }
//
//                    @Override
//                    public Component createDescriptionComponent(String id) {
//                        return new LabelWithHelpPanel(id, Model.of("Pending cert.")) {
//                            @Override
//                            protected IModel<String> getHelpModel() {
//                                return createStringResource("RoleAnalysisOutlierType.anomalyAverageConfidence.help");
//                            }
//                        };
//                    }
//                },

//                new WidgetItemModel(createStringResource(""),
//                        Model.of("Chart")) {
//                    @Override
//                    public Component createValueComponent(String id) {
//                        Label label = new Label(id, "0 (todo)");
//                        label.add(AttributeAppender.append("class", " h4"));
//                        return label;
//                    }
//
//                    @Override
//                    public Component createDescriptionComponent(String id) {
//                        return new LabelWithHelpPanel(id, Model.of("Closed cert.")) {
//                            @Override
//                            protected IModel<String> getHelpModel() {
//                                return createStringResource("RoleAnalysisOutlierType.anomalyAverageConfidence.help");
//                            }
//                        };
//                    }
//                }
        );

        return Model.ofList(detailsModel);
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

}
