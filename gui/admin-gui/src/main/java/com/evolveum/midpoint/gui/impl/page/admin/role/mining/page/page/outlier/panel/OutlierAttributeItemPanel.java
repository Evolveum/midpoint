/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.outlier.panel;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.WidgetItemModel;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisAttributesDto;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.impl.component.menu.listGroup.ListGroupMenuItem;
import com.evolveum.midpoint.gui.impl.component.menu.listGroup.MenuItemLinkPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisWidgetsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisAttributePanel;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;

public class OutlierAttributeItemPanel<T extends Serializable>
        extends BasePanel<ListGroupMenuItem<T>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_LINK = "link";

    private final IModel<RoleAnalysisOutlierPartitionType> partitionModel;
    private final IModel<RoleAnalysisOutlierType> outlierModel;

    public OutlierAttributeItemPanel(@NotNull String id,
            @NotNull IModel<ListGroupMenuItem<T>> model,
            @NotNull IModel<RoleAnalysisOutlierPartitionType> selectionModel,
            @NotNull IModel<RoleAnalysisOutlierType> outlierModel) {
        super(id, model);

        this.partitionModel = selectionModel;
        this.outlierModel = outlierModel;
        initLayout();
    }

    private void initLayout() {
        add(AttributeModifier.append(CLASS_CSS, () -> getModelObject().isOpen() ? "open" : null));
        MenuItemLinkPanel<?> link = new MenuItemLinkPanel<>(ID_LINK, getModel(), 0) {
            @Override
            protected boolean isChevronLinkVisible() {
                return false;
            }

            @SuppressWarnings("rawtypes")
            @Override
            protected void onClickPerformed(AjaxRequestTarget target, ListGroupMenuItem item) {
                OutlierAttributeItemPanel.this.onClickPerformed(target, getDetailsPanelComponent());
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
        Task task = getPageBase().createSimpleTask("loadOutlierDetails");

        RoleAnalysisWidgetsPanel detailsPanel = loadDetailsPanel(id, task);
        detailsPanel.setOutputMarkupId(true);
        return detailsPanel;
    }

    @NotNull
    private RoleAnalysisWidgetsPanel loadDetailsPanel(@NotNull String id, Task task) {

        RoleAnalysisOutlierPartitionType partition = getPartitionModel().getObject();
        AttributeAnalysisType attributeAnalysis = partition.getPartitionAnalysis().getAttributeAnalysis();
        if (attributeAnalysis == null) {
            return new RoleAnalysisWidgetsPanel(id, loadDetailsModel());
        }

        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
        ObjectReferenceType targetSessionRef = partition.getTargetSessionRef();

        RoleAnalysisOutlierType outlier = getOutlierModel().getObject();
        ObjectReferenceType targetUserRef = outlier.getObjectRef();
        PrismObject<RoleAnalysisSessionType> session = roleAnalysisService.getSessionTypeObject(
                targetSessionRef.getOid(), task, task.getResult());
        PrismObject<UserType> userPrismObject = roleAnalysisService.getUserTypeObject(
                targetUserRef.getOid(), task, task.getResult());

        Set<String> userPathToMark = new HashSet<>();
        if (session != null && userPrismObject != null) {
            List<RoleAnalysisAttributeDef> attributesForUserAnalysis = roleAnalysisService.resolveAnalysisAttributes(
                    session.asObjectable(), UserType.COMPLEX_TYPE);
            if (attributesForUserAnalysis != null) {
                userPathToMark = roleAnalysisService.resolveUserValueToMark(userPrismObject, attributesForUserAnalysis);
            }
        }

        Set<String> finalUserPathToMark = userPathToMark;

        return new RoleAnalysisWidgetsPanel(id, loadDetailsModel()) {
            @Override
            protected @NotNull Component getPanelComponent(String id1) {
                LoadableModel<RoleAnalysisAttributesDto> attributesModel = new LoadableModel<>(false) {
                    @Override
                    protected RoleAnalysisAttributesDto load() {
                        return RoleAnalysisAttributesDto.fromPartitionAttributeAnalysis("Role analysis attribute panel", getPartitionModel().getObject());
                    }
                };
                RoleAnalysisAttributePanel roleAnalysisAttributePanel = new RoleAnalysisAttributePanel(id1,
                        attributesModel) {

                    @Override
                    protected @NotNull String getChartContainerStyle() {
                        return "min-height:350px;";
                    }

                    @Override
                    public Set<String> getPathToMark() {
                        return finalUserPathToMark;
                    }
                };

                roleAnalysisAttributePanel.setOutputMarkupId(true);
                return roleAnalysisAttributePanel;
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
        RoleAnalysisPartitionAnalysisType partitionAnalysis = partition.getPartitionAnalysis();
        AttributeAnalysisType attributeAnalysis = partitionAnalysis.getAttributeAnalysis();
        if (attributeAnalysis == null) {
            return Model.ofList(List.of());
        }
        RoleAnalysisAttributeAnalysisResultType userAttributeAnalysisResult = attributeAnalysis.getUserAttributeAnalysisResult();
        RoleAnalysisAttributeAnalysisResultType clusterCompare = attributeAnalysis.getUserClusterCompare();

        List<WidgetItemModel> detailsModel = List.of(
                new WidgetItemModel(createStringResource(""),
                        Model.of("")) {
                    @Override
                    public Component createValueComponent(String id) {
                        List<RoleAnalysisAttributeAnalysisType> attributeAnalysisCluster = userAttributeAnalysisResult.getAttributeAnalysis();

                        double itemsConfidence = resolveItemConfidence(attributeAnalysisCluster);

                        Label label = new Label(id, itemsConfidence + "%");
                        label.add(AttributeModifier.append(CLASS_CSS, " h4"));
                        return label;
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        return new LabelWithHelpPanel(id,
                                createStringResource("OutlierAttributeItemPanel.cluster.attribute.confidence"));
                    }
                },

                new WidgetItemModel(createStringResource(""),
                        Model.of("")) {
                    @Override
                    public Component createValueComponent(String id) {

                        List<RoleAnalysisAttributeAnalysisType> attributeAnalysisCluster = clusterCompare.getAttributeAnalysis();

                        double itemsConfidence = resolveItemConfidence(attributeAnalysisCluster);
                        Label label = new Label(id, itemsConfidence + "%");
                        label.add(AttributeModifier.append(CLASS_CSS, " h4"));
                        return label;
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        return new LabelWithHelpPanel(id,
                                createStringResource("OutlierAttributeItemPanel.outlier.vs.cluster.attribute.confidence"));
                    }
                },

                new WidgetItemModel(createStringResource(""),
                        Model.of("Sort")) {
                    @Override
                    public Component createValueComponent(String id) {
                        List<RoleAnalysisAttributeAnalysisType> attributeAnalysisCluster = clusterCompare.getAttributeAnalysis();

                        int items = 0;
                        if (attributeAnalysisCluster != null) {
                            items = attributeAnalysisCluster.size();
                        }

                        Label label = new Label(id, items);
                        label.add(AttributeModifier.append(CLASS_CSS, " h4"));
                        return label;
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        return new LabelWithHelpPanel(id,
                                createStringResource("OutlierAttributeItemPanel.analysed.attributes"));
                    }
                },

                new WidgetItemModel(createStringResource(""),
                        Model.of("Chart")) {
                    @Override
                    public Component createValueComponent(String id) {
                        Integer similarObjectsCount = partitionAnalysis.getSimilarObjectAnalysis().getSimilarObjectsCount();
                        if (similarObjectsCount == null) {
                            similarObjectsCount = 0;
                        }
                        Label label = new Label(id, similarObjectsCount);
                        label.add(AttributeModifier.append(CLASS_CSS, " h4"));
                        return label;
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        return new LabelWithHelpPanel(id,
                                createStringResource("OutlierAttributeItemPanel.analysed.users"));
                    }
                }
        );

        return Model.ofList(detailsModel);
    }

    private double resolveItemConfidence(List<RoleAnalysisAttributeAnalysisType> attributeAnalysisCluster) {
        double totalDensity = 0.0;
        int totalCount = 0;
        if (attributeAnalysisCluster != null) {
            totalDensity += calculateDensity(attributeAnalysisCluster);
            totalCount += attributeAnalysisCluster.size();
        }

        int itemCount = (attributeAnalysisCluster != null ? attributeAnalysisCluster.size() : 0);

        double itemsConfidence = (totalCount > 0 && totalDensity > 0.0 && itemCount > 0) ? totalDensity / itemCount : 0.0;

        BigDecimal bd = BigDecimal.valueOf(itemsConfidence);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        itemsConfidence = bd.doubleValue();
        return itemsConfidence;
    }

    private double calculateDensity(@NotNull List<RoleAnalysisAttributeAnalysisType> attributeAnalysisList) {
        double totalDensity = 0.0;
        for (RoleAnalysisAttributeAnalysisType attributeAnalysis : attributeAnalysisList) {
            Double density = attributeAnalysis.getDensity();
            if (density != null) {
                totalDensity += density;
            }
        }
        return totalDensity;
    }
}
