/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.component;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_CANDIDATE_ROLE_ID;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.RoleAnalysisTileTableUtils.*;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.gui.api.util.DisplayForLifecycleState;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.component.tile.mining.candidate.RoleAnalysisCandidateTileModel;

import com.evolveum.midpoint.gui.impl.component.tile.mining.candidate.RoleAnalysisCandidateTilePanel;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.model.RoleAnalysisCandidateRolesDto;
import com.evolveum.midpoint.security.api.MidPointPrincipal;

import com.evolveum.midpoint.web.util.TooltipBehavior;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
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
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.component.TogglePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.tile.mining.session.RoleAnalysisSessionTileModel;
import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisCandidateRoleTileTable extends BasePanel<RoleAnalysisCandidateRolesDto> {
    private static final String ID_DATATABLE = "datatable";
    PageBase pageBase;
    IModel<List<Toggle<ViewToggle>>> items;

    public RoleAnalysisCandidateRoleTileTable(
            @NotNull String id,
            @NotNull PageBase pageBase,
            @NotNull IModel<RoleAnalysisCandidateRolesDto> candidateRoleDtoModel) {
        super(id, candidateRoleDtoModel);
        this.pageBase = pageBase;

        add(initTable());
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        this.items = initToggleItems(getTable());
    }

    public TileTablePanel<RoleAnalysisCandidateTileModel<RoleType>, RoleType> initTable() {

        RoleMiningProvider<RoleType> provider = new RoleMiningProvider<>(
                this, () -> getModelObject().getRoles(), false);

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
                return RoleAnalysisCandidateRoleTileTable.this.initColumns();
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
                        RoleAnalysisCandidateRoleTileTable.this);

                AjaxIconButton refreshTable = buildRefreshButton();
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
                return provider;
            }

            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            protected RoleAnalysisCandidateTileModel createTileObject(RoleType role) {
                return new RoleAnalysisCandidateTileModel<>(
                        role, getPageBase(), getClusterRef(), getSessionRef(), getAssociatedCandidateRole(role));
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

    private @NotNull List<IColumn<RoleType, String>> initColumns() {

        List<IColumn<RoleType, String>> columns = new ArrayList<>();

        initIconColumn(columns, getPageBase());

        initNameColumn(columns);

        initLifeCycleStateColumn(columns);

        initMemberCountColumn(columns, getPageBase());

        initInducementCountColumn(columns, getPageBase());

        initCreateTimeStampColumn(columns, getPageBase());

        initMigrationActionColumn(columns);

        initExploreActionColumn(columns);

        return columns;
    }

    private void initExploreActionColumn(@NotNull List<IColumn<RoleType, String>> columns) {
        columns.add(new AbstractColumn<>(createStringResource("RoleAnalysisTable.column.title.explore")) {

            @Override
            public String getSortProperty() {
                return AbstractRoleType.F_INDUCEMENT.getLocalPart();
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
                    item.add(AttributeModifier.append("class", "d-flex align-items-center justify-content-center"));
                    item.add(repeatingView);

                    RoleType role = rowModel.getObject();

                    RoleAnalysisCandidateRoleType candidateRole = getAssociatedCandidateRole(role);
                    AjaxCompositedIconSubmitButton exploreButton = buildExploreButton(repeatingView.newChildId(), candidateRole);
                    repeatingView.add(exploreButton);
                }
            }

        });
    }

    private void initMigrationActionColumn(@NotNull List<IColumn<RoleType, String>> columns) {
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
                Task task = getPageBase().createSimpleTask("MigrateRole");
                OperationResult result = task.getResult();
                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();

                RoleType role = model.getObject();
                PrismObject<RoleAnalysisClusterType> clusterPrism = roleAnalysisService.getClusterTypeObject(
                        getClusterRef().getOid(), task, result);
                if (clusterPrism == null || role == null) {
                    cellItem.add(new EmptyPanel(componentId));
                    return;
                }

                RoleAnalysisCandidateRoleType candidateRoleType = getAssociatedCandidateRole(role);
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
                    AjaxCompositedIconSubmitButton migrationButton = buildMigrationButton(componentId, role, clusterPrism);
                    cellItem.add(migrationButton);
                }
            }

            private @NotNull AjaxCompositedIconSubmitButton buildMigrationButton(
                    @NotNull String componentId,
                    @NotNull RoleType role,
                    @NotNull PrismObject<RoleAnalysisClusterType> clusterPrism) {
                CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                        GuiStyleConstants.CLASS_PLUS_CIRCLE, IconCssStyle.IN_ROW_STYLE);
                AjaxCompositedIconSubmitButton migrationButton = new AjaxCompositedIconSubmitButton(componentId,
                        iconBuilder.build(),
                        createStringResource("RoleMining.button.title.execute.migration")) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        Task task = getPageBase().createSimpleTask("MigrateRole");
                        OperationResult result = task.getResult();

                        String taskOid = UUID.randomUUID().toString();
                        RoleAnalysisCandidateRoleType candidateRoleType = getAssociatedCandidateRole(role);
                        List<ObjectReferenceType> candidateMembers = candidateRoleType.getCandidateMembers();
                        ObjectSetType members = new ObjectSetType();
                        candidateMembers.forEach(member -> members.getObjectRef().add(member.clone()));
                        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();

                        roleAnalysisService.clusterObjectMigrationRecompute(
                                getClusterRef().getOid(), role.getOid(), task, result);

                        ActivityDefinitionType activity = createActivity(members, role.getOid());

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
                    protected void onError(@NotNull AjaxRequestTarget target) {
                        target.add(((PageBase) getPage()).getFeedbackPanel());
                    }

                };
                migrationButton.titleAsLabel(true);
                migrationButton.setOutputMarkupId(true);
                migrationButton.add(AttributeModifier.append("class", "btn btn-success btn-sm"));
                return migrationButton;
            }

            @Override
            public boolean isSortable() {
                return false;
            }

        });
    }

    private void initLifeCycleStateColumn(@NotNull List<IColumn<RoleType, String>> columns) {
        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public String getSortProperty() {
                return FocusType.F_ACTIVATION.getLocalPart();
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
                label.add(AttributeModifier.append("class",
                        "badge " + DisplayForLifecycleState.valueOfOrDefault(lifecycleState).getCssClass()));
                label.add(AttributeModifier.append("style", "width: 100%; height: 100%"));
                label.setOutputMarkupId(true);
                item.add(label);
            }

        });
    }

    @SuppressWarnings("unchecked")
    private TileTablePanel<RoleAnalysisSessionTileModel<SelectableBean<RoleAnalysisSessionType>>, SelectableBean<RoleAnalysisSessionType>> getTable() {
        return (TileTablePanel<RoleAnalysisSessionTileModel<SelectableBean<RoleAnalysisSessionType>>, SelectableBean<RoleAnalysisSessionType>>)
                get(createComponentPath(ID_DATATABLE));
    }

    @NotNull
    private AjaxCompositedIconSubmitButton buildExploreButton(String componentId, RoleAnalysisCandidateRoleType candidateRole) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                GuiStyleConstants.CLASS_ICON_SEARCH, IconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton migrationButton = new AjaxCompositedIconSubmitButton(componentId,
                iconBuilder.build(),
                createStringResource("Explore candidate")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                onExploreSubmitPerform(candidateRole);
            }

            @Override
            protected void onError(@NotNull AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        migrationButton.titleAsLabel(true);
        migrationButton.setOutputMarkupId(true);
        migrationButton.add(AttributeModifier.append("class", "btn btn-primary btn-sm"));
        return migrationButton;
    }

    private void onExploreSubmitPerform(@NotNull RoleAnalysisCandidateRoleType candidateRole) {
        StringBuilder stringBuilder = new StringBuilder();
        Long id = candidateRole.getId();
        stringBuilder.append(id).append(",");
        getPageBase().clearBreadcrumbs();

        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, getClusterRef());
        parameters.add("panelId", "clusterDetails");
        parameters.add(PARAM_CANDIDATE_ROLE_ID, stringBuilder.toString());
        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                .getObjectDetailsPage(RoleAnalysisClusterType.class);
        getPageBase().navigateToNext(detailsPageClass, parameters);
    }

    private @NotNull AjaxIconButton buildRefreshButton() {
        AjaxIconButton refreshTable = new AjaxIconButton("refreshTable",
                Model.of("fa fa-refresh"), Model.of()) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                onRefresh(ajaxRequestTarget);
            }
        };

        refreshTable.setOutputMarkupId(true);
        refreshTable.add(AttributeModifier.replace("title",
                createStringResource("RoleAnalysisTable.refresh")));
        refreshTable.add(new TooltipBehavior());
        return refreshTable;
    }

    @Override
    public PageBase getPageBase() {
        return pageBase;
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

    private ActivityDefinitionType createActivity(ObjectSetType members, String roleOid) {

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

    protected void onRefresh(@NotNull AjaxRequestTarget target) {
        target.add(this);
    }

    private ObjectReferenceType getClusterRef() {
        RoleAnalysisCandidateRolesDto modelObject = RoleAnalysisCandidateRoleTileTable.this.getModelObject();
        return modelObject.getClusterRef();
    }

    private ObjectReferenceType getSessionRef() {
        RoleAnalysisCandidateRolesDto modelObject = RoleAnalysisCandidateRoleTileTable.this.getModelObject();
        return modelObject.getSessionRef();
    }

    public RoleAnalysisCandidateRoleType getAssociatedCandidateRole(@NotNull RoleType role) {
        RoleAnalysisCandidateRolesDto modelObject = RoleAnalysisCandidateRoleTileTable.this.getModelObject();
        return modelObject.getCandidateRole(role.getOid());
    }
}
