/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.densityBasedColor;

import java.io.Serial;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.error.ErrorPanel;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsFragment;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.component.InlineOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.ProgressBar;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.OutlierSummaryPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.DetailsTableItem;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

//TODO correct authorizations
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/roleAnalysisOutlier", matchUrlForSecurity = "/admin/roleAnalysisOutlier")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLE_ANALYSIS_ALL_URL,
                label = "PageRoleAnalysis.auth.roleAnalysisAll.label",
                description = "PageRoleAnalysis.auth.roleAnalysisAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLE_ANALYSIS_SESSION_URL,
                label = "PageRoleAnalysis.auth.roleAnalysisSession.label",
                description = "PageRoleAnalysis.auth.roleAnalysisSession.description")
})

public class PageRoleAnalysisOutlier extends PageAssignmentHolderDetails<RoleAnalysisOutlierType, AssignmentHolderDetailsModel<RoleAnalysisOutlierType>> {

    public static final String PARAM_IS_WIZARD = "isWizard";
    boolean isWizardPanel = false;
    private static final Trace LOGGER = TraceManager.getTrace(RoleAnalysisOutlierType.class);

    public boolean isWizardPanel() {
        StringValue stringValue = getPageParameters().get(PARAM_IS_WIZARD);
        if (stringValue != null) {
            if ("true".equalsIgnoreCase(stringValue.toString())
                    || "false".equalsIgnoreCase(stringValue.toString())) {
                this.isWizardPanel = getPageParameters().get(PARAM_IS_WIZARD).toBoolean();
            } else {
                getPageParameters().remove(PARAM_IS_WIZARD);
            }
        }
        return isWizardPanel;
    }

    public PageRoleAnalysisOutlier() {
        super();
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
    public Class<RoleAnalysisOutlierType> getType() {
        return RoleAnalysisOutlierType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<RoleAnalysisOutlierType> summaryModel) {
        return new OutlierSummaryPanel(id, summaryModel, null);
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return createStringResource("RoleMining.page.cluster.title");
    }

    protected boolean canShowWizard() {
        return isWizardPanel();
    }

    @Override
    protected AssignmentHolderDetailsModel<RoleAnalysisOutlierType> createObjectDetailsModels(PrismObject<RoleAnalysisOutlierType> object) {
        return super.createObjectDetailsModels(object);
    }

    @Override
    protected void onBackPerform(AjaxRequestTarget target) {
        ((PageBase) getPage()).navigateToNext(PageRoleAnalysis.class);
    }

    private DetailsFragment createRoleWizardFragment(Class<? extends AbstractWizardPanel> clazz) {

        return new DetailsFragment(ID_DETAILS_VIEW, ID_TEMPLATE_VIEW, PageRoleAnalysisOutlier.this) {
            @Override
            protected void initFragmentLayout() {
                try {
                    Constructor<? extends AbstractWizardPanel> constructor = clazz.getConstructor(String.class, WizardPanelHelper.class);
                    AbstractWizardPanel wizard = constructor.newInstance(ID_TEMPLATE, createObjectWizardPanelHelper());
                    add(wizard);
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                        InvocationTargetException ignored) {
                    LOGGER.error("Couldn't create wizard panel");
                }
            }
        };

    }

    protected DetailsFragment createDetailsFragment() {
        if (!isNativeRepo()) {
            return new DetailsFragment(ID_DETAILS_VIEW, ID_TEMPLATE_VIEW, PageRoleAnalysisOutlier.this) {
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

        return new DetailsFragment(ID_DETAILS_VIEW, "fragment", PageRoleAnalysisOutlier.this) {

            @Override
            protected void initFragmentLayout() {

                RoleAnalysisOutlierType outlier = getObjectDetailsModels().getObjectType();
                IModel<List<DetailsTableItem>> detailsModelIModel = loadDetailsModel(outlier);

                MidpointForm<?> form = new MidpointForm<>("mainForm") {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected void onDetach() {
                        super.onDetach();
                    }

                };

                form.setMultiPart(true);
                add(form);

                InlineOperationalButtonsPanel<RoleAnalysisOutlierType> opButtonPanel = new InlineOperationalButtonsPanel<>(
                        "buttons", getObjectDetailsModels().getObjectWrapperModel()) {

                    @Override
                    protected IModel<String> getDeleteButtonTitleModel() {
                        return Model.of("Remove outlier");
                    }

                    @Override
                    protected void savePerformed(AjaxRequestTarget target) {
                        PageRoleAnalysisOutlier.this.savePerformed(target);
                    }

                    @Override
                    protected IModel<String> getTitle() {
                        return createStringResource("RoleAnalysis.page.outlier.title");
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
                        return PageRoleAnalysisOutlier.this.hasUnsavedChanges(target);
                    }
                };

                form.add(opButtonPanel);

//                initButtons(form);

                DisplayType displayType = new DisplayType()
                        .label(outlier.getName())
                        .help(getLastRebuildTimeStamp(outlier))
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
            private IModel<List<DetailsTableItem>> loadDetailsModel(@NotNull RoleAnalysisOutlierType outlier) {
                List<RoleAnalysisOutlierDescriptionType> analysisResult = outlier.getResult();
                if (analysisResult == null) {
                    return Model.ofList(List.of());
                }

                PageBase pageBase = getPageBase();
                RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
                //TODO session not cluster
                ObjectReferenceType targetClusterRef = outlier.getTargetSessionRef();
                Task task = pageBase.createSimpleTask("loadDetailsModel");
                OperationResult result = task.getResult();
                @Nullable PrismObject<RoleAnalysisSessionType> sessionPrism = roleAnalysisService
                        .getSessionTypeObject(targetClusterRef.getOid(), task, result);
                if (sessionPrism == null) {
                    return Model.ofList(List.of());
                }
                RoleAnalysisSessionType session = sessionPrism.asObjectable();

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

                double averageConfidence = 0.0;
                for (RoleAnalysisOutlierDescriptionType resultDetails : analysisResult) {
                    Double confidence = resultDetails.getConfidence();
                    if (confidence != null) {
                        averageConfidence += confidence;
                    }
                }
                averageConfidence = averageConfidence / analysisResult.size();
                String formattedConfidence = String.format("%.2f", averageConfidence * 100);

                List<DetailsTableItem> detailsModel = List.of(
                        new DetailsTableItem(createStringResource("Mode"),
                                Model.of(mode)) {
                            @Override
                            public Component createValueComponent(String id) {
                                return new Label(id, getValue());
                            }
                        },
                        new DetailsTableItem(createStringResource("Outlier properties"),
                                Model.of(String.valueOf(analysisResult.size()))) {
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
                        new DetailsTableItem(createStringResource("Association"),
                                Model.of(sessionPrism.getName().getOrig())) {
                            @Override
                            public Component createValueComponent(String id) {
                                return new IconWithLabel(id, getValue()) {
                                    @Override
                                    public String getIconCssClass() {
                                       return GuiStyleConstants.CLASS_ROLE_ANALYSIS_SESSION_ICON;
                                    }

                                    @Override
                                    protected String getComponentCssClass() {
                                        return super.getComponentCssClass() + " justify-content-end";
                                    }
                                };
                            }
                        },
                        new DetailsTableItem(createStringResource("Confidence"),
                                Model.of(formattedConfidence)) {

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

    private String getLastRebuildTimeStamp(@NotNull RoleAnalysisOutlierType outlier) {
        String lastRebuild = "Last rebuild: ";
        MetadataType metadata = outlier.getMetadata();
        if (metadata != null) {
            XMLGregorianCalendar createTimestamp = metadata.getCreateTimestamp();
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
}

