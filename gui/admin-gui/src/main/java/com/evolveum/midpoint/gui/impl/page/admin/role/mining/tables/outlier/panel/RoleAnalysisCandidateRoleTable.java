/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.resolveDateAndTime;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_CANDIDATE_ROLE_ID;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_TABLE_SETTING;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableCellFillResolver.generateObjectColors;
import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;

import java.io.Serial;
import java.util.*;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.LegendPanel;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.DisplayForLifecycleState;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.gui.impl.util.TableUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
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

    private Map<String, String> colorPalete;

    public RoleAnalysisCandidateRoleTable(String id,
            @NotNull RoleAnalysisClusterType cluster,
            HashMap<String, RoleAnalysisCandidateRoleType> cacheCandidate,
            List<RoleType> roles, List<String> selectedCandidates) {
        super(id);
        this.colorPalete = generateObjectColors(selectedCandidates);
        createTable(cacheCandidate, cluster, roles);
    }

    private void createTable(HashMap<String, RoleAnalysisCandidateRoleType> cacheCandidate,
            RoleAnalysisClusterType cluster, List<RoleType> roles) {

        MainObjectListPanel<RoleType> table = new MainObjectListPanel<>(ID_DATATABLE, RoleType.class, null) {

            @Override
            public String getAdditionalBoxCssClasses() {
                return " m-0";
            }

            @Override
            protected IColumn<SelectableBean<RoleType>, String> createCheckboxColumn() {
                return new CheckBoxHeaderColumn<>() {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem, String componentId,
                            IModel<SelectableBean<RoleType>> rowModel) {
                        super.populateItem(cellItem, componentId, rowModel);
                    }

                    @SuppressWarnings("rawtypes")
                    @Override
                    protected void onUpdateHeader(AjaxRequestTarget target, boolean selected, DataTable table) {
                        super.onUpdateHeader(target, selected, table);
                        prepareColorsPalete(cacheCandidate, getSelectedObjects());
                    }

                    @SuppressWarnings("rawtypes")
                    @Override
                    protected void onUpdateRow(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem, AjaxRequestTarget target,
                            DataTable table, IModel<SelectableBean<RoleType>> rowModel, IModel<Boolean> selected) {
                        super.onUpdateRow(cellItem, target, table, rowModel, selected);

                        prepareColorsPalete(cacheCandidate, getSelectedObjects());

                        TableUtil.updateRows(table, target);

                    }
                };
            }

            @Override
            protected List<Component> createToolbarButtonsList(String buttonId) {
                return List.of(RoleAnalysisCandidateRoleTable.this.createRefreshButton(buttonId));
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                if (isDeleteOperationEnabled()) {
                    List<InlineMenuItem> menuItems = new ArrayList<>();
                    menuItems.add(RoleAnalysisCandidateRoleTable.this.createDeleteInlineMenu(cacheCandidate, cluster));
                    return menuItems;
                } else {
                    return null;
                }
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
                        if (lifecycleState == null) {
                            lifecycleState = DisplayForLifecycleState.ACTIVE.name();
                        }
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

                                if (membersCount == 1) {
                                    String oid = candidateMembers.get(0).getOid();
                                    if (oid == null) {
                                        membersCount = 0;
                                    }
                                }
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

                if (isMigrateButtonEnabled()) {
                    column = new AbstractExportableColumn<>(
                            createStringResource("")) {

                        @Override
                        public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
                            return null;
                        }

                        @Override
                        public Component getHeader(String componentId) {
                            return new Label(componentId,
                                    createStringResource("RoleMining.button.title.execute.migration"));
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
                                            roleAnalysisService.executeRoleAnalysisRoleMigrationTask(getPageBase().getModelInteractionService(),
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
                                                        RoleAnalysisOperationType.MIGRATION,
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
                } else {
                    column = new AbstractExportableColumn<>(
                            createStringResource("")) {

                        @Override
                        public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
                            return null;
                        }

                        @Override
                        public Component getHeader(String componentId) {
                            return new Label(componentId,
                                    createStringResource("RoleAnalysisCandidateRoleTable.legend"));
                        }

                        @Override
                        public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                                String componentId, IModel<SelectableBean<RoleType>> model) {
                            SelectableBean<RoleType> object = model.getObject();
                            RoleType role = object.getValue();
                            RoleAnalysisCandidateRoleType candidateRoleType = cacheCandidate.get(role.getOid());
                            Long id = candidateRoleType.getId();

                            LoadableModel<String> loadableModel = new LoadableModel<>() {
                                @Override
                                protected String load() {
                                    if (colorPalete == null || colorPalete.isEmpty()) {
                                        return "#00A65A";
                                    } else {
                                        return colorPalete.get(id.toString());
                                    }
                                }
                            };

                            LegendPanel label = new LegendPanel(componentId, loadableModel);
                            label.setOutputMarkupId(true);
                            cellItem.add(label);
                        }

                        @Override
                        public boolean isSortable() {
                            return false;
                        }

                    };
                    columns.add(column);
                }
                column = new AbstractExportableColumn<>(
                        createStringResource("")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
                        return null;
                    }

                    @Override
                    public Component getHeader(String componentId) {

                        if (isMigrateButtonEnabled()) {
                            return new Label(componentId,
                                    createStringResource("RoleMining.button.title.load"));
                        }

                        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                                GuiStyleConstants.CLASS_ICON_SEARCH, LayeredIconCssStyle.IN_ROW_STYLE);
                        AjaxCompositedIconSubmitButton exploreSelected = new AjaxCompositedIconSubmitButton(componentId,
                                iconBuilder.build(),
                                createStringResource("RoleMining.button.title.load")) {
                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            protected void onSubmit(AjaxRequestTarget target) {
                                List<SelectableBean<RoleType>> selectedObjects = getSelectedObjects();
                                if (!selectedObjects.isEmpty()) {

                                    StringBuilder stringBuilder = new StringBuilder();
                                    for (SelectableBean<RoleType> selectedObject : selectedObjects) {
                                        String rolOid = selectedObject.getValue().getOid();
                                        cacheCandidate.get(rolOid);
                                        RoleAnalysisCandidateRoleType candidateRole = cacheCandidate.get(rolOid);
                                        Long id = candidateRole.getId();
                                        stringBuilder.append(id).append(",");
                                    }
                                    getPageBase().clearBreadcrumbs();
                                    PageParameters parameters = new PageParameters();
                                    String clusterOid = cluster.getOid();
                                    parameters.add(OnePageParameterEncoder.PARAMETER, clusterOid);
                                    parameters.add("panelId", "clusterDetails");
                                    parameters.add(PARAM_CANDIDATE_ROLE_ID, stringBuilder.toString());
                                    Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                                            .getObjectDetailsPage(RoleAnalysisClusterType.class);
                                    getPageBase().navigateToNext(detailsPageClass, parameters);
                                }
                            }

                            @Override
                            protected void onError(AjaxRequestTarget target) {
                                target.add(((PageBase) getPage()).getFeedbackPanel());
                            }
                        };

                        exploreSelected.setEnabled(true);
                        exploreSelected.add(AttributeAppender.append("class", "btn btn-primary btn-sm"));
                        exploreSelected.titleAsLabel(true);
                        exploreSelected.setOutputMarkupId(true);
                        return exploreSelected;
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
                        RoleAnalysisCandidateRoleType candidateRole = cacheCandidate.get(oid);
                        Long id = candidateRole.getId();

                        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                                GuiStyleConstants.CLASS_ICON_SEARCH, LayeredIconCssStyle.IN_ROW_STYLE);
                        AjaxCompositedIconSubmitButton exploreButton = new AjaxCompositedIconSubmitButton(componentId,
                                iconBuilder.build(),
                                createStringResource("RoleMining.button.title.load")) {
                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            protected void onSubmit(AjaxRequestTarget target) {
                                getPageBase().clearBreadcrumbs();
                                PageParameters parameters = new PageParameters();
                                String clusterOid = cluster.getOid();
                                parameters.add(OnePageParameterEncoder.PARAMETER, clusterOid);
                                parameters.add("panelId", "clusterDetails");
                                parameters.add(PARAM_CANDIDATE_ROLE_ID, id.toString());
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

                        exploreButton.setEnabled(true);
                        exploreButton.add(AttributeAppender.append("class", "btn btn-primary btn-sm"));
                        exploreButton.titleAsLabel(true);
                        exploreButton.setOutputMarkupId(true);

                        RoleAnalysisCandidateRoleType candidateRoleType = cacheCandidate.get(oid);
                        RoleAnalysisOperationStatusType operationStatus = candidateRoleType.getOperationStatus();
                        if (operationStatus != null) {
                            OperationResultStatusType status = operationStatus.getStatus();
                            if (status != null
                                    && (status.equals(OperationResultStatusType.IN_PROGRESS)
                                    || status.equals(OperationResultStatusType.SUCCESS))) {
                                exploreButton.setEnabled(false);
                                exploreButton.add(AttributeAppender.append("class", "btn btn-default btn-sm"));

                            } else {
                                exploreButton.setEnabled(true);
                                exploreButton.add(AttributeAppender.append("class", "btn btn-primary btn-sm"));
                            }

                        }

                        cellItem.add(exploreButton);

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

    private void prepareColorsPalete(HashMap<String, RoleAnalysisCandidateRoleType> cacheCandidate,
            List<SelectableBean<RoleType>> selectedObjects) {

        if (selectedObjects == null || selectedObjects.isEmpty()) {
            return;
        }
        List<String> containerIds = new ArrayList<>();
        for (SelectableBean<RoleType> selectedObject : selectedObjects) {
            RoleAnalysisCandidateRoleType candidateRoleType = cacheCandidate.get(selectedObject.getValue().getOid());
            String id = candidateRoleType.getId().toString();
            containerIds.add(id);
        }
        colorPalete = generateObjectColors(containerIds);
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

            @SuppressWarnings("rawtypes")
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

    @SuppressWarnings("unchecked")
    private MainObjectListPanel<RoleType> getTable() {
        return (MainObjectListPanel<RoleType>) get(ID_DATATABLE);
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    protected void onRefresh(AjaxRequestTarget target) {

    }

    protected boolean isMigrateButtonEnabled() {
        return true;
    }

    protected boolean isDeleteOperationEnabled() {
        return true;
    }

}
