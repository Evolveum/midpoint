/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page;

import java.util.Collection;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsFragment;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.component.InlineOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.RoleAnalysisSessionOperationButtonPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.RoleAnalysisSessionSummaryPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.context.AnalysisCategoryMode;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.wizard.RoleAnalysisSessionWizardPanel;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

//TODO correct authorizations
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/roleAnalysisSession", matchUrlForSecurity = "/admin/roleAnalysisSession")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLE_ANALYSIS_ALL_URL,
                label = "PageRoleAnalysis.auth.roleAnalysisAll.label",
                description = "PageRoleAnalysis.auth.roleAnalysisAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLE_ANALYSIS_SESSION_URL,
                label = "PageRoleAnalysis.auth.roleAnalysisSession.label",
                description = "PageRoleAnalysis.auth.roleAnalysisSession.description")
})

public class PageRoleAnalysisSession extends PageAssignmentHolderDetails<RoleAnalysisSessionType, AssignmentHolderDetailsModel<RoleAnalysisSessionType>> {

    public static final Trace LOGGER = TraceManager.getTrace(PageRoleAnalysisSession.class);

    private static final String DOT_CLASS = PageRoleAnalysisSession.class.getName() + ".";
    private static final String OP_DELETE_CLEANUP = DOT_CLASS + "deleteCleanup";
    private static final String OP_PROCESS_CLUSTERING = DOT_CLASS + "processClustering";

    public PageRoleAnalysisSession() {
        super();
    }

    public PageRoleAnalysisSession(PageParameters pageParameters) {
        super(pageParameters);
    }

    public PageRoleAnalysisSession(PrismObject<RoleAnalysisSessionType> roleAnalysisSession) {
        super(roleAnalysisSession);
    }

    @Override
    public void afterDeletePerformed(AjaxRequestTarget target) {
        PageBase pageBase = (PageBase) getPage();
        Task task = pageBase.createSimpleTask(OP_DELETE_CLEANUP);
        OperationResult result = task.getResult();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();

        RoleAnalysisSessionType session = getModelWrapperObject().getObjectOld().asObjectable();
        String sessionOid = session.getOid();
        roleAnalysisService
                .deleteSessionClustersMembers(sessionOid, task, result, false);
    }

    @Override
    public Class<RoleAnalysisSessionType> getType() {
        return RoleAnalysisSessionType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<RoleAnalysisSessionType> summaryModel) {
        return null;
    }

    @Override
    protected Panel createVerticalSummaryPanel(String id, IModel<RoleAnalysisSessionType> summaryModel) {
        return new RoleAnalysisSessionSummaryPanel(id, summaryModel);
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return Model.of();
    }

