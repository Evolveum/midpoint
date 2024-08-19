/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.aspects;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.RoleAnalysisAspectsWebUtils.getSessionWidgetModelOutliers;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.RoleAnalysisAspectsWebUtils.getSessionWidgetModelPatterns;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_DETECTED_PATER_ID;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_TABLE_SETTING;

import java.io.Serial;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

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
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBarPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.MetricValuePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisPartitionOverviewPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.widgets.component.RoleAnalysisIdentifyWidgetPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.widgets.model.IdentifyWidgetItem;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisDetectedPatternDetails;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.RoleAnalysisDistributionProgressPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.RoleAnalysisOutlierDashboardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.RoleAnalysisValueLabelPanel;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ResultHandler;
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

    int userOutlierCount = 0;
    int assignmentAnomalyCount = 0;
    int topConfidence = 0;

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

        OperationResult result = new OperationResult("loadTopClusterPatterns");
        IModel<List<IdentifyWidgetItem>> modelPatterns = getSessionWidgetModelPatterns(getObjectDetailsModels().getObjectType(),
                result, getPageBase(), 5);

        RoleAnalysisIdentifyWidgetPanel panel = new RoleAnalysisIdentifyWidgetPanel(ID_PATTERNS,
                createStringResource("Pattern.suggestions.title"), modelPatterns) {

            @Override
            protected void onClickFooter(AjaxRequestTarget target) {
                ObjectDetailsModels<RoleAnalysisSessionType> objectDetailsModels = RoleAnalysisSessionAnalysisAspectsPanel.this
                        .getObjectDetailsModels();
                String oid = objectDetailsModels.getObjectType().getOid();
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, oid);
                parameters.add("panelId", "topDetectedPattern");

                Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                        .getObjectDetailsPage(RoleAnalysisSessionType.class);
                getPageBase().navigateToNext(detailsPageClass, parameters);
            }

            @Override
            protected Component getBodyHeaderPanel(String id) {
                WebMarkupContainer panel = new WebMarkupContainer(id);
                panel.setOutputMarkupId(true);
                return panel;
            }

            @Override
            protected boolean isHeaderVisible() {
                return false;
            }

            @Override
            protected @NotNull String getIconCssClass() {
                return GuiStyleConstants.CLASS_DETECTED_PATTERN_ICON;
            }

            @Override
            protected String initDefaultCssClass() {
                return "col-12 pl-0";
            }
        };
        panel.setOutputMarkupId(true);
        container.add(panel);
    }

    private void initRoleMiningPart(@NotNull RoleAnalysisService roleAnalysisService,
            RoleAnalysisSessionType session,
            Task task,
            OperationResult result,
            RoleAnalysisSessionStatisticType sessionStatistic,
            WebMarkupContainer container) {
        List<DetectedPattern> topSessionPattern = roleAnalysisService.getTopSessionPattern(session, task, result, true);

        if (topSessionPattern != null && sessionStatistic != null && !topSessionPattern.isEmpty()) {
            DetectedPattern pattern = topSessionPattern.get(0);
            initMiningPartNew(roleAnalysisService, session, task, result, sessionStatistic, container);

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
                    return "d-flex flex-row p-2 align-items-center";
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

    private void initInfoOutlierPanel(@NotNull WebMarkupContainer container) {
        IModel<List<IdentifyWidgetItem>> modelPatterns = getSessionWidgetModelOutliers(getObjectDetailsModels().getObjectType(),
                getPageBase());
        RoleAnalysisIdentifyWidgetPanel panel = new RoleAnalysisIdentifyWidgetPanel(ID_PATTERNS,
                createStringResource("Outlier.suggestions.title"), modelPatterns) {

            @Override
            protected Component getBodyHeaderPanel(String id) {
                WebMarkupContainer panel = new WebMarkupContainer(id);
                panel.setOutputMarkupId(true);
                return panel;
            }

            @Override
            protected void onClickFooter(AjaxRequestTarget target) {
                ObjectDetailsModels<RoleAnalysisSessionType> objectDetailsModels = RoleAnalysisSessionAnalysisAspectsPanel.this
                        .getObjectDetailsModels();
                String oid = objectDetailsModels.getObjectType().getOid();
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, oid);
                //TODO
                parameters.add("panelId", "topOutlierPanel");

                Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                        .getObjectDetailsPage(RoleAnalysisSessionType.class);
                getPageBase().navigateToNext(detailsPageClass, parameters);
            }

            @Override
            protected boolean isHeaderVisible() {
                return false;
            }

            @Override
            protected @NotNull String getIconCssClass() {
                return GuiStyleConstants.CLASS_ICON_OUTLIER;
            }

            @Override
            protected String initDefaultCssClass() {
                return "col-12 pl-0";
            }
        };
        panel.setOutputMarkupId(true);
        container.add(panel);
    }

    protected void initMiningPartNew(
            @NotNull RoleAnalysisService roleAnalysisService,
            RoleAnalysisSessionType session,
            Task task,
            OperationResult result,
            @NotNull RoleAnalysisSessionStatisticType sessionStatistic,
            @NotNull WebMarkupContainer container) {

        RepeatingView cardBodyComponent = new RepeatingView(ID_HEADER_ITEMS);
        cardBodyComponent.setOutputMarkupId(true);
        container.add(cardBodyComponent);

        List<PrismObject<RoleAnalysisClusterType>> sessionClusters = roleAnalysisService.searchSessionClusters(
                session, task, result);

        Integer processedObjectCount = sessionStatistic.getProcessedObjectCount();
        if (processedObjectCount == null) {
            processedObjectCount = 0;
        }
        int clusterOtliers = 0;

        int resolvedPatternCount = 0;
        int candidateRolesCount = 0;
        int totalReduction = 0;

        RoleAnalysisProcessModeType processMode = session.getAnalysisOption().getProcessMode();
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

        int totalAssignmentRoleToUser = roleAnalysisService.countUserOwnedRoleAssignment(result);
        double totalSystemPercentageReduction = 0;
        if (totalReduction != 0 && totalAssignmentRoleToUser != 0) {
            totalSystemPercentageReduction = ((double) totalReduction / totalAssignmentRoleToUser) * 100;
            BigDecimal bd = new BigDecimal(totalSystemPercentageReduction);
            bd = bd.setScale(2, RoundingMode.HALF_UP);
            totalSystemPercentageReduction = bd.doubleValue();
        }

        int finalResolvedPatternCount = resolvedPatternCount;
        int finalCandidateRolesCount = candidateRolesCount;
        RoleAnalysisOutlierDashboardPanel<?> statusHeader = new RoleAnalysisOutlierDashboardPanel<>(cardBodyComponent.newChildId(),
                createStringResource("RoleAnalysisOutlierAnalysisAspectsPanel.widget.session.status")) {
            @Contract(pure = true)
            @Override
            protected @NotNull String getIconCssClass() {
                return GuiStyleConstants.CLASS_ICON_ASSIGNMENTS;
            }

            @Override
            protected String getContainerCssClass() {
                return super.getContainerCssClass();
            }

            @Override
            protected @NotNull Component getPanelComponent(String id) {
                String status = "New (Untouched)";
                if (finalResolvedPatternCount > 0 || finalCandidateRolesCount > 0) {
                    status = "In Progress";
                }
                IconWithLabel iconWithLabel = new IconWithLabel(id, Model.of(status)) {
                    @Contract(pure = true)
                    @Override
                    protected @NotNull String getIconCssClass() {
                        return "fas fa-sync";
                    }

                    @Override
                    protected String getComponentCssClass() {
                        return super.getComponentCssClass() + " gap-2";
                    }

                    @Override
                    protected String getComponentCssStyle() {
                        return "color: #18a2b8; font-size: 20px;";
                    }
                };

                iconWithLabel.add(AttributeAppender.append("class", "badge p-3 m-4 justify-content-center"));
                iconWithLabel.add(AttributeAppender.append("style", "background-color: #dcf1f4;"));
                return iconWithLabel;
            }

            @Override
            protected Component getFooterComponent(String id) {
                RepeatingView cardBodyComponent = new RepeatingView(id);
                cardBodyComponent.setOutputMarkupId(true);

                RoleAnalysisValueLabelPanel<?> pendingValueLabelPanel = new RoleAnalysisValueLabelPanel<>(cardBodyComponent.newChildId()) {
                    @Contract(pure = true)
                    @Override
                    protected @NotNull Component getTitleComponent(String id) {
                        return new IconWithLabel(id,
                                createStringResource("RoleAnalysis.aspect.overview.page.title.candidate.roles")) {
                            @Override
                            protected String getIconCssClass() {
                                return "fas fa-sync text-muted";
                            }

                            @Override
                            protected String getLabelComponentCssClass() {
                                return "text-muted";
                            }

                            @Override
                            protected String getComponentCssClass() {
                                return super.getComponentCssClass() + " gap-2";
                            }
                        };
                    }

                    @Contract(pure = true)
                    @Override
                    protected @NotNull Component getValueComponent(String id) {
                        Label label = new Label(id, finalCandidateRolesCount);
                        label.add(AttributeAppender.append("class", "text-muted"));
                        return label;
                    }
                };
                pendingValueLabelPanel.setOutputMarkupId(true);
                cardBodyComponent.add(pendingValueLabelPanel);

                RoleAnalysisValueLabelPanel<?> solvedValueLabelPanel = new RoleAnalysisValueLabelPanel<>(cardBodyComponent.newChildId()) {

                    @Contract(pure = true)
                    @Override
                    protected @NotNull Component getTitleComponent(String id) {
                        return new IconWithLabel(id,
                                createStringResource("RoleAnalysis.aspect.overview.page.title.resolved.suggestions")) {
                            @Override
                            protected String getIconCssClass() {
                                return "fas fa-trophy text-muted";
                            }

                            @Override
                            protected String getLabelComponentCssClass() {
                                return "text-muted";
                            }

                            @Override
                            protected String getComponentCssClass() {
                                return super.getComponentCssClass() + " gap-2";
                            }
                        };
                    }

                    @Contract(pure = true)
                    @Override
                    protected @NotNull Component getValueComponent(String id) {
                        Label label = new Label(id, finalResolvedPatternCount);
                        label.setOutputMarkupId(true);
                        label.add(AttributeAppender.append("class", "text-muted"));
                        return label;
                    }

                };
                solvedValueLabelPanel.setOutputMarkupId(true);
                cardBodyComponent.add(solvedValueLabelPanel);
                return cardBodyComponent;
            }
        };
        statusHeader.setOutputMarkupId(true);
        statusHeader.add(AttributeAppender.append("class", "col-6 pl-0"));
        cardBodyComponent.add(statusHeader);

        int clusterInliers = processedObjectCount - clusterOtliers;

        List<ProgressBar> progressBars = new ArrayList<>();

        progressBars.add(new ProgressBar(clusterInliers * 100 / (double) processedObjectCount, ProgressBar.State.INFO));
        progressBars.add(new ProgressBar(clusterOtliers * 100 / (double) processedObjectCount, ProgressBar.State.WARNINIG));

        int finalClusterOtliers = clusterOtliers;

        int finalTotalReduction = totalReduction;
        double finalTotalSystemPercentageReduction = totalSystemPercentageReduction;
        RoleAnalysisOutlierDashboardPanel<?> distributionHeader = new RoleAnalysisOutlierDashboardPanel<>(cardBodyComponent.newChildId(),
                createStringResource("RoleAnalysisOutlierAnalysisAspectsPanel.widget.distribution")) {
            @Contract(pure = true)
            @Override
            protected @NotNull String getIconCssClass() {
                return "fa fa-lock";
            }

            @Override
            protected boolean isFooterVisible() {
                return true;
            }

            @Override
            protected @NotNull Component getPanelComponent(String id) {

                RoleAnalysisDistributionProgressPanel<?> panel = new RoleAnalysisDistributionProgressPanel<>(id) {

                    @Override
                    protected Component getPanelComponent(String id) {
                        ProgressBarPanel components = new ProgressBarPanel(id, new LoadableModel<>() {
                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            protected List<ProgressBar> load() {
                                return progressBars;
                            }
                        });
                        components.setOutputMarkupId(true);
                        components.add(AttributeAppender.append("class", "pt-3 pl-2 pr-2"));
                        return components;
                    }

                    @Contract(pure = true)
                    @Override
                    protected @NotNull String getContainerLegendCssClass() {
                        return "d-flex flex-wrap justify-content-between pt-2 pb-2";
                    }

                    @Override
                    protected @NotNull Component getLegendComponent(String id) {
                        RepeatingView view = new RepeatingView(id);
                        MetricValuePanel resolved = new MetricValuePanel(view.newChildId()) {
                            @Contract("_ -> new")
                            @Override
                            protected @NotNull Component getTitleComponent(String id) {
                                return new IconWithLabel(id,
                                        createStringResource("RoleAnalysis.aspect.overview.page.title.cluster.inliers")) {
                                    @Override
                                    protected String getIconCssClass() {
                                        return "fa fa-circle text-info fa-2xs";
                                    }

                                    @Override
                                    protected String getLabelComponentCssClass() {
                                        return "text-info";
                                    }

                                    @Override
                                    protected String getComponentCssClass() {
                                        return super.getComponentCssClass() + " gap-2";
                                    }
                                };
                            }

                            @Contract("_ -> new")
                            @Override
                            protected @NotNull Component getValueComponent(String id) {
                                Label label = new Label(id, clusterInliers);
                                label.add(AttributeAppender.append("class", "d-flex pl-3 m-0"));
                                label.add(AttributeAppender.append("style", "font-size:20px"));
                                return label;
                            }
                        };
                        resolved.setOutputMarkupId(true);
                        view.add(resolved);

                        MetricValuePanel inProgress = new MetricValuePanel(view.newChildId()) {
                            @Contract("_ -> new")
                            @Override
                            protected @NotNull Component getTitleComponent(String id) {
                                return new IconWithLabel(id,
                                        createStringResource("RoleAnalysis.aspect.overview.page.title.cluster.outliers")) {
                                    @Override
                                    protected String getIconCssClass() {
                                        return "fa fa-circle text-warning fa-2xs";
                                    }

                                    @Override
                                    protected String getLabelComponentCssClass() {
                                        return "text-warning";
                                    }

                                    @Override
                                    protected String getComponentCssClass() {
                                        return super.getComponentCssClass() + " gap-2";
                                    }
                                };
                            }

                            @Contract("_ -> new")
                            @Override
                            protected @NotNull Component getValueComponent(String id) {
                                Label label = new Label(id, finalClusterOtliers);
                                label.add(AttributeAppender.append("class", "d-flex pl-3 m-0"));
                                label.add(AttributeAppender.append("style", "font-size:20px"));
                                return label;
                            }
                        };
                        inProgress.setOutputMarkupId(true);
                        view.add(inProgress);

                        return view;

                    }
                };

                panel.setOutputMarkupId(true);
                panel.add(AttributeAppender.append("class", "col-12"));
                return panel;
            }

            @Override
            protected Component getFooterComponent(String id) {
                RepeatingView cardBodyComponent = new RepeatingView(id);
                cardBodyComponent.setOutputMarkupId(true);

                RoleAnalysisValueLabelPanel<?> anomaliesPanel = new RoleAnalysisValueLabelPanel<>(cardBodyComponent.newChildId()) {
                    @Contract(pure = true)
                    @Override
                    protected @NotNull Component getTitleComponent(String id) {
                        return new IconWithLabel(id,
                                createStringResource("RoleAnalysisOutlierAnalysisAspectsPanel.widget.characteristics.relation.reduced")) {
                            @Override
                            protected String getIconCssClass() {
                                return "fa fa-exclamation-triangle text-muted";
                            }

                            @Override
                            protected String getLabelComponentCssClass() {
                                return "text-muted";
                            }

                            @Override
                            protected String getComponentCssClass() {
                                return super.getComponentCssClass() + " gap-2";
                            }
                        };
                    }

                    @Contract(pure = true)
                    @Override
                    protected @NotNull Component getValueComponent(String id) {
                        Label label = new Label(id, finalTotalReduction);
                        label.setOutputMarkupId(true);
                        label.add(AttributeAppender.append("class", "text-muted"));
                        return label;
                    }
                };
                anomaliesPanel.setOutputMarkupId(true);
                cardBodyComponent.add(anomaliesPanel);

                RoleAnalysisValueLabelPanel<?> partitionPanel = new RoleAnalysisValueLabelPanel<>(cardBodyComponent.newChildId()) {
                    @Contract(pure = true)
                    @Override
                    protected @NotNull Component getTitleComponent(String id) {
                        return new IconWithLabel(id,
                                createStringResource("RoleAnalysisOutlierAnalysisAspectsPanel.widget.characteristics.relation.reduced.system")) {
                            @Override
                            protected String getIconCssClass() {
                                return GuiStyleConstants.CLASS_ROLE_ANALYSIS_SESSION_ICON + " text-muted";
                            }

                            @Override
                            protected String getLabelComponentCssClass() {
                                return "text-muted";
                            }

                            @Override
                            protected String getComponentCssClass() {
                                return super.getComponentCssClass() + " gap-2";
                            }
                        };
                    }

                    @Contract(pure = true)
                    @Override
                    protected @NotNull Component getValueComponent(String id) {
                        Label label = new Label(id, finalTotalSystemPercentageReduction + "%");
                        label.setOutputMarkupId(true);
                        label.add(AttributeAppender.append("class", "text-muted"));
                        return label;
                    }
                };
                partitionPanel.setOutputMarkupId(true);
                cardBodyComponent.add(partitionPanel);
                return cardBodyComponent;
            }
        };

        distributionHeader.add(AttributeAppender.append("class", "col-6 pr-0"));

        distributionHeader.setOutputMarkupId(true);
        cardBodyComponent.add(distributionHeader);
        container.add(cardBodyComponent);
    }

    protected void initOutlierPartNew(
            @NotNull RoleAnalysisService roleAnalysisService,
            RoleAnalysisSessionType session,
            Task task,
            OperationResult result,
            @NotNull RoleAnalysisSessionStatisticType sessionStatistic,
            @NotNull WebMarkupContainer container) {

        RepeatingView cardBodyComponent = new RepeatingView(ID_HEADER_ITEMS);
        cardBodyComponent.setOutputMarkupId(true);
        container.add(cardBodyComponent);
        RoleAnalysisOutlierDashboardPanel<?> statusHeader = new RoleAnalysisOutlierDashboardPanel<>(cardBodyComponent.newChildId(),
                createStringResource("RoleAnalysisOutlierAnalysisAspectsPanel.widget.status")) {
            @Contract(pure = true)
            @Override
            protected @NotNull String getIconCssClass() {
                return GuiStyleConstants.CLASS_ICON_ASSIGNMENTS;
            }

            @Override
            protected String getContainerCssClass() {
                return super.getContainerCssClass();
            }

            @Override
            protected @NotNull Component getPanelComponent(String id) {
                IconWithLabel iconWithLabel = new IconWithLabel(id, Model.of("UNKNOWN (TBD)")) {
                    @Contract(pure = true)
                    @Override
                    protected @NotNull String getIconCssClass() {
                        return "fas fa-sync";
                    }

                    @Override
                    protected String getComponentCssClass() {
                        return super.getComponentCssClass() + " gap-2";
                    }

                    @Override
                    protected String getComponentCssStyle() {
                        return "color: #18a2b8; font-size: 20px;";
                    }
                };

                iconWithLabel.add(AttributeAppender.append("class", "badge p-3 m-4 justify-content-center"));
                iconWithLabel.add(AttributeAppender.append("style", "background-color: #dcf1f4;"));
                return iconWithLabel;
            }

            @Override
            protected Component getFooterComponent(String id) {
                RepeatingView cardBodyComponent = new RepeatingView(id);
                cardBodyComponent.setOutputMarkupId(true);

                RoleAnalysisValueLabelPanel<?> pendingValueLabelPanel = new RoleAnalysisValueLabelPanel<>(cardBodyComponent.newChildId()) {
                    @Contract(pure = true)
                    @Override
                    protected @NotNull Component getTitleComponent(String id) {
                        return new IconWithLabel(id,
                                createStringResource("RoleAnalysis.aspect.overview.page.title.pending.recertifications")) {
                            @Override
                            protected String getIconCssClass() {
                                return "fas fa-sync text-muted";
                            }

                            @Override
                            protected String getLabelComponentCssClass() {
                                return "text-muted";
                            }

                            @Override
                            protected String getComponentCssClass() {
                                return super.getComponentCssClass() + " gap-2";
                            }
                        };
                    }

                    @Contract(pure = true)
                    @Override
                    protected @NotNull Component getValueComponent(String id) {
                        Label label = new Label(id, "0");
                        label.add(AttributeAppender.append("class", "text-muted"));
                        return label;
                    }
                };
                pendingValueLabelPanel.setOutputMarkupId(true);
                cardBodyComponent.add(pendingValueLabelPanel);

                RoleAnalysisValueLabelPanel<?> solvedValueLabelPanel = new RoleAnalysisValueLabelPanel<>(cardBodyComponent.newChildId()) {

                    @Contract(pure = true)
                    @Override
                    protected @NotNull Component getTitleComponent(String id) {
                        return new IconWithLabel(id,
                                createStringResource("RoleAnalysis.aspect.overview.page.title.solved.recertifications")) {
                            @Override
                            protected String getIconCssClass() {
                                return "fas fa-trophy text-muted";
                            }

                            @Override
                            protected String getLabelComponentCssClass() {
                                return "text-muted";
                            }

                            @Override
                            protected String getComponentCssClass() {
                                return super.getComponentCssClass() + " gap-2";
                            }
                        };
                    }

                    @Contract(pure = true)
                    @Override
                    protected @NotNull Component getValueComponent(String id) {
                        Label label = new Label(id, "0");
                        label.setOutputMarkupId(true);
                        label.add(AttributeAppender.append("class", "text-muted"));
                        return label;
                    }

                };
                solvedValueLabelPanel.setOutputMarkupId(true);
                cardBodyComponent.add(solvedValueLabelPanel);
                return cardBodyComponent;
            }
        };
        statusHeader.setOutputMarkupId(true);
        statusHeader.add(AttributeAppender.append("class", "col-6 pl-0"));
        cardBodyComponent.add(statusHeader);

        List<PrismObject<RoleAnalysisClusterType>> sessionClusters = roleAnalysisService.searchSessionClusters(
                session, task, result);

        Integer processedObjectCount = sessionStatistic.getProcessedObjectCount();
        if (processedObjectCount == null) {
            processedObjectCount = 0;
        }
        int clusterOtliers = 0;

        for (PrismObject<RoleAnalysisClusterType> prismCluster : sessionClusters) {
            RoleAnalysisClusterType cluster = prismCluster.asObjectable();
            RoleAnalysisClusterCategory category = cluster.getCategory();
            if (category.equals(RoleAnalysisClusterCategory.OUTLIERS)) {
                AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();
                clusterOtliers += clusterStatistics.getUsersCount();
            }
        }

        int clusterInliers = processedObjectCount - clusterOtliers;

        List<ProgressBar> progressBars = new ArrayList<>();

        progressBars.add(new ProgressBar(clusterInliers * 100 / (double) processedObjectCount, ProgressBar.State.INFO));
        progressBars.add(new ProgressBar(clusterOtliers * 100 / (double) processedObjectCount, ProgressBar.State.WARNINIG));

        int finalClusterOtliers = clusterOtliers;

        int anomaliesCount = countAnomalies(session.getOid());
        RoleAnalysisOutlierDashboardPanel<?> distributionHeader = new RoleAnalysisOutlierDashboardPanel<>(cardBodyComponent.newChildId(),
                createStringResource("RoleAnalysisOutlierAnalysisAspectsPanel.widget.distribution")) {
            @Contract(pure = true)
            @Override
            protected @NotNull String getIconCssClass() {
                return "fa fa-lock";
            }

            @Override
            @NotNull
            public Component getPanelComponent(String id) {

                RoleAnalysisDistributionProgressPanel<?> panel = new RoleAnalysisDistributionProgressPanel<>(id) {

                    @Override
                    protected Component getPanelComponent(String id) {
                        ProgressBarPanel components = new ProgressBarPanel(id, new LoadableModel<>() {
                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            protected List<ProgressBar> load() {
                                return progressBars;
                            }
                        });
                        components.setOutputMarkupId(true);
                        components.add(AttributeAppender.append("class", "pt-3 pl-2 pr-2"));
                        return components;
                    }

                    @Contract(pure = true)
                    @Override
                    protected @NotNull String getContainerLegendCssClass() {
                        return "d-flex flex-wrap justify-content-between pt-2 pb-2";
                    }

                    @Override
                    protected @NotNull Component getLegendComponent(String id) {
                        RepeatingView view = new RepeatingView(id);
                        MetricValuePanel resolved = new MetricValuePanel(view.newChildId()) {
                            @Contract("_ -> new")
                            @Override
                            protected @NotNull Component getTitleComponent(String id) {
                                return new IconWithLabel(id,
                                        createStringResource("RoleAnalysis.aspect.overview.page.title.cluster.inliers")) {
                                    @Override
                                    protected String getIconCssClass() {
                                        return "fa fa-circle text-info fa-2xs";
                                    }

                                    @Override
                                    protected String getLabelComponentCssClass() {
                                        return "text-info";
                                    }

                                    @Override
                                    protected String getComponentCssClass() {
                                        return super.getComponentCssClass() + " gap-2";
                                    }
                                };
                            }

                            @Contract("_ -> new")
                            @Override
                            protected @NotNull Component getValueComponent(String id) {
                                Label label = new Label(id, clusterInliers);
                                label.add(AttributeAppender.append("class", "d-flex pl-3 m-0"));
                                label.add(AttributeAppender.append("style", "font-size:20px"));
                                return label;
                            }
                        };
                        resolved.setOutputMarkupId(true);
                        view.add(resolved);

                        MetricValuePanel inProgress = new MetricValuePanel(view.newChildId()) {
                            @Contract("_ -> new")
                            @Override
                            protected @NotNull Component getTitleComponent(String id) {
                                return new IconWithLabel(id,
                                        createStringResource("RoleAnalysis.aspect.overview.page.title.cluster.outliers")) {
                                    @Override
                                    protected String getIconCssClass() {
                                        return "fa fa-circle text-warning fa-2xs";
                                    }

                                    @Override
                                    protected String getLabelComponentCssClass() {
                                        return "text-warning";
                                    }

                                    @Override
                                    protected String getComponentCssClass() {
                                        return super.getComponentCssClass() + " gap-2";
                                    }
                                };
                            }

                            @Contract("_ -> new")
                            @Override
                            protected @NotNull Component getValueComponent(String id) {
                                Label label = new Label(id, finalClusterOtliers);
                                label.add(AttributeAppender.append("class", "d-flex pl-3 m-0"));
                                label.add(AttributeAppender.append("style", "font-size:20px"));
                                return label;
                            }
                        };
                        inProgress.setOutputMarkupId(true);
                        view.add(inProgress);

                        return view;

                    }
                };

                panel.setOutputMarkupId(true);
                panel.add(AttributeAppender.append("class", "col-12"));
                return panel;
            }

            @Override
            protected Component getFooterComponent(String id) {
                RepeatingView cardBodyComponent = new RepeatingView(id);
                cardBodyComponent.setOutputMarkupId(true);

                RoleAnalysisValueLabelPanel<?> anomaliesPanel = new RoleAnalysisValueLabelPanel<>(cardBodyComponent.newChildId()) {
                    @Contract(pure = true)
                    @Override
                    protected @NotNull Component getTitleComponent(String id) {
                        return new IconWithLabel(id,
                                createStringResource("RoleAnalysisOutlierAnalysisAspectsPanel.widget.characteristics.anomalies")) {
                            @Override
                            protected String getIconCssClass() {
                                return "fa fa-exclamation-triangle text-muted";
                            }

                            @Override
                            protected String getLabelComponentCssClass() {
                                return "text-muted";
                            }

                            @Override
                            protected String getComponentCssClass() {
                                return super.getComponentCssClass() + " gap-2";
                            }
                        };
                    }

                    @Contract(pure = true)
                    @Override
                    protected @NotNull Component getValueComponent(String id) {
                        Label label = new Label(id, anomaliesCount);
                        label.setOutputMarkupId(true);
                        label.add(AttributeAppender.append("class", "text-muted"));
                        return label;
                    }
                };
                anomaliesPanel.setOutputMarkupId(true);
                cardBodyComponent.add(anomaliesPanel);

                RoleAnalysisValueLabelPanel<?> partitionPanel = new RoleAnalysisValueLabelPanel<>(cardBodyComponent.newChildId()) {
                    @Contract(pure = true)
                    @Override
                    protected @NotNull Component getTitleComponent(String id) {
                        return new IconWithLabel(id,
                                createStringResource("RoleAnalysisOutlierAnalysisAspectsPanel.widget.characteristics.outliers")) {
                            @Override
                            protected String getIconCssClass() {
                                return GuiStyleConstants.CLASS_ROLE_ANALYSIS_SESSION_ICON + " text-muted";
                            }

                            @Override
                            protected String getLabelComponentCssClass() {
                                return "text-muted";
                            }

                            @Override
                            protected String getComponentCssClass() {
                                return super.getComponentCssClass() + " gap-2";
                            }
                        };
                    }

                    @Contract(pure = true)
                    @Override
                    protected @NotNull Component getValueComponent(String id) {
                        Label label = new Label(id, userOutlierCount);
                        label.setOutputMarkupId(true);
                        label.add(AttributeAppender.append("class", "text-muted"));
                        return label;
                    }
                };
                partitionPanel.setOutputMarkupId(true);
                cardBodyComponent.add(partitionPanel);
                return cardBodyComponent;
            }
        };

        distributionHeader.add(AttributeAppender.append("class", "col-6 pr-0"));

        distributionHeader.setOutputMarkupId(true);
        cardBodyComponent.add(distributionHeader);
        container.add(cardBodyComponent);
    }

    private void initOutlierPart(@NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisSessionType session,
            Task task,
            OperationResult result,
            RoleAnalysisSessionStatisticType sessionStatistic,
            WebMarkupContainer container) {

        List<RoleAnalysisOutlierType> topSessionOutliers = roleAnalysisService.getSessionOutliers(session.getOid(), task, result);

        if (topSessionOutliers != null && sessionStatistic != null && !topSessionOutliers.isEmpty()) {
            initOutlierPartNew(roleAnalysisService, session, task, result, sessionStatistic, container);

            emptyPanel(ID_CARD_TITLE, "Top session outlier", container);

            AjaxCompositedIconSubmitButton components = buildExplorePatternOutlier(topSessionOutliers.get(0));
            container.add(components);

            initOutlierPanel(container, session, topSessionOutliers.get(0));

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
        explorePatternButton.add(AttributeAppender.append("class", "ml-auto btn btn-default btn-sm"));
        explorePatternButton.setOutputMarkupId(true);
        return explorePatternButton;
    }

    private @NotNull AjaxCompositedIconSubmitButton buildExplorePatternOutlier(RoleAnalysisOutlierType outlier) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                GuiStyleConstants.CLASS_ICON_SEARCH, LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton explorePatternButton = new AjaxCompositedIconSubmitButton(
                ID_EXPLORE_PATTERN_BUTTON,
                iconBuilder.build(),
                createStringResource("RoleAnalysis.aspect.overview.page.title.explore.outlier")) {
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
        explorePatternButton.add(AttributeAppender.append("class", "ml-auto btn btn-default btn-sm"));
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

    public void initOutlierPanel(WebMarkupContainer container, RoleAnalysisSessionType session, RoleAnalysisOutlierType topSessionOutlier) {

        if (topSessionOutlier == null) {
            emptyPanel(ID_PANEL, "No data available", container);
            return;
        }

        String sessionOid = session.getOid();
        RoleAnalysisOutlierPartitionType outlierPartition = null;
        List<RoleAnalysisOutlierPartitionType> outlierPartitions = topSessionOutlier.getOutlierPartitions();
        for (RoleAnalysisOutlierPartitionType outlierPartitionNext : outlierPartitions) {
            ObjectReferenceType partitionTargetSessionRef = outlierPartitionNext.getTargetSessionRef();
            if (partitionTargetSessionRef.getOid().equals(sessionOid)) {
                outlierPartition = outlierPartitionNext;
                break;
            }
        }

        RoleAnalysisPartitionOverviewPanel panel = new RoleAnalysisPartitionOverviewPanel(ID_PANEL,
                Model.of(outlierPartition), Model.of(topSessionOutlier)) {
            @Override
            protected @NotNull String replaceWidgetCssClass() {
                return "col-6 mb-3";
            }
        };
        panel.setOutputMarkupId(true);
        container.add(panel);
    }

    private static void emptyPanel(String idPanel, String No_data_available, @NotNull WebMarkupContainer container) {
        Label label = new Label(idPanel, No_data_available);
        label.setOutputMarkupId(true);
        container.add(label);
    }

    protected int countAnomalies(String sessionOid) {
        PageBase pageBase = getPageBase();
        ModelService modelService = pageBase.getModelService();

        Task task = pageBase.createSimpleTask("Search outlier objects");
        ResultHandler<RoleAnalysisOutlierType> handler = (object, parentResult) -> {
            userOutlierCount++;
            RoleAnalysisOutlierType outlier = object.asObjectable();
            List<RoleAnalysisOutlierPartitionType> outlierPartitions = outlier.getOutlierPartitions();

            for (RoleAnalysisOutlierPartitionType outlierPartition : outlierPartitions) {
                List<DetectedAnomalyResult> detectedAnomalyResult = outlierPartition.getDetectedAnomalyResult();
                RoleAnalysisPartitionAnalysisType partitionAnalysis = outlierPartition.getPartitionAnalysis();
                ObjectReferenceType targetSessionRef = outlierPartition.getTargetSessionRef();

                if (targetSessionRef.getOid().equals(sessionOid)) {
                    assignmentAnomalyCount += detectedAnomalyResult.size();
                    Double clusterConfidence = partitionAnalysis.getOverallConfidence();
                    if (clusterConfidence != null && clusterConfidence > topConfidence) {
                        topConfidence = clusterConfidence.intValue();
                    }
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
        return assignmentAnomalyCount;
    }

}

