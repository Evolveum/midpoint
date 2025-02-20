/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier;

import static com.evolveum.midpoint.common.mining.utils.ExtractPatternUtils.transformPatternWithAttributes;
import static com.evolveum.midpoint.gui.api.util.LocalizationUtil.translate;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.buildDensityProgressPanel;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.loadRoleAnalysisTempTable;

import java.io.Serial;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;

import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisAttributesDto;

import com.evolveum.midpoint.web.component.RoleAnalysisTabbedPanel;
import com.evolveum.midpoint.web.component.data.RoleAnalysisTable;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
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

import javax.xml.datatype.XMLGregorianCalendar;

public class RoleAnalysisSinglePartitionAnomalyResultTabPopup extends BasePanel<RoleAnalysisOutlierPartitionType> implements Popupable {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TABS_PANEL = "tabsPanel";
    private static final String ID_WARNING_MESSAGE = "warningMessage";
    private static final String ID_FORM = "form";

    IModel<DetectedAnomalyResultType> anomalyModel;
    IModel<RoleAnalysisOutlierType> outlierModel;

    public RoleAnalysisSinglePartitionAnomalyResultTabPopup(
            @NotNull String id,
            @NotNull IModel<RoleAnalysisOutlierPartitionType> partitionModel,
            @NotNull IModel<DetectedAnomalyResultType> anomalyModel,
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
        createRoleAnalysisTabPabel(form, tabs);
    }

    private void createRoleAnalysisTabPabel(@NotNull Form<?> form, List<ITab> tabs) {
        RoleAnalysisTabbedPanel<ITab> tabPanel = new RoleAnalysisTabbedPanel<>(ID_TABS_PANEL, tabs, null) {
            @Serial private static final long serialVersionUID = 1L;

            @Contract("_, _ -> new")
            @Override
            protected @NotNull WebMarkupContainer newLink(String linkId, final int index) {
                return new AjaxSubmitLink(linkId) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected void onError(AjaxRequestTarget target) {
                        super.onError(target);
                        target.add(getPageBase().getFeedbackPanel());
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        super.onSubmit(target);

                        setSelectedTab(index);
                        if (target != null) {
                            target.add(findParent(TabbedPanel.class));
                            target.add(getPageBase().getFeedbackPanel());
                        }
                    }

                };
            }
        };
        tabPanel.setOutputMarkupId(true);
        tabPanel.setOutputMarkupPlaceholderTag(true);
        tabPanel.add(AttributeModifier.append("class", "m-0"));
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
                getPageBase().createStringResource("RoleAnalysisAnomalyResultTabPopup.tab.title.group"),
                new VisibleEnableBehaviour()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                RoleAnalysisOutlierType outlier = outlierModel.getObject();
                DisplayValueOption displayValueOption = new DisplayValueOption();
                PageBase pageBase = getPageBase();
                RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
                Task task = pageBase.createSimpleTask("loadDetailsPanel");
                RoleAnalysisClusterType cluster = roleAnalysisService.prepareTemporaryCluster(
                        outlier, getModelObject(), displayValueOption, task);
                if (cluster == null) {
                    return new WebMarkupContainer(panelId);
                }

