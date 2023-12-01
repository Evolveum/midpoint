/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.resolveDateAndTime;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_CANDIDATE_ROLE_ID;
import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;

import java.io.Serial;
import java.util.*;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.DisplayForLifecycleState;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisCandidateRoleTable extends BasePanel<String> {
    private static final String ID_DATATABLE = "datatable";
    private static final String DOT_CLASS = RoleAnalysisCandidateRoleTable.class.getName() + ".";
    private static final String OP_PREPARE_OBJECTS = DOT_CLASS + "prepareObjects";
    private static final String OP_UPDATE_STATUS = DOT_CLASS + "updateOperationStatus";
    private static final String OP_DELETE_PERFORM = DOT_CLASS + "deletePerformed";

    public RoleAnalysisCandidateRoleTable(String id,
            @NotNull RoleAnalysisClusterType cluster,
            HashMap<String, RoleAnalysisCandidateRoleType> cacheCandidate,
            List<RoleType> roles) {
        super(id);

        createTable(cacheCandidate, cluster, roles);
    }

    private void createTable(HashMap<String, RoleAnalysisCandidateRoleType> cacheCandidate,
            RoleAnalysisClusterType cluster, List<RoleType> roles) {
        MainObjectListPanel<RoleType> table = new MainObjectListPanel<>(ID_DATATABLE, RoleType.class, null) {

            @Override
            protected IColumn<SelectableBean<RoleType>, String> createCheckboxColumn() {
                return super.createCheckboxColumn();
            }

            @Override
            protected List<Component> createToolbarButtonsList(String buttonId) {
                return List.of(RoleAnalysisCandidateRoleTable.this.createRefreshButton(buttonId));
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                List<InlineMenuItem> menuItems = new ArrayList<>();
                menuItems.add(RoleAnalysisCandidateRoleTable.this.createDeleteInlineMenu(cacheCandidate, cluster));
                return menuItems;
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return null;
            }

            @Override
            protected boolean isHeaderVisible() {
                return false;
            }

            @Override
            protected boolean isCreateNewObjectVisible() {
                return false;
            }

            @Override
            protected IColumn<SelectableBean<RoleType>, String> createIconColumn() {
                return super.createIconColumn();
            }

            @Override
            protected ISelectableDataProvider<SelectableBean<RoleType>> createProvider() {
                return RoleAnalysisCandidateRoleTable.this.createProvider(roles);
            }

            @Override
            protected List<IColumn<SelectableBean<RoleType>, String>> createDefaultColumns() {

                List<IColumn<SelectableBean<RoleType>, String>> columns = new ArrayList<>();

                IColumn<SelectableBean<RoleType>, String> column;

                column = new AbstractExportableColumn<>(
                        createStringResource("")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
                        return null;
                    }

                    @Override
                    public Component getHeader(String componentId) {
                        return new Label(componentId,
                                createStringResource("RoleAnalysisCandidateRoleTable.lifecycleState"));
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleType>> model) {
                        SelectableBean<RoleType> object = model.getObject();
                        RoleType role = object.getValue();

                        String lifecycleState = role.getLifecycleState();
                        Label label = new Label(componentId, lifecycleState);
                        label.add(AttributeAppender.append("class",
                                "badge " + DisplayForLifecycleState.valueOfOrDefault(lifecycleState).getCssClass()));
                        label.add(AttributeAppender.append("style", "width: 100%; height: 100%"));
                        label.setOutputMarkupId(true);
                        cellItem.add(label);
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                columns.add(column);
                column = new AbstractExportableColumn<>(
                        createStringResource("")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
                        return null;
                    }

                    @Override
                    public Component getHeader(String componentId) {
                        return new Label(componentId,
                                createStringResource("RoleAnalysisCandidateRoleTable.members.count"));
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleType>> model) {
                        SelectableBean<RoleType> object = model.getObject();
                        RoleType role = object.getValue();

                        RoleAnalysisCandidateRoleType candidateRoleType = cacheCandidate.get(role.getOid());
                        int membersCount = 0;
                        if (candidateRoleType != null) {
                            List<ObjectReferenceType> candidateMembers = candidateRoleType.getCandidateMembers();
                            if (candidateMembers != null) {
                                membersCount = candidateMembers.size();
                            }
                        }

                        cellItem.add(new Label(componentId, membersCount));

                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
                        return null;
                    }

                    @Override
                    public Component getHeader(String componentId) {
                        return new Label(componentId,
                                createStringResource("RoleAnalysisCandidateRoleTable.inducements.count"));
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleType>> model) {
                        SelectableBean<RoleType> object = model.getObject();
                        RoleType role = object.getValue();
                        cellItem.add(new Label(componentId, role.getInducement().size()));
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
                        return null;
                    }

                    @Override
                    public Component getHeader(String componentId) {
                        return new Label(componentId,
                                createStringResource("RoleAnalysisCandidateRoleTable.create.timestamp"));
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleType>> model) {
                        SelectableBean<RoleType> object = model.getObject();
                        RoleType role = object.getValue();

                        cellItem.add(new Label(componentId, resolveDateAndTime(role.getMetadata().getCreateTimestamp())));

                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
                        return null;
                    }

                    @Override
                    public Component getHeader(String componentId) {
                        return new Label(componentId,
                                createStringResource("RoleAnalysisCandidateRoleTable.migrate.perform"));
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleType>> model) {
                        SelectableBean<RoleType> object = model.getObject();
                        if (object == null) {
                            cellItem.add(new EmptyPanel(componentId));
                            return;
                        }

                        RoleType role = object.getValue();

                        Task task = getPageBase().createSimpleTask(OP_UPDATE_STATUS);
                        OperationResult result = task.getResult();
                        RoleAnalysisCandidateRoleType candidateRoleType = cacheCandidate.get(role.getOid());
                        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
                        String stateString = roleAnalysisService.recomputeAndResolveClusterCandidateRoleOpStatus(
                                cluster.asPrismObject(), candidateRoleType,
                                result, task);

                        RoleAnalysisOperationStatus operationStatus = candidateRoleType.getOperationStatus();
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
                                    createStringResource("RoleMining.button.title.process")) {
                                @Serial private static final long serialVersionUID = 1L;

                                @Override
                                protected void onSubmit(AjaxRequestTarget target) {
                                    Task task = getPageBase().createSimpleTask(OP_PREPARE_OBJECTS);
                                    OperationResult result = task.getResult();

                                    String taskOid = UUID.randomUUID().toString();
                                    RoleAnalysisCandidateRoleType candidateRoleType = cacheCandidate.get(role.getOid());
                                    List<ObjectReferenceType> candidateMembers = candidateRoleType.getCandidateMembers();
                                    ObjectSetType members = new ObjectSetType();
                                    candidateMembers.forEach(member -> members.getObjectRef().add(member.clone()));
                                    RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();

                                    roleAnalysisService.clusterObjectMigrationRecompute(
                                            cluster.getOid(), role.getOid(), task, result);

                                    ActivityDefinitionType activity = null;
                                    try {
                                        activity = createActivity(members, role.getOid());
                                    } catch (SchemaException e) {
                                        LOGGER.error("Couldn't create activity for role migration: " + role.getOid(), e);
                                    }
                                    if (activity != null) {
                                        roleAnalysisService.executeMigrationTask(
                                                cluster.asPrismObject(), activity, role.asPrismObject(), taskOid,
                                                null, task, result);
                                        if (result.isWarning()) {
                                            warn(result.getMessage());
                                            target.add(((PageBase) getPage()).getFeedbackPanel());
                                        } else {
                                            MidPointPrincipal user = AuthUtil.getPrincipalUser();
                                            roleAnalysisService.setCandidateRoleOpStatus(cluster.asPrismObject(),
                                                    candidateRoleType,
                                                    taskOid,
                                                    OperationResultStatusType.IN_PROGRESS,
                                                    null,
                                                    result,
                                                    task,
                                                    RoleAnalysisOperation.MIGRATION,
                                                    user.getFocus());
                                            navigateToRoleAnalysisCluster(cluster.getOid());
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

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
                        return null;
                    }

                    @Override
                    public Component getHeader(String componentId) {
                        return new Label(componentId,
                                createStringResource("RoleMining.button.title.load"));
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleType>> model) {
                        SelectableBean<RoleType> object = model.getObject();

                        if (object == null) {
                            cellItem.add(new EmptyPanel(componentId));
                            return;
                        }

                        RoleType role = object.getValue();

                        String oid = role.getOid();

                        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                                GuiStyleConstants.CLASS_ICON_SEARCH, LayeredIconCssStyle.IN_ROW_STYLE);
                        AjaxCompositedIconSubmitButton migrationButton = new AjaxCompositedIconSubmitButton(componentId,
                                iconBuilder.build(),
                                createStringResource("RoleMining.button.title.load")) {
                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            protected void onSubmit(AjaxRequestTarget target) {
                                PageParameters parameters = new PageParameters();
                                String clusterOid = cluster.getOid();
                                parameters.add(OnePageParameterEncoder.PARAMETER, clusterOid);
                                parameters.add("panelId", "clusterDetails");
                                parameters.add(PARAM_CANDIDATE_ROLE_ID, oid);
                                Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                                        .getObjectDetailsPage(RoleAnalysisClusterType.class);
                                getPageBase().navigateToNext(detailsPageClass, parameters);
                            }

                            @Override
                            protected void onError(AjaxRequestTarget target) {
                                target.add(((PageBase) getPage()).getFeedbackPanel());
                            }
                        };

                        migrationButton.setEnabled(true);
                        migrationButton.add(AttributeAppender.append("class", "btn btn-primary btn-sm"));
                        migrationButton.titleAsLabel(true);
                        migrationButton.setOutputMarkupId(true);

                        RoleAnalysisCandidateRoleType candidateRoleType = cacheCandidate.get(oid);
                        RoleAnalysisOperationStatus operationStatus = candidateRoleType.getOperationStatus();
                        if (operationStatus != null) {
                            OperationResultStatusType status = operationStatus.getStatus();
                            if (status != null
                                    && (status.equals(OperationResultStatusType.IN_PROGRESS)
                                    || status.equals(OperationResultStatusType.SUCCESS))) {
                                migrationButton.setEnabled(false);
                                migrationButton.add(AttributeAppender.append("class", "btn btn-default btn-sm"));

                            } else {
                                migrationButton.setEnabled(true);
                                migrationButton.add(AttributeAppender.append("class", "btn btn-primary btn-sm"));
                            }

                        }

                        cellItem.add(migrationButton);

                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                columns.add(column);

                return columns;
            }
        };

        table.setOutputMarkupId(true);
        add(table);
    }

    private AjaxIconButton createRefreshButton(String buttonId) {
        AjaxIconButton refreshIcon = new AjaxIconButton(buttonId, new Model<>(GuiStyleConstants.CLASS_RECONCILE),
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

    private SelectableBeanObjectDataProvider<RoleType> createProvider(List<RoleType> roles) {

        return new SelectableBeanObjectDataProvider<>(
                RoleAnalysisCandidateRoleTable.this, Set.of()) {

            @Override
            protected List<RoleType> searchObjects(Class type,
                    ObjectQuery query,
                    Collection collection,
                    Task task,
                    OperationResult result) {
                Integer offset = query.getPaging().getOffset();
                Integer maxSize = query.getPaging().getMaxSize();
                return roles.subList(offset, offset + maxSize);
            }

            @Override
            protected Integer countObjects(Class<RoleType> type,
                    ObjectQuery query,
                    Collection<SelectorOptions<GetOperationOptions>> currentOptions,
                    Task task,
                    OperationResult result) {
                return roles.size();
            }
        };
    }

    @NotNull
    private AjaxLinkPanel taskLinkPanel(String componentId, String stateString, RoleAnalysisOperationStatus operationExecution) {
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

    private InlineMenuItem createDeleteInlineMenu(HashMap<String, RoleAnalysisCandidateRoleType> cacheCandidate,
            RoleAnalysisClusterType cluster) {
        return new ButtonInlineMenuItem(createStringResource("MainObjectListPanel.menu.delete")) {
            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_TRASH);
            }

            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<RoleType>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        Task task = getPageBase().createSimpleTask(OP_DELETE_PERFORM);
                        OperationResult result = task.getResult();

                        List<SelectableBean<RoleType>> selectedObjects = getTable().getSelectedObjects();
                        PageBase page = (PageBase) getPage();
                        RoleAnalysisService roleAnalysisService = page.getRoleAnalysisService();

                        if (selectedObjects.size() == 1 && getRowModel() == null) {
                            try {
                                SelectableBean<RoleType> selectedRole = selectedObjects.get(0);
                                String oid = selectedRole.getValue().getOid();
                                RoleAnalysisCandidateRoleType candidateRoleType = cacheCandidate.get(oid);
                                roleAnalysisService.deleteSingleCandidateRole(
                                        cluster.asPrismObject(),
                                        candidateRoleType,
                                        result,
                                        task);
                            } catch (Exception e) {
                                throw new RuntimeException("Couldn't delete selected cluster", e);
                            }
                        } else if (getRowModel() != null) {
                            try {
                                IModel<SelectableBean<RoleType>> rowModel = getRowModel();
                                String oid = rowModel.getObject().getValue().getOid();
                                RoleAnalysisCandidateRoleType candidateRoleType = cacheCandidate.get(oid);
                                roleAnalysisService.deleteSingleCandidateRole(
                                        cluster.asPrismObject(),
                                        candidateRoleType,
                                        result,
                                        task);
                            } catch (Exception e) {
                                throw new RuntimeException("Couldn't delete selected cluster", e);
                            }
                        } else {
                            for (SelectableBean<RoleType> selectedObject : selectedObjects) {
                                try {
                                    RoleType role = selectedObject.getValue();
                                    String oid = role.getOid();
                                    RoleAnalysisCandidateRoleType candidateRoleType = cacheCandidate.get(oid);
                                    roleAnalysisService.deleteSingleCandidateRole(
                                            cluster.asPrismObject(),
                                            candidateRoleType,
                                            result,
                                            task);
                                } catch (Exception e) {
                                    throw new RuntimeException("Couldn't delete selected cluster", e);
                                }
                            }
                        }

                        onRefresh(target);
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                ColumnMenuAction<?> action = (ColumnMenuAction<?>) getAction();
                return RoleAnalysisCandidateRoleTable.this.getConfirmationMessageModel(action);
            }
        };
    }

    private IModel<String> getConfirmationMessageModel(ColumnMenuAction<?> action) {
        if (action.getRowModel() == null) {
            return createStringResource("RoleAnalysisCandidateRoleTable.message.deleteActionAllObjects");
        } else {
            return createStringResource("RoleAnalysisCandidateRoleTable.message.deleteAction");
        }
    }

    private MainObjectListPanel<RoleType> getTable() {
        return (MainObjectListPanel<RoleType>) get(ID_DATATABLE);
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    protected void onRefresh(AjaxRequestTarget target) {

    }

}
