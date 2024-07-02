/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

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
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.wizard.RoleAnalysisSessionWizardPanel;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
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

    private static final String DOT_CLASS = PageRoleAnalysisSession.class.getName() + ".";
    private static final String OP_DELETE_CLEANUP = DOT_CLASS + "deleteCleanup";
    private static final String OP_PERFORM_CLUSTERING = DOT_CLASS + "performClustering";
    private static final Trace LOGGER = TraceManager.getTrace(PageRoleAnalysisSession.class);

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
                .deleteSessionClustersMembers(sessionOid, task, result);
    }

    @Override
    public void savePerformed(AjaxRequestTarget target) {
        super.savePerformed(target);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
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
    protected InlineOperationalButtonsPanel<RoleAnalysisSessionType> createInlineButtonsPanel(String idButtons, LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapperModel) {
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
    protected AssignmentHolderDetailsModel<RoleAnalysisSessionType> createObjectDetailsModels(PrismObject<RoleAnalysisSessionType> object) {
        return super.createObjectDetailsModels(object);
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
        RoleAnalysisCategoryType analysisCategory = analysisOption.getAnalysisCategory();

        List<ContainerPanelConfigurationType> object = panelConfigurations.getObject();
        for (ContainerPanelConfigurationType containerPanelConfigurationType : object) {

            if (containerPanelConfigurationType.getIdentifier().equals("topDetectedPattern")) {
                if (RoleAnalysisCategoryType.OUTLIERS.equals(analysisCategory)) {
                    containerPanelConfigurationType.setVisibility(UserInterfaceElementVisibilityType.HIDDEN);
                    continue;
                }
            }

            if (containerPanelConfigurationType.getIdentifier().equals("userModeSettings")) {
                if (RoleAnalysisProcessModeType.ROLE.equals(processMode)) {
                    containerPanelConfigurationType.setVisibility(UserInterfaceElementVisibilityType.HIDDEN);
                }

            } else if (containerPanelConfigurationType.getIdentifier().equals("roleModeSettings")) {
                if (RoleAnalysisProcessModeType.USER.equals(processMode)) {
                    containerPanelConfigurationType.setVisibility(UserInterfaceElementVisibilityType.HIDDEN);
                }
            }

        }
        return panelConfigurations;
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
                add(new RoleAnalysisSessionWizardPanel(ID_TEMPLATE, createObjectWizardPanelHelper()));
            }
        };

    }

    @Override
    protected VisibleEnableBehaviour getPageTitleBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }
}

