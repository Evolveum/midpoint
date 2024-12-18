/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier;

import static com.evolveum.midpoint.gui.api.util.LocalizationUtil.translate;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.densityBasedColorOposite;

import java.io.Serial;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisAttributesDto;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.ProgressBar;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisAttributePanel;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisMultiplePartitionAnomalyResultTabPopup extends BasePanel<List<RoleAnalysisOutlierPartitionType>> implements Popupable {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TABS_PANEL = "tabsPanel";
    private static final String ID_WARNING_MESSAGE = "warningMessage";
    private static final String ID_FORM = "form";

    IModel<DetectedAnomalyResult> anomalyModel;
    IModel<RoleAnalysisOutlierType> outlierModel;

    public RoleAnalysisMultiplePartitionAnomalyResultTabPopup(
            @NotNull String id,
            @NotNull IModel<List<RoleAnalysisOutlierPartitionType>> partitionModel,
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
                return getAnomalyOverview(panelId);
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
                List<RoleAnalysisOutlierPartitionType> partitions = getModelObject();
                ObjectReferenceType userRef = getOutlierModelObject().getObjectRef();
                DetectedAnomalyResult anomalyModelObject = getAnomalyModelObject();
                ObjectReferenceType anomalyRef = anomalyModelObject.getTargetObjectRef();

                Set<RoleAnalysisAttributeDef> attributesForUserAnalysis = new HashSet<>();
                for (RoleAnalysisOutlierPartitionType partition : partitions) {
                    ObjectReferenceType sessionRef = partition.getTargetSessionRef();

                    PrismObject<RoleAnalysisSessionType> sessionPrism = roleAnalysisService.getSessionTypeObject(
                            sessionRef.getOid(), task, task.getResult());

                    if (sessionPrism == null) {
                        return new WebMarkupContainer(panelId);
                    }

                    List<RoleAnalysisAttributeDef> attributeDefs = roleAnalysisService
                            .resolveAnalysisAttributes(sessionPrism.asObjectable(), UserType.COMPLEX_TYPE);
                    if (attributeDefs != null) {
                        attributesForUserAnalysis.addAll(attributeDefs);
                    }
                }

                PrismObject<UserType> userTypeObject = WebModelServiceUtils.loadObject(UserType.class, userRef.getOid(),
                        getPageBase(), task, operationResult);

                if (userTypeObject == null || attributesForUserAnalysis.isEmpty()) {
                    return new WebMarkupContainer(panelId);
                }

                Set<String> userPathToMark = roleAnalysisService.resolveUserValueToMark(
                        userTypeObject, new ArrayList<>(attributesForUserAnalysis));

                //TODO this take a lot of time when role is popular. Think about better solution (MAJOR).
                RoleAnalysisAttributeAnalysisResult roleAnalysisAttributeAnalysisResult = roleAnalysisService
                        .resolveRoleMembersAttribute(anomalyRef.getOid(),
                                task, task.getResult(), new ArrayList<>(attributesForUserAnalysis));

                RoleAnalysisAttributeAnalysisResult userAttributes = roleAnalysisService.
                        resolveUserAttributes(userTypeObject, new ArrayList<>(attributesForUserAnalysis));

                RoleAnalysisAttributeAnalysisResult compareAttributeResult = roleAnalysisService
                        .resolveSimilarAspect(userAttributes, roleAnalysisAttributeAnalysisResult);

                if (compareAttributeResult == null) {
                    return new WebMarkupContainer(panelId);
                }

                return new RoleAnalysisWidgetsPanel(panelId, loadOutlierVsRoleMemberModel()) {
                    @Override
                    protected @NotNull Component getPanelComponent(String id1) {
                        LoadableModel<RoleAnalysisAttributesDto> attributesModel = new LoadableModel<>(false) {
                            @Override
                            protected RoleAnalysisAttributesDto load() {
                                return RoleAnalysisAttributesDto.ofCompare("RoleAnalysis.analysis.attribute.panel", roleAnalysisAttributeAnalysisResult, compareAttributeResult);
                            }
                        };

                        RoleAnalysisAttributePanel roleAnalysisAttributePanel = new RoleAnalysisAttributePanel(id1,
                                attributesModel) {

                            @Override
                            protected @NotNull String getChartContainerStyle() {
                                return "min-height:350px;";
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
        RoleAnalysisMultiplePartitionAnomalyResultTabPopup components = this;
        components.add(AttributeModifier.append("class", "p-0"));
        return components;
    }

    private @NotNull IModel<List<WidgetItemModel>> loadOutlierVsRoleMemberModel() {

        List<WidgetItemModel> detailsModel = List.of(
//                new WidgetItemModel(createStringResource(""),
//                        Model.of("")) {
//                    @Override
//                    public Component createValueComponent(String id) {
//                        Label label = new Label(id, "0 (todo)");
//                        label.add(AttributeAppender.append("class", " h4"));
//                        return label;
//                    }
//
//                    @Override
//                    public Component createDescriptionComponent(String id) {
//                        return new LabelWithHelpPanel(id, createStringResource("RoleAnalysisOutlierType.anomalyCount")) {
//                            @Override
//                            protected IModel<String> getHelpModel() {
//                                return createStringResource("RoleAnalysisOutlierType.anomalyCount.help");
//                            }
//                        };
//                    }
//                },

//                new WidgetItemModel(createStringResource(""),
//                        Model.of("")) {
//                    @Override
//                    public Component createValueComponent(String id) {
//                        Label label = new Label(id, "0 (todo)");
//                        label.add(AttributeAppender.append("class", " h4"));
//                        return label;
//                    }
//
//                    @Override
//                    public Component createDescriptionComponent(String id) {
//                        return new LabelWithHelpPanel(id,
//                                createStringResource("RoleAnalysisOutlierType.anomalyAverageConfidence")) {
//                            @Override
//                            protected IModel<String> getHelpModel() {
//                                return createStringResource("RoleAnalysisOutlierType.anomalyAverageConfidence.help");
//                            }
//                        };
//                    }
//                },

//                new WidgetItemModel(createStringResource(""),
//                        createStringResource("RoleAnalysis.sort")) {
//                    @Override
//                    public Component createValueComponent(String id) {
//                        Label label = new Label(id, "0 (todo)");
//                        label.add(AttributeAppender.append("class", " h4"));
//                        return label;
//                    }
//
//                    @Override
//                    public Component createDescriptionComponent(String id) {
//                        return new LabelWithHelpPanel(id, Model.of("TBD")) {
//                            @Override
//                            protected IModel<String> getHelpModel() {
//                                return createStringResource("RoleAnalysisOutlierType.anomalyAverageConfidence.help");
//                            }
//                        };
//                    }
//                },

//                new WidgetItemModel(createStringResource(""),
//                        createStringResource("RoleAnalysis.chart")) {
//                    @Override
//                    public Component createValueComponent(String id) {
//                        Label label = new Label(id, "0 (todo)");
//                        label.add(AttributeAppender.append("class", " h4"));
//                        return label;
//                    }
//
//                    @Override
//                    public Component createDescriptionComponent(String id) {
//                        return new LabelWithHelpPanel(id, Model.of("TBD")) {
//                            @Override
//                            protected IModel<String> getHelpModel() {
//                                return createStringResource("RoleAnalysisOutlierType.anomalyAverageConfidence.help");
//                            }
//                        };
//                    }
//                }
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

    private @NotNull RoleAnalysisHeaderWithWidgetsPanel getAnomalyOverview(String id) {
        RoleAnalysisHeaderWithWidgetsPanel components = new RoleAnalysisHeaderWithWidgetsPanel(id, loadDetailsModel()) {
            @Override
            protected @NotNull Component getHeaderComponent(String id) {
                return getAnomalyHeaderComponent(id);
            }
        };
        components.setOutputMarkupId(true);
        return components;
    }

    private @NotNull Component getAnomalyHeaderComponent(String id) {
        DetectedAnomalyResult object = getAnomalyModelObject();

        PageBase pageBase = getPageBase();
        Task simpleTask = pageBase.createSimpleTask("Load object");
        OperationResult result = simpleTask.getResult();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        PrismObject<RoleType> rolePrismObject = roleAnalysisService
                .getRoleTypeObject(object.getTargetObjectRef().getOid(), simpleTask, result);

        if (rolePrismObject == null) {
            return new WebMarkupContainer(id);
        }

        RoleType role = rolePrismObject.asObjectable();
        String outlierName = role.getName().getOrig();

        //TODO check why targetName is null
//        PolyStringType targetName = object.getTargetObjectRef().getTargetName();
//        String outlierName = "unknown";
//        if (targetName != null) {
//            outlierName = targetName.getOrig();
//        }

        List<RoleAnalysisOutlierPartitionType> partitionList = getModelObject();
        Date latestDate = null;
        for (RoleAnalysisOutlierPartitionType partition : partitionList) {
            XMLGregorianCalendar createTimestamp = partition.getCreateTimestamp();
            GregorianCalendar gregorianCalendar = createTimestamp.toGregorianCalendar();
            Date date = gregorianCalendar.getTime();

            if (latestDate == null || date.after(latestDate)) {
                latestDate = date;
            }
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = sdf.format(latestDate);

        RoleAnalysisOutlierType outlierModelObject = getOutlierModelObject();
        Double overallConfidence = outlierModelObject.getAnomalyObjectsConfidence();
        if (overallConfidence == null) {
            overallConfidence = 0.0;
        }
        BigDecimal bd = new BigDecimal(overallConfidence);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        double pointsDensity = bd.doubleValue();

        OutlierHeaderResultPanel components = new OutlierHeaderResultPanel(id,
                outlierName,
                translate("Analysis.anomaly.result.panel.title"),
                String.valueOf(pointsDensity), formattedDate);
        components.setOutputMarkupId(true);
        return components;
    }

    private @NotNull IModel<List<WidgetItemModel>> loadDetailsModel() {

        DetectedAnomalyResult anomalyModelObject = getAnomalyModelObject();
        DetectedAnomalyStatistics anomalyModelStatistics = getAnomalyModelStatistics();
        RoleAnalysisPatternAnalysis patternAnalysis = anomalyModelStatistics.getPatternAnalysis();
        RoleAnalysisOutlierType outlierModelObject = getOutlierModelObject();
        List<ObjectReferenceType> duplicatedRoleAssignment = outlierModelObject.getDuplicatedRoleAssignment();
        String oid = anomalyModelObject.getTargetObjectRef().getOid();
        boolean isDuplicate = false;
        for (ObjectReferenceType objectReferenceType : duplicatedRoleAssignment) {
            if (objectReferenceType.getOid() != null && objectReferenceType.getOid().equals(oid)) {
                isDuplicate = true;
                break;
            }
        }

        boolean finalIsDuplicate = isDuplicate;
        List<WidgetItemModel> detailsModel = List.of(

                new WidgetItemModel(createStringResource(""),
                        Model.of("")) {

                    @Override
                    public Component createTitleComponent(String id) {
                        return new Label(id, createStringResource("RoleAnalysisOutlierType.widget.attribute"));
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        String localizedKey = "RoleAnalysisOutlierType.widget.attribute.description.role.member";
                        return new Label(id, createStringResource(localizedKey));
                    }

                    @Override
                    public Component createValueComponent(String id) {
                        Double itemFactorConfidence = anomalyModelStatistics.getItemFactorConfidence();
                        if (itemFactorConfidence == null) {
                            itemFactorConfidence = 0.0;
                        }
                        BigDecimal bd = new BigDecimal(itemFactorConfidence);
                        bd = bd.setScale(2, RoundingMode.HALF_UP);
                        double pointsDensity = bd.doubleValue();
                        String title = createStringResource("RoleAnalysisOutlierType.widget.attribute.confidence")
                                .getString();
                        return getDensityProgressPanel(id, pointsDensity, title);
                    }

                    @Override
                    protected String replaceValueCssClass() {
                        return "pt-3 pb-1";
                    }

                    @Override
                    public Component createFooterComponent(String id) {

                        AttributeAnalysis attributeAnalysis = anomalyModelStatistics.getAttributeAnalysis();

                        int attributeAboveThreshold = 0;
                        int threshold = 80;

                        RoleAnalysisAttributeAnalysisResult userClusterCompare = attributeAnalysis.getUserRoleMembersCompare();
                        if (userClusterCompare != null && userClusterCompare.getAttributeAnalysis() != null) {
                            List<RoleAnalysisAttributeAnalysis> analysedAttributeAnalysis = userClusterCompare.getAttributeAnalysis();

                            for (RoleAnalysisAttributeAnalysis attribute : analysedAttributeAnalysis) {
                                Double density = attribute.getDensity();
                                if (density != null) {
                                    if (density >= threshold) {
                                        attributeAboveThreshold++;
                                    }
                                }
                            }
                        }

                        return new Label(id, createStringResource("RoleAnalysisOutlierType.widget.attribute.footer",
                                attributeAboveThreshold));
                    }
                },

                new WidgetItemModel(createStringResource(""),
                        Model.of("")) {

                    @Override
                    public Component createTitleComponent(String id) {
                        return new Label(id, createStringResource("RoleAnalysisOutlierType.widget.duplicated"));
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        String localizedKey = "RoleAnalysisOutlierType.widget.duplicated.description.none";
                        if (finalIsDuplicate) {
                            localizedKey = "RoleAnalysisOutlierType.widget.duplicated.description";
                        }

                        return new Label(id, createStringResource(localizedKey));
                    }

                    @Override
                    public Component createValueComponent(String id) {
                        String category = createStringResource(
                                "RoleAnalysisOutlierType.widget.duplicated.value.notFound").getString();
                        if (finalIsDuplicate) {
                            category = createStringResource(
                                    "RoleAnalysisOutlierType.widget.duplicated.value.found").getString();
                        }

                        Label label = new Label(id, category);
                        label.setOutputMarkupId(true);
                        return label;
                    }

                    @Override
                    protected String replaceValueCssClass() {
                        return "d-flex align-items-center badge p-3 m-3 justify-content-center";
                    }

                    @Override
                    protected String replaceValueCssStyle() {
                        return "color: #18a2b8; font-size: 20px; background-color: #dcf1f4;";
                    }

                    @Override
                    public Component createFooterComponent(String id) {
                        return new Label(id, createStringResource("RoleAnalysisOutlierType.widget.duplicated.footer",
                                1));
                    }
                },

                new WidgetItemModel(createStringResource(""),
                        Model.of("")) {

                    @Override
                    public Component createTitleComponent(String id) {
                        return new Label(id, createStringResource("RoleAnalysisOutlierType.widget.repo.role.popularity"));
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        String localizedKey = "RoleAnalysisOutlierType.widget.repo.role.popularity.description";
                        return new Label(id, createStringResource(localizedKey));
                    }

                    @Override
                    public Component createValueComponent(String id) {
                        double memberCoverageConfidence = 0.0;
                        FrequencyType memberFrequencyContainer = anomalyModelStatistics.getMemberCoverageConfidence();
                        if (memberFrequencyContainer != null && memberFrequencyContainer.getPercentageRatio() != null) {
                            memberCoverageConfidence = memberFrequencyContainer.getPercentageRatio();
                        }
                        BigDecimal bd = BigDecimal.valueOf(memberCoverageConfidence);
                        bd = bd.setScale(2, RoundingMode.HALF_UP);
                        double pointsDensity = bd.doubleValue();
                        String title = createStringResource("RoleAnalysisOutlierType.widget.repo.role.popularity.confidence")
                                .getString();
                        return getDensityProgressPanel(id, pointsDensity, title);
                    }

                    @Override
                    protected String replaceValueCssClass() {
                        return "pt-3 pb-1 mt-auto";
                    }

                }

        );

        return Model.ofList(detailsModel);
    }

    private static @NotNull ProgressBar getDensityProgressPanel(
            @NotNull String componentId,
            @NotNull Double density,
            @NotNull String title) {

        BigDecimal bd = new BigDecimal(Double.toString(density));
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        double pointsDensity = bd.doubleValue();

        String colorClass = densityBasedColorOposite(pointsDensity);

        ProgressBar progressBar = new ProgressBar(componentId) {

            @Override
            public double getActualValue() {
                return pointsDensity;
            }

            @Override
            public String getProgressBarColor() {
                return colorClass;
            }

            @Override
            protected String getProgressBarContainerStyle() {
                return "border-radius: 10px; height:10px;";
            }

            @Override
            public String getBarTitle() {
                return title;
            }
        };
        progressBar.setOutputMarkupId(true);
        return progressBar;
    }
}
