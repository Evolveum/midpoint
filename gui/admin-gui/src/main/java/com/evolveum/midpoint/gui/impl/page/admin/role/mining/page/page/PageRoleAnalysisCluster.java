/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page;

import java.io.Serial;
import java.util.List;

import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.component.InlineOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationButtonPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterSummaryPanel;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.common.mining.objects.detection.PatternDetectionOption;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;

//TODO correct authorizations
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/roleAnalysisCluster", matchUrlForSecurity = "/admin/roleAnalysisCluster")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLE_ANALYSIS_ALL_URL,
                label = "PageRoleAnalysis.auth.roleAnalysisAll.label",
                description = "PageRoleAnalysis.auth.roleAnalysisAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLE_ANALYSIS_CLUSTER_URL,
                label = "PageRoleAnalysis.auth.roleAnalysisCluster.label",
                description = "PageRoleAnalysis.auth.roleAnalysisCluster.description")
})

public class PageRoleAnalysisCluster extends PageAssignmentHolderDetails<RoleAnalysisClusterType, AssignmentHolderDetailsModel<RoleAnalysisClusterType>> {

    private static final String DOT_CLASS = PageRoleAnalysisCluster.class.getName() + ".";
    private static final String OP_PATTERN_DETECTION = DOT_CLASS + "patternDetection";
    private static final String OP_RECOMPUTE_SESSION_STAT = DOT_CLASS + "recomputeSessionStatistic";

    @Override
    protected String getMainPanelCssClass() {
        return "col p-0 rounded";
    }

    @Override
    protected String getMainPanelCssStyle() {
        //TODO it make trouble on overview pages (borders are not visible)
        return "align-items:stretch;overflow:visible;min-width:0;";
    }

    @Override
    protected void onBackPerform(AjaxRequestTarget target) {
        PageParameters parameters = new PageParameters();

        ObjectReferenceType roleAnalysisSessionRef = getModelObjectType().getRoleAnalysisSessionRef();
        parameters.add(OnePageParameterEncoder.PARAMETER, roleAnalysisSessionRef.getOid());
        parameters.add("panelId", getBackPerformPanelId(getModelObjectType()));
        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                .getObjectDetailsPage(RoleAnalysisSessionType.class);
        ((PageBase) getPage()).navigateToNext(detailsPageClass, parameters);
    }

    private @Nullable String getBackPerformPanelId(@NotNull RoleAnalysisClusterType cluster) {
        PageBase pageBase = ((PageBase) getPage());
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        Task task = pageBase.createSimpleTask("Retrieve cluster session");
        OperationResult result = task.getResult();
        PrismObject<RoleAnalysisSessionType> sessionTypeObject = roleAnalysisService
                .getSessionTypeObject(cluster.getRoleAnalysisSessionRef().getOid(), task, result);
        if (sessionTypeObject == null) {
            return null;
        }
        RoleAnalysisOptionType analysisOption = sessionTypeObject.asObjectable().getAnalysisOption();
        RoleAnalysisProcedureType procedureType = analysisOption.getAnalysisProcedureType();
        if (procedureType.equals(RoleAnalysisProcedureType.OUTLIER_DETECTION)) {
            return "outlier-clustering-result";
        }

        return "mining-clustering-result";
    }

    @Override
    public void addAdditionalButtons(RepeatingView repeatingView) {

        RoleAnalysisClusterType cluster = getModelWrapperObject().getObjectOld().asObjectable();
        ObjectReferenceType roleAnalysisSessionRef = cluster.getRoleAnalysisSessionRef();
        PageBase pageBase = (PageBase) getPage();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        Task task = pageBase.createSimpleTask("Resolving cluster option type");
        OperationResult result = task.getResult();
        PrismObject<RoleAnalysisSessionType> session = roleAnalysisService.getSessionTypeObject(roleAnalysisSessionRef.getOid(), task, result);

        if (session == null) {
            return;
        }

        RoleAnalysisOptionType analysisOption = session.asObjectable().getAnalysisOption();
        RoleAnalysisProcedureType procedureType = analysisOption.getAnalysisProcedureType();
        if (procedureType == null || procedureType.equals(RoleAnalysisProcedureType.OUTLIER_DETECTION)) {
            return;
        }

        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                GuiStyleConstants.CLASS_OBJECT_TASK_ICON, IconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton detection = new AjaxCompositedIconSubmitButton(
                repeatingView.newChildId(),
                iconBuilder.build(),
                setDetectionButtonTitle()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                detectionPerform(target);
            }

            @Override
            protected void onError(@NotNull AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        detection.titleAsLabel(true);
        detection.setOutputMarkupId(true);
        detection.add(AttributeModifier.append(CLASS_CSS, "btn btn-primary "));
        repeatingView.add(detection);

        Form<?> form = detection.findParent(Form.class);
        if (form != null) {
            form.setDefaultButton(detection);
        }

        initEditConfigurationButton(repeatingView);

    }

    public void detectionPerform(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OP_PATTERN_DETECTION);

        String clusterOid = getObjectDetailsModels().getObjectType().getOid();
        PrismObject<RoleAnalysisClusterType> clusterPrismObject = getObjectDetailsModels().getObjectWrapper().getObject();
        RoleAnalysisClusterType cluster = clusterPrismObject.asObjectable();

        PageBase pageBase = (PageBase) getPage();
        Task task = pageBase.createSimpleTask(OP_PATTERN_DETECTION);
        PatternDetectionOption detectionOption = new PatternDetectionOption(cluster);
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();

        @NotNull String status = roleAnalysisService.recomputeAndResolveClusterOpStatus(clusterPrismObject.getOid(), result, task,
                true, pageBase.getModelInteractionService());

        if (status.equals("processing")) {
            warn("Couldn't start detection. Some process is already in progress.");
            target.add(getFeedbackPanel());
            return;
        }

        roleAnalysisService.recomputeClusterDetectionOptions(clusterOid, detectionOption,
                task, result);

        roleAnalysisService.executeDetectionTask(getModelInteractionService(), cluster.asPrismObject(), null,
                null, task, result, status);

        if (result.isWarning()) {
            warn(result.getMessage());
            target.add(pageBase.getFeedbackPanel());
        } else {
            PageParameters params = new PageParameters();
            params.add(OnePageParameterEncoder.PARAMETER, clusterOid);
            Class<? extends PageBase> detailsPageClass = DetailsPageUtil.getObjectDetailsPage(RoleAnalysisClusterType.class);
            pageBase.navigateToNext(detailsPageClass, params);

            pageBase.showResult(result);
            target.add(getFeedbackPanel());
        }

    }

