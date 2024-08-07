/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier;

import static com.evolveum.midpoint.common.mining.utils.ExtractPatternUtils.transformPatternWithAttributes;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel.generateAssignmentOutlierResultModel;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.DetailsTableItem;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisAttributePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisDetectedPatternDetails;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

public class RoleAnalysisAnomalyResultTabPopup extends BasePanel<RoleAnalysisOutlierPartitionType> implements Popupable {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TABS_PANEL = "tabsPanel";
    private static final String ID_WARNING_MESSAGE = "warningMessage";
    private static final String ID_FORM = "form";

    IModel<DetectedAnomalyResult> anomalyModel;
    IModel<RoleAnalysisOutlierType> outlierModel;

    public RoleAnalysisAnomalyResultTabPopup(
            @NotNull String id,
            @NotNull IModel<RoleAnalysisOutlierPartitionType> partitionModel,
            @NotNull IModel<DetectedAnomalyResult> anomalyModel,
            @NotNull IModel<RoleAnalysisOutlierType> outlierModel) {
        super(id, partitionModel);

        //TODO this should be passed as a direct constructor parameter? (remove from constructor)
        this.anomalyModel = anomalyModel;
        this.outlierModel = outlierModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        Form<?> form = new Form<>(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);

        addOrReplaceTabPanels(form);

        MessagePanel<?> warningMessage = new MessagePanel<>(
                ID_WARNING_MESSAGE,
                MessagePanel.MessagePanelType.WARN, getWarningMessageModel());
        warningMessage.setOutputMarkupId(true);
        warningMessage.add(new VisibleBehaviour(() -> getWarningMessageModel() != null));
        add(warningMessage);

    }

    private void addOrReplaceTabPanels(@NotNull Form<?> form) {
        List<ITab> tabs = createAnomalyResultTabs();
        TabbedPanel<ITab> tabPanel = WebComponentUtil.createTabPanel(ID_TABS_PANEL, getPageBase(), tabs, null);
        tabPanel.setOutputMarkupId(true);
        tabPanel.setOutputMarkupPlaceholderTag(true);
        form.addOrReplace(tabPanel);
    }

