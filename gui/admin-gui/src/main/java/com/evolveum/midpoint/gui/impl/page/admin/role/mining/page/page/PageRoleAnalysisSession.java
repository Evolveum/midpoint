/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page;

import java.io.Serial;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.error.ErrorPanel;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsFragment;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.SessionSummaryPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.wizard.RoleAnalysisSessionWizardPanel;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.generator.RoleAnalysisDataGeneratorUtils.*;

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
    public void addAdditionalButtons(RepeatingView repeatingView) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_OBJECT_TASK_ICON,
                LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton rebuildButton = new AjaxCompositedIconSubmitButton(repeatingView.newChildId(),
                iconBuilder.build(),
                setDetectionButtonTitle()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                clusteringPerform(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        rebuildButton.titleAsLabel(true);
        rebuildButton.setOutputMarkupId(true);
        rebuildButton.add(AttributeAppender.append("class", "btn btn-primary btn-sm"));
        repeatingView.add(rebuildButton);

        Form<?> form = rebuildButton.findParent(Form.class);
        if (form != null) {
            form.setDefaultButton(rebuildButton);
        }

        boolean generationButtonEnabled = false;
        String orig = getModelObjectType().getName().getOrig();
        if (orig.equals("generator")) {
            generationButtonEnabled = true;
        }
        AjaxCompositedIconSubmitButton generate = new AjaxCompositedIconSubmitButton(repeatingView.newChildId(),
                iconBuilder.build(),
                ((PageBase) getPage()).createStringResource("Generate")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                PageBase page = (PageBase) getPage();
                RepositoryService repositoryService = page.getRepositoryService();
                generateTD(repositoryService, page);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        generate.titleAsLabel(true);
        generate.setOutputMarkupId(true);
        generate.setVisible(generationButtonEnabled);
        generate.add(AttributeAppender.append("class", "btn btn-primary btn-sm"));
        repeatingView.add(generate);

        AjaxCompositedIconSubmitButton remake = new AjaxCompositedIconSubmitButton(repeatingView.newChildId(),
                iconBuilder.build(),
                ((PageBase) getPage()).createStringResource("Remake")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                PageBase page = (PageBase) getPage();
                RepositoryService repositoryService = page.getRepositoryService();

                remakeBusinessRoles(repositoryService, page, page.getRoleAnalysisService());
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        remake.titleAsLabel(true);
        remake.setOutputMarkupId(true);
        remake.setVisible(generationButtonEnabled);
        remake.add(AttributeAppender.append("class", "btn btn-primary btn-sm"));
        repeatingView.add(remake);

        AjaxCompositedIconSubmitButton unassign = new AjaxCompositedIconSubmitButton(repeatingView.newChildId(),
                iconBuilder.build(),
                ((PageBase) getPage()).createStringResource("Unassign")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                PageBase page = (PageBase) getPage();
                RepositoryService repositoryService = page.getRepositoryService();

                unnassignAll(repositoryService, page, page.getRoleAnalysisService());
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        unassign.titleAsLabel(true);
        unassign.setOutputMarkupId(true);
        unassign.setVisible(generationButtonEnabled);
        unassign.add(AttributeAppender.append("class", "btn btn-primary btn-sm"));
        repeatingView.add(unassign);
    }

    public void clusteringPerform(@NotNull AjaxRequestTarget target) {

        Task task = getPageBase().createSimpleTask(OP_PERFORM_CLUSTERING);
        OperationResult result = task.getResult();

        AssignmentHolderDetailsModel<RoleAnalysisSessionType> objectDetailsModels = getObjectDetailsModels();

        RoleAnalysisSessionType session = objectDetailsModels.getObjectType();

        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();

        try {
            ModelService modelService = getPageBase().getModelService();

            Collection<ObjectDelta<? extends ObjectType>> objectDeltas = objectDetailsModels.collectDeltas(result);
            if (objectDeltas != null && !objectDeltas.isEmpty()) {
                modelService.executeChanges(objectDeltas, null, task, result);
            }
        } catch (CommonException e) {
            LOGGER.error("Couldn't execute changes on RoleAnalysisSessionType object: {}", session.getOid(), e);
        }

        roleAnalysisService.executeClusteringTask(getModelInteractionService(), session.asPrismObject(), null, null, task, result);

        if (result.isWarning()) {
            warn(result.getMessage());
            target.add(getPageBase().getFeedbackPanel());
        } else {
            result.recordSuccessIfUnknown();
            setResponsePage(PageRoleAnalysis.class);
            ((PageBase) getPage()).showResult(result);
            target.add(getFeedbackPanel());
        }

    }

    public StringResourceModel setDetectionButtonTitle() {
        return ((PageBase) getPage()).createStringResource("PageAnalysisSession.button.save");
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
        return new SessionSummaryPanel(id, summaryModel, null);
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return createStringResource("RoleMining.page.cluster.title");
    }

    @Override
    protected boolean canShowWizard() {
        return !isEditObject();
    }

    protected DetailsFragment createDetailsFragment() {
        if (!isNativeRepo()) {
            return new DetailsFragment(ID_DETAILS_VIEW, ID_TEMPLATE_VIEW, PageRoleAnalysisSession.this) {
                @Override
                protected void initFragmentLayout() {
                    add(new ErrorPanel(ID_TEMPLATE,
                            createStringResource("RoleAnalysis.menu.nonNativeRepositoryWarning")));
                }
            };
        }

        return super.createDetailsFragment();
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

            if (containerPanelConfigurationType.getIdentifier().equals("matchingOptions")) {
                if (!analysisCategory.equals(RoleAnalysisCategoryType.ADVANCED)) {
                    containerPanelConfigurationType.setVisibility(UserInterfaceElementVisibilityType.HIDDEN);
                    continue;
                }
            }

            if (containerPanelConfigurationType.getIdentifier().equals("topDetectedPattern")) {
                if (analysisCategory.equals(RoleAnalysisCategoryType.OUTLIERS)) {
                    containerPanelConfigurationType.setVisibility(UserInterfaceElementVisibilityType.HIDDEN);
                    continue;
                }
            }

            if (containerPanelConfigurationType.getIdentifier().equals("sessionOptions")) {
                List<VirtualContainersSpecificationType> container = containerPanelConfigurationType.getContainer();

                for (VirtualContainersSpecificationType virtualContainersSpecificationType : container) {
                    if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
                        if (virtualContainersSpecificationType.getPath().getItemPath()
                                .equivalent(RoleAnalysisSessionType.F_USER_MODE_OPTIONS)) {
                            containerPanelConfigurationType.setVisibility(UserInterfaceElementVisibilityType.HIDDEN);
                        }
                    } else {
                        if (virtualContainersSpecificationType.getPath().getItemPath()
                                .equivalent(RoleAnalysisSessionType.F_ROLE_MODE_OPTIONS)) {
                            containerPanelConfigurationType.setVisibility(UserInterfaceElementVisibilityType.HIDDEN);
                        }
                    }

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
}