    public StringResourceModel setDetectionButtonTitle() {
        return ((PageBase) getPage()).createStringResource("PageAnalysisCluster.button.save");
    }

    @Override
    public void afterDeletePerformed(AjaxRequestTarget target) {
        PageBase pageBase = (PageBase) getPage();
        Task task = pageBase.createSimpleTask(OP_RECOMPUTE_SESSION_STAT);
        OperationResult result = task.getResult();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();

        RoleAnalysisClusterType cluster = getModelWrapperObject().getObjectOld().asObjectable();
        ObjectReferenceType roleAnalysisSessionRef = cluster.getRoleAnalysisSessionRef();
        roleAnalysisService.recomputeSessionStatics(
                roleAnalysisSessionRef.getOid(), cluster, task, result);

        PrismObject<RoleAnalysisSessionType> session = roleAnalysisService.getSessionTypeObject(roleAnalysisSessionRef.getOid(), task, result);

        if (session == null) {
            return;
        }

        RoleAnalysisOptionType analysisOption = session.asObjectable().getAnalysisOption();
        RoleAnalysisProcedureType procedureType = analysisOption.getAnalysisProcedureType();
        if (procedureType.equals(RoleAnalysisProcedureType.OUTLIER_DETECTION)) {
            roleAnalysisService.deleteClusterOutlierOrPartition(cluster, task, result);
        }
    }

    public PageRoleAnalysisCluster() {
        super();
    }

    @Override
    public Class<RoleAnalysisClusterType> getType() {
        return RoleAnalysisClusterType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<RoleAnalysisClusterType> summaryModel) {
        return null;
    }

    @Override
    protected Panel createVerticalSummaryPanel(String id, IModel<RoleAnalysisClusterType> summaryModel) {
        return new RoleAnalysisClusterSummaryPanel(id, summaryModel);
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return Model.of();
    }

    @Override
    public IModel<List<ContainerPanelConfigurationType>> getPanelConfigurations() {

        IModel<List<ContainerPanelConfigurationType>> panelConfigurations = super.getPanelConfigurations();
        @NotNull RoleAnalysisClusterType cluster = getObjectDetailsModels()
                .getObjectWrapper()
                .getObject()
                .asObjectable();

        RoleAnalysisClusterCategory category = cluster.getCategory();

        RoleAnalysisService roleAnalysisService = getRoleAnalysisService();
        PageBase pageBase = (PageBase) getPage();
        Task task = pageBase.createSimpleTask("Resolving cluster option type");

        RoleAnalysisOptionType roleAnalysisOptionType = roleAnalysisService
                .resolveClusterOptionType(cluster.asPrismObject(), task, task.getResult());

        RoleAnalysisProcedureType procedureType = roleAnalysisOptionType.getAnalysisProcedureType();
        if (procedureType == null) {
            return super.getPanelConfigurations();
        }

        List<ContainerPanelConfigurationType> object = panelConfigurations.getObject();
        for (ContainerPanelConfigurationType containerPanelConfigurationType : object) {
            if (containerPanelConfigurationType.getIdentifier().equals("actions")) {
                resolveActionPanel(containerPanelConfigurationType, category, roleAnalysisOptionType);
            } else if (containerPanelConfigurationType.getIdentifier().equals("clusterRoleSuggestions")
                    && procedureType.equals(RoleAnalysisProcedureType.OUTLIER_DETECTION)) {
                containerPanelConfigurationType.setVisibility(UserInterfaceElementVisibilityType.HIDDEN);
            }

        }

        return panelConfigurations;
    }

