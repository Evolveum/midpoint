/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile.mining.candidate;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_CANDIDATE_ROLE_ID;
import static com.evolveum.midpoint.gui.impl.util.DetailsPageUtil.dispatchToObjectDetailsPage;
import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.DisplayForLifecycleState;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisCandidateTilePanel<T extends Serializable> extends BasePanel<RoleAnalysisCandidateTileModel<T>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_OBJECT_TITLE = "objectTitle";
    private static final String ID_ICON = "icon";
    private static final String ID_TITLE = "title";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_EXPLORE_PATTERN_BUTTON = "explorePatternButton";
    private static final String ID_MIGRATION_BUTTON = "migrationButton";
    private static final String ID_PROCESS_MODE = "mode";
    private static final String ID_USER_COUNT = "users-count";
    private static final String ID_ROLE_COUNT = "roles-count";
    private static final String ID_STATUS_BAR = "status";
    private static final String ID_BUTTON_BAR = "buttonBar";

    public RoleAnalysisCandidateTilePanel(String id, IModel<RoleAnalysisCandidateTileModel<T>> model) {
        super(id, model);
        initLayout();
    }

    protected void initLayout() {
        RoleAnalysisCandidateTileModel<T> modelObject = getModelObject();

        initDefaultStyle();

        initStatusBar();

        initButtonToolBarPanel();

        initNamePanel(modelObject);

        initDescriptionPanel();

        buildExploreButton();

        initMigrationButton();

        initProcessModePanel();

        initFirstCountPanel();

        initSecondCountPanel();

    }

    private void initSecondCountPanel() {
        IconWithLabel processedObjectCount = new IconWithLabel(ID_ROLE_COUNT, () -> getModelObject().getInducementsCount()) {
            @Override
            public String getIconCssClass() {
                return "fe fe-assignment";
            }
        };
        processedObjectCount.setOutputMarkupId(true);
        processedObjectCount.add(AttributeAppender.replace("title", () -> "Inducements count: " +
                getModelObject().getInducementsCount()));
        processedObjectCount.add(new TooltipBehavior());
        add(processedObjectCount);
    }

    private void initFirstCountPanel() {
        IconWithLabel clusterCount = new IconWithLabel(ID_USER_COUNT, () -> getModelObject().getMembersCount()) {
            @Override
            public String getIconCssClass() {
                return "fa fa-users";
            }
        };

        clusterCount.setOutputMarkupId(true);
        clusterCount.add(AttributeAppender.replace("title", () -> "User count: " +
                getModelObject().getMembersCount()));
        clusterCount.add(new TooltipBehavior());
        add(clusterCount);
    }

    private void initProcessModePanel() {
        String processModeTitle = getModelObject().getProcessMode();
        IconWithLabel mode = new IconWithLabel(ID_PROCESS_MODE, () -> processModeTitle) {
            @Override
            public String getIconCssClass() {
                return "fa fa-cogs";
            }
        };
        mode.add(AttributeAppender.replace("title", () -> "Process mode: " + processModeTitle));
        mode.add(new TooltipBehavior());
        mode.setOutputMarkupId(true);
        add(mode);
    }

    private void initDefaultStyle() {
        setOutputMarkupId(true);
        add(AttributeAppender.append("class",
                "catalog-tile-panel d-flex flex-column align-items-center border w-100 h-100 p-3"));
        add(AttributeAppender.append("style", "width:25%"));
    }

    private void initDescriptionPanel() {
        Label description = new Label(ID_DESCRIPTION, () -> getModelObject().getCreateDate());
        description.add(AttributeAppender.replace("title", () -> getModelObject().getCreateDate()));
        description.add(new TooltipBehavior());
        add(description);
    }

    private void initNamePanel(RoleAnalysisCandidateTileModel<T> modelObject) {
        IconWithLabel objectTitle = new IconWithLabel(ID_OBJECT_TITLE, () -> getModelObject().getName()) {
            @Override
            public String getIconCssClass() {
                return RoleAnalysisCandidateTilePanel.this.getModelObject().getIcon();
            }

            @Override
            protected boolean isLink() {
                return true;
            }

            @Override
            protected void onClickPerform(AjaxRequestTarget target) {
                String oid = modelObject.getRole().getOid();
                dispatchToObjectDetailsPage(RoleType.class, oid, getPageBase(), true);
            }

        };
        objectTitle.setOutputMarkupId(true);
        objectTitle.add(AttributeAppender.replace("style", "font-size:20px"));
        objectTitle.add(AttributeAppender.replace("title", () -> getModelObject().getName()));
        objectTitle.add(new TooltipBehavior());
        add(objectTitle);
    }

    private void initButtonToolBarPanel() {
        DropdownButtonPanel barMenu = new DropdownButtonPanel(ID_BUTTON_BAR, new DropdownButtonDto(
                null, "fa fa-ellipsis-v", null, createMenuItems())) {
            @Override
            protected boolean hasToggleIcon() {
                return false;
            }

            @Override
            protected String getSpecialButtonClass() {
                return " p-0 ";
            }

        };
        barMenu.setOutputMarkupId(true);
        barMenu.add(AttributeModifier.replace("title",
                createStringResource("RoleAnalysis.menu.moreOptions")));
        barMenu.add(new TooltipBehavior());
        add(barMenu);
    }

    private void exploreRolePerform() {
        StringBuilder stringBuilder = new StringBuilder();
        Long id = getModelObject().getId();
        stringBuilder.append(id).append(",");
        getPageBase().clearBreadcrumbs();

        PageParameters parameters = new PageParameters();
        String clusterOid = getModelObject().getClusterOid();
        parameters.add(OnePageParameterEncoder.PARAMETER, clusterOid);
        parameters.add("panelId", "clusterDetails");
        parameters.add(PARAM_CANDIDATE_ROLE_ID, stringBuilder.toString());
        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                .getObjectDetailsPage(RoleAnalysisClusterType.class);
        getPageBase().navigateToNext(detailsPageClass, parameters);

    }

    protected Label getTitle() {
        return (Label) get(ID_TITLE);
    }

    protected WebMarkupContainer getIcon() {
        return (WebMarkupContainer) get(ID_ICON);
    }

    public void initStatusBar() {
        RoleAnalysisCandidateTileModel<T> modelObject = getModelObject();
        String status = modelObject.getStatus();

        Label statusBar = new Label(ID_STATUS_BAR, Model.of(status));
        statusBar.add(AttributeAppender.append("class",
                "badge-pill badge " + DisplayForLifecycleState.valueOfOrDefault(status).getCssClass()));
        statusBar.add(AttributeAppender.append("style", "width: 80px"));
        statusBar.setOutputMarkupId(true);
        add(statusBar);
    }

    private void buildExploreButton() {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                GuiStyleConstants.CLASS_ICON_SEARCH, LayeredIconCssStyle.IN_ROW_STYLE);

        RoleAnalysisCandidateTileModel<T> modelObject = getModelObject();
        RoleAnalysisCandidateRoleType candidateRole = modelObject.getCandidateRole();
        RoleAnalysisOperationStatus operationStatus = candidateRole.getOperationStatus();
        boolean isButtonEnable = operationStatus == null
                || operationStatus.getOperationChannel() == null
                || !operationStatus.getOperationChannel().equals(RoleAnalysisOperation.MIGRATION);
        AjaxCompositedIconSubmitButton migrationButton = new AjaxCompositedIconSubmitButton(
                RoleAnalysisCandidateTilePanel.ID_EXPLORE_PATTERN_BUTTON,
                iconBuilder.build(),
                createStringResource("Explore candidate role")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                exploreRolePerform();
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        migrationButton.titleAsLabel(true);
        migrationButton.setOutputMarkupId(true);
        migrationButton.add(AttributeAppender.append("class", "btn btn-primary btn-sm"));
        migrationButton.setOutputMarkupId(true);
        migrationButton.setEnabled(isButtonEnable);
        add(migrationButton);
    }

    public List<InlineMenuItem> createMenuItems() {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(new InlineMenuItem(createStringResource("Explore in the cluster")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, createModel().getObject().getClusterOid());
                        parameters.add("panelId", "clusterDetails");

                        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                                .getObjectDetailsPage(RoleAnalysisClusterType.class);
                        getPageBase().navigateToNext(detailsPageClass, parameters);
                    }
                };
            }

        });

        items.add(new InlineMenuItem(createStringResource("Details view")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        String oid = getModelObject().getRole().getOid();
                        dispatchToObjectDetailsPage(RoleType.class, oid, getPageBase(), true);
                    }
                };
            }

        });

        return items;
    }

    public void initMigrationButton() {
        RoleAnalysisCandidateTileModel<T> modelObject = getModelObject();
        RoleType role = modelObject.getRole();
        RoleAnalysisCandidateRoleType candidateRole = modelObject.getCandidateRole();
        String clusterOid = modelObject.getClusterOid();
        PageBase pageBase = getModelObject().getPageBase();
        Task task = pageBase.createSimpleTask("Migration process");
        OperationResult result = task.getResult();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        PrismObject<RoleAnalysisClusterType> clusterPrism = roleAnalysisService.getClusterTypeObject(clusterOid, task, result);
        RoleAnalysisClusterType cluster;
        if (clusterPrism == null) {
            add(new EmptyPanel("buttonBar"));
            return;
        }

        cluster = clusterPrism.asObjectable();

        String stateString = roleAnalysisService.recomputeAndResolveClusterCandidateRoleOpStatus(
                cluster.asPrismObject(), candidateRole,
                result, task);

        RoleAnalysisOperationStatus operationStatus = candidateRole.getOperationStatus();
        if (operationStatus != null
                && operationStatus.getTaskRef() != null
                && operationStatus.getTaskRef().getOid() != null) {
            @NotNull AjaxCompositedIconSubmitButton taskPanel = taskLinkPanel(stateString, operationStatus);
            add(taskPanel);
        } else {
            CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                    GuiStyleConstants.CLASS_PLUS_CIRCLE, LayeredIconCssStyle.IN_ROW_STYLE);
            AjaxCompositedIconSubmitButton migrationButton = new AjaxCompositedIconSubmitButton(ID_MIGRATION_BUTTON,
                    iconBuilder.build(),
                    createStringResource("RoleMining.button.title.execute.migration")) {
                @Serial private static final long serialVersionUID = 1L;

                @Override
                protected void onSubmit(AjaxRequestTarget target) {
                    Task task = pageBase.createSimpleTask("countRoleMembers");
                    OperationResult result = task.getResult();

                    String taskOid = UUID.randomUUID().toString();
                    List<ObjectReferenceType> candidateMembers = candidateRole.getCandidateMembers();
                    ObjectSetType members = new ObjectSetType();
                    candidateMembers.forEach(member -> members.getObjectRef().add(member.clone()));
                    RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();

                    roleAnalysisService.clusterObjectMigrationRecompute(
                            clusterOid, role.getOid(), task, result);

                    ActivityDefinitionType activity = null;
                    try {
                        activity = createActivity(members, role.getOid());
                    } catch (SchemaException e) {
                        LOGGER.error("Couldn't create activity for role migration: " + role.getOid(), e);
                    }
                    if (activity != null) {
                        roleAnalysisService.executeMigrationTask(pageBase.getModelInteractionService(),
                                clusterPrism, activity, role.asPrismObject(), taskOid,
                                null, task, result);
                        if (result.isWarning()) {
                            warn(result.getMessage());
                            target.add(((PageBase) getPage()).getFeedbackPanel());
                        } else {
                            MidPointPrincipal user = AuthUtil.getPrincipalUser();
                            roleAnalysisService.setCandidateRoleOpStatus(clusterPrism,
                                    candidateRole,
                                    taskOid,
                                    OperationResultStatusType.IN_PROGRESS,
                                    null,
                                    result,
                                    task,
                                    RoleAnalysisOperation.MIGRATION,
                                    user.getFocus());
                            navigateToRoleAnalysisCluster(clusterOid);
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

            add(migrationButton);
        }
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

    private @NotNull AjaxCompositedIconSubmitButton taskLinkPanel(String stateString,
            RoleAnalysisOperationStatus operationExecution) {

        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                GuiStyleConstants.CLASS_OBJECT_TASK_ICON, LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton taskPanel = new AjaxCompositedIconSubmitButton(
                RoleAnalysisCandidateTilePanel.ID_MIGRATION_BUTTON,
                iconBuilder.build(),
                Model.of(stateString)) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                String oid = operationExecution.getTaskRef().getOid();
                DetailsPageUtil.dispatchToObjectDetailsPage(TaskType.class, oid,
                        this, true);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        taskPanel.titleAsLabel(true);
        taskPanel.setOutputMarkupId(true);
        taskPanel.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        taskPanel.setOutputMarkupId(true);
        add(taskPanel);

        return taskPanel;
    }
}
