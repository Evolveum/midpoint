/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.resolveDateAndTime;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_CANDIDATE_ROLE_ID;
import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;

import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.gui.api.util.DisplayForLifecycleState;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.CompositedIconColumn;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.tile.mining.candidate.RoleAnalysisCandidateTileModel;

import com.evolveum.midpoint.gui.impl.component.tile.mining.candidate.RoleAnalysisCandidateTilePanel;

import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.component.TogglePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.component.tile.mining.session.RoleAnalysisSessionTileModel;
import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.PageableListView;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisCandidateRoleTileTable extends BasePanel<String> {
    private static final String ID_DATATABLE = "datatable";
    PageBase pageBase;
    IModel<List<Toggle<ViewToggle>>> items;
    IModel<ObjectReferenceType> clusterRef;
    IModel<ObjectReferenceType> sessionRef;

    public RoleAnalysisCandidateRoleTileTable(
            @NotNull String id,
            @NotNull PageBase pageBase,
            @NotNull LoadableDetachableModel<List<RoleType>> roles,
            @NotNull HashMap<String, RoleAnalysisCandidateRoleType> cacheCandidate,
            @NotNull ObjectReferenceType clusterRef,
            @NotNull ObjectReferenceType sessionRef) {
        super(id);
        this.pageBase = pageBase;
        this.clusterRef = new LoadableModel<>() {
            @Override
            protected ObjectReferenceType load() {
                return clusterRef;
            }
        };

        this.sessionRef = new LoadableModel<>() {
            @Override
            protected ObjectReferenceType load() {
                return sessionRef;
            }
        };
        this.items = new LoadableModel<>(false) {

            @Override
            protected @NotNull List<Toggle<ViewToggle>> load() {
                List<Toggle<ViewToggle>> list = new ArrayList<>();

                Toggle<ViewToggle> asList = new Toggle<>("fa-solid fa-table-list", null);

                ViewToggle object = getTable().getViewToggleModel().getObject();

                asList.setValue(ViewToggle.TABLE);
                asList.setActive(object == ViewToggle.TABLE);
                list.add(asList);

                Toggle<ViewToggle> asTile = new Toggle<>("fa-solid fa-table-cells", null);
                asTile.setValue(ViewToggle.TILE);
                asTile.setActive(object == ViewToggle.TILE);
                list.add(asTile);

                return list;
            }
        };
        add(initTable(roles, cacheCandidate));
    }

    public TileTablePanel<RoleAnalysisCandidateTileModel<RoleType>, RoleType> initTable(
            @NotNull LoadableDetachableModel<List<RoleType>> roles,
            @NotNull HashMap<String, RoleAnalysisCandidateRoleType> cacheCandidate) {

        RoleMiningProvider<RoleType> provider = new RoleMiningProvider<>(
                this, new ListModel<>(roles.getObject()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<RoleType> object) {
                super.setObject(object);
            }

        }, false);

        return new TileTablePanel<>(
                ID_DATATABLE,
                Model.of(ViewToggle.TILE),
                UserProfileStorage.TableId.PANEL_CANDIDATE_ROLES) {

            @Override
            protected String getAdditionalBoxCssClasses() {
                return " m-0";
            }

            @Override
            protected List<IColumn<RoleType, String>> createColumns() {
                return RoleAnalysisCandidateRoleTileTable.this.initColumns(cacheCandidate);
            }

            @Override
            protected WebMarkupContainer createTableButtonToolbar(String id) {
                Fragment fragment = new Fragment(id, "tableFooterFragment",
                        RoleAnalysisCandidateRoleTileTable.this);

                AjaxIconButton refreshTable = new AjaxIconButton("refreshTable",
                        Model.of("fa fa-refresh"),
                        Model.of()) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        onRefresh(ajaxRequestTarget);
                    }
                };

                refreshTable.setOutputMarkupId(true);
                refreshTable.add(AttributeModifier.replace("title",
                        createStringResource("Refresh table")));
                refreshTable.add(new TooltipBehavior());
                fragment.add(refreshTable);
                TogglePanel<ViewToggle> viewToggle = new TogglePanel<>("viewToggle", items) {

                    @Override
                    protected void itemSelected(AjaxRequestTarget target, IModel<Toggle<ViewToggle>> item) {
                        getViewToggleModel().setObject(item.getObject().getValue());
                        target.add(this);
                        target.add(RoleAnalysisCandidateRoleTileTable.this);
                    }
                };

                viewToggle.add(AttributeModifier.replace("title", createStringResource("Change view")));
                viewToggle.add(new TooltipBehavior());
                fragment.add(viewToggle);

                return fragment;
            }

            @Override
            protected WebMarkupContainer createTilesButtonToolbar(String id) {
                Fragment fragment = new Fragment(id, "tableFooterFragment",
                        RoleAnalysisCandidateRoleTileTable.this);

                AjaxIconButton refreshTable = new AjaxIconButton("refreshTable",
                        Model.of("fa fa-refresh"), Model.of()) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        onRefresh(ajaxRequestTarget);
                    }
                };

                refreshTable.setOutputMarkupId(true);
                fragment.add(refreshTable);

                TogglePanel<ViewToggle> viewToggle = new TogglePanel<>("viewToggle", items) {

                    @Override
                    protected void itemSelected(@NotNull AjaxRequestTarget target, @NotNull IModel<Toggle<ViewToggle>> item) {
                        getViewToggleModel().setObject(item.getObject().getValue());
                        getTable().refreshSearch();
                        target.add(RoleAnalysisCandidateRoleTileTable.this);
                    }
                };

                fragment.add(viewToggle);

                return fragment;
            }

            @Override
            protected void onInitialize() {
                super.onInitialize();
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
                return provider;
            }

            @Override
            protected PageableListView<?, ?> createTilesPanel(String tilesId, ISortableDataProvider<RoleType, String> provider1) {
                return super.createTilesPanel(tilesId, provider1);
            }

            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            protected RoleAnalysisCandidateTileModel createTileObject(RoleType role) {
                RoleAnalysisCandidateRoleType candidateRole = cacheCandidate.get(role.getOid());
                return new RoleAnalysisCandidateTileModel<>(
                        role, getPageBase(), getClusterRef(), getSessionRef(), candidateRole);
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
            protected Component createTile(String id, IModel<RoleAnalysisCandidateTileModel<RoleType>> model) {
                return new RoleAnalysisCandidateTilePanel<>(id, model);
            }
        };
    }

    protected CompiledObjectCollectionView getObjectCollectionView() {
        return null;
    }

    private @NotNull List<IColumn<RoleType, String>> initColumns(@NotNull HashMap<String, RoleAnalysisCandidateRoleType> cacheCandidate) {

        List<IColumn<RoleType, String>> columns = new ArrayList<>();

        columns.add(new CompositedIconColumn<>(createStringResource("")) {

            @Override
            protected CompositedIcon getCompositedIcon(IModel<RoleType> rowModel) {
                if (rowModel == null || rowModel.getObject() == null || rowModel.getObject() == null) {
                    return new CompositedIconBuilder().build();
                }
                OperationResult result = new OperationResult("getIconDisplayType");

                return WebComponentUtil.createCompositeIconForObject(rowModel.getObject(),
                        result, pageBase);
            }
        });

        columns.add(new AbstractColumn<>(createStringResource("ObjectType.name")) {

            @Override
            public String getSortProperty() {
                return RoleType.F_NAME.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleType>> item, String componentId,
                    IModel<RoleType> rowModel) {

                String name = rowModel.getObject().getName().toString();
                String oid = rowModel.getObject().getOid();

                AjaxLinkPanel ajaxLinkPanel = new AjaxLinkPanel(componentId, Model.of(name)) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, oid);

                        ((PageBase) getPage()).navigateToNext(PageRole.class, parameters);
                    }
                };
                ajaxLinkPanel.setOutputMarkupId(true);
                item.add(ajaxLinkPanel);
            }

        });

        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public String getSortProperty() {
                return RoleType.F_ACTIVATION.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId,
                        createStringResource("RoleAnalysisCandidateRoleTable.lifecycleState"));
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleType>> item, String componentId,
                    IModel<RoleType> rowModel) {
                RoleType role = rowModel.getObject();
                String lifecycleState = role.getLifecycleState();
                if (lifecycleState == null) {
                    lifecycleState = DisplayForLifecycleState.ACTIVE.name();
                }
                Label label = new Label(componentId, lifecycleState);
                label.add(AttributeAppender.append("class",
                        "badge " + DisplayForLifecycleState.valueOfOrDefault(lifecycleState).getCssClass()));
                label.add(AttributeAppender.append("style", "width: 100%; height: 100%"));
                label.setOutputMarkupId(true);
                item.add(label);
            }

        });

        columns.add(new AbstractColumn<>(createStringResource("Members count")) {

            @Override
            public String getSortProperty() {
                return RoleType.F_ASSIGNMENT.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleType>> item, String componentId,
                    IModel<RoleType> rowModel) {
                Task task = getPageBase().createSimpleTask("countRoleMembers");

                Integer membersCount = getPageBase().getRoleAnalysisService()
                        .countUserTypeMembers(null, rowModel.getObject().getOid(),
                                task, task.getResult());

                item.add(new Label(componentId, membersCount));
            }

        });

        columns.add(new AbstractColumn<>(createStringResource("Inducement count")) {

            @Override
            public String getSortProperty() {
                return RoleType.F_INDUCEMENT.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleType>> item, String componentId,
                    IModel<RoleType> rowModel) {
                item.add(new Label(componentId, rowModel.getObject().getInducement().size()));
            }

        });

        columns.add(new AbstractColumn<>(createStringResource("Created Timestamp")) {

            @Override
            public String getSortProperty() {
                return RoleType.F_INDUCEMENT.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleType>> item, String componentId,
                    IModel<RoleType> rowModel) {
                RoleType object = rowModel.getObject();
                if (object == null || object.getMetadata() == null || object.getMetadata().getCreateTimestamp() == null) {
                    item.add(new Label(componentId, ""));
                } else {
                    item.add(new Label(componentId, resolveDateAndTime(object.getMetadata().getCreateTimestamp())));
                }
            }

        });

        columns.add(new AbstractColumn<>(
                createStringResource("")) {

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId,
                        createStringResource("RoleMining.button.title.execute.migration"));
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleType>> cellItem,
                    String componentId, IModel<RoleType> model) {
                RoleType role = model.getObject();
                if (role == null) {
                    cellItem.add(new EmptyPanel(componentId));
                    return;
                }

                Task task = getPageBase().createSimpleTask("MigrateRole");
                OperationResult result = task.getResult();
                RoleAnalysisCandidateRoleType candidateRoleType = cacheCandidate.get(role.getOid());
                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();

                PrismObject<RoleAnalysisClusterType> clusterPrism = roleAnalysisService.getClusterTypeObject(getClusterRef().getOid(), task, result);
                if (clusterPrism == null) {
                    cellItem.add(new EmptyPanel(componentId));
                    return;
                }

                String stateString = roleAnalysisService.recomputeAndResolveClusterCandidateRoleOpStatus(
                        clusterPrism, candidateRoleType,
                        result, task);

                RoleAnalysisOperationStatusType operationStatus = candidateRoleType.getOperationStatus();
                if (operationStatus != null
                        && operationStatus.getTaskRef() != null
                        && operationStatus.getTaskRef().getOid() != null) {
                    AjaxLinkPanel ajaxLinkPanel = taskLinkPanel(componentId, stateString, operationStatus);
                    cellItem.add(ajaxLinkPanel);
                } else {
                    CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                            GuiStyleConstants.CLASS_PLUS_CIRCLE, LayeredIconCssStyle.IN_ROW_STYLE);
                    AjaxCompositedIconSubmitButton migrationButton = new AjaxCompositedIconSubmitButton(componentId,
                            iconBuilder.build(),
                            createStringResource("RoleMining.button.title.execute.migration")) {
                        @Serial private static final long serialVersionUID = 1L;

                        @Override
                        protected void onSubmit(AjaxRequestTarget target) {
                            Task task = getPageBase().createSimpleTask("MigrateRole");
                            OperationResult result = task.getResult();

                            String taskOid = UUID.randomUUID().toString();
                            RoleAnalysisCandidateRoleType candidateRoleType = cacheCandidate.get(role.getOid());
                            List<ObjectReferenceType> candidateMembers = candidateRoleType.getCandidateMembers();
                            ObjectSetType members = new ObjectSetType();
                            candidateMembers.forEach(member -> members.getObjectRef().add(member.clone()));
                            RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();

                            roleAnalysisService.clusterObjectMigrationRecompute(
                                    getClusterRef().getOid(), role.getOid(), task, result);

                            ActivityDefinitionType activity = null;
                            try {
                                activity = createActivity(members, role.getOid());
                            } catch (SchemaException e) {
                                LOGGER.error("Couldn't create activity for role migration: " + role.getOid(), e);
                            }
                            if (activity != null) {
                                roleAnalysisService.executeRoleAnalysisRoleMigrationTask(getPageBase().getModelInteractionService(),
                                        clusterPrism, activity, role.asPrismObject(), taskOid,
                                        null, task, result);
                                if (result.isWarning()) {
                                    warn(result.getMessage());
                                    target.add(((PageBase) getPage()).getFeedbackPanel());
                                } else {
                                    MidPointPrincipal user = AuthUtil.getPrincipalUser();
                                    roleAnalysisService.setCandidateRoleOpStatus(clusterPrism,
                                            candidateRoleType,
                                            taskOid,
                                            OperationResultStatusType.IN_PROGRESS,
                                            null,
                                            result,
                                            task,
                                            RoleAnalysisOperationType.MIGRATION,
                                            user.getFocus());
                                    navigateToRoleAnalysisCluster(getClusterRef().getOid());
                                }
                            }

                        }

                        @Override
                        protected void onError(AjaxRequestTarget target) {
                            target.add(((PageBase) getPage()).getFeedbackPanel());
                        }

                    };
                    migrationButton.titleAsLabel(true);
                    migrationButton.setOutputMarkupId(true);
                    migrationButton.add(AttributeAppender.append("class", "btn btn-success btn-sm"));

                    cellItem.add(migrationButton);
                }
            }

            @Override
            public boolean isSortable() {
                return false;
            }

        });

        columns.add(new AbstractColumn<>(createStringResource("Action")) {

            @Override
            public String getSortProperty() {
                return RoleType.F_INDUCEMENT.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleType>> item, String componentId,
                    IModel<RoleType> rowModel) {
                if (rowModel.getObject() == null) {
                    item.add(new EmptyPanel(componentId));
                } else {
                    RepeatingView repeatingView = new RepeatingView(componentId);
                    item.add(AttributeAppender.append("class", "d-flex align-items-center justify-content-center"));
                    item.add(repeatingView);

                    RoleType role = rowModel.getObject();

                    RoleAnalysisCandidateRoleType candidateRole = cacheCandidate.get(role.getOid());
                    AjaxCompositedIconSubmitButton exploreButton = buildExploreButton(repeatingView.newChildId(), candidateRole);
                    repeatingView.add(exploreButton);
                }
            }

        });

        return columns;
    }

    @SuppressWarnings("unchecked")
    private TileTablePanel<RoleAnalysisSessionTileModel<SelectableBean<RoleAnalysisSessionType>>, SelectableBean<RoleAnalysisSessionType>> getTable() {
        return (TileTablePanel<RoleAnalysisSessionTileModel<SelectableBean<RoleAnalysisSessionType>>, SelectableBean<RoleAnalysisSessionType>>)
                get(createComponentPath(ID_DATATABLE));
    }

    @NotNull
    private AjaxCompositedIconSubmitButton buildExploreButton(String componentId, RoleAnalysisCandidateRoleType candidateRole) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                GuiStyleConstants.CLASS_ICON_SEARCH, LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton migrationButton = new AjaxCompositedIconSubmitButton(componentId,
                iconBuilder.build(),
                createStringResource("Explore candidate")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                StringBuilder stringBuilder = new StringBuilder();
                Long id = candidateRole.getId();
                stringBuilder.append(id).append(",");
                getPageBase().clearBreadcrumbs();

                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, clusterRef);
                parameters.add("panelId", "clusterDetails");
                parameters.add(PARAM_CANDIDATE_ROLE_ID, stringBuilder.toString());
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
        migrationButton.add(AttributeAppender.append("class", "btn btn-primary btn-sm"));
        return migrationButton;
    }

    public IModel<List<Toggle<ViewToggle>>> getItems() {
        return items;
    }

    @Override
    public PageBase getPageBase() {
        return pageBase;
    }

    private @NotNull IModel<String> extractProcessMode() {
        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
        Task task = pageBase.createSimpleTask("getClusterOptionType");
        OperationResult result = task.getResult();
        PrismObject<RoleAnalysisClusterType> clusterPrism = roleAnalysisService
                .getClusterTypeObject(getClusterRef().getOid(), task, result);
        if (clusterPrism == null) {
            return Model.of("");
        } else {
            RoleAnalysisOptionType analysisOptionType = roleAnalysisService.resolveClusterOptionType(clusterPrism, task, result);
            RoleAnalysisProcessModeType processMode = analysisOptionType.getProcessMode();
            RoleAnalysisCategoryType analysisCategory = analysisOptionType.getAnalysisCategory();
            return Model.of(processMode.value() + "/" + analysisCategory.value());
        }
    }

    @NotNull
    private AjaxLinkPanel taskLinkPanel(String componentId, String stateString, RoleAnalysisOperationStatusType operationExecution) {
        AjaxLinkPanel ajaxLinkPanel = new AjaxLinkPanel(componentId, Model.of(stateString)) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                String oid = operationExecution.getTaskRef().getOid();
                super.onClick(target);
                DetailsPageUtil.dispatchToObjectDetailsPage(TaskType.class, oid,
                        this, true);
            }
        };
        ajaxLinkPanel.setEnabled(true);
        ajaxLinkPanel.setOutputMarkupId(true);
        return ajaxLinkPanel;
    }

    private void navigateToRoleAnalysisCluster(String clusterOid) {
        getPageBase().clearBreadcrumbs();
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, clusterOid);
        parameters.add("panelId", "candidateRoles");
        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                .getObjectDetailsPage(RoleAnalysisClusterType.class);
        getPageBase().navigateToNext(detailsPageClass, parameters);
    }

    private ActivityDefinitionType createActivity(ObjectSetType members, String roleOid) throws SchemaException {

        ObjectReferenceType objectReferenceType = new ObjectReferenceType();
        objectReferenceType.setType(RoleType.COMPLEX_TYPE);
        objectReferenceType.setOid(roleOid);

        RoleMembershipManagementWorkDefinitionType roleMembershipManagementWorkDefinitionType =
                new RoleMembershipManagementWorkDefinitionType();
        roleMembershipManagementWorkDefinitionType.setRoleRef(objectReferenceType);

        roleMembershipManagementWorkDefinitionType.setMembers(members);

        return new ActivityDefinitionType()
                .work(new WorkDefinitionsType()
                        .roleMembershipManagement(roleMembershipManagementWorkDefinitionType));
    }

    protected void onRefresh(AjaxRequestTarget target) {

    }

    private ObjectReferenceType getClusterRef() {
        return clusterRef.getObject();
    }

    private ObjectReferenceType getSessionRef() {
        return sessionRef.getObject();
    }

    private IModel<ObjectReferenceType> getClusterRefModel() {
        return clusterRef;
    }

    private IModel<ObjectReferenceType> getSessionRefModel() {
        return sessionRef;
    }
}
