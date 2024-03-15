/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables;

import static com.evolveum.midpoint.common.mining.utils.ExtractPatternUtils.transformDefaultPattern;
import static com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil.createDisplayType;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_DETECTED_PATER_ID;
import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;
import static com.evolveum.midpoint.web.component.data.mining.RoleAnalysisCollapsableTablePanel.*;

import java.io.Serial;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.RepeatingAttributeForm;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisAttributeChartPopupPanel;

import com.evolveum.midpoint.web.component.data.mining.RoleAnalysisCollapsableTablePanel;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.AjaxCompositedIconButton;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleApplicationDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleDto;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyHeaderPanel;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperHeaderModel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class RoleAnalysisDetectedPatternTable extends BasePanel<String> {

    private static final String ID_DATATABLE = "datatable";
    boolean isPopup;
    private static final String DOT_CLASS = RoleAnalysisDetectedPatternTable.class.getName() + ".";
    private static final String OP_PREPARE_OBJECTS = DOT_CLASS + "prepareObjects";
    OperationResult result = new OperationResult(OP_PREPARE_OBJECTS);

    //TODO brutal ugly just for fast solution (clusterModel)
    LoadableDetachableModel<ObjectDetailsModels<RoleAnalysisClusterType>> clusterModel;

    public RoleAnalysisDetectedPatternTable(
            String id,
            LoadableDetachableModel<List<DetectedPattern>> detectedPatternList,
            ObjectDetailsModels<RoleAnalysisClusterType> cluster, boolean isPopup) {
        super(id);
        this.isPopup = isPopup;

        clusterModel = new LoadableDetachableModel<>() {
            @Override
            protected ObjectDetailsModels<RoleAnalysisClusterType> load() {
                return cluster;
            }
        };

        RoleMiningProvider<DetectedPattern> provider = new RoleMiningProvider<>(
                this, new ListModel<>(detectedPatternList.getObject()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<DetectedPattern> object) {
                super.setObject(object);
            }

        }, true);

        provider.setSort(DetectedPattern.F_METRIC, SortOrder.DESCENDING);

        RoleAnalysisCollapsableTablePanel<DetectedPattern> table = new RoleAnalysisCollapsableTablePanel<>(
                ID_DATATABLE, provider, initColumns(false)) {

            @Override
            protected Item<DetectedPattern> newRowItem(String id, int index, Item<DetectedPattern> item, @NotNull IModel<DetectedPattern> rowModel) {
                DetectedPattern detectedPattern = null;
                RoleAnalysisAttributeAnalysisResult roleAnalysisResult = null;
                RoleAnalysisAttributeAnalysisResult userAnalysisResult = null;
                if (rowModel.getObject() != null) {
                    detectedPattern = rowModel.getObject();
                    roleAnalysisResult = detectedPattern.getRoleAttributeAnalysisResult();
                    userAnalysisResult = detectedPattern.getUserAttributeAnalysisResult();
                }

                WebMarkupContainer webMarkupContainerUser = new WebMarkupContainer(ID_FIRST_COLLAPSABLE_CONTAINER);
                webMarkupContainerUser.setOutputMarkupId(true);
                webMarkupContainerUser.add(AttributeModifier.replace("class", "collapse"));
                webMarkupContainerUser.add(AttributeModifier.replace("style", "display: none;"));
                item.add(webMarkupContainerUser);

                if (userAnalysisResult != null) {
                    RepeatingAttributeForm repeatingAttributeForm = new RepeatingAttributeForm(
                            ID_COLLAPSABLE_CONTENT, userAnalysisResult, detectedPattern.getUsers(), RoleAnalysisProcessModeType.USER);
                    repeatingAttributeForm.setOutputMarkupId(true);
                    webMarkupContainerUser.add(repeatingAttributeForm);
                } else {
                    Label label = new Label(ID_COLLAPSABLE_CONTENT, "No data available");
                    label.setOutputMarkupId(true);
                    webMarkupContainerUser.add(label);
                }

                WebMarkupContainer webMarkupContainerRole = new WebMarkupContainer(ID_SECOND_COLLAPSABLE_CONTAINER);
                webMarkupContainerRole.setOutputMarkupId(true);
                webMarkupContainerRole.add(AttributeModifier.replace("class", "collapse"));
                webMarkupContainerRole.add(AttributeModifier.replace("style", "display: none;"));
                item.add(webMarkupContainerRole);

                if (roleAnalysisResult != null) {
                    RepeatingAttributeForm repeatingAttributeForm = new RepeatingAttributeForm(
                            ID_COLLAPSABLE_CONTENT, roleAnalysisResult, detectedPattern.getRoles(), RoleAnalysisProcessModeType.ROLE);
                    repeatingAttributeForm.setOutputMarkupId(true);
                    webMarkupContainerRole.add(repeatingAttributeForm);
                } else {
                    Label label = new Label(ID_COLLAPSABLE_CONTENT, "No data available");
                    label.setOutputMarkupId(true);
                    webMarkupContainerRole.add(label);
                }

                return customizeNewRowItem(item, rowModel);
            }

            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {
                AjaxIconButton refreshIcon = new AjaxIconButton(id, new Model<>(GuiStyleConstants.CLASS_RECONCILE),
                        createStringResource("MainObjectListPanel.refresh")) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        onRefresh(target);
                    }
                };
                refreshIcon.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
                return refreshIcon;
            }
        };
        table.setOutputMarkupId(true);
        table.getDataTable().setItemsPerPage(10);
        table.enableSavePageSize();

        add(table);
    }

    public RoleAnalysisDetectedPatternTable(
            String id, RoleAnalysisSessionType session, boolean isPopup, boolean isTopPatterns) {
        super(id);
        this.isPopup = false;

        LoadableDetachableModel<List<DetectedPattern>> loadableDetachableModel = new LoadableDetachableModel<>() {
            @Override
            protected List<DetectedPattern> load() {
                return getTopPatterns(session);
            }
        };

        RoleMiningProvider<DetectedPattern> provider = new RoleMiningProvider<>(
                this, loadableDetachableModel, true);

        provider.setSort(DetectedPattern.F_METRIC, SortOrder.DESCENDING);

        RoleAnalysisCollapsableTablePanel<DetectedPattern> table = new RoleAnalysisCollapsableTablePanel<>(
                ID_DATATABLE, provider, initColumns(isTopPatterns)) {

            @Override
            protected Item<DetectedPattern> newRowItem(String id, int index, Item<DetectedPattern> item, @NotNull IModel<DetectedPattern> rowModel) {
                DetectedPattern detectedPattern = null;
                RoleAnalysisAttributeAnalysisResult roleAnalysisResult = null;
                RoleAnalysisAttributeAnalysisResult userAnalysisResult = null;
                if (rowModel.getObject() != null) {
                    detectedPattern = rowModel.getObject();
                    roleAnalysisResult = detectedPattern.getRoleAttributeAnalysisResult();
                    userAnalysisResult = detectedPattern.getUserAttributeAnalysisResult();
                }

                WebMarkupContainer webMarkupContainerUser = new WebMarkupContainer(ID_FIRST_COLLAPSABLE_CONTAINER);
                webMarkupContainerUser.setOutputMarkupId(true);
                webMarkupContainerUser.add(AttributeModifier.replace("class", "collapse"));
                webMarkupContainerUser.add(AttributeModifier.replace("style", "display: none;"));
                item.add(webMarkupContainerUser);

                if (userAnalysisResult != null) {
                    RepeatingAttributeForm repeatingAttributeForm = new RepeatingAttributeForm(
                            ID_COLLAPSABLE_CONTENT, userAnalysisResult, detectedPattern.getUsers(), RoleAnalysisProcessModeType.USER);
                    repeatingAttributeForm.setOutputMarkupId(true);
                    webMarkupContainerUser.add(repeatingAttributeForm);
                } else {
                    Label label = new Label(ID_COLLAPSABLE_CONTENT, "No data available");
                    label.setOutputMarkupId(true);
                    webMarkupContainerUser.add(label);
                }

                WebMarkupContainer webMarkupContainerRole = new WebMarkupContainer(ID_SECOND_COLLAPSABLE_CONTAINER);
                webMarkupContainerRole.setOutputMarkupId(true);
                webMarkupContainerRole.add(AttributeModifier.replace("class", "collapse"));
                webMarkupContainerRole.add(AttributeModifier.replace("style", "display: none;"));
                item.add(webMarkupContainerRole);

                if (roleAnalysisResult != null) {
                    RepeatingAttributeForm repeatingAttributeForm = new RepeatingAttributeForm(
                            ID_COLLAPSABLE_CONTENT, roleAnalysisResult, detectedPattern.getRoles(), RoleAnalysisProcessModeType.ROLE);
                    repeatingAttributeForm.setOutputMarkupId(true);
                    webMarkupContainerRole.add(repeatingAttributeForm);
                } else {
                    Label label = new Label(ID_COLLAPSABLE_CONTENT, "No data available");
                    label.setOutputMarkupId(true);
                    webMarkupContainerRole.add(label);
                }

                return customizeNewRowItem(item, rowModel);
            }

            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {
                AjaxIconButton refreshIcon = new AjaxIconButton(id, new Model<>(GuiStyleConstants.CLASS_RECONCILE),
                        createStringResource("MainObjectListPanel.refresh")) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        onRefresh(target);
                    }
                };
                refreshIcon.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
                return refreshIcon;
            }
        };
        table.setOutputMarkupId(true);
        table.getDataTable().setItemsPerPage(10);
        table.enableSavePageSize();

        add(table);
    }

    private @NotNull List<DetectedPattern> getTopPatterns(RoleAnalysisSessionType session) {
        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();

        Task task = getPageBase().createSimpleTask("getTopPatterns");
        OperationResult result = task.getResult();
        List<PrismObject<RoleAnalysisClusterType>> prismObjects = roleAnalysisService.searchSessionClusters(session, task, result);

        List<DetectedPattern> topDetectedPatterns = new ArrayList<>();
        for (PrismObject<RoleAnalysisClusterType> prismObject : prismObjects) {
            List<DetectedPattern> detectedPatterns = transformDefaultPattern(prismObject.asObjectable());

            double maxOverallConfidence = 0;
            DetectedPattern topDetectedPattern = null;
            for (DetectedPattern detectedPattern : detectedPatterns) {
                double itemsConfidence = detectedPattern.getItemsConfidence();
                double reductionFactorConfidence = detectedPattern.getReductionFactorConfidence();
                double overallConfidence = itemsConfidence + reductionFactorConfidence;
                if (overallConfidence > maxOverallConfidence) {
                    maxOverallConfidence = overallConfidence;
                    topDetectedPattern = detectedPattern;
                }
            }
            if (topDetectedPattern != null) {
                topDetectedPatterns.add(topDetectedPattern);
            }

        }
        return topDetectedPatterns;
    }

    public RoleAnalysisDetectedPatternTable(
            String id,
            LoadableDetachableModel<List<DetectedPattern>> detectedPatternList,
            boolean isPopup) {
        super(id);
        this.isPopup = isPopup;

        RoleMiningProvider<DetectedPattern> provider = new RoleMiningProvider<>(
                this, new ListModel<>(detectedPatternList.getObject()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<DetectedPattern> object) {
                super.setObject(object);
            }

        }, true);

        provider.setSort(DetectedPattern.F_METRIC, SortOrder.DESCENDING);

        BoxedTablePanel<DetectedPattern> table = new BoxedTablePanel<>(
                ID_DATATABLE, provider, initColumns(isPopup)) {
            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {
                AjaxIconButton refreshIcon = new AjaxIconButton(id, new Model<>(GuiStyleConstants.CLASS_RECONCILE),
                        createStringResource("MainObjectListPanel.refresh")) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        onRefresh(target);
                    }
                };
                refreshIcon.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
                return refreshIcon;
            }
        };
        table.setOutputMarkupId(true);
        table.getDataTable().setItemsPerPage(10);
        table.enableSavePageSize();

        add(table);
    }

    public List<IColumn<DetectedPattern, String>> initColumns(boolean isTopPatterns) {

        List<IColumn<DetectedPattern, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<DetectedPattern> rowModel) {

                return createDisplayType(GuiStyleConstants.CLASS_OBJECT_ROLE_ICON, "green", "");
            }
        });