    @Override
    protected boolean canShowWizard() {
        return !isEditObject();
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
    protected InlineOperationalButtonsPanel<RoleAnalysisSessionType> createInlineButtonsPanel(String idButtons,
            LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapperModel) {
        return new RoleAnalysisSessionOperationButtonPanel(idButtons, objectWrapperModel, getObjectDetailsModels()) {
            @Override
            protected void submitPerformed(AjaxRequestTarget target) {
                PageRoleAnalysisSession.this.savePerformed(target);
            }

            @Override
            protected void backPerformed(AjaxRequestTarget target) {
                super.backPerformed(target);
                onBackPerform(target);
            }

            @Override
            protected void deleteConfirmPerformed(AjaxRequestTarget target) {
                super.deleteConfirmPerformed(target);
                afterDeletePerformed(target);
            }

            @Override
            protected boolean hasUnsavedChanges(AjaxRequestTarget target) {
                return PageRoleAnalysisSession.this.hasUnsavedChanges(target);
            }
        };
    }

    @Override
    protected String getMainPanelCssClass() {
        return "col p-0 rounded";
    }

    @Override
    protected String getMainPanelCssStyle() {
        return "align-items: stretch; overflow: visible;min-width:0;";
    }

    @Override
    public IModel<List<ContainerPanelConfigurationType>> getPanelConfigurations() {

        IModel<List<ContainerPanelConfigurationType>> panelConfigurations = super.getPanelConfigurations();
        RoleAnalysisSessionType session = getObjectDetailsModels()
                .getObjectWrapper()
                .getObject()
                .asObjectable();

        RoleAnalysisOptionType processModeObject = session.getAnalysisOption();
        RoleAnalysisProcessModeType processMode = processModeObject.getProcessMode();

        if (processMode == null) {
            return super.getPanelConfigurations();
        }
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        RoleAnalysisProcedureType analysisProcedureType = analysisOption.getAnalysisProcedureType();

        List<ContainerPanelConfigurationType> object = panelConfigurations.getObject();
        for (ContainerPanelConfigurationType containerPanelConfigurationType : object) {
            if (containerPanelConfigurationType.getIdentifier().equals("topDetectedPattern")
                    && analysisProcedureType == RoleAnalysisProcedureType.OUTLIER_DETECTION) {
                containerPanelConfigurationType.setVisibility(UserInterfaceElementVisibilityType.HIDDEN);
            } else if (containerPanelConfigurationType.getIdentifier().equals("outlierActions")
                    && analysisProcedureType != RoleAnalysisProcedureType.OUTLIER_DETECTION) {
                containerPanelConfigurationType.setVisibility(UserInterfaceElementVisibilityType.HIDDEN);
            } else {
                resolveSessionSettingPanels(containerPanelConfigurationType, processMode);
            }

        }
        return panelConfigurations;
    }

    private static void resolveSessionSettingPanels(
            @NotNull ContainerPanelConfigurationType containerPanelConfigurationType,
            @NotNull RoleAnalysisProcessModeType processMode) {
        if (containerPanelConfigurationType.getIdentifier().equals("userModeSettings")) {
            if (RoleAnalysisProcessModeType.ROLE.equals(processMode)) {
                containerPanelConfigurationType.setVisibility(UserInterfaceElementVisibilityType.HIDDEN);
            }

        } else if (containerPanelConfigurationType.getIdentifier().equals("roleModeSettings")
                && RoleAnalysisProcessModeType.USER.equals(processMode)) {
            containerPanelConfigurationType.setVisibility(UserInterfaceElementVisibilityType.HIDDEN);
        }
    }

    @Override
    protected void onBackPerform(AjaxRequestTarget target) {
        ((PageBase) getPage()).navigateToNext(PageRoleAnalysis.class);
    }

    @Override
    protected DetailsFragment createWizardFragment() {
        return new DetailsFragment(ID_DETAILS_VIEW, ID_TEMPLATE_VIEW, PageRoleAnalysisSession.this) {
            @Override
            protected void initFragmentLayout() {
                add(new RoleAnalysisSessionWizardPanel(ID_TEMPLATE, createObjectWizardPanelHelper()) {

                    @Override
                    protected AssignmentHolderDetailsModel<RoleAnalysisSessionType> reloadWrapperWithDefaultConfiguration(RoleAnalysisSessionType session) {
                        reloadObjectDetailsModel(session.asPrismObject());
                        return getObjectDetailsModels();
                    }

                    @Override
                    protected void finalSubmitPerform(@NotNull AjaxRequestTarget target) {
                        PageRoleAnalysisSession.this.submitWizardAndPerformAnalysis(target);
                    }
                });
            }
        };

    }

    private void submitWizardAndPerformAnalysis(AjaxRequestTarget target) {
        Task task = getPageBase().createSimpleTask(OP_PROCESS_CLUSTERING);
        OperationResult result = task.getResult();

        Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas = saveOrPreviewPerformed(target, result, false, task);
        PrismObject<RoleAnalysisSessionType> sessionTypeObject = getRoleAnalysisSession(executedDeltas);

        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();

        if (sessionTypeObject != null) {
            ModelInteractionService modelInteractionService = getPageBase().getModelInteractionService();
            roleAnalysisService.executeClusteringTask(modelInteractionService, sessionTypeObject,
                    task, result);
        }

        setResponsePage(PageRoleAnalysis.class);
        showResult(result);
        target.add(getFeedbackPanel());
    }

    private PrismObject<RoleAnalysisSessionType> getRoleAnalysisSession(Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas) {
        return (PrismObject<RoleAnalysisSessionType>) executedDeltas.iterator().next().getObjectDelta().getObjectToAdd();
    }

    public static @NotNull PrismContainerWrapperModel<RoleAnalysisSessionType, AbstractAnalysisSessionOptionType> getSessionOptionContainer(
            @NotNull AssignmentHolderDetailsModel<RoleAnalysisSessionType> detailsModel) {
        LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapperModel = detailsModel.getObjectWrapperModel();
        RoleAnalysisOptionType processModeObject = objectWrapperModel.getObject().getObject().asObjectable().getAnalysisOption();
        RoleAnalysisProcessModeType processMode = processModeObject.getProcessMode();
        PrismContainerWrapperModel<RoleAnalysisSessionType, AbstractAnalysisSessionOptionType> containerWrapperModel;
        if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            containerWrapperModel = PrismContainerWrapperModel.fromContainerWrapper(objectWrapperModel,
                    ItemPath.create(RoleAnalysisSessionType.F_ROLE_MODE_OPTIONS));
        } else {
            containerWrapperModel = PrismContainerWrapperModel.fromContainerWrapper(objectWrapperModel,
                    ItemPath.create(RoleAnalysisSessionType.F_USER_MODE_OPTIONS));
        }
        containerWrapperModel.getObject().setExpanded(true);
        return containerWrapperModel;
    }

    @Override
    protected VisibleEnableBehaviour getPageTitleBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }
}

