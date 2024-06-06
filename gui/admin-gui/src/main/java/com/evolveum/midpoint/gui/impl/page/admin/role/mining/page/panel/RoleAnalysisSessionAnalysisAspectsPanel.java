/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_DETECTED_PATER_ID;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_TABLE_SETTING;

import java.io.Serial;
import java.util.List;

import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.model.InfoBoxModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.*;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

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

        List<PrismObject<RoleAnalysisClusterType>> sessionClusters = roleAnalysisService.searchSessionClusters(session, task, result);

        List<DetectedPattern> topSessionPattern = roleAnalysisService.getTopSessionPattern(session, task, result, true);

        if (topSessionPattern != null && sessionStatistic != null && !topSessionPattern.isEmpty()) {
            DetectedPattern pattern = topSessionPattern.get(0);
            RepeatingView headerItems = new RepeatingView(ID_HEADER_ITEMS);
            headerItems.setOutputMarkupId(true);
            container.add(headerItems);
            RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
            RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();
            initHeaderPanel(headerItems, sessionClusters, processMode);

            Label cardTitle = new Label(ID_CARD_TITLE, "Top session pattern");
            cardTitle.setOutputMarkupId(true);
            container.add(cardTitle);

            AjaxCompositedIconSubmitButton components = buildExploreButton(pattern);
            container.add(components);

            RoleAnalysisDetectedPatternDetails statisticsPanel = new RoleAnalysisDetectedPatternDetails(ID_PANEL,
                    Model.of(pattern)) {

                @Contract(pure = true)
                @Override
                protected @NotNull String getCssClassForCardContainer() {
                    return "m-0 border-0";
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

            RoleAnalysisItemPanel roleAnalysisInfoPanel = new RoleAnalysisItemPanel(ID_PATTERNS, Model.of("Recent detected")) {
                @Override
                public void addItem(RepeatingView repeatingView) {
                    PageBase pageBase = RoleAnalysisSessionAnalysisAspectsPanel.this.getPageBase();
                    RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
                    Task task = pageBase.createSimpleTask("loadRoleAnalysisInfo");
                    OperationResult result = task.getResult();
                    List<DetectedPattern> topPatters = roleAnalysisService.getTopSessionPattern(session, task, result, false);
                    for (DetectedPattern pattern : topPatters) {
                        double reductionFactorConfidence = pattern.getMetric();
                        String formattedReductionFactorConfidence = String.format("%.0f", reductionFactorConfidence);
                        double itemsConfidence = pattern.getItemsConfidence();
                        String formattedItemConfidence = String.format("%.1f", itemsConfidence);
                        String label = "Detected a potential reduction of " +
                                formattedReductionFactorConfidence +
                                "x relationships with a confidence of  " +
                                formattedItemConfidence + "%";
                        repeatingView.add(new RoleAnalysisInfoItem(repeatingView.newChildId(), Model.of(label)) {

                            @Override
                            protected String getIconContainerCssClass() {
                                return "btn btn-outline-dark";
                            }

                            @Override
                            protected void addDescriptionComponents() {
                                appendText("Detected a potential reduction from ");
                                appendIcon("fe fe-assignment", "color: red;");
                                appendText(" " + formattedReductionFactorConfidence + " assignments, ");
                                appendText("with a attributes confidence of");
                                appendIcon("fa fa-leaf", "color: green");
                                appendText(" " + formattedItemConfidence + "%.");
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
                    return " overflow-auto";
                }

                @Override
                public String getCardBodyStyle() {
                    return " height:41vh;";
                }
            };
            roleAnalysisInfoPanel.setOutputMarkupId(true);
            add(roleAnalysisInfoPanel);

            container.add(roleAnalysisInfoPanel);

        } else {
            Label label = new Label(ID_PANEL, "No data available");
            label.setOutputMarkupId(true);
            container.add(label);

            WebMarkupContainer headerItems = new WebMarkupContainer(ID_HEADER_ITEMS);
            headerItems.setOutputMarkupId(true);
            container.add(headerItems);

            Label cardTitle = new Label(ID_CARD_TITLE, "No data available");
            cardTitle.setOutputMarkupId(true);
            headerItems.add(cardTitle);

            WebMarkupContainer exploreButton = new WebMarkupContainer(ID_EXPLORE_PATTERN_BUTTON);
            exploreButton.setOutputMarkupId(true);
            container.add(exploreButton);

            WebMarkupContainer panel = new WebMarkupContainer(ID_PATTERNS);
            panel.setOutputMarkupId(true);
            container.add(panel);
        }
    }

    private void initHeaderPanel(RepeatingView headerItems, List<PrismObject<RoleAnalysisClusterType>> sessionClusters, RoleAnalysisProcessModeType processMode) {

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

        RoleAnalysisInfoBox infoBoxReductionLabel = new RoleAnalysisInfoBox(headerItems.newChildId(), Model.of(infoBoxReduction)) {
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

        RoleAnalysisInfoBox outliersLabel = new RoleAnalysisInfoBox(headerItems.newChildId(), Model.of(infoBoxOutliers)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-primary";
            }
        };
        outliersLabel.add(AttributeModifier.replace("class", "col-md-6"));
        outliersLabel.setOutputMarkupId(true);
        headerItems.add(outliersLabel);

        InfoBoxModel infoBoxResolvedPattern = new InfoBoxModel(GuiStyleConstants.CLASS_DETECTED_PATTERN_ICON + " text-white",
                "Resolved pattern",
                String.valueOf(resolvedPatternCount),
                100,
                "Number of resolved patterns");

        RoleAnalysisInfoBox resolvedPatternLabel = new RoleAnalysisInfoBox(headerItems.newChildId(), Model.of(infoBoxResolvedPattern)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-primary";
            }
        };
        resolvedPatternLabel.add(AttributeModifier.replace("class", "col-md-6"));
        resolvedPatternLabel.setOutputMarkupId(true);
        headerItems.add(resolvedPatternLabel);

        InfoBoxModel infoBoxCandidateRoles = new InfoBoxModel(GuiStyleConstants.CLASS_CANDIDATE_ROLE_ICON + " text-white",
                "Candidate roles",
                String.valueOf(candidateRolesCount),
                100,
                "Number of candidate roles");

        RoleAnalysisInfoBox candidateRolesLabel = new RoleAnalysisInfoBox(headerItems.newChildId(), Model.of(infoBoxCandidateRoles)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-primary";
            }
        };
        candidateRolesLabel.add(AttributeModifier.replace("class", "col-md-6"));
        candidateRolesLabel.setOutputMarkupId(true);
        headerItems.add(candidateRolesLabel);
    }

    private @NotNull AjaxCompositedIconSubmitButton buildExploreButton(DetectedPattern pattern) {
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
}

