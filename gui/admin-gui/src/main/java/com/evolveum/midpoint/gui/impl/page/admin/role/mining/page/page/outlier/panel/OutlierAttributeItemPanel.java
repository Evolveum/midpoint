/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.outlier.panel;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel.generateUserOutlierResultModel;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.impl.component.menu.listGroup.ListGroupMenuItem;
import com.evolveum.midpoint.gui.impl.component.menu.listGroup.MenuItemLinkPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisWidgetsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisAttributePanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.DetailsTableItem;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
        add(AttributeAppender.append("class", () -> getModelObject().isOpen() ? "open" : null));
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
        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
        Task task = getPageBase().createSimpleTask("loadOutlierDetails");

        RoleAnalysisOutlierType outlier = getOutlierModel().getObject();
        RoleAnalysisOutlierPartitionType partition = getPartitionModel().getObject();

        //TODO!
        OutlierObjectModel outlierObjectModel = generateUserOutlierResultModel(roleAnalysisService, outlier,
                task, task.getResult(), partition, getPageBase());

        if (outlierObjectModel == null) {
            return new WebMarkupContainer(id);
        }

        RoleAnalysisWidgetsPanel detailsPanel = loadDetailsPanel(id, task);
        detailsPanel.setOutputMarkupId(true);
        return detailsPanel;
    }

    @NotNull
    private RoleAnalysisWidgetsPanel loadDetailsPanel(@NotNull String id, Task task) {

        RoleAnalysisOutlierPartitionType partition = getPartitionModel().getObject();
        AttributeAnalysis attributeAnalysis = partition.getPartitionAnalysis().getAttributeAnalysis();
        RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = attributeAnalysis.getUserAttributeAnalysisResult();
        RoleAnalysisAttributeAnalysisResult clusterCompare = attributeAnalysis.getUserClusterCompare();
        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
        ObjectReferenceType targetSessionRef = partition.getTargetSessionRef();

        RoleAnalysisOutlierType outlier = getOutlierModel().getObject();
        ObjectReferenceType targetUserRef = outlier.getTargetObjectRef();
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
                RoleAnalysisAttributePanel roleAnalysisAttributePanel = new RoleAnalysisAttributePanel(id1,
                        Model.of("Role analysis attribute panel"),
                        null, userAttributeAnalysisResult,
                        null, clusterCompare) {
                    @Override
                    protected @NotNull String getChartContainerStyle() {
                        return "height:30vh;";
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

    private @NotNull IModel<List<DetailsTableItem>> loadDetailsModel() {

        List<DetailsTableItem> detailsModel = List.of(
                new DetailsTableItem(createStringResource(""),
                        Model.of("")) {
                    @Override
                    public Component createValueComponent(String id) {
                        Label label = new Label(id, "0 (todo)");
                        label.add(AttributeAppender.append("class", " h4"));
                        return label;
                    }

                    @Override
                    public Component createLabelComponent(String id) {
                        return new LabelWithHelpPanel(id, createStringResource("RoleAnalysisOutlierType.anomalyCount")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisOutlierType.anomalyCount.help");
                            }
                        };
                    }
                },

                new DetailsTableItem(createStringResource(""),
                        Model.of("")) {
                    @Override
                    public Component createValueComponent(String id) {
                        Label label = new Label(id, "0 (todo)");
                        label.add(AttributeAppender.append("class", " h4"));
                        return label;
                    }

                    @Override
                    public Component createLabelComponent(String id) {
                        return new LabelWithHelpPanel(id,
                                createStringResource("RoleAnalysisOutlierType.anomalyAverageConfidence")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisOutlierType.anomalyAverageConfidence.help");
                            }
                        };
                    }
                },

                new DetailsTableItem(createStringResource(""),
                        Model.of("Sort")) {
                    @Override
                    public Component createValueComponent(String id) {
                        Label label = new Label(id, "0 (todo)");
                        label.add(AttributeAppender.append("class", " h4"));
                        return label;
                    }

                    @Override
                    public Component createLabelComponent(String id) {
                        return new LabelWithHelpPanel(id, Model.of("TBD")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisOutlierType.anomalyAverageConfidence.help");
                            }
                        };
                    }
                },

                new DetailsTableItem(createStringResource(""),
                        Model.of("Chart")) {
                    @Override
                    public Component createValueComponent(String id) {
                        Label label = new Label(id, "0 (todo)");
                        label.add(AttributeAppender.append("class", " h4"));
                        return label;
                    }

                    @Override
                    public Component createLabelComponent(String id) {
                        return new LabelWithHelpPanel(id, Model.of("TBD")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisOutlierType.anomalyAverageConfidence.help");
                            }
                        };
                    }
                }
        );

        return Model.ofList(detailsModel);
    }

}
