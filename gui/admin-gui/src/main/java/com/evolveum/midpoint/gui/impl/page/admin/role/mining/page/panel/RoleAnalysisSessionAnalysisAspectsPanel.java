/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel;

import static com.evolveum.midpoint.common.mining.utils.ExtractPatternUtils.transformDefaultPattern;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_DETECTED_PATER_ID;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_TABLE_SETTING;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel.generateRoleOutlierResultModel;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel.generateUserOutlierResultModel;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierHeaderResultPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierItemResultPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierResultPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.model.InfoBoxModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.*;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "sessionOverView", defaultContainerPath = "empty")
@PanelInstance(identifier = "sessionOverView",
        applicableForType = RoleAnalysisSessionType.class,
        defaultPanel = true,
        display = @PanelDisplay(
                label = "RoleAnalysis.overview.panel",
                icon = GuiStyleConstants.CLASS_LINE_CHART_ICON,
                order = 10))
public class RoleAnalysisSessionAnalysisAspectsPanel extends AbstractObjectMainPanel<RoleAnalysisSessionType, ObjectDetailsModels<RoleAnalysisSessionType>> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_HEADER_ITEMS = "header-items";
    private static final String ID_PANEL = "panelId";
    private static final String ID_CARD_TITLE = "card-title";
    private static final String ID_EXPLORE_PATTERN_BUTTON = "explore-pattern-button";
    private static final String ID_PATTERNS = "patterns";

    public RoleAnalysisSessionAnalysisAspectsPanel(String id, ObjectDetailsModels<RoleAnalysisSessionType> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    protected void initLayout() {
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        ObjectDetailsModels<RoleAnalysisSessionType> objectDetailsModels = getObjectDetailsModels();
        RoleAnalysisSessionType session = objectDetailsModels.getObjectType();
        RoleAnalysisSessionStatisticType sessionStatistic = session.getSessionStatistic();

        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
        Task task = getPageBase().createSimpleTask("Get top session pattern");
        OperationResult result = task.getResult();

        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        RoleAnalysisCategoryType analysisCategory = analysisOption.getAnalysisCategory();

        if (RoleAnalysisCategoryType.OUTLIERS.equals(analysisCategory)) {
            initOutlierPart(roleAnalysisService, session, task, result, sessionStatistic, container);
            initInfoOutlierPanel(container);
        } else {
            initRoleMiningPart(roleAnalysisService, session, task, result, sessionStatistic, container);
            initInfoPatternPanel(container);
        }

    }

    private void initInfoPatternPanel(@NotNull WebMarkupContainer container) {
        RoleAnalysisItemPanel roleAnalysisInfoPatternPanel = new RoleAnalysisItemPanel(ID_PATTERNS,
                Model.of("Top role suggestions for session")) {
            @Override
            protected void addItem(RepeatingView repeatingView) {
                List<DetectedPattern> topPatters = getTopPatterns(getObjectDetailsModels().getObjectType());
                for (int i = 0; i < topPatters.size(); i++) {
                    DetectedPattern pattern = topPatters.get(i);
                    double reductionFactorConfidence = pattern.getMetric();
                    String formattedReductionFactorConfidence = String.format("%.0f", reductionFactorConfidence);
                    double itemsConfidence = pattern.getItemsConfidence();
                    String formattedItemConfidence = String.format("%.1f", itemsConfidence);
                    String label = "Detected a potential reduction of " +
                            formattedReductionFactorConfidence +
                            "x relationships with a confidence of  " +
                            formattedItemConfidence + "%";
                    int finalI = i;
                    repeatingView.add(new RoleAnalysisInfoItem(repeatingView.newChildId(), Model.of(label)) {

                        @Override
                        protected String getIconBoxText() {
//                            return "#" + (finalI + 1);
                            return null;
                        }

                        @Override
                        protected String getIconBoxIconStyle() {
                            return super.getIconBoxIconStyle();
                        }

                        @Override
                        protected String getIconContainerCssClass() {
                            return "btn btn-outline-dark";
                        }

                        @Override
                        protected void addDescriptionComponents() {
                            WebMarkupContainer container = new WebMarkupContainer(getRepeatedView().newChildId());
                            container.add(AttributeAppender.append("class", "d-flex"));
                            appendComponent(container);
                            appendText(" Involves ");
                            appendIcon("fe fe-assignment", "color: red;");
                            appendText(" " + formattedReductionFactorConfidence + " relations ");
                            appendText("with ");
                            appendIcon("fa fa-leaf", "color: green");
                            appendText(" " + formattedItemConfidence + "% confidence.");
                        }

                        @Override
                        protected IModel<String> getDescriptionModel() {
                            String description = "A potential reduction has been detected. The reduction involves " +
                                    formattedReductionFactorConfidence + " assignments and is associated with "
                                    + "an attribute confidence of " +
                                    formattedItemConfidence + "%.";
                            return Model.of(description);
                        }

                        @Override
                        protected IModel<String> getLinkModel() {
                            IModel<String> linkModel = super.getLinkModel();
                            return Model.of(linkModel.getObject() + " role suggestion #" + (finalI + 1));
                        }

                        @Override
                        protected void onClickLinkPerform(AjaxRequestTarget target) {
                            PageParameters parameters = new PageParameters();
                            String clusterOid = pattern.getClusterRef().getOid();
                            parameters.add(OnePageParameterEncoder.PARAMETER, clusterOid);
                            parameters.add("panelId", "clusterDetails");
                            parameters.add(PARAM_DETECTED_PATER_ID, pattern.getId());
                            StringValue fullTableSetting = getPageBase().getPageParameters().get(PARAM_TABLE_SETTING);
                            if (fullTableSetting != null && fullTableSetting.toString() != null) {
                                parameters.add(PARAM_TABLE_SETTING, fullTableSetting.toString());
                            }

                            Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                                    .getObjectDetailsPage(RoleAnalysisClusterType.class);
                            getPageBase().navigateToNext(detailsPageClass, parameters);
                        }

                        @Override
                        protected void onClickIconPerform(AjaxRequestTarget target) {
                            RoleAnalysisDetectedPatternDetailsPopup component = new RoleAnalysisDetectedPatternDetailsPopup(
                                    ((PageBase) getPage()).getMainPopupBodyId(),
                                    Model.of(pattern));
                            ((PageBase) getPage()).showMainPopup(component, target);
                        }
                    });
                }
            }

            @Override
            public String getCardBodyCssClass() {
                return " overflow-auto ";
            }

            @Override
            public String replaceCardCssClass() {
                return "card p-0";
            }

            @Override
            public String getCardBodyStyle() {
                return " height:58vh;";
            }

            @Override
            public String replaceBtnToolCssClass() {
                return " position-relative  ml-auto btn btn-primary btn-sm";
            }
        };
        roleAnalysisInfoPatternPanel.setOutputMarkupId(true);
        container.add(roleAnalysisInfoPatternPanel);
    }

    private void initInfoOutlierPanel(WebMarkupContainer container) {
        RoleAnalysisItemPanel roleAnalysisInfoPatternPanel = new RoleAnalysisItemPanel(ID_PATTERNS,
                Model.of("Discovered session outliers")) {
            @Override
            protected void addItem(RepeatingView repeatingView) {
                PageBase pageBase = RoleAnalysisSessionAnalysisAspectsPanel.this.getPageBase();
                ModelService modelService = pageBase.getModelService();
                Task task = pageBase.createSimpleTask("loadRoleAnalysisInfo");
                RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
                OperationResult result = task.getResult();
                SearchResultList<PrismObject<RoleAnalysisOutlierType>> searchResultList;
                try {
                    searchResultList = modelService
                            .searchObjects(RoleAnalysisOutlierType.class, null, null, task, result);
                } catch (SchemaException | ObjectNotFoundException | SecurityViolationException |
                        CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
                    throw new RuntimeException(e);
                }

                if (searchResultList == null || searchResultList.isEmpty()) {
                    return;
                }

                for (int i = 0; i < searchResultList.size(); i++) {
                    PrismObject<RoleAnalysisOutlierType> outlierTypePrismObject = searchResultList.get(i);
                    RoleAnalysisOutlierType outlierObject = outlierTypePrismObject.asObjectable();
                    List<RoleAnalysisOutlierDescriptionType> outlierStatResult = outlierObject.getResult();
                    Double clusterConfidence = outlierObject.getClusterConfidence();
                    String formattedConfidence = String.format("%.2f", clusterConfidence);
                    String label;

                    ObjectReferenceType targetClusterRef = outlierObject.getTargetClusterRef();
                    PrismObject<RoleAnalysisClusterType> prismCluster = roleAnalysisService
                            .getClusterTypeObject(targetClusterRef.getOid(), task, result);
                    String clusterName = "unknown";
                    if (prismCluster != null && prismCluster.getName() != null) {
                        clusterName = prismCluster.getName().getOrig();
                    }

                    if (outlierStatResult.size() > 1) {
                        label = outlierStatResult.size() + " anomalies "
                                + "with confidence of " + formattedConfidence + "% (" + clusterName.toLowerCase() + ").";
                    } else {
                        label = "1 anomalies with confidence of " + formattedConfidence
                                + "% (" + clusterName.toLowerCase() + ").";
                    }

                    int finalI = i;
                    String finalLabel = label;
                    repeatingView.add(new RoleAnalysisInfoItem(repeatingView.newChildId(), Model.of(finalLabel)) {

                        @Override
                        protected String getIconBoxText() {
//                            return "#" + (finalI + 1);
                            return null;
                        }

                        @Override
                        protected String getIconClass() {
                            return "fa-2x " + GuiStyleConstants.CLASS_OUTLIER_ICON;
                        }

                        @Override
                        protected String getIconBoxIconStyle() {
                            return super.getIconBoxIconStyle();
                        }

                        @Override
                        protected String getIconContainerCssClass() {
                            return "btn btn-outline-dark";
                        }

                        @Override
                        protected void addDescriptionComponents() {
                            appendText(finalLabel);
                        }

                        @Override
                        protected IModel<String> getDescriptionModel() {
                            return Model.of(finalLabel);
                        }

                        @Override
                        protected IModel<String> getLinkModel() {
                            IModel<String> linkModel = super.getLinkModel();
                            return Model.of(linkModel.getObject() + " outlier #" + (finalI + 1));
                        }

                        @Override
                        protected void onClickLinkPerform(AjaxRequestTarget target) {
                            PageParameters parameters = new PageParameters();
                            String outlierOid = outlierObject.getOid();
                            parameters.add(OnePageParameterEncoder.PARAMETER, outlierOid);
                            StringValue fullTableSetting = getPageBase().getPageParameters().get(PARAM_TABLE_SETTING);
                            if (fullTableSetting != null && fullTableSetting.toString() != null) {
                                parameters.add(PARAM_TABLE_SETTING, fullTableSetting.toString());
                            }

                            Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                                    .getObjectDetailsPage(RoleAnalysisOutlierType.class);
                            getPageBase().navigateToNext(detailsPageClass, parameters);

                        }

                        @Override
                        protected void onClickIconPerform(AjaxRequestTarget target) {
                            OutlierObjectModel outlierObjectModel;

                            PageBase pageBase = getPageBase();
                            RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
                            Task task = pageBase.createSimpleTask("loadOutlierDetails");
                            ObjectReferenceType targetSessionRef = outlierObject.getTargetSessionRef();
                            PrismObject<RoleAnalysisSessionType> sessionTypeObject = roleAnalysisService
                                    .getSessionTypeObject(targetSessionRef.getOid(), task, task.getResult());
                            if (sessionTypeObject == null) {
                                return;
                            }
                            RoleAnalysisSessionType sessionType = sessionTypeObject.asObjectable();
                            RoleAnalysisProcessModeType processMode = sessionType.getAnalysisOption().getProcessMode();

                            ObjectReferenceType targetClusterRef = outlierObject.getTargetClusterRef();
                            PrismObject<RoleAnalysisClusterType> clusterTypeObject = roleAnalysisService
                                    .getClusterTypeObject(targetClusterRef.getOid(), task, task.getResult());
                            if (clusterTypeObject == null) {
                                return;
                            }
                            RoleAnalysisClusterType cluster = clusterTypeObject.asObjectable();
                            if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
                                outlierObjectModel = generateUserOutlierResultModel(
                                        roleAnalysisService, outlierObject, task, task.getResult(), cluster);
                            } else {
                                outlierObjectModel = generateRoleOutlierResultModel(
                                        roleAnalysisService, outlierObject, task, task.getResult(), cluster);
                            }

                            if (outlierObjectModel == null) {
                                return;
                            }
                            String outlierName = outlierObjectModel.getOutlierName();
                            double outlierConfidence = outlierObjectModel.getOutlierConfidence();
                            String outlierDescription = outlierObjectModel.getOutlierDescription();
                            String timeCreated = outlierObjectModel.getTimeCreated();

                            OutlierResultPanel detailsPanel = new OutlierResultPanel(
                                    ((PageBase) getPage()).getMainPopupBodyId(),
                                    Model.of("Outlier details")) {

                                @Override
                                public String getCardCssClass() {
                                    return "";
                                }

                                @Override
                                public Component getCardHeaderBody(String componentId) {
                                    OutlierHeaderResultPanel components = new OutlierHeaderResultPanel(componentId, outlierName,
                                            outlierDescription, String.valueOf(outlierConfidence), timeCreated);
                                    components.setOutputMarkupId(true);
                                    return components;
                                }

                                @Override
                                public Component getCardBodyComponent(String componentId) {
                                    //TODO just for testing
                                    RepeatingView cardBodyComponent = (RepeatingView) super.getCardBodyComponent(componentId);
                                    outlierObjectModel.getOutlierItemModels()
                                            .forEach(outlierItemModel
                                                    -> cardBodyComponent.add(
                                                    new OutlierItemResultPanel(cardBodyComponent.newChildId(), outlierItemModel)));
                                    return cardBodyComponent;
                                }

                                @Override
                                public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                                    super.onClose(ajaxRequestTarget);
                                }

                            };
                            ((PageBase) getPage()).showMainPopup(detailsPanel, target);
                        }
                    });
                }
            }

            @Override
            public String getCardBodyCssClass() {
                return " overflow-auto ";
            }

            @Override
            public String replaceCardCssClass() {
                return "card p-0";
            }

            @Override
            public String getCardBodyStyle() {
                return " height:104vh;";
            }

            @Override
            public String replaceBtnToolCssClass() {
                return " position-relative  ml-auto btn btn-primary btn-sm";
            }
        };
        roleAnalysisInfoPatternPanel.setOutputMarkupId(true);
        container.add(roleAnalysisInfoPatternPanel);
    }

    private void initRoleMiningPart(@NotNull RoleAnalysisService roleAnalysisService,
            RoleAnalysisSessionType session,
            Task task,
            OperationResult result,
            RoleAnalysisSessionStatisticType sessionStatistic,
            WebMarkupContainer container) {
        List<PrismObject<RoleAnalysisClusterType>> sessionClusters = roleAnalysisService.searchSessionClusters(
                session, task, result);

        List<DetectedPattern> topSessionPattern = roleAnalysisService.getTopSessionPattern(session, task, result, true);

        if (topSessionPattern != null && sessionStatistic != null && !topSessionPattern.isEmpty()) {
            DetectedPattern pattern = topSessionPattern.get(0);
            RepeatingView headerItems = new RepeatingView(ID_HEADER_ITEMS);
            headerItems.setOutputMarkupId(true);
            container.add(headerItems);
            RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
            RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();

            initRoleMiningHeaders(headerItems, sessionClusters, processMode);

            emptyPanel(ID_CARD_TITLE, "Top suggested role", container);

            AjaxCompositedIconSubmitButton components = buildExplorePatternButton(pattern);
            container.add(components);

            RoleAnalysisDetectedPatternDetails statisticsPanel = new RoleAnalysisDetectedPatternDetails(ID_PANEL,
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
            container.add(statisticsPanel);

        } else {
            emptyPanel(ID_PANEL, "No data available", container);

            WebMarkupContainer headerItems = new WebMarkupContainer(ID_HEADER_ITEMS);
            headerItems.setOutputMarkupId(true);
            container.add(headerItems);

            emptyPanel(ID_CARD_TITLE, "No data available", container);

            WebMarkupContainer exploreButton = new WebMarkupContainer(ID_EXPLORE_PATTERN_BUTTON);
            exploreButton.setOutputMarkupId(true);
            container.add(exploreButton);
        }
    }

    private void initOutlierPart(@NotNull RoleAnalysisService roleAnalysisService,
            RoleAnalysisSessionType session,
            Task task,
            OperationResult result,
            RoleAnalysisSessionStatisticType sessionStatistic,
            WebMarkupContainer container) {
        List<PrismObject<RoleAnalysisClusterType>> sessionClusters = roleAnalysisService.searchSessionClusters(
                session, task, result);

        List<DetectedPattern> topSessionPattern = roleAnalysisService.getTopSessionPattern(session, task, result, true);

        if (topSessionPattern != null && sessionStatistic != null) {
            RepeatingView headerItems = new RepeatingView(ID_HEADER_ITEMS);
            headerItems.setOutputMarkupId(true);
            container.add(headerItems);
            RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
            RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();

            initOutliersAnalysisHeaders(headerItems, sessionClusters, processMode);

            emptyPanel(ID_CARD_TITLE, "Top session outlier", container);

            AjaxCompositedIconSubmitButton components = buildExplorePatternOutlier(topOutlier);
            container.add(components);

            initOutlierPanel(container);

        } else {
            emptyPanel(ID_PANEL, "No data available", container);

            WebMarkupContainer headerItems = new WebMarkupContainer(ID_HEADER_ITEMS);
            headerItems.setOutputMarkupId(true);
            container.add(headerItems);

            emptyPanel(ID_CARD_TITLE, "No data available", container);

            WebMarkupContainer exploreButton = new WebMarkupContainer(ID_EXPLORE_PATTERN_BUTTON);
            exploreButton.setOutputMarkupId(true);
            container.add(exploreButton);
        }
    }

    private void initRoleMiningHeaders(RepeatingView headerItems,
            List<PrismObject<RoleAnalysisClusterType>> sessionClusters,
            RoleAnalysisProcessModeType processMode) {

        if (sessionClusters == null) {
            return;
        }

        int resolvedPatternCount = 0;
        int candidateRolesCount = 0;
        int totalReduction = 0;
        int outliers = 0;

        for (PrismObject<RoleAnalysisClusterType> prismCluster : sessionClusters) {
            RoleAnalysisClusterType cluster = prismCluster.asObjectable();
            AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();

            RoleAnalysisClusterCategory category = cluster.getCategory();
            if (category == RoleAnalysisClusterCategory.OUTLIERS) {
                if (processMode == RoleAnalysisProcessModeType.ROLE) {
                    outliers += clusterStatistics.getRolesCount();
                } else if (processMode == RoleAnalysisProcessModeType.USER) {
                    outliers += clusterStatistics.getUsersCount();
                }
            }

            List<ObjectReferenceType> resolvedPattern = cluster.getResolvedPattern();
            if (resolvedPattern != null) {
                resolvedPatternCount += resolvedPattern.size();
            }

            List<RoleAnalysisCandidateRoleType> candidateRoles = cluster.getCandidateRoles();
            if (candidateRoles != null) {
                candidateRolesCount += candidateRoles.size();
            }

            totalReduction += clusterStatistics.getDetectedReductionMetric();

        }

        InfoBoxModel infoBoxReduction = new InfoBoxModel(GuiStyleConstants.ARROW_LONG_DOWN + " text-white",
                "Reduction",
                String.valueOf(totalReduction),
                100,
                "Possible overall reduction");

        RoleAnalysisInfoBox infoBoxReductionLabel = new RoleAnalysisInfoBox(
                headerItems.newChildId(), Model.of(infoBoxReduction)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-primary";
            }
        };
        infoBoxReductionLabel.add(AttributeModifier.replace("class", "col-md-6"));
        infoBoxReductionLabel.setOutputMarkupId(true);
        headerItems.add(infoBoxReductionLabel);

        InfoBoxModel infoBoxOutliers = new InfoBoxModel(GuiStyleConstants.CLASS_OUTLIER_ICON + " text-white",
                "Outliers",
                String.valueOf(outliers),
                100,
                "Number of outliers");

        RoleAnalysisInfoBox outliersLabel = new RoleAnalysisInfoBox(
                headerItems.newChildId(), Model.of(infoBoxOutliers)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-primary";
            }
        };
        outliersLabel.add(AttributeModifier.replace("class", "col-md-6"));
        outliersLabel.setOutputMarkupId(true);
        headerItems.add(outliersLabel);

        InfoBoxModel infoBoxResolvedPattern = new InfoBoxModel(
                GuiStyleConstants.CLASS_DETECTED_PATTERN_ICON + " text-white",
                "Resolved suggestion",
                String.valueOf(resolvedPatternCount),
                100,
                "Number of resolved suggestion roles");

        RoleAnalysisInfoBox resolvedPatternLabel = new RoleAnalysisInfoBox(
                headerItems.newChildId(), Model.of(infoBoxResolvedPattern)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-primary";
            }
        };
        resolvedPatternLabel.add(AttributeModifier.replace("class", "col-md-6"));
        resolvedPatternLabel.setOutputMarkupId(true);
        headerItems.add(resolvedPatternLabel);

        InfoBoxModel infoBoxCandidateRoles = new InfoBoxModel(
                GuiStyleConstants.CLASS_CANDIDATE_ROLE_ICON + " text-white",
                "Candidate roles",
                String.valueOf(candidateRolesCount),
                100,
                "Number of candidate roles");

        RoleAnalysisInfoBox candidateRolesLabel = new RoleAnalysisInfoBox(
                headerItems.newChildId(), Model.of(infoBoxCandidateRoles)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-primary";
            }
        };
        candidateRolesLabel.add(AttributeModifier.replace("class", "col-md-6"));
        candidateRolesLabel.setOutputMarkupId(true);
        headerItems.add(candidateRolesLabel);
    }

    int userOutlierCount = 0;
    int assignmentAnomalyCount = 0;

    transient RoleAnalysisOutlierType topOutlier = null;
    int topConfidence = 0;

    private void initOutliersAnalysisHeaders(
            @NotNull RepeatingView headerItems,
            List<PrismObject<RoleAnalysisClusterType>> sessionClusters,
            @NotNull RoleAnalysisProcessModeType processMode) {

        if (sessionClusters == null) {
            return;
        }

        int resolvedOutliers = 0;

        int clusterOtliers = 0;

        for (PrismObject<RoleAnalysisClusterType> prismCluster : sessionClusters) {
            RoleAnalysisClusterType cluster = prismCluster.asObjectable();
            AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();

            RoleAnalysisClusterCategory category = cluster.getCategory();
            if (category == RoleAnalysisClusterCategory.OUTLIERS) {
                if (processMode == RoleAnalysisProcessModeType.ROLE) {
                    clusterOtliers += clusterStatistics.getRolesCount();
                } else if (processMode == RoleAnalysisProcessModeType.USER) {
                    clusterOtliers += clusterStatistics.getUsersCount();
                }
            }
        }

        PageBase pageBase = getPageBase();
        ModelService modelService = pageBase.getModelService();

        Task task = pageBase.createSimpleTask("Search outlier objects");
        ResultHandler<RoleAnalysisOutlierType> handler = (object, parentResult) -> {
            userOutlierCount++;
            RoleAnalysisOutlierType outlier = object.asObjectable();
            List<RoleAnalysisOutlierDescriptionType> result = outlier.getResult();
            if (result != null) {
                assignmentAnomalyCount += result.size();

                Double clusterConfidence = outlier.getClusterConfidence();
                if (clusterConfidence != null && clusterConfidence > topConfidence) {
                    topConfidence = clusterConfidence.intValue();
                    topOutlier = outlier;
                }

            }
            return true;
        };

        try {
            modelService.searchObjectsIterative(RoleAnalysisOutlierType.class, null, handler, null,
                    task, task.getResult());
        } catch (SchemaException | ObjectNotFoundException | CommunicationException | ConfigurationException |
                SecurityViolationException | ExpressionEvaluationException e) {
            throw new RuntimeException(e);
        }

        InfoBoxModel infoBoxReduction = new InfoBoxModel(GuiStyleConstants.ARROW_LONG_DOWN + " text-white",
                "User outliers",
                String.valueOf(userOutlierCount),
                100,
                "Number of user outliers");

        RoleAnalysisInfoBox infoBoxReductionLabel = new RoleAnalysisInfoBox(
                headerItems.newChildId(), Model.of(infoBoxReduction)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-primary";
            }
        };
        infoBoxReductionLabel.add(AttributeModifier.replace("class", "col-md-6"));
        infoBoxReductionLabel.setOutputMarkupId(true);
        headerItems.add(infoBoxReductionLabel);

        InfoBoxModel infoBoxOutliers = new InfoBoxModel(GuiStyleConstants.CLASS_OUTLIER_ICON + " text-white",
                "Clustering outliers",
                String.valueOf(clusterOtliers),
                100,
                "Number of clustering outliers");

        RoleAnalysisInfoBox outliersLabel = new RoleAnalysisInfoBox(
                headerItems.newChildId(), Model.of(infoBoxOutliers)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-primary";
            }
        };
        outliersLabel.add(AttributeModifier.replace("class", "col-md-6"));
        outliersLabel.setOutputMarkupId(true);
        headerItems.add(outliersLabel);

        InfoBoxModel infoBoxResolvedPattern = new InfoBoxModel(
                GuiStyleConstants.CLASS_DETECTED_PATTERN_ICON + " text-white",
                "Assignment anomalies",
                String.valueOf(assignmentAnomalyCount),
                100,
                "Number of assignment anomalies");

        RoleAnalysisInfoBox resolvedPatternLabel = new RoleAnalysisInfoBox(
                headerItems.newChildId(), Model.of(infoBoxResolvedPattern)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-primary";
            }
        };
        resolvedPatternLabel.add(AttributeModifier.replace("class", "col-md-6"));
        resolvedPatternLabel.setOutputMarkupId(true);
        headerItems.add(resolvedPatternLabel);

        InfoBoxModel infoBoxCandidateRoles = new InfoBoxModel(
                GuiStyleConstants.CLASS_CANDIDATE_ROLE_ICON + " text-white",
                "Resolved outliers",
                String.valueOf(resolvedOutliers),
                100,
                "Number of resolved outliers");

        RoleAnalysisInfoBox candidateRolesLabel = new RoleAnalysisInfoBox(
                headerItems.newChildId(), Model.of(infoBoxCandidateRoles)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-primary";
            }
        };
        candidateRolesLabel.add(AttributeModifier.replace("class", "col-md-6"));
        candidateRolesLabel.setOutputMarkupId(true);
        headerItems.add(candidateRolesLabel);
    }

    private @NotNull AjaxCompositedIconSubmitButton buildExplorePatternButton(DetectedPattern pattern) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                GuiStyleConstants.CLASS_ICON_SEARCH, LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton explorePatternButton = new AjaxCompositedIconSubmitButton(
                ID_EXPLORE_PATTERN_BUTTON,
                iconBuilder.build(),
                createStringResource("RoleAnalysis.explore.button.title")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                explorePatternPerform(pattern);
            }

            @Override
            protected void onError(@NotNull AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        explorePatternButton.titleAsLabel(true);
        explorePatternButton.setOutputMarkupId(true);
        explorePatternButton.add(AttributeAppender.append("class", "ml-auto btn btn-primary btn-sm"));
        explorePatternButton.setOutputMarkupId(true);
        return explorePatternButton;
    }

    private @NotNull AjaxCompositedIconSubmitButton buildExplorePatternOutlier(RoleAnalysisOutlierType outlier) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                GuiStyleConstants.CLASS_ICON_SEARCH, LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton explorePatternButton = new AjaxCompositedIconSubmitButton(
                ID_EXPLORE_PATTERN_BUTTON,
                iconBuilder.build(),
                createStringResource("Explore outlier")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                if (outlier == null) {
                    return;
                }
                PageParameters parameters = new PageParameters();
                String clusterOid = outlier.getOid();
                parameters.add(OnePageParameterEncoder.PARAMETER, clusterOid);

                Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                        .getObjectDetailsPage(RoleAnalysisOutlierType.class);
                getPageBase().navigateToNext(detailsPageClass, parameters);
            }

            @Override
            protected void onError(@NotNull AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        explorePatternButton.titleAsLabel(true);
        explorePatternButton.setOutputMarkupId(true);
        explorePatternButton.add(AttributeAppender.append("class", "ml-auto btn btn-primary btn-sm"));
        explorePatternButton.setOutputMarkupId(true);
        return explorePatternButton;
    }

    private void explorePatternPerform(@NotNull DetectedPattern pattern) {
        PageParameters parameters = new PageParameters();
        String clusterOid = pattern.getClusterRef().getOid();
        parameters.add(OnePageParameterEncoder.PARAMETER, clusterOid);
        parameters.add("panelId", "clusterDetails");
        parameters.add(PARAM_DETECTED_PATER_ID, pattern.getId());
        StringValue fullTableSetting = getPageBase().getPageParameters().get(PARAM_TABLE_SETTING);
        if (fullTableSetting != null && fullTableSetting.toString() != null) {
            parameters.add(PARAM_TABLE_SETTING, fullTableSetting.toString());
        }

        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                .getObjectDetailsPage(RoleAnalysisClusterType.class);
        getPageBase().navigateToNext(detailsPageClass, parameters);
    }

    public void initOutlierPanel(WebMarkupContainer container) {
        OutlierObjectModel outlierObjectModel;

        PageBase pageBase = getPageBase();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        Task task = pageBase.createSimpleTask("loadOutlierDetails");
        RoleAnalysisOutlierType outlierObject = topOutlier;
        if (outlierObject == null) {
            emptyPanel(ID_PANEL, "No data available", container);
            return;
        }
        ObjectReferenceType targetSessionRef = outlierObject.getTargetSessionRef();
        PrismObject<RoleAnalysisSessionType> sessionTypeObject = roleAnalysisService
                .getSessionTypeObject(targetSessionRef.getOid(), task, task.getResult());

        if (sessionTypeObject == null) {
            emptyPanel(ID_PANEL, "No data available", container);
            return;
        }
        RoleAnalysisSessionType sessionType = sessionTypeObject.asObjectable();
        RoleAnalysisProcessModeType processMode = sessionType.getAnalysisOption().getProcessMode();

        ObjectReferenceType targetClusterRef = outlierObject.getTargetClusterRef();
        PrismObject<RoleAnalysisClusterType> clusterTypeObject = roleAnalysisService
                .getClusterTypeObject(targetClusterRef.getOid(), task, task.getResult());
        if (clusterTypeObject == null) {
            emptyPanel(ID_PANEL, "No data available", container);
            return;
        }
        RoleAnalysisClusterType cluster = clusterTypeObject.asObjectable();
        if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
            outlierObjectModel = generateUserOutlierResultModel(
                    roleAnalysisService, outlierObject, task, task.getResult(), cluster);
        } else {
            outlierObjectModel = generateRoleOutlierResultModel(
                    roleAnalysisService, outlierObject, task, task.getResult(), cluster);
        }

        if (outlierObjectModel == null) {
            emptyPanel(ID_PANEL, "No data available", container);
            return;
        }

        String outlierName = outlierObjectModel.getOutlierName();
        double outlierConfidence = outlierObjectModel.getOutlierConfidence();
        String outlierDescription = outlierObjectModel.getOutlierDescription();
        String timeCreated = outlierObjectModel.getTimeCreated();

        OutlierResultPanel detailsPanel = new OutlierResultPanel(
                ID_PANEL,
                Model.of("Analyzed members details panel")) {

            @Override
            public String getCardCssClass() {
                return "";
            }

            @Override
            public Component getCardHeaderBody(String componentId) {
                OutlierHeaderResultPanel components = new OutlierHeaderResultPanel(componentId, outlierName,
                        outlierDescription, String.valueOf(outlierConfidence), timeCreated);
                components.setOutputMarkupId(true);
                return components;
            }

            @Override
            public Component getCardBodyComponent(String componentId) {
                //TODO just for testing
                RepeatingView cardBodyComponent = (RepeatingView) super.getCardBodyComponent(componentId);
                outlierObjectModel.getOutlierItemModels()
                        .forEach(outlierItemModel
                                -> cardBodyComponent.add(
                                new OutlierItemResultPanel(cardBodyComponent.newChildId(), outlierItemModel)));
                return cardBodyComponent;
            }

            @Override
            public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                super.onClose(ajaxRequestTarget);
            }

        };
        detailsPanel.setOutputMarkupId(true);
        container.add(detailsPanel);

    }

    private static void emptyPanel(String idPanel, String No_data_available, WebMarkupContainer container) {
        Label label = new Label(idPanel, No_data_available);
        label.setOutputMarkupId(true);
        container.add(label);
    }

    private @NotNull List<DetectedPattern> getTopPatterns(RoleAnalysisSessionType session) {
        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();

        Task task = getPageBase().createSimpleTask("getTopPatterns");
        OperationResult result = task.getResult();
        List<PrismObject<RoleAnalysisClusterType>> prismObjects = roleAnalysisService.searchSessionClusters(session, task, result);

        List<DetectedPattern> topDetectedPatterns = new ArrayList<>();
        for (PrismObject<RoleAnalysisClusterType> prismObject : prismObjects) {
            List<DetectedPattern> detectedPatterns = transformDefaultPattern(prismObject.asObjectable());

            double maxOverallConfidence = 0;
            DetectedPattern topDetectedPattern = null;
            for (DetectedPattern detectedPattern : detectedPatterns) {
                double itemsConfidence = detectedPattern.getItemsConfidence();
                double reductionFactorConfidence = detectedPattern.getReductionFactorConfidence();
                double overallConfidence = itemsConfidence + reductionFactorConfidence;
                if (overallConfidence > maxOverallConfidence) {
                    maxOverallConfidence = overallConfidence;
                    topDetectedPattern = detectedPattern;
                }
            }
            if (topDetectedPattern != null) {
                topDetectedPatterns.add(topDetectedPattern);
            }

        }
        return topDetectedPatterns;
    }
}

