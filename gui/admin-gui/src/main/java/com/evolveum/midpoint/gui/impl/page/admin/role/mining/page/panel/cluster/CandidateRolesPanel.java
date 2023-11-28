/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.resolveDateAndTime;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_CANDIDATE_ROLE_ID;
import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;

import java.io.Serial;
import java.util.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "candidateRoles")
@PanelInstance(
        identifier = "candidateRoles",
        applicableForType = RoleAnalysisClusterType.class,
        display = @PanelDisplay(
                label = "RoleAnalysisClusterType.candidateRoles",
                icon = GuiStyleConstants.CLASS_GROUP_ICON,
                order = 30
        )
)
public class CandidateRolesPanel extends AbstractObjectMainPanel<RoleAnalysisClusterType, ObjectDetailsModels<RoleAnalysisClusterType>> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_PANEL = "panelId";

    private static final String DOT_CLASS = CandidateRolesPanel.class.getName() + ".";
    private static final String OP_PREPARE_OBJECTS = DOT_CLASS + "prepareObjects";
    private final OperationResult result = new OperationResult(OP_PREPARE_OBJECTS);
    private static final String OP_UPDATE_STATUS = DOT_CLASS + "updateOperationStatus";

    public CandidateRolesPanel(String id, ObjectDetailsModels<RoleAnalysisClusterType> model,
            ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        Task task = getPageBase().createSimpleTask(OP_PREPARE_OBJECTS);

        RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();
        List<RoleAnalysisCandidateRoleType> candidateRoles = cluster.getCandidateRoles();

        HashMap<String, RoleAnalysisCandidateRoleType> cacheCandidate = new HashMap<>();
        List<RoleType> roles = new ArrayList<>();
        for (RoleAnalysisCandidateRoleType candidateRoleType : candidateRoles) {
            ObjectReferenceType candidateRoleRef = candidateRoleType.getCandidateRoleRef();
            PrismObject<RoleType> role = ((PageBase) getPage()).getRoleAnalysisService()
                    .getRoleTypeObject(candidateRoleRef.getOid(), task, result);
            if (Objects.nonNull(role)) {
                cacheCandidate.put(candidateRoleRef.getOid(), candidateRoleType);
                roles.add(role.asObjectable());
            }
        }

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        RoleMiningProvider<RoleType> provider = new RoleMiningProvider<>(
                this, new ListModel<>(roles) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<RoleType> object) {
                super.setObject(roles);
            }
        }, false);

        BoxedTablePanel<RoleType> panel = generateTable(provider, cacheCandidate, cluster);
        container.add(panel);

    }

    private BoxedTablePanel<RoleType> generateTable(RoleMiningProvider<RoleType> provider,
            HashMap<String, RoleAnalysisCandidateRoleType> cacheCandidate, RoleAnalysisClusterType cluster) {

        BoxedTablePanel<RoleType> table = new BoxedTablePanel<>(
                ID_PANEL, provider, initColumns(cacheCandidate, cluster)) {
            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {
                AjaxIconButton refreshIcon = new AjaxIconButton(id, new Model<>(GuiStyleConstants.CLASS_RECONCILE),
                        createStringResource("MainObjectListPanel.refresh")) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        onRefresh();
                    }
                };
                refreshIcon.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
                return refreshIcon;
            }
        };
        table.setOutputMarkupId(true);
        return table;
    }

    private List<IColumn<RoleType, String>> initColumns(HashMap<String, RoleAnalysisCandidateRoleType> candidateRoles,
            RoleAnalysisClusterType cluster) {

        List<IColumn<RoleType, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<RoleType>> cellItem, String componentId, IModel<RoleType> rowModel) {
                super.populateItem(cellItem, componentId, rowModel);
            }

            @Override
            protected DisplayType getIconDisplayType(IModel<RoleType> rowModel) {
                return GuiDisplayTypeUtil
                        .createDisplayType(IconAndStylesUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));
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

        columns.add(new AbstractColumn<>(createStringResource("Status")) {

            @Override
            public String getSortProperty() {
                return RoleType.F_ACTIVATION.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleType>> item, String componentId,
                    IModel<RoleType> rowModel) {
                item.add(new Label(componentId, rowModel.getObject().getActivation().getEffectiveStatus()));
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
                RoleAnalysisCandidateRoleType candidateRoleType = candidateRoles.get(rowModel.getObject().getOid());
                int membersCount = 0;
                if (candidateRoleType != null) {
                    List<ObjectReferenceType> candidateMembers = candidateRoleType.getCandidateMembers();
                    if (candidateMembers != null) {
                        membersCount = candidateMembers.size();
                    }
                }

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
                item.add(new Label(componentId, resolveDateAndTime(rowModel.getObject().getMetadata().getCreateTimestamp())));
            }

        });

        columns.add(new AbstractExportableColumn<>(createStringResource("Migrate")) {

            @Override
            public IModel<?> getDataModel(IModel<RoleType> iModel) {
                return null;
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
                }

                Task task = getPageBase().createSimpleTask(OP_UPDATE_STATUS);
                OperationResult result = task.getResult();
                RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();
                RoleAnalysisCandidateRoleType candidateRoleType = candidateRoles.get(rowModel.getObject().getOid());
                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
                String stateString = roleAnalysisService.recomputeAndResolveClusterCandidateRoleOpStatus(
                        cluster.asPrismObject(), candidateRoleType,
                        result, task);

                OperationExecutionType operationExecution = candidateRoleType.getOperationExecution();
                if (operationExecution != null
                        && operationExecution.getTaskRef() != null
                        && operationExecution.getTaskRef().getOid() != null) {
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
                    item.add(ajaxLinkPanel);
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

                            OperationResultStatusType status = getPageBase().getRoleAnalysisService()
                                    .getOperationExecutionStatus(cluster.asPrismObject(), task, result);

                            if (status != null && status.equals(OperationResultStatusType.IN_PROGRESS)) {
                                warn("Couldn't start migration. Some process is already in progress.");
                                LOGGER.error("Couldn't start migration. Some process is already in progress.");
                                target.add(getPageBase().getFeedbackPanel());
                                return;
                            }

                            RoleType role = rowModel.getObject();
                            String taskOid = UUID.randomUUID().toString();
                            RoleAnalysisCandidateRoleType candidateRoleType = candidateRoles.get(role.getOid());
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
                                navigateToRoleAnalysisCluster(cluster.getOid());
                            }

                            info("Role migration started.");
                            target.add(getPageBase().getFeedbackPanel());
                            roleAnalysisService.setCandidateRoleOpStatus(getObjectDetailsModels().getObjectType().asPrismObject(),
                                    candidateRoleType, taskOid, OperationResultStatusType.IN_PROGRESS, null,
                                    result, task, OperationExecutionRecordTypeType.SIMPLE);
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

        });

        columns.add(new AbstractExportableColumn<>(createStringResource("RoleMining.button.title.load")) {

            @Override
            public IModel<?> getDataModel(IModel<RoleType> iModel) {
                return null;
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
                            String clusterOid = getObjectDetailsModels().getObjectType().getOid();
                            parameters.add(OnePageParameterEncoder.PARAMETER, clusterOid);
                            parameters.add("panelId", "clusterDetails");
                            parameters.add(PARAM_CANDIDATE_ROLE_ID, rowModel.getObject().getOid());
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

    private void onRefresh() {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, getObjectDetailsModels().getObjectType().getOid());
        parameters.add(ID_PANEL, getPanelConfiguration().getIdentifier());
        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                .getObjectDetailsPage(RoleAnalysisClusterType.class);
        getPageBase().navigateToNext(detailsPageClass, parameters);
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

}