//        columns.add(new AbstractColumn<>(getHeaderTitle("")) {
//
//            @Override
//            public String getSortProperty() {
//                return DetectedPattern.F_METRIC;
//            }
//
//            @Override
//            public boolean isSortable() {
//                return true;
//            }
//
//            @Override
//            public void populateItem(Item<ICellPopulator<DetectedPattern>> item, String componentId,
//                    IModel<DetectedPattern> rowModel) {
//                if (rowModel.getObject() != null) {
//                    item.add(new Label(componentId, rowModel.getObject().getMetric()));
//                }
//            }
//
//            @Override
//            public Component getHeader(String componentId) {
//                return createColumnHeader(componentId, containerDefinitionModel
//                );
//
//            }
//
//        });

        columns.add(new AbstractColumn<>(getHeaderTitle("")) {

            @Override
            public String getSortProperty() {
                return DetectedPattern.F_METRIC;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<DetectedPattern>> item, String componentId,
                    IModel<DetectedPattern> rowModel) {
                if (rowModel.getObject() != null) {
                    double reductionFactorConfidence = rowModel.getObject().getReductionFactorConfidence();
                    String formattedReductionFactorConfidence = String.format("%.2f", reductionFactorConfidence);
                    double metric = rowModel.getObject().getMetric();
                    String formattedMetric = String.format("%.2f", metric);
                    item.add(new Label(componentId, formattedReductionFactorConfidence + "% (" + formattedMetric + ")"));
                }
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(
                        componentId,
                        createStringResource("RoleMining.cluster.table.column.header.reduction.factor.confidence"));

            }

        });

        columns.add(new AbstractColumn<>(getHeaderTitle("")) {

            @Override
            public String getSortProperty() {
                return DetectedPattern.F_METRIC;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<DetectedPattern>> item, String componentId,
                    IModel<DetectedPattern> rowModel) {
                if (rowModel.getObject() != null) {

                    double itemsConfidence = rowModel.getObject().getItemsConfidence();
                    String formattedItemsConfidence = String.format("%.2f", itemsConfidence);
                    item.add(new Label(componentId, formattedItemsConfidence + "%"));
                }
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(
                        componentId,
                        createStringResource("RoleMining.cluster.table.column.header.items.confidence"));

            }

        });

        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<DetectedPattern>> item, String componentId,
                    IModel<DetectedPattern> rowModel) {

                if (rowModel.getObject() != null && rowModel.getObject().getRoles() != null) {
                    int rolesCount = rowModel.getObject().getRoles().size();

                    CompositedIconBuilder iconBuilder = new CompositedIconBuilder()
                            .setBasicIcon(GuiStyleConstants.CLASS_OBJECT_ROLE_ICON, LayeredIconCssStyle.IN_ROW_STYLE);

                    AjaxCompositedIconButton objectButton = new AjaxCompositedIconButton(componentId, iconBuilder.build(),
                            Model.of(String.valueOf(rolesCount))) {

                        @Serial private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            Component collapseContainer = item.findParent(Item.class).get(ID_SECOND_COLLAPSABLE_CONTAINER);
                            Component collapseContainerUser = item.findParent(Item.class).get(ID_FIRST_COLLAPSABLE_CONTAINER);

                            target.appendJavaScript(getCollapseScript(collapseContainer, collapseContainerUser));
                        }

                    };
                    objectButton.titleAsLabel(true);
                    objectButton.add(AttributeAppender.append("class", "btn btn-outline-success btn-sm"));
                    objectButton.add(AttributeAppender.append("style", "width:150px"));

                    if (isPopup) {
                        objectButton.setEnabled(false);
                    }

                    item.add(objectButton);
                } else {
                    item.add(new EmptyPanel(componentId));
                }
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(
                        componentId, createStringResource("RoleMining.cluster.table.column.header.role.analysis"));
            }

        });

        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<DetectedPattern>> item, String componentId,
                    IModel<DetectedPattern> rowModel) {

                if (rowModel.getObject() != null && rowModel.getObject().getUsers() != null) {
                    int usersCount = rowModel.getObject().getUsers().size();

                    CompositedIconBuilder iconBuilder = new CompositedIconBuilder()
                            .setBasicIcon(GuiStyleConstants.CLASS_OBJECT_USER_ICON, LayeredIconCssStyle.IN_ROW_STYLE);

                    AjaxCompositedIconButton objectButton = new AjaxCompositedIconButton(componentId, iconBuilder.build(),
                            Model.of(String.valueOf(usersCount))) {

                        @Serial private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            Component collapseContainer = item.findParent(Item.class).get(ID_FIRST_COLLAPSABLE_CONTAINER);
                            Component collapseContainerRole = item.findParent(Item.class).get(ID_SECOND_COLLAPSABLE_CONTAINER);

                            target.appendJavaScript(getCollapseScript(collapseContainer, collapseContainerRole));

                        }

                    };
                    objectButton.titleAsLabel(true);
                    objectButton.add(AttributeAppender.append("class", "btn btn-outline-danger btn-sm"));
                    objectButton.add(AttributeAppender.append("style", "width:150px"));

                    if (isPopup) {
                        objectButton.setEnabled(false);
                    }

                    item.add(objectButton);
                } else {
                    item.add(new EmptyPanel(componentId));
                }

            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(
                        componentId, createStringResource("RoleMining.cluster.table.column.header.user.analysis"));
            }

        });
        columns.add(new AbstractExportableColumn<>(getHeaderTitle("display")) {

            @Override
            public IModel<?> getDataModel(IModel<DetectedPattern> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<DetectedPattern>> item, String componentId,
                    IModel<DetectedPattern> rowModel) {

                if (rowModel.getObject() == null) {
                    item.add(new EmptyPanel(componentId));
                } else {
                    CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                            GuiStyleConstants.CLASS_PLUS_CIRCLE, LayeredIconCssStyle.IN_ROW_STYLE);
                    AjaxCompositedIconSubmitButton migrationButton = new AjaxCompositedIconSubmitButton(componentId,
                            iconBuilder.build(),
                            createStringResource("RoleMining.button.title.candidate")) {
                        @Serial private static final long serialVersionUID = 1L;

                        @Override
                        protected void onSubmit(AjaxRequestTarget target) {
                            Task task = getPageBase().createSimpleTask(OP_PREPARE_OBJECTS);
                            OperationResult result = task.getResult();
                            RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();

                            DetectedPattern object = rowModel.getObject();
                            ObjectReferenceType clusterRef = object.getClusterRef();
                            @NotNull String status = roleAnalysisService
                                    .recomputeAndResolveClusterOpStatus(clusterRef.getOid(), result, task);

                            if (status.equals("processing")) {
                                warn("Couldn't start detection. Some process is already in progress.");
                                LOGGER.error("Couldn't start detection. Some process is already in progress.");
                                target.add(getFeedbackPanel());
                                return;
                            }
                            DetectedPattern pattern = rowModel.getObject();
                            if (pattern == null) {
                                return;
                            }

                            Set<String> roles = pattern.getRoles();
                            Set<String> users = pattern.getUsers();
                            Long patternId = pattern.getId();

                            Set<RoleType> candidateInducements = new HashSet<>();

                            for (String roleOid : roles) {
                                PrismObject<RoleType> roleObject = roleAnalysisService
                                        .getRoleTypeObject(roleOid, task, result);
                                if (roleObject != null) {
                                    candidateInducements.add(roleObject.asObjectable());
                                }
                            }

                            PrismObject<RoleType> businessRole = roleAnalysisService
                                    .generateBusinessRole(new HashSet<>(), PolyStringType.fromOrig(""));

                            List<BusinessRoleDto> roleApplicationDtos = new ArrayList<>();

                            for (String userOid : users) {
                                PrismObject<UserType> userObject = roleAnalysisService
                                        .getUserTypeObject(userOid, task, result);
                                if (userObject != null) {
                                    roleApplicationDtos.add(new BusinessRoleDto(userObject,
                                            businessRole, candidateInducements, getPageBase()));
                                }
                            }

                            PrismObject<RoleAnalysisClusterType> prismObjectCluster = roleAnalysisService
                                    .getClusterTypeObject(clusterRef.getOid(), task, result);

                            BusinessRoleApplicationDto operationData = new BusinessRoleApplicationDto(
                                    prismObjectCluster, businessRole, roleApplicationDtos, candidateInducements);
                            operationData.setPatternId(patternId);

                            PageRole pageRole = new PageRole(operationData.getBusinessRole(), operationData);
                            setResponsePage(pageRole);
                        }

                        @Override
                        protected void onError(AjaxRequestTarget target) {
                            target.add(((PageBase) getPage()).getFeedbackPanel());
                        }
                    };
                    migrationButton.titleAsLabel(true);
                    migrationButton.setOutputMarkupId(true);
                    migrationButton.add(AttributeAppender.append("class", "btn btn-success btn-sm"));

                    item.add(migrationButton);
                }

            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, getHeaderTitle("display"));
            }

        });

        if (!isTopPatterns) {
            columns.add(new AbstractColumn<>(createStringResource("chart")) {

                @Override
                public boolean isSortable() {
                    return false;
                }

                @Override
                public void populateItem(Item<ICellPopulator<DetectedPattern>> item, String componentId,
                        IModel<DetectedPattern> rowModel) {

                    if (rowModel.getObject() != null && rowModel.getObject().getRoleAttributeAnalysisResult() != null
                            && rowModel.getObject().getUserAttributeAnalysisResult() != null) {
                        RoleAnalysisAttributeAnalysisResult roleAnalysisResult = rowModel.getObject().getRoleAttributeAnalysisResult();
                        RoleAnalysisAttributeAnalysisResult userAnalysisResult = rowModel.getObject().getUserAttributeAnalysisResult();
                        DetectedPattern detectedPattern = rowModel.getObject();
                        CompositedIconBuilder iconBuilder = new CompositedIconBuilder()
                                .setBasicIcon(GuiStyleConstants.CLASS_OBJECT_ROLE_ICON, LayeredIconCssStyle.IN_ROW_STYLE);

                        AjaxCompositedIconButton objectButton = new AjaxCompositedIconButton(componentId, iconBuilder.build(),
                                Model.of(createStringResource("RoleMining.button.title.chart").getString())) {

                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            public void onClick(AjaxRequestTarget target) {

                                RoleAnalysisAttributeChartPopupPanel detailsPanel = new RoleAnalysisAttributeChartPopupPanel(
                                        ((PageBase) getPage()).getMainPopupBodyId(),
                                        Model.of("Analyzed members details panel"),
                                        roleAnalysisResult, userAnalysisResult, clusterModel, detectedPattern) {
                                    @Override
                                    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                                        super.onClose(ajaxRequestTarget);
                                    }
                                };
                                ((PageBase) getPage()).showMainPopup(detailsPanel, target);
                            }

                        };
                        objectButton.titleAsLabel(true);
                        objectButton.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
                        objectButton.add(AttributeAppender.append("style", "width:150px"));
                        item.add(objectButton);
                        if (isPopup) {
                            objectButton.setEnabled(false);
                        }
                    } else {
                        item.add(new EmptyPanel(componentId));
                    }

                }

                @Override
                public Component getHeader(String componentId) {
                    return new Label(
                            componentId, createStringResource("RoleMining.cluster.table.column.header.attribute.statistics"));
                }

            });
        }
        columns.add(new AbstractExportableColumn<>(createStringResource("RoleMining.button.title.load")) {

            @Override
            public IModel<?> getDataModel(IModel<DetectedPattern> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<DetectedPattern>> item, String componentId,
                    IModel<DetectedPattern> rowModel) {

                if (rowModel.getObject() == null) {
                    item.add(new EmptyPanel(componentId));
                }

                CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                        GuiStyleConstants.CLASS_ICON_SEARCH, LayeredIconCssStyle.IN_ROW_STYLE);
                AjaxCompositedIconSubmitButton migrationButton = new AjaxCompositedIconSubmitButton(componentId,
                        iconBuilder.build(),
                        createStringResource("RoleMining.button.title.load")) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        PageParameters parameters = new PageParameters();
                        String clusterOid = rowModel.getObject().getClusterRef().getOid();
                        parameters.add(OnePageParameterEncoder.PARAMETER, clusterOid);
                        parameters.add("panelId", "clusterDetails");
                        parameters.add(PARAM_DETECTED_PATER_ID, rowModel.getObject().getId());
                        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                                .getObjectDetailsPage(RoleAnalysisClusterType.class);
                        getPageBase().navigateToNext(detailsPageClass, parameters);
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target) {
                        target.add(((PageBase) getPage()).getFeedbackPanel());
                    }
                };
                migrationButton.titleAsLabel(true);
                migrationButton.setOutputMarkupId(true);
                migrationButton.add(AttributeAppender.append("class", "btn btn-primary btn-sm"));

                item.add(migrationButton);
            }

        });

        return columns;
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    protected DataTable<?, ?> getDataTable() {
        return ((BoxedTablePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE))).getDataTable();
    }

    protected BoxedTablePanel<?> getTable() {
        return ((BoxedTablePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE)));
    }

    protected StringResourceModel getHeaderTitle(String identifier) {
        return createStringResource("RoleMining.cluster.table.column.header." + identifier);
    }

    private <C extends Containerable> PrismPropertyHeaderPanel<?> createColumnHeader(String componentId,
            LoadableModel<PrismContainerDefinition<C>> containerDefinitionModel) {

        return new PrismPropertyHeaderPanel<>(componentId, new PrismPropertyWrapperHeaderModel<>(
                containerDefinitionModel,
                RoleAnalysisDetectionPatternType.F_CLUSTER_METRIC,
                (PageBase) getPage())) {

            @Override
            protected boolean isAddButtonVisible() {
                return false;
            }

            @Override
            protected boolean isButtonEnabled() {
                return false;
            }
        };
    }

    protected void onRefresh(AjaxRequestTarget target) {

    }
}
