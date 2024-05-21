/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page;

import java.io.Serial;
import java.text.DecimalFormat;
import java.util.List;

import com.evolveum.midpoint.gui.impl.error.ErrorPanel;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsFragment;
import com.evolveum.midpoint.gui.impl.page.admin.component.InlineOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.ProgressBar;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisClusterOccupationPanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.DetailsTableItem;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.web.component.form.MidpointForm;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
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
import com.evolveum.midpoint.common.mining.objects.detection.DetectionOption;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.component.AssignmentHolderOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.ClusterSummaryPanel;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.densityBasedColor;

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
    protected AssignmentHolderOperationalButtonsPanel<RoleAnalysisClusterType> createButtonsPanel(String id, LoadableModel<PrismObjectWrapper<RoleAnalysisClusterType>> wrapperModel) {
        return super.createButtonsPanel(id, wrapperModel);
    }

    @Override
    protected void onBackPerform(AjaxRequestTarget target) {
        PageParameters parameters = new PageParameters();
        ObjectReferenceType roleAnalysisSessionRef = getModelObjectType().getRoleAnalysisSessionRef();
        parameters.add(OnePageParameterEncoder.PARAMETER, roleAnalysisSessionRef.getOid());
        parameters.add("panelId", "clusters");
        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                .getObjectDetailsPage(RoleAnalysisSessionType.class);
        ((PageBase) getPage()).navigateToNext(detailsPageClass, parameters);
    }

    @Override
    public void addAdditionalButtons(RepeatingView repeatingView) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_OBJECT_TASK_ICON, LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton detection = new AjaxCompositedIconSubmitButton(repeatingView.newChildId(), iconBuilder.build(),
                setDetectionButtonTitle()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                detectionPerform(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        detection.titleAsLabel(true);
        detection.setOutputMarkupId(true);
        detection.add(AttributeAppender.append("class", "btn btn-primary btn-sm"));
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
        DetectionOption detectionOption = new DetectionOption(cluster);
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();

        @NotNull String status = roleAnalysisService.recomputeAndResolveClusterOpStatus(clusterPrismObject.getOid(), result, task);

        if (status.equals("processing")) {
            warn("Couldn't start detection. Some process is already in progress.");
            target.add(getFeedbackPanel());
            return;
        }

        roleAnalysisService.recomputeClusterDetectionOptions(clusterOid, detectionOption,
                task, result);

        roleAnalysisService.executeDetectionTask(getModelInteractionService(), cluster.asPrismObject(), null,
                null, task, result);

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
    }

    public PageRoleAnalysisCluster() {
        super();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    @Override
    public Class<RoleAnalysisClusterType> getType() {
        return RoleAnalysisClusterType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<RoleAnalysisClusterType> summaryModel) {
        return new ClusterSummaryPanel(id, summaryModel, null);
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

        RoleAnalysisService roleAnalysisService = getRoleAnalysisService();
        PageBase pageBase = (PageBase) getPage();
        Task task = pageBase.createSimpleTask("Resolving cluster option type");

        RoleAnalysisOptionType roleAnalysisOptionType = roleAnalysisService
                .resolveClusterOptionType(cluster.asPrismObject(), task, task.getResult());

        RoleAnalysisCategoryType analysisCategory = roleAnalysisOptionType.getAnalysisCategory();
        if (analysisCategory == null) {
            return super.getPanelConfigurations();
        }

        List<ContainerPanelConfigurationType> object = panelConfigurations.getObject();
        for (ContainerPanelConfigurationType containerPanelConfigurationType : object) {
            if (containerPanelConfigurationType.getIdentifier().equals("outlierPanel")) {
                if (!analysisCategory.equals(RoleAnalysisCategoryType.OUTLIERS)) {
                    containerPanelConfigurationType.setVisibility(UserInterfaceElementVisibilityType.HIDDEN);
                }
            } else if (containerPanelConfigurationType.getIdentifier().equals("detectedPattern")) {
                if (analysisCategory.equals(RoleAnalysisCategoryType.OUTLIERS)) {
                    containerPanelConfigurationType.setVisibility(UserInterfaceElementVisibilityType.HIDDEN);
                }
            }
        }

        return panelConfigurations;
    }

    protected DetailsFragment createDetailsFragment() {
        if (!isNativeRepo()) {
            return new DetailsFragment(ID_DETAILS_VIEW, ID_TEMPLATE_VIEW, PageRoleAnalysisCluster.this) {
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

        return new DetailsFragment(ID_DETAILS_VIEW, "fragment", PageRoleAnalysisCluster.this) {

            @Override
            protected void initFragmentLayout() {

                RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();
                IModel<List<DetailsTableItem>> detailsModelIModel = loadDetailsModel(cluster);

                MidpointForm<?> form = new MidpointForm<>("mainForm") {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected void onDetach() {
                        super.onDetach();
                    }

                };

                form.setMultiPart(true);
                add(form);

                InlineOperationalButtonsPanel<RoleAnalysisClusterType> opButtonPanel = new InlineOperationalButtonsPanel<>("buttons", getObjectDetailsModels().getObjectWrapperModel()) {

                    @Override
                    protected IModel<String> getDeleteButtonTitleModel() {
                        return Model.of("Remove cluster");
                    }

                    @Override
                    protected void savePerformed(AjaxRequestTarget target) {
                        PageRoleAnalysisCluster.this.savePerformed(target);
                    }

                    @Override
                    protected IModel<String> getTitle() {
                        return createStringResource("RoleAnalysis.page.cluster.title");
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
                        return PageRoleAnalysisCluster.this.hasUnsavedChanges(target);
                    }
                };

                form.add(opButtonPanel);

//                initButtons(form);

                DisplayType displayType = new DisplayType()
                        .label(cluster.getName())
                        .help(getLastRebuildTimeStamp(cluster))
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
                details.setOutputMarkupId(true);
                form.add(details);

                ContainerPanelConfigurationType defaultConfiguration = findDefaultConfiguration();
                initMainPanel(defaultConfiguration, form);

            }

            @NotNull
            private IModel<List<DetailsTableItem>> loadDetailsModel(@NotNull RoleAnalysisClusterType cluster) {
                AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();

                ObjectReferenceType roleAnalysisSessionRef = cluster.getRoleAnalysisSessionRef();
                PageBase pageBase = (PageBase) getPage();
                RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();

                Task task = pageBase.createSimpleTask("Loading session");
                OperationResult result = task.getResult();

                PrismObject<RoleAnalysisSessionType> sessionPrismObject = roleAnalysisService.getSessionTypeObject(roleAnalysisSessionRef.getOid(), task, result);

                if (sessionPrismObject == null) {
                    return Model.ofList(List.of());
                }

                RoleAnalysisSessionType session = sessionPrismObject.asObjectable();

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

                Double density = clusterStatistics.getMembershipDensity();
                if (density == null) {
                    density = 0.0;
                }

                String propertyTitle = "Assignment range";

                String formattedDensity = new DecimalFormat("#.###")
                        .format(Math.round(density * 1000.0) / 1000.0);

                Integer rolesCount = clusterStatistics.getRolesCount();
                Integer usersCount = clusterStatistics.getUsersCount();

                Double membershipMean = clusterStatistics.getMembershipMean();
                String formattedMembershipMean = new DecimalFormat("#.###")
                        .format(Math.round(membershipMean * 1000.0) / 1000.0);

                Double detectedReductionMetric = clusterStatistics.getDetectedReductionMetric();
                String formattedDetectedReductionMetric = new DecimalFormat("#.###")
                        .format(Math.round(detectedReductionMetric * 1000.0) / 1000.0);

                List<DetailsTableItem> detailsModel = List.of(
                        new DetailsTableItem(createStringResource("Mode"),
                                Model.of(mode)) {
                            @Override
                            public Component createValueComponent(String id) {
                                return new Label(id, getValue());
                            }
                        },

                        new DetailsTableItem(createStringResource("Max reduction"),
                                Model.of(formattedDetectedReductionMetric)) {
                            @Override
                            public Component createValueComponent(String id) {
                                return new IconWithLabel(id, getValue()) {
                                    @Override
                                    public String getIconCssClass() {
                                        return "fa fa-arrow-down";
                                    }

                                    @Override
                                    protected String getComponentCssClass() {
                                        return super.getComponentCssClass() + " justify-content-end";
                                    }
                                };
                            }
                        },

                        new DetailsTableItem(createStringResource("Membership mean"),
                                Model.of(formattedMembershipMean)) {
                            @Override
                            public Component createValueComponent(String id) {
                                return new IconWithLabel(id, getValue()) {
                                    @Override
                                    public String getIconCssClass() {
                                        return "fe fe-assignment";
                                    }

                                    @Override
                                    protected String getComponentCssClass() {
                                        return super.getComponentCssClass() + " justify-content-end";
                                    }
                                };
                            }
                        },
                        new DetailsTableItem(createStringResource("Occupation"),
                                Model.of("")) {
                            @Override
                            public Component createValueComponent(String id) {

                                IModel<String> roleObjectCount = Model.of(rolesCount.toString());
                                IModel<String> userObjectCount = Model.of(usersCount.toString());

                                RoleAnalysisClusterOccupationPanel occupationPanel = new RoleAnalysisClusterOccupationPanel(id) {
                                    @Override
                                    public Component createFirstPanel(String idFirstPanel) {
                                        return new IconWithLabel(idFirstPanel, userObjectCount) {
                                            @Override
                                            public String getIconCssClass() {
                                                return "fa fa-user object-user-color";
                                            }
                                        };
                                    }

                                    @Override
                                    public Component createSecondPanel(String idSecondPanel) {
                                        return new IconWithLabel(idSecondPanel, roleObjectCount) {
                                            @Override
                                            public String getIconCssClass() {
                                                return "fe fe-role object-role-color";
                                            }
                                        };
                                    }

                                    @Override
                                    public Component createSeparatorPanel(String idSeparatorPanel) {
                                        Label separator = new Label(idSeparatorPanel, "");
                                        separator.add(AttributeModifier.replace("class",
                                                "d-flex align-items-center gap-3 fa-solid fa-grip-lines-vertical"));
                                        separator.setOutputMarkupId(true);
                                        add(separator);
                                        return separator;
                                    }

                                    @Override
                                    public @NotNull String getComponentCssClass() {
                                        return super.getComponentCssClass() + " justify-content-end";
                                    }
                                };

                                occupationPanel.setOutputMarkupId(true);
                                return occupationPanel;
                            }
                        },

                        new DetailsTableItem(Model.of(propertyTitle),
                                Model.of("")) {
                            @Override
                            public Component createValueComponent(String id) {
                                RangeType membershipRange = clusterStatistics.getMembershipRange();

                                IModel<String> min = Model.of(membershipRange.getMin().toString());
                                IModel<String> max = Model.of(membershipRange.getMax().toString());

                                RoleAnalysisClusterOccupationPanel occupationPanel = new RoleAnalysisClusterOccupationPanel(id) {
                                    @Override
                                    public Component createFirstPanel(String idFirstPanel) {
                                        return new IconWithLabel(idFirstPanel, min);
                                    }

                                    @Override
                                    public Component createSecondPanel(String idSecondPanel) {
                                        return new IconWithLabel(idSecondPanel, max);
                                    }

                                    @Override
                                    public Component createSeparatorPanel(String idSeparatorPanel) {
                                        Label separator = new Label(idSeparatorPanel, "");
                                        separator.add(AttributeModifier.replace("class",
                                                "d-flex align-items-center gap-3 fa-solid fa fa-arrows-h"));
                                        separator.setOutputMarkupId(true);
                                        add(separator);
                                        return separator;
                                    }

                                    @Override
                                    public @NotNull String getComponentCssClass() {
                                        return super.getComponentCssClass() + " justify-content-end";
                                    }
                                };

                                occupationPanel.setOutputMarkupId(true);
                                return occupationPanel;
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

    private void initEditConfigurationButton(@NotNull RepeatingView repeatingView) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_EDIT_MENU_ITEM,
                LayeredIconCssStyle.IN_ROW_STYLE);
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
        editConfigurationButton.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        repeatingView.add(editConfigurationButton);
    }

    private String getLastRebuildTimeStamp(@NotNull RoleAnalysisClusterType objectType) {
//        String lastRebuild = "Last rebuild: ";
//        List<RoleAnalysisOperationStatus> operationStatus = objectType.getOperationStatus();
//        if (operationStatus != null) {
//            XMLGregorianCalendar createTimestamp = operationStatus.getCreateTimestamp();
//            if (createTimestamp != null) {
//                int eonAndYear = createTimestamp.getYear();
//                int month = createTimestamp.getMonth();
//                int day = createTimestamp.getDay();
//                String time = day + "/" + month + "/" + eonAndYear;
//                lastRebuild = lastRebuild + time;
//            }
//        }
        return "unknown";
    }

}

