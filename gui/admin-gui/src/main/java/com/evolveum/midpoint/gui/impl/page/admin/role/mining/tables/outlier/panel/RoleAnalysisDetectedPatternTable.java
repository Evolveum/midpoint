/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel;

import static com.evolveum.midpoint.common.mining.utils.ExtractPatternUtils.transformDefaultPattern;
import static com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil.createDisplayType;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_DETECTED_PATER_ID;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_TABLE_SETTING;
import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;
import static com.evolveum.midpoint.web.component.data.mining.RoleAnalysisCollapsableTablePanel.*;

import java.io.Serial;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisAttributesDto;

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
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.AjaxCompositedIconButton;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleApplicationDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisAttributePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisClusterOccupationPanel;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.mining.CollapsableContainerPanel;
import com.evolveum.midpoint.web.component.data.mining.RoleAnalysisCollapsableTablePanel;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisDetectedPatternTable extends BasePanel<String> {

    private static final String ID_DATATABLE = "datatable";
    private static final String DOT_CLASS = RoleAnalysisDetectedPatternTable.class.getName() + ".";
    private static final String OP_PREPARE_OBJECTS = DOT_CLASS + "prepareObjects";

    PageBase pageBase;

    public RoleAnalysisDetectedPatternTable(
            @NotNull String id,
            @NotNull PageBase pageBase,
            @NotNull LoadableDetachableModel<List<DetectedPattern>> detectedPatternList) {
        super(id);
        this.pageBase = pageBase;
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
                ID_DATATABLE, provider, initColumns()) {

            @Override
            public String getAdditionalBoxCssClasses() {
                return " m-0";
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
            @NotNull String id,
            @NotNull PageBase pageBase,
            @NotNull RoleAnalysisSessionType session) {
        super(id);
        this.pageBase = pageBase;

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
                ID_DATATABLE, provider, initColumns()) {

            @Override
            public String getAdditionalBoxCssClasses() {
                return " m-0";
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

    public List<IColumn<DetectedPattern, String>> initColumns() {

        List<IColumn<DetectedPattern, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<DetectedPattern> rowModel) {

                return createDisplayType(GuiStyleConstants.CLASS_OBJECT_ROLE_ICON, "green", "");
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
                    double reductionFactorConfidence = rowModel.getObject().getReductionFactorConfidence();
                    String formattedReductionFactorConfidence = String.format("%.2f", reductionFactorConfidence);
                    double metric = rowModel.getObject().getMetric();
                    String formattedMetric = String.format("%.2f", metric);

                    IconWithLabel icon = new IconWithLabel(componentId, Model.of(formattedReductionFactorConfidence + "% ")) {
                        @Override
                        public String getIconCssClass() {
                            return "fa fa-arrow-down";
                        }

                        @Override
                        protected Component getSubComponent(String id) {
                            Label label = new Label(id, Model.of("(" + formattedMetric + ")"));
                            label.add(AttributeAppender.append("class", "text-muted"));
                            return label;
                        }
                    };

                    item.add(icon);

                }
            }

            @Override
            public Component getHeader(String componentId) {
                return new LabelWithHelpPanel(componentId,
                        createStringResource("RoleMining.cluster.table.column.header.reduction.factor.confidence")) {
                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleAnalysisCluster.table.header.cluster.attribute.statistic.help");
                    }
                };

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

                DetectedPattern pattern = rowModel.getObject();
                if (pattern == null || pattern.getRoles() == null || pattern.getUsers() == null) {
                    item.add(new EmptyPanel(componentId));
                    return;
                }

                IModel<String> roleObjectCount = Model.of(String.valueOf(pattern.getRoles().size()));
                IModel<String> userObjectCount = Model.of(String.valueOf(pattern.getUsers().size()));

                RoleAnalysisClusterOccupationPanel occupationPanel = new RoleAnalysisClusterOccupationPanel(componentId) {
                    @Override
                    public Component createFirstPanel(String idFirstPanel) {
                        return new IconWithLabel(idFirstPanel, userObjectCount) {
                            @Override
                            public String getIconCssClass() {
                                return "fa fa-user object-user-color";
                            }
                        };
                    }

                    @Override
                    public Component createSecondPanel(String idSecondPanel) {
                        return new IconWithLabel(idSecondPanel, roleObjectCount) {
                            @Override
                            public String getIconCssClass() {
                                return "fe fe-role object-role-color d-block";
                            }

                            @Override
                            protected String getIconComponentCssStyle() {
                                return "font-size:18px!important;line-height:1;";
                            }
                        };
                    }

                    @Override
                    public Component createSeparatorPanel(String idSeparatorPanel) {
                        Label separator = new Label(idSeparatorPanel, "/");
                        separator.add(AttributeModifier.replace("class",
                                "d-flex align-items-center"));
                        separator.setOutputMarkupId(true);
                        add(separator);
                        return separator;
                    }
                };

                occupationPanel.setOutputMarkupId(true);
                item.add(occupationPanel);

            }

            @Override
            public Component getHeader(String componentId) {
                return new LabelWithHelpPanel(componentId,
                        createStringResource("RoleAnalysisCluster.table.header.cluster.occupation")) {
                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleAnalysisCluster.table.header.cluster.attribute.statistic.help");
                    }
                };
            }

        });

        columns.add(new AbstractColumn<>(
                createStringResource("")) {

            @Override
            public Component getHeader(String componentId) {
                return new LabelWithHelpPanel(componentId,
                        createStringResource("RoleAnalysisCluster.table.header.cluster.attribute.statistic")) {
                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleAnalysisCluster.table.header.cluster.attribute.statistic.help");
                    }
                };
            }

            @Override
            public void populateItem(Item<ICellPopulator<DetectedPattern>> cellItem, String componentId, IModel<DetectedPattern> model) {
                String confidence = "";
                if (model.getObject() != null) {
                    DetectedPattern pattern = model.getObject();
                    confidence = String.format("%.2f", pattern.getItemsConfidence()) + "%";
                } else {
                    cellItem.add(new EmptyPanel(componentId));
                }

                CompositedIconBuilder iconBuilder = new CompositedIconBuilder()
                        .setBasicIcon("fas fa-chart-bar", LayeredIconCssStyle.IN_ROW_STYLE);

                AjaxCompositedIconButton objectButton = new AjaxCompositedIconButton(componentId, iconBuilder.build(),
                        Model.of(confidence)) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        CollapsableContainerPanel collapseContainerUser = (CollapsableContainerPanel) cellItem
                                .findParent(Item.class).get(ID_FIRST_COLLAPSABLE_CONTAINER);
                        CollapsableContainerPanel collapseContainerRole = (CollapsableContainerPanel) cellItem
                                .findParent(Item.class).get(ID_SECOND_COLLAPSABLE_CONTAINER);

                        if (!collapseContainerUser.isExpanded()) {
//                            RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = null;
//                            RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResult = null;
//                            if (model.getObject() != null) {
//                                DetectedPattern pattern = model.getObject();
//                                userAttributeAnalysisResult = pattern.getUserAttributeAnalysisResult();
//                                roleAttributeAnalysisResult = pattern.getRoleAttributeAnalysisResult();
//                            }

                            CollapsableContainerPanel webMarkupContainerUser = new CollapsableContainerPanel(
                                    ID_FIRST_COLLAPSABLE_CONTAINER);
                            webMarkupContainerUser.setOutputMarkupId(true);
                            webMarkupContainerUser.add(AttributeModifier.replace("class", "collapse"));
                            webMarkupContainerUser.add(AttributeModifier.replace("style", "display: none;"));
                            webMarkupContainerUser.setExpanded(true);

                            LoadableModel<RoleAnalysisAttributesDto> attributeModel = new LoadableModel<>(false) {
                                @Override
                                protected RoleAnalysisAttributesDto load() {
                                    return RoleAnalysisAttributesDto.loadFromDetectedPattern("RoleAnalysis.analysis.attribute.panel", model.getObject());
                                }
                            };
                            RoleAnalysisAttributePanel roleAnalysisAttributePanel = new RoleAnalysisAttributePanel(ID_COLLAPSABLE_CONTENT,
                                    attributeModel);
                            roleAnalysisAttributePanel.setOutputMarkupId(true);
                            webMarkupContainerUser.add(roleAnalysisAttributePanel);

                            collapseContainerUser.replaceWith(webMarkupContainerUser);
                            target.add(webMarkupContainerUser);
                        }
                        target.appendJavaScript(getCollapseScript(collapseContainerUser, collapseContainerRole));
                    }

                };
                objectButton.titleAsLabel(true);
                objectButton.add(AttributeAppender.append("class", "btn btn-link btn-sm"));
                objectButton.add(AttributeAppender.append("style", "width:120px"));

                cellItem.add(objectButton);
            }

            @Override
            public boolean isSortable() {
                return false;
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
                    RepeatingView repeatingView = new RepeatingView(componentId);
                    item.add(AttributeAppender.append("class", "d-flex align-items-center justify-content-center"));
                    item.add(repeatingView);

                    AjaxCompositedIconSubmitButton migrationButton = buildCandidateButton(repeatingView.newChildId(), rowModel);
                    migrationButton.add(AttributeAppender.append("class", "mr-1"));
                    repeatingView.add(migrationButton);

                    AjaxCompositedIconSubmitButton exploreButton = buildExploreButton(repeatingView.newChildId(), rowModel);
                    repeatingView.add(exploreButton);
                }

            }

            @Override
            public Component getHeader(String componentId) {
                LabelWithHelpPanel display = new LabelWithHelpPanel(componentId,
                        getHeaderTitle("display")) {
                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleAnalysisCluster.table.header.cluster.attribute.statistic.help");
                    }
                };
                display.setOutputMarkupId(true);
                display.add(AttributeAppender.append("class", "d-flex align-items-center justify-content-center"));
                return display;
            }

        });

        return columns;
    }

    @NotNull
    private AjaxCompositedIconSubmitButton buildCandidateButton(String componentId, IModel<DetectedPattern> rowModel) {
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
                        .recomputeAndResolveClusterOpStatus(clusterRef.getOid(), result, task, true, null);

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

                Set<PrismObject<RoleType>> candidateInducements = new HashSet<>();

                for (String roleOid : roles) {
                    PrismObject<RoleType> roleObject = roleAnalysisService
                            .getRoleTypeObject(roleOid, task, result);
                    if (roleObject != null) {
                        candidateInducements.add(roleObject);
                    }
                }

                PrismObject<RoleType> businessRole = new RoleType().asPrismObject();

                List<BusinessRoleDto> roleApplicationDtos = new ArrayList<>();

                for (String userOid : users) {
                    PrismObject<UserType> userObject = WebModelServiceUtils.loadObject(UserType.class, userOid, getPageBase(), task, result);
//                            roleAnalysisService
//                            .getUserTypeObject(userOid, task, result);
                    if (userObject != null) {
                        roleApplicationDtos.add(new BusinessRoleDto(userObject,
                                businessRole, candidateInducements, getPageBase()));
                    }
                }

                PrismObject<RoleAnalysisClusterType> prismObjectCluster = roleAnalysisService
                        .getClusterTypeObject(clusterRef.getOid(), task, result);

                if (prismObjectCluster == null) {
                    return;
                }

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
        return migrationButton;
    }

    @NotNull
    private AjaxCompositedIconSubmitButton buildExploreButton(String componentId, IModel<DetectedPattern> rowModel) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                GuiStyleConstants.CLASS_ICON_SEARCH, LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton migrationButton = new AjaxCompositedIconSubmitButton(componentId,
                iconBuilder.build(),
                createStringResource("RoleAnalysis.explore.button.title")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                PageParameters parameters = new PageParameters();
                String clusterOid = rowModel.getObject().getClusterRef().getOid();
                parameters.add(OnePageParameterEncoder.PARAMETER, clusterOid);
                parameters.add("panelId", "clusterDetails");
                parameters.add(PARAM_DETECTED_PATER_ID, rowModel.getObject().getId());
                StringValue fullTableSetting = getPageBase().getPageParameters().get(PARAM_TABLE_SETTING);
                if (fullTableSetting != null && fullTableSetting.toString() != null) {
                    parameters.add(PARAM_TABLE_SETTING, fullTableSetting.toString());
                }

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
        return migrationButton;
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

    protected DataTable<?, ?> getDataTable() {
        return ((BoxedTablePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE))).getDataTable();
    }

    protected BoxedTablePanel<?> getTable() {
        return ((BoxedTablePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE)));
    }

    protected StringResourceModel getHeaderTitle(String identifier) {
        return createStringResource("RoleMining.cluster.table.column.header." + identifier);
    }

    protected void onRefresh(AjaxRequestTarget target) {

    }

    @Override
    public PageBase getPageBase() {
        return pageBase;
    }
}
