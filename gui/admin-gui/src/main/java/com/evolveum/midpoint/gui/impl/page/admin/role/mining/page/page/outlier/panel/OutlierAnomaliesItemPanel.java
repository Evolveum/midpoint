/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.outlier.panel;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.WidgetItemModel;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.AnomalyObjectDto;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DetectedAnomalyResultType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.impl.component.menu.listGroup.ListGroupMenuItem;
import com.evolveum.midpoint.gui.impl.component.menu.listGroup.MenuItemLinkPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisWidgetsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.RoleAnalysisDetectedAnomalyTable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierPartitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierType;

public class OutlierAnomaliesItemPanel<T extends Serializable>
        extends BasePanel<ListGroupMenuItem<T>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_LINK = "link";

    private final IModel<RoleAnalysisOutlierPartitionType> partitionModel;
    private final IModel<RoleAnalysisOutlierType> outlierModel;

    public OutlierAnomaliesItemPanel(@NotNull String id,
            @NotNull IModel<ListGroupMenuItem<T>> model,
            @NotNull IModel<RoleAnalysisOutlierPartitionType> selectionModel,
            @NotNull IModel<RoleAnalysisOutlierType> outlierModel) {
        super(id, model);

        this.partitionModel = selectionModel;
        this.outlierModel = outlierModel;
        initLayout();
    }

    private void initLayout() {
        add(AttributeModifier.append("class", () -> getModelObject().isOpen() ? "open" : null));
        MenuItemLinkPanel<?> link = new MenuItemLinkPanel<>(ID_LINK, getModel(), 0) {
            @Override
            protected boolean isChevronLinkVisible() {
                return false;
            }

            @SuppressWarnings("rawtypes")
            @Override
            protected void onClickPerformed(AjaxRequestTarget target, ListGroupMenuItem item) {
                OutlierAnomaliesItemPanel.this.onClickPerformed(target, getDetailsPanelComponent());
            }
        };
        add(link);
    }

    protected void onClickPerformed(@NotNull AjaxRequestTarget target, @NotNull Component panelComponent) {
        dispatchComponent(target, panelComponent);
    }

    private void dispatchComponent(@NotNull AjaxRequestTarget target, @NotNull Component component) {
        component.replaceWith(buildDetailsPanel(component.getId()));
        target.add(getDetailsPanelComponent());
    }

    private @NotNull Component buildDetailsPanel(@NotNull String id) {
        RoleAnalysisWidgetsPanel detailsPanel = loadDetailsPanel(id);
        detailsPanel.setOutputMarkupId(true);

        return detailsPanel;
    }

    @NotNull
    private RoleAnalysisWidgetsPanel loadDetailsPanel(@NotNull String id) {
        return new RoleAnalysisWidgetsPanel(id, loadDetailsModel()) {
            @Override
            protected @NotNull Component getPanelComponent(String id1) {
                Task task = getPageBase().createSimpleTask("loadDetailsPanel");
                OperationResult result = task.getResult();

                RoleAnalysisOutlierType outlierObject = getOutlierModel().getObject();
                RoleAnalysisOutlierPartitionType associatedPartition = getPartitionModel().getObject();

                AnomalyObjectDto dto = new AnomalyObjectDto(
                        getPageBase().getRoleAnalysisService(), outlierObject, associatedPartition, false, task, result);
                RoleAnalysisDetectedAnomalyTable detectedAnomalyTable = new RoleAnalysisDetectedAnomalyTable(id1, Model.of(dto));
                detectedAnomalyTable.setOutputMarkupId(true);
                return detectedAnomalyTable;
            }
        };
    }

    protected @NotNull Component getDetailsPanelComponent() {
        return getPageBase().get("form").get("panel");
    }

    public IModel<RoleAnalysisOutlierPartitionType> getPartitionModel() {
        return partitionModel;
    }

    public IModel<RoleAnalysisOutlierType> getOutlierModel() {
        return outlierModel;
    }

    private @NotNull IModel<List<WidgetItemModel>> loadDetailsModel() {
        RoleAnalysisOutlierPartitionType partition = getPartitionModel().getObject();
        List<DetectedAnomalyResultType> detectedAnomalyResult = partition.getDetectedAnomalyResult();

        List<WidgetItemModel> detailsModel = List.of(
                new WidgetItemModel(createStringResource(""),
                        Model.of("")) {
                    @Override
                    public Component createValueComponent(String id) {
                        Label label = new Label(id, detectedAnomalyResult.size());
                        label.add(AttributeAppender.append("class", " h4"));
                        return label;
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        return new Label(id, createStringResource("RoleAnalysisOutlierType.anomalyCount"));
                    }
                },

                new WidgetItemModel(createStringResource(""),
                        Model.of("")) {
                    @Override
                    public Component createValueComponent(String id) {
                        Double anomalyObjectsConfidence = partition.getPartitionAnalysis().getAnomalyObjectsConfidence();
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
                        return new Label(id,
                                createStringResource("RoleAnalysisOutlierType.anomalyAverageConfidence"));
                    }
                }

        );

        return Model.ofList(detailsModel);
    }

}
