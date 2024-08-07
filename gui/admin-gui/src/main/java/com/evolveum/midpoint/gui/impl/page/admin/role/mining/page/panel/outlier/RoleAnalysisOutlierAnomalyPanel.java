/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.RoleAnalysisOutlierTable;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.AnomalyTableCategory;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.RoleAnalysisDetectedAnomalyTable;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.RoleAnalysisTabbedPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierPartitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierType;

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
        tabPanel.add(AttributeAppender.append("class", "p-0 m-0"));
        container.add(tabPanel);
    }

    protected List<ITab> createPartitionTabs(RoleAnalysisOutlierType outlier) {
        List<ITab> tabs = new ArrayList<>();

        tabs.add(new PanelTab(getPageBase().createStringResource("All anomalies"), new VisibleEnableBehaviour()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                RoleAnalysisDetectedAnomalyTable detectedAnomalyTable = new RoleAnalysisDetectedAnomalyTable(panelId,
                        outlier, null, AnomalyTableCategory.OUTLIER_ANOMALY);
                detectedAnomalyTable.setOutputMarkupId(true);
                return detectedAnomalyTable;
            }
        });

        List<RoleAnalysisOutlierPartitionType> outlierPartitions = outlier.getOutlierPartitions();

        for (RoleAnalysisOutlierPartitionType partition : outlierPartitions) {
            String targetName = partition.getTargetSessionRef().getTargetName().toString();
            tabs.add(new PanelTab(Model.of(targetName + " partition anomalies"), new VisibleEnableBehaviour()) {

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
        RepeatingView headerItems = new RepeatingView(ID_HEADER_ITEMS);
        container.add(headerItems);

        MetricValuePanel anomalyCountPanel = new MetricValuePanel(headerItems.newChildId()) {
            @Contract("_ -> new")
            @Override
            protected @NotNull Component getTitleComponent(String id) {
                return new LabelWithHelpPanel(id, createStringResource("RoleAnalysisOutlierType.anomalyCount")) {
                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleAnalysisOutlierType.anomalyCount.help");
                    }
                };
            }

            @Contract("_ -> new")
            @Override
            protected @NotNull Component getValueComponent(String id) {
                Label label = new Label(id, "0 (todo)");
                label.add(AttributeAppender.append("class", " h4"));
                return label;
            }
        };
        headerItems.add(anomalyCountPanel);

        MetricValuePanel anomalyAverageConfidence = new MetricValuePanel(headerItems.newChildId()) {
            @Override
            protected Component getTitleComponent(String id) {
                return new LabelWithHelpPanel(id,
                        createStringResource("RoleAnalysisOutlierType.anomalyAverageConfidence")) {
                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleAnalysisOutlierType.anomalyAverageConfidence.help");
                    }
                };
            }

            @Override
            protected Component getValueComponent(String id) {
                Label label = new Label(id, "0 (todo)");
                label.add(AttributeAppender.append("class", " h4"));
                return label;
            }
        };
        headerItems.add(anomalyAverageConfidence);

        MetricValuePanel tbd1 = new MetricValuePanel(headerItems.newChildId()) {
            @Override
            protected Component getTitleComponent(String id) {
                return new LabelWithHelpPanel(id, Model.of("TBD")) {
                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleAnalysisOutlierType.anomalyAverageConfidence.help");
                    }
                };
            }

            @Override
            protected Component getValueComponent(String id) {
                Label label = new Label(id, "0 (todo)");
                label.add(AttributeAppender.append("class", " h4"));
                return label;
            }
        };
        headerItems.add(tbd1);

        MetricValuePanel tbd2 = new MetricValuePanel(headerItems.newChildId()) {
            @Override
            protected Component getTitleComponent(String id) {
                return new LabelWithHelpPanel(id, Model.of("TBD")) {
                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleAnalysisOutlierType.anomalyAverageConfidence.help");
                    }
                };
            }

            @Override
            protected Component getValueComponent(String id) {
                Label label = new Label(id, "0 (todo)");
                label.add(AttributeAppender.append("class", " h4"));
                return label;
            }
        };
        headerItems.add(tbd2);
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    protected RoleAnalysisOutlierTable getTable() {
        return (RoleAnalysisOutlierTable) get(((PageBase) getPage()).createComponentPath(ID_CONTAINER, ID_PANEL));
    }

}