    protected List<ITab> createAnomalyResultTabs() {
        List<ITab> tabs = prepareCustomTabPanels();
        if (!tabs.isEmpty() && initDefaultTabPanels()) {
            return tabs;
        }

        tabs.add(new PanelTab(
                getPageBase().createStringResource("RoleAnalysisAnomalyResultTabPopup.tab.title.overview"),
                new VisibleEnableBehaviour()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                Task task = getPageBase().createSimpleTask("Load object");
                OperationResult operationResult = task.getResult();
                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
                ObjectReferenceType userRef = getOutlierModelObject().getTargetObjectRef();

                PrismObject<UserType> userTypeObject = WebModelServiceUtils.loadObject(UserType.class, userRef.getOid(),
                        getPageBase(), task, operationResult);

                if (userTypeObject == null) {
                    return new WebMarkupContainer(panelId);
                }

                OutlierObjectModel outlierObjectModel = generateAssignmentOutlierResultModel(
                        roleAnalysisService, getAnomalyModelObject(), getModelObject(), userTypeObject, getOutlierModelObject(),
                        task, operationResult);

                if (outlierObjectModel == null) {
                    return new WebMarkupContainer(panelId);
                }

                OutlierResultPanel detailsPanel = new OutlierResultPanel(
                        panelId,
                        Model.of("Analyzed members details panel")) {

                    @Override
                    public StringResourceModel getTitle() {
                        return createStringResource("Outlier assignment description");
                    }

                    @Override
                    public @NotNull Component getCardHeaderBody(String componentId) {
                        String outlierName = outlierObjectModel.getOutlierName();
                        Double outlierConfidence = outlierObjectModel.getOutlierConfidence();
                        String description = outlierObjectModel.getOutlierDescription();
                        String timestamp = outlierObjectModel.getTimeCreated();

                        OutlierHeaderResultPanel components = new OutlierHeaderResultPanel(componentId, outlierName,
                                description, String.valueOf(outlierConfidence), timestamp);
                        components.setOutputMarkupId(true);
                        return components;
                    }

                    @Override
                    public Component getCardBodyComponent(String componentId) {
                        //TODO just for testing
                        RepeatingView cardBodyComponent = (RepeatingView) super.getCardBodyComponent(componentId);
                        outlierObjectModel.getOutlierItemModels().forEach(outlierItemModel -> cardBodyComponent.add(
                                new OutlierItemResultPanel(cardBodyComponent.newChildId(), outlierItemModel)));
                        return cardBodyComponent;
                    }

                    @Override
                    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                        super.onClose(ajaxRequestTarget);
                    }

                };
                detailsPanel.setOutputMarkupId(true);
                return detailsPanel;
            }
        });

        tabs.add(new PanelTab(
                getPageBase().createStringResource("RoleAnalysisAnomalyResultTabPopup.tab.title.attribute"),
                new VisibleEnableBehaviour()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                //TODO this is just test. We need to implement this properly. Data must be stored in the
                Task task = getPageBase().createSimpleTask("Load object");
                OperationResult operationResult = task.getResult();
                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
                ObjectReferenceType sessionRef = getModelObject().getTargetSessionRef();
                ObjectReferenceType userRef = getOutlierModelObject().getTargetObjectRef();

                PrismObject<RoleAnalysisSessionType> sessionPrism = roleAnalysisService.getSessionTypeObject(
                        sessionRef.getOid(), task, task.getResult());

                if (sessionPrism == null) {
                    return new WebMarkupContainer(panelId);
                }

                List<RoleAnalysisAttributeDef> attributesForUserAnalysis = roleAnalysisService
                        .resolveAnalysisAttributes(sessionPrism.asObjectable(), UserType.COMPLEX_TYPE);
                PrismObject<UserType> userTypeObject = WebModelServiceUtils.loadObject(UserType.class, userRef.getOid(),
                        getPageBase(), task, operationResult);

                if (userTypeObject == null || attributesForUserAnalysis == null) {
                    return new WebMarkupContainer(panelId);
                }

                Set<String> userPathToMark = roleAnalysisService.resolveUserValueToMark(
                        userTypeObject, attributesForUserAnalysis);

                DetectedAnomalyStatistics anomalyModelStatistics = getAnomalyModelStatistics();
                AttributeAnalysis attributeAnalysis = anomalyModelStatistics.getAttributeAnalysis();
                if (attributeAnalysis == null) {
                    return new WebMarkupContainer(panelId);
                }

                RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResult = attributeAnalysis
                        .getRoleAttributeAnalysisResult();
                RoleAnalysisAttributeAnalysisResult userRoleMembersCompare = attributeAnalysis
                        .getUserRoleMembersCompare();

                if (roleAttributeAnalysisResult == null || userRoleMembersCompare == null) {
                    return new WebMarkupContainer(panelId);
                }

                return new RoleAnalysisWidgetsPanel(panelId, loadDetailsModel()) {
                    @Override
                    protected @NotNull Component getPanelComponent(String id1) {
                        RoleAnalysisAttributePanel roleAnalysisAttributePanel = new RoleAnalysisAttributePanel(id1,
                                Model.of("Role analysis attribute panel"),
                                null, roleAttributeAnalysisResult,
                                null, userRoleMembersCompare) {
                            @Override
                            protected @NotNull String getChartContainerStyle() {
                                return "height:30vh;";
                            }

                            @Override
                            public Set<String> getPathToMark() {
                                return userPathToMark;
                            }
                        };

                        roleAnalysisAttributePanel.setOutputMarkupId(true);
                        return roleAnalysisAttributePanel;
                    }
                };
            }
        });

        DetectedAnomalyStatistics anomalyStatistics = getAnomalyModelStatistics();
        RoleAnalysisPatternAnalysis patternAnalysis = anomalyStatistics.getPatternAnalysis();
        RoleAnalysisDetectionPatternType topDetectedPattern = patternAnalysis.getTopDetectedPattern();

        if (topDetectedPattern != null) {
            tabs.add(new PanelTab(
                    getPageBase().createStringResource("RoleAnalysisAnomalyResultTabPopup.tab.title.pattern"),
                    new VisibleEnableBehaviour()) {

                @Serial private static final long serialVersionUID = 1L;

                @Override
                public WebMarkupContainer createPanel(String panelId) {
                    DetectedPattern pattern = transformPatternWithAttributes(topDetectedPattern);

                    return new RoleAnalysisWidgetsPanel(panelId, loadDetailsModel()) {
                        @Override
                        protected @NotNull Component getPanelComponent(String id1) {
                            RoleAnalysisDetectedPatternDetails statisticsPanel = new RoleAnalysisDetectedPatternDetails(id1,
                                    Model.of(pattern)) {

                                @Contract(pure = true)
                                @Override
                                protected @NotNull String getCssClassForCardContainer() {
                                    return "m-0 border-0";
                                }

                                @Override
                                protected String getIconBoxContainerCssStyle() {
                                    return "width:40px";
                                }

                                @Contract(pure = true)
                                @Override
                                protected @NotNull String getCssClassForHeaderItemsContainer() {
                                    return "row pl-4 pr-4 pt-4";
                                }

                                @Contract(pure = true)
                                @Override
                                protected @NotNull String getCssClassForStatisticsPanelContainer() {
                                    return "col-12 p-0 border-top";
                                }

                                @Contract(pure = true)
                                @Override
                                protected @NotNull String getCssClassForStatisticsPanel() {
                                    return "col-12 p-0";
                                }

                                @Override
                                protected String getInfoBoxClass() {
                                    return super.getInfoBoxClass();
                                }
                            };
                            statisticsPanel.setOutputMarkupId(true);
                            statisticsPanel.add(
                                    AttributeAppender.append("class", "bg-white rounded elevation-1"));

                            return statisticsPanel;
                        }
                    };
                }
            });
        }

        return tabs;
    }

    protected @NotNull List<ITab> prepareCustomTabPanels() {
        return new ArrayList<>();
    }

    protected boolean initDefaultTabPanels() {
        return true;
    }

    protected IModel<String> getWarningMessageModel() {
        return null;
    }

    public int getWidth() {
        return 80;
    }

    public int getHeight() {
        return 60;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    public StringResourceModel getTitle() {
        return createStringResource("RoleAnalysisAnomalyResultTabPopup.popup.title");
    }

    public Component getContent() {
        RoleAnalysisAnomalyResultTabPopup components = this;
        components.add(AttributeModifier.append("class", "p-0"));
        return components;
    }

    private @NotNull IModel<List<DetailsTableItem>> loadDetailsModel() {

        List<DetailsTableItem> detailsModel = List.of(
                new DetailsTableItem(createStringResource(""),
                        Model.of("")) {
                    @Override
                    public Component createValueComponent(String id) {
                        Label label = new Label(id, "0 (todo)");
                        label.add(AttributeAppender.append("class", " h4"));
                        return label;
                    }

                    @Override
                    public Component createLabelComponent(String id) {
                        return new LabelWithHelpPanel(id, createStringResource("RoleAnalysisOutlierType.anomalyCount")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisOutlierType.anomalyCount.help");
                            }
                        };
                    }
                },

                new DetailsTableItem(createStringResource(""),
                        Model.of("")) {
                    @Override
                    public Component createValueComponent(String id) {
                        Label label = new Label(id, "0 (todo)");
                        label.add(AttributeAppender.append("class", " h4"));
                        return label;
                    }

                    @Override
                    public Component createLabelComponent(String id) {
                        return new LabelWithHelpPanel(id,
                                createStringResource("RoleAnalysisOutlierType.anomalyAverageConfidence")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisOutlierType.anomalyAverageConfidence.help");
                            }
                        };
                    }
                },

                new DetailsTableItem(createStringResource(""),
                        Model.of("Sort")) {
                    @Override
                    public Component createValueComponent(String id) {
                        Label label = new Label(id, "0 (todo)");
                        label.add(AttributeAppender.append("class", " h4"));
                        return label;
                    }

                    @Override
                    public Component createLabelComponent(String id) {
                        return new LabelWithHelpPanel(id, Model.of("TBD")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisOutlierType.anomalyAverageConfidence.help");
                            }
                        };
                    }
                },

                new DetailsTableItem(createStringResource(""),
                        Model.of("Chart")) {
                    @Override
                    public Component createValueComponent(String id) {
                        Label label = new Label(id, "0 (todo)");
                        label.add(AttributeAppender.append("class", " h4"));
                        return label;
                    }

                    @Override
                    public Component createLabelComponent(String id) {
                        return new LabelWithHelpPanel(id, Model.of("TBD")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisOutlierType.anomalyAverageConfidence.help");
                            }
                        };
                    }
                }
        );

        return Model.ofList(detailsModel);
    }

    public @NotNull DetectedAnomalyResult getAnomalyModelObject() {
        return anomalyModel.getObject();
    }

    public @NotNull DetectedAnomalyStatistics getAnomalyModelStatistics() {
        return getAnomalyModelObject().getStatistics();
    }

    public @NotNull RoleAnalysisOutlierType getOutlierModelObject() {
        return outlierModel.getObject();
    }

    @Override
    public @NotNull Component getFooter() {
        Component footer = Popupable.super.getFooter();
        footer.add(new VisibleBehaviour(() -> false));
        return footer;
    }

    @Override
    public @Nullable Component getTitleComponent() {
        Component titleComponent = Popupable.super.getTitleComponent();
        if (titleComponent != null) {
            titleComponent.add(AttributeAppender.append("class", "m-0"));
        }
        return titleComponent;
    }
}
