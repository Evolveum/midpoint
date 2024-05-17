/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.densityBasedColor;

import java.io.Serial;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.error.ErrorPanel;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsFragment;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.component.InlineOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.ProgressBar;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.SessionSummaryPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.wizard.RoleAnalysisSessionWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.DetailsTableItem;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
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
    public void addAdditionalButtons(RepeatingView repeatingView) {

        initEditConfigurationButton(repeatingView);

        initRebuildButton(repeatingView);

    }

    public void clusteringPerform(@NotNull AjaxRequestTarget target) {

        Task task = getPageBase().createSimpleTask(OP_PERFORM_CLUSTERING);
        OperationResult result = task.getResult();

        AssignmentHolderDetailsModel<RoleAnalysisSessionType> objectDetailsModels = getObjectDetailsModels();

        RoleAnalysisSessionType session = objectDetailsModels.getObjectType();

        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();

        roleAnalysisService.deleteSessionTask(session.getOid(), task, result);

        try {
            ModelService modelService = getPageBase().getModelService();

            Collection<ObjectDelta<? extends ObjectType>> objectDeltas = objectDetailsModels.collectDeltas(result);
            if (objectDeltas != null && !objectDeltas.isEmpty()) {
                modelService.executeChanges(objectDeltas, null, task, result);
            }
        } catch (CommonException e) {
            LOGGER.error("Couldn't execute changes on RoleAnalysisSessionType object: {}", session.getOid(), e);
        }

        roleAnalysisService.executeClusteringTask(getModelInteractionService(), session.asPrismObject(),
                null, null, task, result, new TaskType());

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
        return Model.of();
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

        if (canShowWizard()) {
            setShowedByWizard(true);
            getObjectDetailsModels().reset();
            return createWizardFragment();
        }

        return new DetailsFragment(ID_DETAILS_VIEW, "fragment", PageRoleAnalysisSession.this) {

            @Override
            protected void initFragmentLayout() {

                RoleAnalysisSessionType session = getObjectDetailsModels().getObjectType();
                IModel<List<DetailsTableItem>> detailsModelIModel = loadDetailsModel(session);

                MidpointForm<?> form = new MidpointForm<>("mainForm") {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected void onDetach() {
                        super.onDetach();
                    }

                };

                form.setMultiPart(true);
                add(form);

                InlineOperationalButtonsPanel<RoleAnalysisSessionType> opButtonPanel = new InlineOperationalButtonsPanel<>("buttons", getObjectDetailsModels().getObjectWrapperModel()) {

                    @Override
                    protected IModel<String> getDeleteButtonTitleModel() {
                        return Model.of("Remove session");
                    }

                    @Override
                    protected void savePerformed(AjaxRequestTarget target) {
                        PageRoleAnalysisSession.this.savePerformed(target);
                    }

                    @Override
                    protected IModel<String> getTitle() {
                        return createStringResource("RoleAnalysis.page.session.title");
                    }

                    @Override
                    protected void backPerformed(AjaxRequestTarget target) {
                        super.backPerformed(target);
                        onBackPerform(target);
                    }

                    @Override
                    protected void addButtons(RepeatingView repeatingView) {
                        addAdditionalButtons(repeatingView);
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

                form.add(opButtonPanel);

//                initButtons(form);

                DisplayType displayType = new DisplayType()
                        .label(session.getName())
                        .help(getLastRebuildTimeStamp(session))
                        .icon(new IconType()
                                .cssClass(IconAndStylesUtil.createDefaultColoredIcon(RoleAnalysisSessionType.COMPLEX_TYPE) + " fa-2x fa-inverse"));

                NavigationDetailsTablePanel details = new NavigationDetailsTablePanel("navigationHeader",
                        Model.of(displayType),
                        detailsModelIModel) {

                    @Override
                    public Component getNavigationComponent() {
                        return initNavigation();
                    }
                };
                form.add(details);

                ContainerPanelConfigurationType defaultConfiguration = findDefaultConfiguration();
                initMainPanel(defaultConfiguration, form);

            }

            @NotNull
            private IModel<List<DetailsTableItem>> loadDetailsModel(@NotNull RoleAnalysisSessionType session) {
                RoleAnalysisSessionStatisticType sessionStatistic = session.getSessionStatistic();

                if (sessionStatistic == null) {
                    return Model.ofList(List.of());
                }

                RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
                RoleAnalysisCategoryType analysisCategory = analysisOption.getAnalysisCategory();
                RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();

                String mode = Character.
                        toUpperCase(processMode.value().charAt(0))
                        + processMode.value().substring(1)
                        + "/"
                        + Character
                        .toUpperCase(analysisCategory.value().charAt(0))
                        + analysisCategory.value().substring(1);

                Double density = sessionStatistic.getMeanDensity();
                if (density == null) {
                    density = 0.0;
                }

                String formattedDensity = new DecimalFormat("#.###")
                        .format(Math.round(density * 1000.0) / 1000.0);

                List<DetailsTableItem> detailsModel = List.of(
                        new DetailsTableItem(createStringResource("Mode"),
                                Model.of(mode)) {
                            @Override
                            public Component createValueComponent(String id) {
                                return new Label(id, getValue());
                            }
                        },
                        new DetailsTableItem(createStringResource("Cluster count"),
                                Model.of(sessionStatistic.getClusterCount().toString())) {
                            @Override
                            public Component createValueComponent(String id) {
                                return new IconWithLabel(id, getValue()) {
                                    @Override
                                    public String getIconCssClass() {
                                        return IconAndStylesUtil.createDefaultColoredIcon(RoleAnalysisClusterType.COMPLEX_TYPE);
                                    }

                                    @Override
                                    protected String getComponentCssClass() {
                                        return super.getComponentCssClass() + " justify-content-end";
                                    }
                                };
                            }
                        },
                        new DetailsTableItem(createStringResource("Processed objects"),
                                Model.of(sessionStatistic.getProcessedObjectCount().toString())) {
                            @Override
                            public Component createValueComponent(String id) {
                                return new IconWithLabel(id, getValue()) {
                                    @Override
                                    public String getIconCssClass() {
                                        if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
                                            return IconAndStylesUtil.createDefaultColoredIcon(RoleType.COMPLEX_TYPE);
                                        }
                                        return IconAndStylesUtil.createDefaultColoredIcon(UserType.COMPLEX_TYPE);
                                    }

                                    @Override
                                    protected String getComponentCssClass() {
                                        return super.getComponentCssClass() + " justify-content-end";
                                    }
                                };
                            }
                        },
                        new DetailsTableItem(createStringResource("Mean density"),
                                Model.of(formattedDensity)) {

                            @Override
                            public Component createValueComponent(String id) {
                                String colorClass = densityBasedColor(Double.parseDouble(getValue().getObject()));
                                ProgressBar progressBar = new ProgressBar(id) {

                                    @Override
                                    public boolean isInline() {
                                        return true;
                                    }

                                    @Override
                                    public double getActualValue() {
                                        return Double.parseDouble(getValue().getObject());
                                    }

                                    @Override
                                    public String getProgressBarColor() {
                                        return colorClass;
                                    }

                                    @Override
                                    public String getBarTitle() {
                                        return "";
                                    }
                                };
                                progressBar.setOutputMarkupId(true);
                                return progressBar;
                            }

                        });

                return Model.ofList(detailsModel);
            }
        };

    }

    private String getLastRebuildTimeStamp(@NotNull RoleAnalysisSessionType objectType) {
        String lastRebuild = "Last rebuild: ";
        RoleAnalysisOperationStatus operationStatus = objectType.getOperationStatus();
        if (operationStatus != null) {
            XMLGregorianCalendar createTimestamp = operationStatus.getCreateTimestamp();
            if (createTimestamp != null) {
                int eonAndYear = createTimestamp.getYear();
                int month = createTimestamp.getMonth();
                int day = createTimestamp.getDay();
                String time = day + "/" + month + "/" + eonAndYear;
                lastRebuild = lastRebuild + time;
            }
        }
        return lastRebuild;
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

    private void initEditConfigurationButton(@NotNull RepeatingView repeatingView) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_EDIT_MENU_ITEM,
                LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton editConfigurationButton = new AjaxCompositedIconSubmitButton(repeatingView.newChildId(),
                iconBuilder.build(),
                ((PageBase) getPage()).createStringResource("PageRoleAnalysisSession.button.configure")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                RoleAnalysisReconfigureSessionPopupPanel detailsPanel = new RoleAnalysisReconfigureSessionPopupPanel(((PageBase) getPage()).getMainPopupBodyId(),
                        getObjectDetailsModels());

                getPageBase().showMainPopup(detailsPanel, target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        editConfigurationButton.titleAsLabel(true);
        editConfigurationButton.setOutputMarkupId(true);
        editConfigurationButton.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        repeatingView.add(editConfigurationButton);
    }

    private void initRebuildButton(@NotNull RepeatingView repeatingView) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_REFRESH,
                LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton rebuildButton = new AjaxCompositedIconSubmitButton(repeatingView.newChildId(),
                iconBuilder.build(),
                ((PageBase) getPage()).createStringResource("PageRoleAnalysisSession.button.rebuild")) {
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
    }

}

