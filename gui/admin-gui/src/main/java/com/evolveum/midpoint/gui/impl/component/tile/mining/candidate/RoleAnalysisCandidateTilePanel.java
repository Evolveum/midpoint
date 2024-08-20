/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile.mining.candidate;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_CANDIDATE_ROLE_ID;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.confidenceBasedTwoColor;
import static com.evolveum.midpoint.gui.impl.util.DetailsPageUtil.dispatchToObjectDetailsPage;
import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.Contract;
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
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.ProgressBarNew;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysisCluster;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysisSession;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.MetricValuePanel;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisCandidateTilePanel<T extends Serializable> extends BasePanel<RoleAnalysisCandidateTileModel<T>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TITLE = "objectTitle";
    private static final String ID_STATUS_BAR = "status";
    private static final String ID_BUTTON_BAR = "buttonBar";
    private static final String ID_PROGRESS_BAR = "progress-bar";
    private static final String ID_USER_COUNT = "users-count";
    private static final String ID_ROLE_COUNT = "roles-count";
    private static final String ID_LOCATION = "location";
    private static final String ID_MIGRATION_BUTTON = "migration-button";

    public RoleAnalysisCandidateTilePanel(String id, IModel<RoleAnalysisCandidateTileModel<T>> model) {
        super(id, model);
        initLayout();
    }

    protected void initLayout() {

        initDefaultStyle();

        initStatusBar();

        initButtonToolBarPanel();

        initNamePanel();

        initProgressBar();

        initLocationButtons();

        initMigrationButton();

        initFirstCountPanel();

        initSecondCountPanel();

    }

    private void initLocationButtons() {
        ObjectReferenceType clusterRef = getModelObject().getClusterRef();
        ObjectReferenceType sessionRef = getModelObject().getSessionRef();

        MetricValuePanel panel = new MetricValuePanel(ID_LOCATION) {
            @Override
            protected @NotNull Component getTitleComponent(String id) {
                Label label = new Label(id, createStringResource("RoleAnalysis.title.panel.location"));
                label.setOutputMarkupId(true);
                label.add(AttributeAppender.append("class", "text-muted"));
                return label;
            }

            @Override
            protected Component getValueComponent(String id) {
                RepeatingView view = new RepeatingView(id);
                view.setOutputMarkupId(true);
                AjaxLinkPanel sessionLink = new AjaxLinkPanel(view.newChildId(), Model.of(sessionRef.getTargetName().getOrig())) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, sessionRef.getOid());
                        getPageBase().navigateToNext(PageRoleAnalysisSession.class, parameters);
                    }
                };

                sessionLink.setOutputMarkupId(true);
                view.add(sessionLink);

                Label separator = new Label(view.newChildId(), "/");
                separator.setOutputMarkupId(true);
                view.add(separator);

                AjaxLinkPanel clusterLink = new AjaxLinkPanel(view.newChildId(), Model.of(clusterRef.getTargetName().getOrig())) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, clusterRef.getOid());
                        getPageBase().navigateToNext(PageRoleAnalysisCluster.class, parameters);
                    }
                };
                clusterLink.setOutputMarkupId(true);
                view.add(clusterLink);
                return view;
            }
        };

        panel.setOutputMarkupId(true);
        add(panel);

    }

    private void initProgressBar() {
        RoleAnalysisCandidateTileModel<T> modelObject = getModelObject();
        RoleAnalysisCandidateRoleType candidateRole = modelObject.getCandidateRole();
        String clusterOid = modelObject.getClusterRef().getOid();
        PageBase pageBase = getModelObject().getPageBase();
        Task task = pageBase.createSimpleTask("Migration process");
        OperationResult result = task.getResult();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        PrismObject<RoleAnalysisClusterType> clusterPrism = roleAnalysisService.getClusterTypeObject(clusterOid, task, result);
        if (clusterPrism == null) {
            add(new EmptyPanel("buttonBar"));
            return;
        }

        RoleAnalysisOperationStatus operationStatus = candidateRole.getOperationStatus();

        int[] taskProgressIfExist = roleAnalysisService.getTaskProgressIfExist(operationStatus, result);

        int actualProgress = taskProgressIfExist[0];
        int expectedProgress = taskProgressIfExist[1];
        double progressInPercent = 0;

        if (actualProgress != 0 && expectedProgress != 0) {
            progressInPercent = ((double) actualProgress / expectedProgress) * 100;
        }

        BigDecimal bd = new BigDecimal(progressInPercent);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        double finalProgress = bd.doubleValue();

        String colorClass = confidenceBasedTwoColor(finalProgress);

        ProgressBarNew progressBar = new ProgressBarNew(ID_PROGRESS_BAR) {

            @Override
            public double getActualValue() {
                return finalProgress;
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getProgressBarContainerCssStyle() {
                return "border-radius: 3px; height:13px;";
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getProgressBarContainerCssClass() {
                return "col-12 pl-0 pr-0";
            }

            @Override
            public String getProgressBarColor() {
                return colorClass;
            }

            @Contract(pure = true)
            @Override
            public @NotNull String getBarTitle() {
                return "Migration status";
            }
        };
        progressBar.setOutputMarkupId(true);
        progressBar.add(AttributeModifier.replace("title", () -> "Attribute confidence: " + finalProgress + "%"));
        progressBar.add(new TooltipBehavior());
        add(progressBar);
    }

    private void initSecondCountPanel() {
        MetricValuePanel panel = new MetricValuePanel(ID_ROLE_COUNT) {
            @Override
            protected @NotNull Component getTitleComponent(String id) {
                Label label = new Label(id, createStringResource("RoleAnalysis.tile.panel.induced.roles"));
                label.setOutputMarkupId(true);
                label.add(AttributeAppender.append("class", "text-muted"));
                label.add(AttributeAppender.append("style", "font-size: 16px"));
                return label;
            }

            @Override
            protected @NotNull Component getValueComponent(String id) {
                String inducementsCount = RoleAnalysisCandidateTilePanel.this.getModelObject().getInducementsCount();
                Label inducementPanel = new Label(id, () -> inducementsCount) {
                };

                inducementPanel.setOutputMarkupId(true);
                inducementPanel.add(AttributeAppender.replace("title", () -> "Induced roles count: " + inducementsCount));
                inducementPanel.add(new TooltipBehavior());
                return inducementPanel;
            }
        };
        panel.setOutputMarkupId(true);
        add(panel);
    }

    private void initFirstCountPanel() {
        MetricValuePanel panel = new MetricValuePanel(ID_USER_COUNT) {
            @Override
            protected @NotNull Component getTitleComponent(String id) {
                Label label = new Label(id, createStringResource("RoleAnalysis.tile.panel.user.members"));
                label.setOutputMarkupId(true);
                label.add(AttributeAppender.append("class", "text-muted"));
                label.add(AttributeAppender.append("style", "font-size: 16px"));

                return label;
            }

            @Override
            protected @NotNull Component getValueComponent(String id) {
                String membersCount = RoleAnalysisCandidateTilePanel.this.getModelObject().getMembersCount();
                Label memberPanel = new Label(id, () -> membersCount) {
                };

                memberPanel.setOutputMarkupId(true);
                memberPanel.add(AttributeAppender.replace("title", () -> "User members count: " + membersCount));
                memberPanel.add(new TooltipBehavior());
                return memberPanel;
            }
        };
        panel.setOutputMarkupId(true);
        add(panel);
    }

    private void initDefaultStyle() {
        setOutputMarkupId(true);
        add(AttributeAppender.append("class",
                "bg-white d-flex flex-column align-items-center elevation-1 rounded w-100 h-100 p-0"));
    }

    private void initNamePanel() {
        AjaxLinkPanel objectTitle = new AjaxLinkPanel(ID_TITLE, () -> getModelObject().getName()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                RoleAnalysisCandidateTileModel<T> modelObject = RoleAnalysisCandidateTilePanel.this.getModelObject();
                String oid = modelObject.getRole().getOid();
                dispatchToObjectDetailsPage(RoleType.class, oid, getPageBase(), true);
            }
        };
        objectTitle.setOutputMarkupId(true);
        objectTitle.add(AttributeAppender.replace("style", "font-size:18px"));
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
        String clusterOid = getModelObject().getClusterRef().getOid();
        parameters.add(OnePageParameterEncoder.PARAMETER, clusterOid);
        parameters.add("panelId", "clusterDetails");
        parameters.add(PARAM_CANDIDATE_ROLE_ID, stringBuilder.toString());
        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                .getObjectDetailsPage(RoleAnalysisClusterType.class);
        getPageBase().navigateToNext(detailsPageClass, parameters);

    }

    public void initStatusBar() {
        RoleAnalysisCandidateTileModel<T> modelObject = getModelObject();
        String status = modelObject.getStatus();

        Label statusBar = new Label(ID_STATUS_BAR, Model.of(status));
        statusBar.add(AttributeAppender.append("class",
                "badge " + DisplayForLifecycleState.valueOfOrDefault(status).getCssClass()));
        statusBar.add(AttributeAppender.append("style", "width: 80px"));
        statusBar.setOutputMarkupId(true);
        add(statusBar);
    }

    public List<InlineMenuItem> createMenuItems() {
        List<InlineMenuItem> items = new ArrayList<>();

        RoleAnalysisCandidateTileModel<T> modelObject = getModelObject();

        items.add(new InlineMenuItem(createStringResource("RoleAnalysis.title.panel.explore.in.cluster")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, modelObject.getClusterRef().getOid());
                        parameters.add("panelId", "clusterDetails");

                        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                                .getObjectDetailsPage(RoleAnalysisClusterType.class);
                        getPageBase().navigateToNext(detailsPageClass, parameters);
                    }
                };
            }

        });

        items.add(new InlineMenuItem(createStringResource("RoleAnalysis.tile.panel.details.view")) {
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
        String clusterOid = modelObject.getClusterRef().getOid();
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
                    "fa fa-bolt", LayeredIconCssStyle.IN_ROW_STYLE);
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
            migrationButton.add(AttributeAppender.append("class", "btn btn-primary btn-sm"));

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