    private static void resolveActionPanel(
            @NotNull ContainerPanelConfigurationType containerPanelConfigurationType,
            @NotNull RoleAnalysisClusterCategory category,
            @NotNull RoleAnalysisOptionType analysisOption) {
        List<ContainerPanelConfigurationType> panel = containerPanelConfigurationType.getPanel();
        for (ContainerPanelConfigurationType panelType : panel) {
            String identifier = panelType.getIdentifier();
            if (category == RoleAnalysisClusterCategory.OUTLIERS) {
                resolveOutlierClusterActionPanel(analysisOption, panelType, identifier);
            } else {
                resolveBasicClusterPanel(analysisOption, panelType, identifier);
            }
        }
    }

    private static void resolveBasicClusterPanel(
            @NotNull RoleAnalysisOptionType analysisOption,
            @NotNull ContainerPanelConfigurationType panelType,
            @NotNull String identifier) {
        if (identifier.equals("outlierPanel")) {
            if (!analysisOption.getAnalysisProcedureType().equals(RoleAnalysisProcedureType.OUTLIER_DETECTION)) {
                panelType.setVisibility(UserInterfaceElementVisibilityType.HIDDEN);
            }
        } else if (identifier.equals("uniqueOutlierPanel") || identifier.equals("accessNoiseOutlierPanel")) {
            panelType.setVisibility(UserInterfaceElementVisibilityType.HIDDEN);
        } else {
            if (analysisOption.getAnalysisProcedureType().equals(RoleAnalysisProcedureType.OUTLIER_DETECTION)) {
                panelType.setVisibility(UserInterfaceElementVisibilityType.HIDDEN);
            }
        }
    }

    private static void resolveOutlierClusterActionPanel(
            @NotNull RoleAnalysisOptionType analysisOption,
            @NotNull ContainerPanelConfigurationType panelType,
            @NotNull String identifier) {
        if (identifier.equals("outlierPanel")
                || identifier.equals("uniqueOutlierPanel")
                || identifier.equals("accessNoiseOutlierPanel")) {
            if (!analysisOption.getAnalysisProcedureType().equals(RoleAnalysisProcedureType.OUTLIER_DETECTION)) {
                panelType.setVisibility(UserInterfaceElementVisibilityType.HIDDEN);
            }
        } else {
            if (analysisOption.getAnalysisProcedureType().equals(RoleAnalysisProcedureType.OUTLIER_DETECTION)) {
                panelType.setVisibility(UserInterfaceElementVisibilityType.HIDDEN);
            }
        }
    }

    @Override
    protected boolean supportGenericRepository() {
        return false;
    }

    @Override
    protected boolean supportNewDetailsLook() {
        return true;
    }

    @Override
    public IModel<String> getPageTitleModel() {
        return createStringResource("RoleMining.page.cluster.title");
    }

    @Override
    protected InlineOperationalButtonsPanel<RoleAnalysisClusterType> createInlineButtonsPanel(String idButtons, LoadableModel<PrismObjectWrapper<RoleAnalysisClusterType>> objectWrapperModel) {
        return new RoleAnalysisClusterOperationButtonPanel(idButtons, objectWrapperModel) {
            @Override
            protected void submitPerformed(AjaxRequestTarget target) {
                PageRoleAnalysisCluster.this.savePerformed(target);
            }

            @Override
            protected void backPerformed(AjaxRequestTarget target) {
                super.backPerformed(target);
                onBackPerform(target);
            }

            @Override
            protected void addRightButtons(@NotNull RepeatingView rightButtonsView) {
                addAdditionalButtons(rightButtonsView);
            }

            @Override
            protected void deleteConfirmPerformed(AjaxRequestTarget target) {
                super.deleteConfirmPerformed(target);
                afterDeletePerformed(target);
            }

            @Override
            protected boolean hasUnsavedChanges(AjaxRequestTarget target) {
                return PageRoleAnalysisCluster.this.hasUnsavedChanges(target);
            }
        };
    }

    private void initEditConfigurationButton(@NotNull RepeatingView repeatingView) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_EDIT_MENU_ITEM,
                IconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton editConfigurationButton = new AjaxCompositedIconSubmitButton(repeatingView.newChildId(),
                iconBuilder.build(),
                ((PageBase) getPage()).createStringResource("PageRoleAnalysisCluster.button.configure")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                RoleAnalysisReconfigureClusterPopupPanel detailsPanel = new RoleAnalysisReconfigureClusterPopupPanel(((PageBase) getPage()).getMainPopupBodyId(),
                        getObjectDetailsModels()) {
                    @Override
                    protected void finalSubmitPerform(AjaxRequestTarget target) {
                        detectionPerform(target);
                    }
                };

                ((PageBase) getPage()).showMainPopup(detailsPanel, target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        editConfigurationButton.titleAsLabel(true);
        editConfigurationButton.setOutputMarkupId(true);
        editConfigurationButton.add(AttributeModifier.append(CLASS_CSS, "btn btn-default "));
        repeatingView.add(editConfigurationButton);
    }

    @Override
    protected VisibleEnableBehaviour getPageTitleBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }
}

