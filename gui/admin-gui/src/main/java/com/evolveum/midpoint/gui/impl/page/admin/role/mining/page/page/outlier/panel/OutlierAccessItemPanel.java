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
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.menu.listGroup.ListGroupMenuItem;
import com.evolveum.midpoint.gui.impl.component.menu.listGroup.MenuItemLinkPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisAccessTabPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisWidgetsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.WidgetItemModel;
import com.evolveum.midpoint.gui.impl.util.AccessMetadataUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class OutlierAccessItemPanel<T extends Serializable>
        extends BasePanel<ListGroupMenuItem<T>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_LINK = "link";

    private final IModel<RoleAnalysisOutlierPartitionType> partitionModel;
    private final IModel<RoleAnalysisOutlierType> outlierModel;

    public OutlierAccessItemPanel(@NotNull String id,
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
                OutlierAccessItemPanel.this.onClickPerformed(target, getDetailsPanelComponent());
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

        RoleAnalysisWidgetsPanel detailsPanel = loadDetailsPanel(id);
        detailsPanel.setOutputMarkupId(true);
        return detailsPanel;
    }

    private RoleAnalysisWidgetsPanel loadDetailsPanel(@NotNull String id) {

        return new RoleAnalysisWidgetsPanel(id, loadDetailsModel()) {
            @Override
            protected @NotNull Component getPanelComponent(String id1) {
                RoleAnalysisAccessTabPanel roleAnalysisAccessTabPanel = new RoleAnalysisAccessTabPanel(id1, getPartitionModel(),
                        getOutlierModel());
                roleAnalysisAccessTabPanel.setOutputMarkupId(true);

//        RoleAnalysisDetectedAnomalyTable detectedAnomalyTable = new RoleAnalysisDetectedAnomalyTable(id,
//                getOutlierModel().getObject(), getPartitionModel().getObject(), AnomalyTableCategory.OUTLIER_ACESS);
//        detectedAnomalyTable.setOutputMarkupId(true);
                return roleAnalysisAccessTabPanel;
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
        List<DetectedAnomalyResult> detectedAnomalyResult = partition.getDetectedAnomalyResult();

        RoleAnalysisOutlierType outlier = getOutlierModel().getObject();
        ObjectReferenceType targetObjectRef = outlier.getTargetObjectRef();
        String userOid = targetObjectRef.getOid();

        PageBase pageBase = getPageBase();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        Task simpleTask = pageBase.createSimpleTask("loadOutlierDetails");
        OperationResult result = simpleTask.getResult();
        PrismObject<UserType> prismUser = roleAnalysisService
                .getUserTypeObject(userOid, simpleTask, result);

        //TODO maybe think about collecting oids so you can later show them
        int directAssignment = 0;
        int indirectAssignment = 0;
        int duplicatedRoleAssignmentCount = 0;
        int allAssignmentCount = 0;

        if (prismUser != null) {
            UserType user = prismUser.asObjectable();
            List<ObjectReferenceType> refsToRoles = user.getRoleMembershipRef()
                    .stream()
                    .filter(ref -> QNameUtil.match(ref.getType(), RoleType.COMPLEX_TYPE)) //TODO maybe also check relation?
                    .toList();

            allAssignmentCount = refsToRoles.size();

            for (ObjectReferenceType ref : refsToRoles) {
                List<AssignmentPathMetadataType> metadataPaths = AccessMetadataUtil.computeAssignmentPaths(ref);
                if (metadataPaths.size() == 1) {
                    List<AssignmentPathSegmentMetadataType> segments = metadataPaths.get(0).getSegment();
                    if (CollectionUtils.isEmpty(segments) || segments.size() == 1) {
                        directAssignment++;
                    } else {
                        indirectAssignment++;
                    }
                } else {
                    boolean foundDirect = false;
                    boolean foundIndirect = false;
                    for (AssignmentPathMetadataType metadata : metadataPaths) {
                        List<AssignmentPathSegmentMetadataType> segments = metadata.getSegment();
                        if (CollectionUtils.isEmpty(segments) || segments.size() == 1) {
                            foundDirect = true;
                            if (foundIndirect) {
                                indirectAssignment--;
                                duplicatedRoleAssignmentCount++;
                            } else {
                                directAssignment++;
                            }

                        } else {
                            foundIndirect = true;
                            if (foundDirect) {
                                directAssignment--;
                                duplicatedRoleAssignmentCount++;
                            } else {
                                indirectAssignment++;
                            }
                        }
                    }
                }

            }

        }

        int finalDirectAssignment = directAssignment;
        int finalIndirectAssignment = indirectAssignment;
        int finalDuplicatedRoleAssignmentCount = duplicatedRoleAssignmentCount;
        int finalAllAssignmentCount = allAssignmentCount;

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

                        Label label = new Label(id, finalDirectAssignment);
                        label.add(AttributeAppender.append("class", " h4"));
                        return label;
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        return new LabelWithHelpPanel(id,
                                createStringResource("Direct access")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisOutlierType.anomalyAverageConfidence.help");
                            }
                        };
                    }
                },

                new WidgetItemModel(createStringResource(""),
                        Model.of("Sort")) {
                    @Override
                    public Component createValueComponent(String id) {
                        Label label = new Label(id, finalIndirectAssignment);
                        label.add(AttributeAppender.append("class", " h4"));
                        return label;
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        return new LabelWithHelpPanel(id, Model.of("Indirect access")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisOutlierType.anomalyAverageConfidence.help");
                            }
                        };
                    }
                },

                new WidgetItemModel(createStringResource(""),
                        Model.of("Chart")) {
                    @Override
                    public Component createValueComponent(String id) {
                        Label label = new Label(id, finalDuplicatedRoleAssignmentCount);
                        label.add(AttributeAppender.append("class", " h4"));
                        return label;
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        return new LabelWithHelpPanel(id, Model.of("Duplicated access")) {
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