                RoleAnalysisTable<MiningUserTypeChunk, MiningRoleTypeChunk> table = loadRoleAnalysisTempTable(
                        panelId, pageBase, Collections.singletonList(getAnomalyModelObject()), getModelObject(), outlier, cluster);
                table.setOutputMarkupId(true);
                return table;
            }
        });

        tabs.add(new PanelTab(
                getPageBase().createStringResource("RoleAnalysisAnomalyResultTabPopup.tab.title.attribute"),
                new VisibleEnableBehaviour()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
                AttributeAnalysisType attributeAnalysis = getAnomalyModelStatistics().getAttributeAnalysis();
                if (attributeAnalysis == null || attributeAnalysis.getUserAttributeAnalysisResult() == null) {
                    return new WebMarkupContainer(panelId);
                }
                RoleAnalysisAttributeAnalysisResultType userAttributeAnalysisResult = attributeAnalysis.getUserAttributeAnalysisResult();

                Set<String> userPathToMark = roleAnalysisService.resolveUserValueToMark(userAttributeAnalysisResult);

                return new RoleAnalysisWidgetsPanel(panelId, loadOutlierVsRoleMemberModel()) {
                    @Override
                    protected @NotNull Component getPanelComponent(String id1) {

                        LoadableModel<RoleAnalysisAttributesDto> attributesModel = new LoadableModel<>(false) {
                            @Override
                            protected RoleAnalysisAttributesDto load() {
                                return RoleAnalysisAttributesDto.fromAnomalyStatistics(
                                        "RoleAnalysis.analysis.attribute.panel", getAnomalyModelStatistics());
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

        DetectedAnomalyStatisticsType anomalyStatistics = getAnomalyModelStatistics();
        RoleAnalysisPatternAnalysisType patternAnalysis = anomalyStatistics.getPatternAnalysis();
        RoleAnalysisDetectionPatternType topDetectedPattern = patternAnalysis.getTopDetectedPattern();

        if (topDetectedPattern != null) {
            tabs.add(new PanelTab(
                    getPageBase().createStringResource("RoleAnalysisAnomalyResultTabPopup.tab.title.pattern"),
                    new VisibleEnableBehaviour()) {

                @Serial private static final long serialVersionUID = 1L;

                @Override
                public WebMarkupContainer createPanel(String panelId) {
                    DetectedPattern pattern = transformPatternWithAttributes(topDetectedPattern);

                    return new RoleAnalysisWidgetsPanel(panelId, loadOutlierVsRoleMemberModel()) {
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
                                    return "d-flex flex-wrap p-2";
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
        RoleAnalysisSinglePartitionAnomalyResultTabPopup components = this;
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
//                        Model.of("Sort")) {
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
//                        Model.of("Chart")) {
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

    public @NotNull DetectedAnomalyResultType getAnomalyModelObject() {
        return anomalyModel.getObject();
    }

    public @NotNull DetectedAnomalyStatisticsType getAnomalyModelStatistics() {
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
        DetectedAnomalyResultType object = getAnomalyModelObject();
        DetectedAnomalyStatisticsType statistics = object.getStatistics();

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

        RoleAnalysisOutlierPartitionType partition = getModelObject();
        XMLGregorianCalendar createTimestamp = partition.getCreateTimestamp();
        GregorianCalendar gregorianCalendar = createTimestamp.toGregorianCalendar();
        Date date = gregorianCalendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = sdf.format(date);

        Double overallConfidence = statistics.getConfidence();
        if (overallConfidence == null) {
            overallConfidence = 0.0;
        }
        BigDecimal bd = new BigDecimal(overallConfidence);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        double pointsDensity = bd.doubleValue();

        OutlierHeaderResultPanel components = new OutlierHeaderResultPanel(id,
                getOutlierModelObject().getOid(),
                outlierName,
                translate("Analysis.anomaly.result.panel.title"),
                String.valueOf(pointsDensity), formattedDate);
        components.setOutputMarkupId(true);
        return components;
    }

    private @NotNull IModel<List<WidgetItemModel>> loadDetailsModel() {

        DetectedAnomalyResultType anomalyModelObject = getAnomalyModelObject();
        DetectedAnomalyStatisticsType anomalyModelStatistics = getAnomalyModelStatistics();
        RoleAnalysisPatternAnalysisType patternAnalysis = anomalyModelStatistics.getPatternAnalysis();
        RoleAnalysisOutlierType outlierModelObject = getOutlierModelObject();
        List<ObjectReferenceType> duplicatedRoleAssignment = outlierModelObject.getDuplicatedRoleAssignment();
        String oid = anomalyModelObject.getTargetObjectRef().getOid();
        boolean isDuplicate = false;
        //TODO think about it. The problem may be caused by missing synchronization (out of date)
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
                        return new Label(id, createStringResource("RoleAnalysisOutlierType.widget.patterns"));
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        String localizedKey = "RoleAnalysisOutlierType.widget.patterns.description.single";
                        if (patternAnalysis.getDetectedPatternCount() > 1) {
                            localizedKey = "RoleAnalysisOutlierType.widget.patterns.description.multiple";
                        }
                        return new Label(id, createStringResource(localizedKey, patternAnalysis.getTotalRelations()));
                    }

                    @Override
                    public Component createValueComponent(String id) {
                        Double confidence = patternAnalysis.getConfidence();
                        String title = createStringResource("RoleAnalysisOutlierType.widget.patterns.confidence")
                                .getString();
                        return buildDensityProgressPanel(id, confidence, title);
                    }

                    @Override
                    protected String replaceValueCssClass() {
                        return "pt-3 pb-1";
                    }

                    @Override
                    public Component createFooterComponent(String id) {
                        return new Label(id, createStringResource("RoleAnalysisOutlierType.widget.patterns.footer",
                                patternAnalysis.getDetectedPatternCount()));
                    }
                },

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
                        return buildDensityProgressPanel(id, pointsDensity, title);
                    }

                    @Override
                    protected String replaceValueCssClass() {
                        return "pt-3 pb-1";
                    }

                    @Override
                    public Component createFooterComponent(String id) {
                        AttributeAnalysisType attributeAnalysis = anomalyModelStatistics.getAttributeAnalysis();

                        int attributeAboveThreshold = 0;
                        int threshold = 80;

                        RoleAnalysisAttributeAnalysisResultType userClusterCompare = attributeAnalysis.getUserRoleMembersCompare();
                        if (userClusterCompare != null && userClusterCompare.getAttributeAnalysis() != null) {
                            List<RoleAnalysisAttributeAnalysisType> analysedAttributeAnalysis = userClusterCompare.getAttributeAnalysis();

                            for (RoleAnalysisAttributeAnalysisType attribute : analysedAttributeAnalysis) {
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
                        FrequencyType memberFrequencyContainer = anomalyModelStatistics.getMemberCoverageConfidenceStat();

                        if (memberFrequencyContainer != null && memberFrequencyContainer.getPercentageRatio() != null) {
                            memberCoverageConfidence = memberFrequencyContainer.getPercentageRatio();
                        }

                        //Deprecated
                        memberCoverageConfidence = getMemberCoverageConfidence(memberCoverageConfidence);

                        BigDecimal bd = BigDecimal.valueOf(memberCoverageConfidence);
                        bd = bd.setScale(2, RoundingMode.HALF_UP);
                        double pointsDensity = bd.doubleValue();
                        String title = createStringResource("RoleAnalysisOutlierType.widget.repo.role.popularity.confidence")
                                .getString();
                        return buildDensityProgressPanel(id, pointsDensity, title);
                    }

                    @Deprecated
                    private double getMemberCoverageConfidence(double memberCoverageConfidence) {
                        FrequencyType memberFrequencyContainer = anomalyModelStatistics.getMemberCoverageConfidenceStat();
                        if (memberFrequencyContainer == null || memberFrequencyContainer.getPercentageRatio() == null) {
                            Double deprecatedMemberCoverageConfidence = anomalyModelStatistics.getMemberCoverageConfidence();
                            if (deprecatedMemberCoverageConfidence != null) {
                                memberCoverageConfidence = deprecatedMemberCoverageConfidence;
                            }
                        }
                        return memberCoverageConfidence;
                    }

                    @Override
                    protected String replaceValueCssClass() {
                        return "pt-3 pb-1 mt-auto";
                    }

                },

                new WidgetItemModel(createStringResource(""),
                        Model.of("")) {

                    @Override
                    public Component createTitleComponent(String id) {
                        return new Label(id, createStringResource("RoleAnalysisOutlierType.widget.anomaly.cluster.coverage"));
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        String localizedKey = "RoleAnalysisOutlierType.widget.anomaly.cluster.coverage.description";
                        return new Label(id, createStringResource(localizedKey));
                    }

                    @Override
                    public Component createValueComponent(String id) {
                        Double memberCoverageConfidence = anomalyModelStatistics.getOutlierCoverageConfidence();
                        if (memberCoverageConfidence == null) {
                            memberCoverageConfidence = 0.0;
                        }
                        BigDecimal bd = new BigDecimal(memberCoverageConfidence);
                        bd = bd.setScale(2, RoundingMode.HALF_UP);
                        double pointsDensity = bd.doubleValue();
                        String title = createStringResource("RoleAnalysisOutlierType.widget.anomaly.cluster.coverage.confidence")
                                .getString();
                        return buildDensityProgressPanel(id, pointsDensity, title);
                    }

                    @Override
                    protected String replaceValueCssClass() {
                        return "pt-3 pb-1 mt-auto";
                    }

                },

                new WidgetItemModel(createStringResource(""),
                        Model.of("")) {

                    @Override
                    public Component createTitleComponent(String id) {
                        return new Label(id, createStringResource("RoleAnalysisOutlierType.widget.deviation"));
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        String localizedKey = "RoleAnalysisOutlierType.widget.deviation.description";
                        return new Label(id, createStringResource(localizedKey));
                    }

                    @Override
                    public Component createValueComponent(String id) {
                        Double confidenceDeviation = anomalyModelStatistics.getConfidenceDeviation();
                        if (confidenceDeviation == null) {
                            confidenceDeviation = 0.0;
                        }
                        confidenceDeviation *= 100;
                        BigDecimal bd = new BigDecimal(confidenceDeviation);
                        bd = bd.setScale(2, RoundingMode.HALF_UP);
                        double pointsDensity = bd.doubleValue();
                        String title = createStringResource("RoleAnalysisOutlierType.widget.deviation.confidence")
                                .getString();
                        return buildDensityProgressPanel(id, pointsDensity, title);
                    }

                    @Override
                    protected String replaceValueCssClass() {
                        return "pt-3 pb-1 ";
                    }

                }

        );

        return Model.ofList(detailsModel);
    }

}
