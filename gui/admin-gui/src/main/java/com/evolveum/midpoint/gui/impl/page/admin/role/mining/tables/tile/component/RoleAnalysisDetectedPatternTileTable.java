/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.component;

import static com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil.createDisplayType;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_DETECTED_PATER_ID;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_TABLE_SETTING;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.RoleAnalysisTileTableUtils.*;
import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;

import java.io.Serial;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.LinkIconLabelIconPanel;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.model.RoleAnalysisDetectedPatternsDto;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.component.TogglePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.gui.impl.component.tile.mining.pattern.RoleAnalysisPatternTileModel;
import com.evolveum.midpoint.gui.impl.component.tile.mining.pattern.RoleAnalysisPatternTilePanel;
import com.evolveum.midpoint.gui.impl.component.tile.mining.session.RoleAnalysisSessionTileModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleApplicationDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisDetectedPatternDetailsPopup;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisDetectedPatternTileTable extends BasePanel<RoleAnalysisDetectedPatternsDto> {

    private static final String DOT_CLASS = RoleAnalysisDetectedPatternTileTable.class.getName() + ".";
    private static final String OP_PREPARE_OBJECTS = DOT_CLASS + "prepareObjects";
    private static final String ID_DATATABLE = "datatable";

    PageBase pageBase;
    IModel<List<Toggle<ViewToggle>>> items;

    public RoleAnalysisDetectedPatternTileTable(
            @NotNull String id,
            @NotNull PageBase pageBase,
            @NotNull IModel<RoleAnalysisDetectedPatternsDto> detectedPatternsDtoIModel) {
        super(id, detectedPatternsDtoIModel);
        this.pageBase = pageBase;

        add(initTable());
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        this.items = initToggleItems(getTable());
    }

    public TileTablePanel<RoleAnalysisPatternTileModel<DetectedPattern>, DetectedPattern> initTable() {

        return new TileTablePanel<>(
                ID_DATATABLE,
                RoleAnalysisDetectedPatternTileTable.this.defaultViewToggleModel(),
                UserProfileStorage.TableId.PANEL_DETECTED_PATTERN) {

            @Override
            protected String getAdditionalBoxCssClasses() {
                return " m-0";
            }

            @Override
            protected List<IColumn<DetectedPattern, String>> createColumns() {
                return RoleAnalysisDetectedPatternTileTable.this.initColumns();
            }

            @Override
            protected VisibleEnableBehaviour getHeaderFragmentVisibility() {
                return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
            }

            @Override
            protected WebMarkupContainer createTableButtonToolbar(String id) {
                return initButtonToolbar(id);
            }

            @Override
            protected WebMarkupContainer createTilesButtonToolbar(String id) {
                return initButtonToolbar(id);
            }

            private @NotNull Fragment initButtonToolbar(String id) {
                Fragment fragment = new Fragment(id, "tableFooterFragment",
                        RoleAnalysisDetectedPatternTileTable.this);

                AjaxIconButton refreshTable = buildRefreshToggleTablePanel("refreshTable", target -> onRefresh(target));
                fragment.add(refreshTable);

                TogglePanel<ViewToggle> viewToggle = buildViewToggleTablePanel(
                        "viewToggle", items, getViewToggleModel(), this);
                fragment.add(viewToggle);

                return fragment;
            }

            @Override
            protected String getTilesFooterCssClasses() {
                return "card-footer";
            }

            @Override
            protected String getTilesContainerAdditionalClass() {
                return " m-0";
            }

            @Override
            protected ISortableDataProvider<?, ?> createProvider() {
                RoleMiningProvider<DetectedPattern> provider = new RoleMiningProvider<>(
                        this, () -> RoleAnalysisDetectedPatternTileTable.this.getModelObject().getDetectedPatterns(),
                        true);
                provider.setSort(DetectedPattern.F_METRIC, SortOrder.DESCENDING);
                return provider;
            }

            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            protected RoleAnalysisPatternTileModel createTileObject(DetectedPattern pattern) {
                Long id = pattern.getId();
                StringResourceModel patternName = pageBase.createStringResource(
                        "RoleAnalysis.role.suggestion.title", (id));
                return new RoleAnalysisPatternTileModel<>(pattern, patternName.getString(),
                        RoleAnalysisDetectedPatternTileTable.this.getModelObject().getTotalRoleToUserAssignments());
            }

            @Override
            protected String getTileCssStyle() {
                return " min-height:170px ";
            }

            @Override
            protected String getTileCssClasses() {
                return "col-4 pb-3 pl-2 pr-2";
            }

            @Override
            protected String getTileContainerCssClass() {
                return "row justify-content-left ";
            }

            @Override
            protected Component createTile(String id, IModel<RoleAnalysisPatternTileModel<DetectedPattern>> model) {
                return new RoleAnalysisPatternTilePanel<>(id, model);
            }
        };
    }

    protected boolean displaySessionNameColumn() {
        return false;
    }

    protected boolean displayClusterNameColumn() {
        return false;
    }

    protected CompiledObjectCollectionView getObjectCollectionView() {
        return null;
    }

    public List<IColumn<DetectedPattern, String>> initColumns() {

        List<IColumn<DetectedPattern, String>> columns = new ArrayList<>();

        initIconColumn(columns);

        if (displaySessionNameColumn()) {
            initSessionNameColumn(columns);
        }

        if (displayClusterNameColumn()) {
            initClusterNameColumn(columns);
        }

        initReductionColumn(columns);

        initMemberCountColumn(columns);

        initRoleCountCoulumn(columns);

        initAttributeStatisticsColumn(columns);

        initExploreColumn(columns);

        return columns;
    }

    private void initExploreColumn(@NotNull List<IColumn<DetectedPattern, String>> columns) {
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
                    item.add(AttributeModifier.append(CLASS_CSS, "d-flex align-items-center justify-content-center"));
                    item.add(repeatingView);

                    AjaxCompositedIconSubmitButton migrationButton = buildCandidateButton(repeatingView.newChildId(), rowModel);
                    migrationButton.add(AttributeModifier.append(CLASS_CSS, "mr-1"));
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
                display.add(AttributeModifier.append(CLASS_CSS, "d-flex align-items-center justify-content-center"));
                return display;
            }

        });
    }

    private void initAttributeStatisticsColumn(@NotNull List<IColumn<DetectedPattern, String>> columns) {
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
            public void populateItem(Item<ICellPopulator<DetectedPattern>> cellItem, String componentId,
                    IModel<DetectedPattern> model) {
                String confidence = "";
                if (model == null || model.getObject() == null) {
                    cellItem.add(new EmptyPanel(componentId));
                    return;
                }

                DetectedPattern pattern = model.getObject();
                confidence = String.format("%.2f", pattern.getItemsConfidence()) + "%";

                LinkIconLabelIconPanel components = new LinkIconLabelIconPanel(componentId, Model.of(confidence)) {
                    @Contract(pure = true)
                    @Override
                    public @NotNull String getIconCssClass() {
                        return "fa fa-chart-bar";
                    }

                    @Override
                    protected void onClickPerform(AjaxRequestTarget target) {
                        if (model.getObject() != null) {
                            RoleAnalysisDetectedPatternDetailsPopup component = new RoleAnalysisDetectedPatternDetailsPopup(
                                    ((PageBase) getPage()).getMainPopupBodyId(),
                                    model);
                            ((PageBase) getPage()).showMainPopup(component, target);
                        }
                    }
                };

                cellItem.add(components);
            }

            @Override
            public boolean isSortable() {
                return false;
            }

        });
    }

    private void initRoleCountCoulumn(@NotNull List<IColumn<DetectedPattern, String>> columns) {
        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<DetectedPattern>> item, String componentId,
                    IModel<DetectedPattern> rowModel) {

                if (rowModel.getObject() == null || rowModel.getObject().getRoles() == null) {
                    item.add(new EmptyPanel(componentId));
                    return;
                }

                DetectedPattern pattern = rowModel.getObject();
                Set<String> roles = pattern.getRoles();
                IModel<String> roleObjectCount = Model.of(String.valueOf(roles.size()));

                IconWithLabel components = new IconWithLabel(componentId, roleObjectCount) {
                    @Contract(pure = true)
                    @Override
                    public @NotNull String getIconCssClass() {
                        return GuiStyleConstants.CLASS_OBJECT_ROLE_ICON;
                    }

                    @Override
                    protected @NotNull String getComponentCssClass() {
                        return super.getComponentCssClass() + TEXT_MUTED;
                    }
                };

                components.setOutputMarkupId(true);
                item.add(components);
            }

            @Override
            public Component getHeader(String componentId) {
                return new LabelWithHelpPanel(componentId,
                        createStringResource("RoleAnalysis.tile.panel.roles")) {
                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleAnalysisCluster.table.header.cluster.occupation.help");
                    }
                };
            }

        });
    }

    private void initMemberCountColumn(@NotNull List<IColumn<DetectedPattern, String>> columns) {
        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<DetectedPattern>> item, String componentId,
                    IModel<DetectedPattern> rowModel) {

                if (rowModel.getObject() == null || rowModel.getObject().getUsers() == null) {
                    item.add(new EmptyPanel(componentId));
                    return;
                }
                DetectedPattern pattern = rowModel.getObject();
                Set<String> users = pattern.getUsers();
                IModel<String> userObjectCount = Model.of(String.valueOf(users.size()));

                IconWithLabel components = new IconWithLabel(componentId, userObjectCount) {
                    @Contract(pure = true)
                    @Override
                    public @NotNull String getIconCssClass() {
                        return GuiStyleConstants.CLASS_OBJECT_USER_ICON;
                    }

                    @Override
                    protected @NotNull String getComponentCssClass() {
                        return super.getComponentCssClass() + TEXT_MUTED;
                    }
                };

                components.setOutputMarkupId(true);
                item.add(components);
            }

            @Override
            public Component getHeader(String componentId) {
                return new LabelWithHelpPanel(componentId,
                        createStringResource("RoleAnalysis.tile.panel.users")) {
                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleAnalysisCluster.table.header.cluster.occupation.help");
                    }
                };
            }

        });
    }

    private void initReductionColumn(@NotNull List<IColumn<DetectedPattern, String>> columns) {
        columns.add(new AbstractColumn<>(getHeaderTitle("")) {

            @Override
            public void populateItem(Item<ICellPopulator<DetectedPattern>> item, String componentId, IModel<DetectedPattern> rowModel) {

                IconWithLabel icon = buildReductionPanel(componentId, rowModel,
                        RoleAnalysisDetectedPatternTileTable.this.getModelObject().getTotalRoleToUserAssignments());
                item.add(icon);
            }

            @Override
            public String getSortProperty() {
                return DetectedPattern.F_METRIC;
            }

            @Override
            public boolean isSortable() {
                return true;
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
    }

    private void initClusterNameColumn(@NotNull List<IColumn<DetectedPattern, String>> columns) {
        columns.add(new AbstractColumn<>(getHeaderTitle("")) {

            @Override
            public void populateItem(Item<ICellPopulator<DetectedPattern>> item, String componentId, IModel<DetectedPattern> rowModel) {
                @NotNull AjaxLinkPanel linkPanel = buildClusterLinkPanel(componentId, rowModel);
                linkPanel.setOutputMarkupId(true);
                item.add(linkPanel);
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public Component getHeader(String componentId) {
                return new LabelWithHelpPanel(componentId,
                        createStringResource("RoleMining.cluster.table.column.header.cluster.link")) {
                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.cluster.table.column.header.cluster.link.help");
                    }
                };

            }

        });
    }

    private void initSessionNameColumn(@NotNull List<IColumn<DetectedPattern, String>> columns) {
        columns.add(new AbstractColumn<>(getHeaderTitle("")) {

            @Override
            public void populateItem(Item<ICellPopulator<DetectedPattern>> item, String componentId, IModel<DetectedPattern> rowModel) {
                @NotNull AjaxLinkPanel linkPanel = buildSessionLinkPanel(componentId, rowModel);
                linkPanel.setOutputMarkupId(true);
                item.add(linkPanel);
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public Component getHeader(String componentId) {
                return new LabelWithHelpPanel(componentId,
                        createStringResource("RoleMining.cluster.table.column.header.session.link")) {
                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.cluster.table.column.header.session.link.help");
                    }
                };

            }

        });
    }

    private static void initIconColumn(@NotNull List<IColumn<DetectedPattern, String>> columns) {
        columns.add(new IconColumn<>(null) {
            @Override
            protected DisplayType getIconDisplayType(IModel<DetectedPattern> rowModel) {
                return createDisplayType(GuiStyleConstants.CLASS_DETECTED_PATTERN_ICON, "", "");
            }
        });
    }

    private @NotNull AjaxLinkPanel buildSessionLinkPanel(String componentId, @NotNull IModel<DetectedPattern> rowModel) {
        DetectedPattern pattern = rowModel.getObject();
        ObjectReferenceType sessionRef = pattern.getSessionRef();
        PolyStringType targetName = sessionRef.getTargetName();
        AjaxLinkPanel linkPanel = new AjaxLinkPanel(componentId, Model.of(targetName)) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, sessionRef.getOid());

                Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                        .getObjectDetailsPage(RoleAnalysisSessionType.class);
                pageBase.navigateToNext(detailsPageClass, parameters);
            }
        };
        linkPanel.setOutputMarkupId(true);
        return linkPanel;
    }

    private @NotNull AjaxLinkPanel buildClusterLinkPanel(String componentId, @NotNull IModel<DetectedPattern> rowModel) {
        DetectedPattern pattern = rowModel.getObject();
        ObjectReferenceType clusterRef = pattern.getClusterRef();
        PolyStringType targetName = clusterRef.getTargetName();
        AjaxLinkPanel linkPanel = new AjaxLinkPanel(componentId, Model.of(targetName)) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, clusterRef.getOid());

                Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                        .getObjectDetailsPage(RoleAnalysisClusterType.class);
                pageBase.navigateToNext(detailsPageClass, parameters);
            }
        };
        linkPanel.setOutputMarkupId(true);
        return linkPanel;
    }

    private static @NotNull IconWithLabel buildReductionPanel(String componentId, IModel<DetectedPattern> rowModel, int allUserOwnedRoleAssignments) {
        return new IconWithLabel(componentId, extractSystemReductionMetric(rowModel, allUserOwnedRoleAssignments)) {
            @Contract(pure = true)
            @Override
            public @NotNull String getIconCssClass() {
                return "fa fa-arrow-down text-success";
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getLabelComponentCssClass() {
                return "ml-1 text-success";
            }

            @Override
            protected @NotNull Component getSubComponent(String id) {
                Label label = new Label(id, extractReductionMetric(rowModel));
                label.add(AttributeModifier.append(CLASS_CSS, TEXT_MUTED));
                label.add(AttributeModifier.append(STYLE_CSS, "font-size: 14px"));
                return label;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private TileTablePanel<RoleAnalysisSessionTileModel<SelectableBean<RoleAnalysisSessionType>>, SelectableBean<RoleAnalysisSessionType>> getTable() {
        return (TileTablePanel<RoleAnalysisSessionTileModel<SelectableBean<RoleAnalysisSessionType>>, SelectableBean<RoleAnalysisSessionType>>)
                get(createComponentPath(ID_DATATABLE));
    }

    protected StringResourceModel getHeaderTitle(String identifier) {
        return createStringResource("RoleMining.cluster.table.column.header." + identifier);
    }

    @NotNull
    private AjaxCompositedIconSubmitButton buildCandidateButton(String componentId, IModel<DetectedPattern> rowModel) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                GuiStyleConstants.CLASS_PLUS_CIRCLE, IconCssStyle.IN_ROW_STYLE);
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

                PrismObject<RoleAnalysisClusterType> prismObjectCluster = roleAnalysisService
                        .getClusterTypeObject(clusterRef.getOid(), task, result);
                DetectedPattern pattern = rowModel.getObject();

                if (prismObjectCluster == null || pattern == null) {
                    return;
                }

                if (status.equals("processing")) {
                    warn("Couldn't start detection. Some process is already in progress.");
                    LOGGER.error("Couldn't start detection. Some process is already in progress.");
                    target.add(getFeedbackPanel());
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
                    PrismObject<UserType> userObject = WebModelServiceUtils.loadObject(UserType.class, userOid,
                            getPageBase(), task, result);
//                            roleAnalysisService
//                            .getUserTypeObject(userOid, task, result);
                    if (userObject != null) {
                        roleApplicationDtos.add(new BusinessRoleDto(userObject,
                                businessRole, candidateInducements, getPageBase()));
                    }
                }

                BusinessRoleApplicationDto operationData = new BusinessRoleApplicationDto(
                        prismObjectCluster, businessRole, roleApplicationDtos, candidateInducements);
                operationData.setPatternId(patternId);

                PageRole pageRole = new PageRole(operationData.getBusinessRole(), operationData);
                setResponsePage(pageRole);
            }

            @Override
            protected void onError(@NotNull AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        migrationButton.titleAsLabel(true);
        migrationButton.setOutputMarkupId(true);
        migrationButton.add(AttributeModifier.append(CLASS_CSS, "btn btn-success btn-sm"));
        return migrationButton;
    }

    @NotNull
    private AjaxCompositedIconSubmitButton buildExploreButton(String componentId, IModel<DetectedPattern> rowModel) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                GuiStyleConstants.CLASS_ICON_SEARCH, IconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton migrationButton = new AjaxCompositedIconSubmitButton(componentId,
                iconBuilder.build(),
                createStringResource("RoleAnalysis.explore.button.title")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                DetectedPattern pattern = rowModel.getObject();
                PageParameters parameters = new PageParameters();
                String clusterOid = pattern.getClusterRef().getOid();
                parameters.add(OnePageParameterEncoder.PARAMETER, clusterOid);
                parameters.add("panelId", "clusterDetails");
                parameters.add(PARAM_DETECTED_PATER_ID, pattern.getId());
                StringValue fullTableSetting = getPageBase().getPageParameters().get(PARAM_TABLE_SETTING);
                if (fullTableSetting != null && fullTableSetting.toString() != null) {
                    parameters.add(PARAM_TABLE_SETTING, fullTableSetting.toString());
                }

                Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                        .getObjectDetailsPage(RoleAnalysisClusterType.class);
                getPageBase().navigateToNext(detailsPageClass, parameters);
            }

            @Override
            protected void onError(@NotNull AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        migrationButton.titleAsLabel(true);
        migrationButton.setOutputMarkupId(true);
        migrationButton.add(AttributeModifier.append(CLASS_CSS, "btn btn-primary btn-sm"));
        return migrationButton;
    }

    public IModel<List<Toggle<ViewToggle>>> getItems() {
        return items;
    }

    @Override
    public PageBase getPageBase() {
        return pageBase;
    }

    protected void onRefresh(AjaxRequestTarget target) {
        //override
    }

    private static @NotNull IModel<String> extractSystemReductionMetric(
            @NotNull IModel<DetectedPattern> model,
            int allUserOwnedRoleAssignments) {
        DetectedPattern value = model.getObject();
        if (value != null) {
            Double detectedReductionMetric = value.getMetric();
            double percentagePart = 0;
            if (detectedReductionMetric != null && detectedReductionMetric != 0 && allUserOwnedRoleAssignments != 0) {
                percentagePart = (detectedReductionMetric / allUserOwnedRoleAssignments) * 100;
            }
            String formattedReductionFactorConfidence = String.format("%.2f", percentagePart);
            return Model.of(formattedReductionFactorConfidence + "%");
        } else {
            return Model.of("00.00%");
        }
    }

    private static @NotNull IModel<String> extractReductionMetric(
            @NotNull IModel<DetectedPattern> model) {
        DetectedPattern value = model.getObject();
        if (value != null) {
            Double detectedReductionMetric = value.getMetric();
            if (detectedReductionMetric == null) {
                detectedReductionMetric = 0.0;
            }
            return Model.of("(" + detectedReductionMetric + ")");
        } else {
            return Model.of("(0)");
        }
    }

    protected Model<ViewToggle> defaultViewToggleModel() {
        return Model.of(ViewToggle.TILE);
    }
}
