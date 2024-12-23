/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.aspects;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_DETECTED_PATER_ID;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_TABLE_SETTING;

import java.io.Serial;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.AnalysisInfoWidgetDto;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
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
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.MetricValuePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisPartitionOverviewPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.widgets.component.RoleAnalysisIdentifyWidgetPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisDetectedPatternDetails;
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

import org.jetbrains.annotations.Nullable;

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
    private static final String ID_PANEL_CONTAINER = "panel-container";

    private static final String FLEX_SHRINK_GROW = "flex-shrink-1 flex-grow-1 p-0";

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
        RoleAnalysisProcedureType analysisCategory = analysisOption.getAnalysisProcedureType();

        if (analysisCategory == null || analysisCategory.equals(RoleAnalysisProcedureType.ROLE_MINING)) {
            initRoleMiningPart(roleAnalysisService, session, task, result, sessionStatistic, container);
            initInfoPatternPanel(container);
        } else {
            AnalysisInfoWidgetDto analysisInfoWidgetDto = new AnalysisInfoWidgetDto();
            PageBase pageBase = getPageBase();
            OperationResult resultOp = new OperationResult("loadOutlierModels");

            analysisInfoWidgetDto.loadSessionOutlierModels(getObjectDetailsModels().getObjectType(), pageBase, resultOp, roleAnalysisService, task);

            LoadableModel<AnalysisInfoWidgetDto> model = new LoadableModel<>(false) {
                @Override
                protected AnalysisInfoWidgetDto load() {
                    return analysisInfoWidgetDto;
                }
            };
            initOutlierPart(roleAnalysisService, session, task, result, sessionStatistic, container, model);
            initInfoOutlierPanel(container, model);
        }

        RoleAnalysisViewAllPanel<String> outlierOverviewTable = buildOutlierOverviewTable(session);
        container.add(outlierOverviewTable);

    }

    private @NotNull RoleAnalysisViewAllPanel<String> buildOutlierOverviewTable(RoleAnalysisSessionType session) {
        RoleAnalysisViewAllPanel<String> accessPanel = new RoleAnalysisViewAllPanel<>("table",
                createStringResource("RoleAnalysis.aspect.overview.page.title.session.outliers")) {
            @Contract(pure = true)
            @Override
            protected @NotNull String getIconCssClass() {
                return GuiStyleConstants.CLASS_ICON_ASSIGNMENTS;
            }

            @Contract(" -> new")
            @Override
            protected @NotNull IModel<String> getLinkModel() {
                return createStringResource(
                        "RoleAnalysis.aspect.overview.page.title.view.all.session.outliers");
            }

            @Override
            protected void onLinkClick(AjaxRequestTarget target) {
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, session.getOid());
                parameters.add("panelId", "outliers");
                Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                        .getObjectDetailsPage(RoleAnalysisSessionType.class);
                ((PageBase) getPage()).navigateToNext(detailsPageClass, parameters);
            }

            @Override
            protected @NotNull Component getPanelComponent(String id) {
                RoleAnalysisSessionOutlierTable table = new RoleAnalysisSessionOutlierTable(id,
                        RoleAnalysisSessionAnalysisAspectsPanel.this.getPageBase(),
                        new LoadableDetachableModel<>() {
                            @Override
                            protected RoleAnalysisSessionType load() {
                                return session;
                            }
                        }) {

                    @Contract(pure = true)
                    @Override
                    public @NotNull Integer getLimit() {
                        return 5;
                    }

                    @Override
                    public boolean isPaginationVisible() {
                        return false;
                    }

                    @Override
                    public boolean hideFooter() {
                        return true;
                    }

                    @Override
                    public boolean isShowAsCard() {
                        return false;
                    }
                };
                table.setOutputMarkupId(true);
                table.add(AttributeModifier.append("style", "min-height: auto;"));
                return table;
            }
        };

        accessPanel.setOutputMarkupId(true);
        return accessPanel;
    }

    private void initInfoPatternPanel(@NotNull WebMarkupContainer container) {
        AnalysisInfoWidgetDto analysisInfoWidgetDto = new AnalysisInfoWidgetDto();
        OperationResult result = new OperationResult("loadTopClusterPatterns");
        analysisInfoWidgetDto.loadSessionPatternModels(getObjectDetailsModels().getObjectType(), getPageBase(), result);

        RoleAnalysisIdentifyWidgetPanel panel = new RoleAnalysisIdentifyWidgetPanel(ID_PATTERNS,
                createStringResource("Pattern.suggestions.title"), Model.ofList(analysisInfoWidgetDto.getPatternModelData())) {

            @Override
            protected void onClickFooter(AjaxRequestTarget target) {
                ObjectDetailsModels<RoleAnalysisSessionType> objectDetailsModels = RoleAnalysisSessionAnalysisAspectsPanel.this
                        .getObjectDetailsModels();
                String oid = objectDetailsModels.getObjectType().getOid();
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, oid);
                parameters.add(PANEL_ID, "sessionRoleSuggestions");

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
                return "p-0";
            }
        };
        panel.setOutputMarkupId(true);
        container.add(panel);
    }

    private void initRoleMiningPart(@NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisSessionType session,
            @NotNull Task task,
            @NotNull OperationResult result,
            @Nullable RoleAnalysisSessionStatisticType sessionStatistic,
            @NotNull WebMarkupContainer container) {
        List<DetectedPattern> topSessionPattern = roleAnalysisService.getSessionRoleSuggestion(
                session.getOid(), 1, true, result);

        WebMarkupContainer panelContainer = new WebMarkupContainer(ID_PANEL_CONTAINER);
        panelContainer.setOutputMarkupId(true);
        container.add(panelContainer);

        if (topSessionPattern != null && sessionStatistic != null && !topSessionPattern.isEmpty()) {
            DetectedPattern pattern = topSessionPattern.get(0);
            initMiningPartNew(roleAnalysisService, session, task, result, sessionStatistic, container);
            IconWithLabel titlePanel = new IconWithLabel(ID_CARD_TITLE, Model.of("Best role suggestion analysis")) { //TODO localization
                @Contract(pure = true)
                @Override
                protected @NotNull String getIconCssClass() {
                    return "fa fa-cube fa-sm";
                }
            };
            titlePanel.setOutputMarkupId(true);
            container.add(titlePanel);

            AjaxCompositedIconSubmitButton components = buildExplorePatternButton(pattern);
            container.add(components);

            RoleAnalysisDetectedPatternDetails statisticsPanel = new RoleAnalysisDetectedPatternDetails(ID_PANEL,
                    Model.of(pattern)) {

                @Override
                protected boolean isCardTitleVisible() {
                    return false;
                }

                @Contract(pure = true)
                @Override
                protected @NotNull String getCssClassForCardContainer() {
                    return "m-0 border-0";
                }

                @Override
                protected String getIconBoxContainerCssStyle() {
                    return "width:50px;"; /* width:40px */
                }

                @Contract(pure = true)
                @Override
                protected @NotNull String getCssClassForHeaderItemsContainer() {
                    return "row m-0 p-1 d-flex"; /* row m-0 p-1 */
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

            };
            statisticsPanel.setOutputMarkupId(true);
            panelContainer.add(statisticsPanel);

        } else {
            emptyPanel(ID_PANEL, panelContainer);

            WebMarkupContainer headerItems = new WebMarkupContainer(ID_HEADER_ITEMS);
            headerItems.setOutputMarkupId(true);
            container.add(headerItems);

            emptyPanel(ID_CARD_TITLE, container);

            WebMarkupContainer exploreButton = new WebMarkupContainer(ID_EXPLORE_PATTERN_BUTTON);
            exploreButton.setOutputMarkupId(true);
            container.add(exploreButton);
        }
    }

    private void initInfoOutlierPanel(@NotNull WebMarkupContainer container, LoadableModel<AnalysisInfoWidgetDto> analysisInfoWidgetDto) {

        RoleAnalysisIdentifyWidgetPanel panel = new RoleAnalysisIdentifyWidgetPanel(ID_PATTERNS,
                createStringResource("Outlier.suggestions.title"), Model.ofList(analysisInfoWidgetDto.getObject().getOutlierModelData())) {

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
                parameters.add(PANEL_ID, "allOutlierPanel");

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
                return FLEX_SHRINK_GROW;
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
        double totalSystemPercentageReduction = getTotalSystemPercentageReduction(totalReduction, totalAssignmentRoleToUser);

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
            protected @NotNull Component getPanelComponent(String id) {
                String status = resolveOutlierStatus(finalResolvedPatternCount, finalCandidateRolesCount);
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

                iconWithLabel.add(AttributeModifier.append(CLASS_CSS, "badge p-3 my-auto justify-content-center flex-grow-1 flex-shrink-1"));
                iconWithLabel.add(AttributeModifier.append(STYLE_CSS, "background-color: #dcf1f4;"));
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
                                return TEXT_MUTED;
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
                        label.add(AttributeModifier.append(CLASS_CSS, TEXT_MUTED));
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
                                return TEXT_MUTED;
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
                        label.add(AttributeModifier.append(CLASS_CSS, TEXT_MUTED));
                        return label;
                    }

                };
                solvedValueLabelPanel.setOutputMarkupId(true);
                cardBodyComponent.add(solvedValueLabelPanel);
                return cardBodyComponent;
            }
        };
        statusHeader.setOutputMarkupId(true);
        statusHeader.add(AttributeModifier.append(CLASS_CSS, FLEX_SHRINK_GROW)); /* col-6 pl-0 */
        cardBodyComponent.add(statusHeader);

        int clusterInliers = processedObjectCount - clusterOtliers;

        List<ProgressBar> progressBars = new ArrayList<>();

        double clusterInliersValue = 0;
        double clusterOtliersValue = 0;
        if (processedObjectCount != 0) {
            clusterInliersValue = clusterInliers * 100 / (double) processedObjectCount;
            clusterOtliersValue = clusterOtliers * 100 / (double) processedObjectCount;
        }

        progressBars.add(new ProgressBar(clusterInliersValue, ProgressBar.State.INFO));
        progressBars.add(new ProgressBar(clusterOtliersValue, ProgressBar.State.WARNING));

        int finalClusterOtliers = clusterOtliers;

        int finalTotalReduction = totalReduction;
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
                        /*components.add(AttributeModifier.append(CLASS_CSS, "pt-3 pl-2 pr-2"));*/
                        return components;
                    }

                    @Contract(pure = true)
                    @Override
                    protected @NotNull String getContainerLegendCssClass() {
                        return "d-flex flex-wrap justify-content-between pt-2 pb-0 px-0";
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
                                        return "fa fa-circle text-info fa-2xs align-middle";
                                    }

                                    @Override
                                    protected String getIconComponentCssStyle() {
                                        return "font-size:8px;margin-bottom:2px;";
                                    }

                                    @Override
                                    protected String getLabelComponentCssClass() {
                                        return "txt-toned";
                                    }

                                    @Override
                                    protected String getComponentCssClass() {
                                        return super.getComponentCssClass() + "mb-1 gap-2";
                                    }
                                };
                            }

                            @Contract("_ -> new")
                            @Override
                            protected @NotNull Component getValueComponent(String id) {
                                Label label = new Label(id, clusterInliers);
                                label.add(AttributeModifier.append(CLASS_CSS, "d-flex pl-3 m-0 lh-1 text-bold txt-toned"));
                                label.add(AttributeModifier.append(STYLE_CSS, "font-size:18px"));
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
                                        return "fa fa-circle text-warning fa-2xs align-middle";
                                    }

                                    @Override
                                    protected String getIconComponentCssStyle() {
                                        return "font-size:8px;margin-bottom:2px;";
                                    }

                                    @Override
                                    protected String getLabelComponentCssClass() {
                                        return "txt-toned";
                                    }

                                    @Override
                                    protected String getComponentCssClass() {
                                        return super.getComponentCssClass() + "mb-1 gap-2";
                                    }
                                };
                            }

                            @Contract("_ -> new")
                            @Override
                            protected @NotNull Component getValueComponent(String id) {
                                Label label = new Label(id, finalClusterOtliers);
                                label.add(AttributeModifier.append(CLASS_CSS, "d-flex pl-3 m-0 lh-1 text-bold txt-toned"));
                                label.add(AttributeModifier.append(STYLE_CSS, "font-size:18px"));
                                return label;
                            }
                        };
                        inProgress.setOutputMarkupId(true);
                        view.add(inProgress);

                        return view;

                    }
                };

                panel.setOutputMarkupId(true);
                panel.add(AttributeModifier.append(CLASS_CSS, "col-12"));
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
                                return "fa fa-exclamation-triangle " + TEXT_MUTED;
                            }

                            @Override
                            protected String getLabelComponentCssClass() {
                                return TEXT_MUTED;
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
                        label.add(AttributeModifier.append(CLASS_CSS, TEXT_MUTED));
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
                                return GuiStyleConstants.CLASS_ROLE_ANALYSIS_SESSION_ICON + " " + TEXT_MUTED;
                            }

                            @Override
                            protected String getLabelComponentCssClass() {
                                return TEXT_MUTED;
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
                        Label label = new Label(id, totalSystemPercentageReduction + "%");
                        label.setOutputMarkupId(true);
                        label.add(AttributeModifier.append(CLASS_CSS, TEXT_MUTED));
                        return label;
                    }
                };
                partitionPanel.setOutputMarkupId(true);
                cardBodyComponent.add(partitionPanel);
                return cardBodyComponent;
            }
        };

        distributionHeader.add(AttributeModifier.append(CLASS_CSS, FLEX_SHRINK_GROW));

        distributionHeader.setOutputMarkupId(true);
        cardBodyComponent.add(distributionHeader);
        container.add(cardBodyComponent);
    }

    private static @NotNull String resolveOutlierStatus(int finalResolvedPatternCount, int finalCandidateRolesCount) {
        String status = "New (Untouched)";
        if (finalResolvedPatternCount > 0 || finalCandidateRolesCount > 0) {
            status = "In Progress";
        }
        return status;
    }

    private static double getTotalSystemPercentageReduction(int totalReduction, int totalAssignmentRoleToUser) {
        double totalSystemPercentageReduction = 0;
        if (totalReduction != 0 && totalAssignmentRoleToUser != 0) {
            totalSystemPercentageReduction = ((double) totalReduction / totalAssignmentRoleToUser) * 100;
            BigDecimal bd = BigDecimal.valueOf(totalSystemPercentageReduction);
            bd = bd.setScale(2, RoundingMode.HALF_UP);
            totalSystemPercentageReduction = bd.doubleValue();
        }
        return totalSystemPercentageReduction;
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

        //TODO check after implementation outlier certification
//        RoleAnalysisOutlierDashboardPanel<?> statusHeader = new RoleAnalysisOutlierDashboardPanel<>(cardBodyComponent.newChildId(),
//                createStringResource("RoleAnalysisOutlierAnalysisAspectsPanel.widget.status")) {
//            @Contract(pure = true)
//            @Override
//            protected @NotNull String getIconCssClass() {
//                return GuiStyleConstants.CLASS_ICON_ASSIGNMENTS;
//            }
//
//            @Override
//            protected @NotNull Component getPanelComponent(String id) {
//                IconWithLabel iconWithLabel = new IconWithLabel(id, Model.of("UNKNOWN (TBD)")) {
//                    @Contract(pure = true)
//                    @Override
//                    protected @NotNull String getIconCssClass() {
//                        return "fas fa-sync";
//                    }
//
//                    @Override
//                    protected String getComponentCssClass() {
//                        return super.getComponentCssClass() + " gap-2";
//                    }
//
//                    @Override
//                    protected String getComponentCssStyle() {
//                        return "color: #18a2b8; font-size: 20px;";
//                    }
//                };
//
//                iconWithLabel.add(AttributeModifier.append(CLASS_CSS, "badge p-3 my-auto justify-content-center flex-grow-1 flex-shrink-1"));
//                iconWithLabel.add(AttributeModifier.append(STYLE_CSS, "background-color: #dcf1f4;"));
//                return iconWithLabel;
//            }
//
//            @Override
//            protected Component getFooterComponent(String id) {
//                RepeatingView cardBodyComponent = new RepeatingView(id);
//                cardBodyComponent.setOutputMarkupId(true);
//
//                RoleAnalysisValueLabelPanel<?> pendingValueLabelPanel = new RoleAnalysisValueLabelPanel<>(cardBodyComponent.newChildId()) {
//                    @Contract(pure = true)
//                    @Override
//                    protected @NotNull Component getTitleComponent(String id) {
//                        return new IconWithLabel(id,
//                                createStringResource("RoleAnalysis.aspect.overview.page.title.pending.recertifications")) {
//                            @Override
//                            protected String getIconCssClass() {
//                                return "fas fa-sync " + TEXT_MUTED;
//                            }
//
//                            @Override
//                            protected String getLabelComponentCssClass() {
//                                return TEXT_MUTED;
//                            }
//
//                            @Override
//                            protected String getComponentCssClass() {
//                                return super.getComponentCssClass() + " gap-2";
//                            }
//                        };
//                    }
//
//                    @Contract(pure = true)
//                    @Override
//                    protected @NotNull Component getValueComponent(String id) {
//                        Label label = new Label(id, "0");
//                        label.add(AttributeModifier.append(CLASS_CSS, TEXT_MUTED));
//                        return label;
//                    }
//                };
//                pendingValueLabelPanel.setOutputMarkupId(true);
//                cardBodyComponent.add(pendingValueLabelPanel);
//
//                RoleAnalysisValueLabelPanel<?> solvedValueLabelPanel = new RoleAnalysisValueLabelPanel<>(cardBodyComponent.newChildId()) {
//
//                    @Contract(pure = true)
//                    @Override
//                    protected @NotNull Component getTitleComponent(String id) {
//                        return new IconWithLabel(id,
//                                createStringResource("RoleAnalysis.aspect.overview.page.title.solved.recertifications")) {
//                            @Override
//                            protected String getIconCssClass() {
//                                return "fas fa-trophy " + TEXT_MUTED;
//                            }
//
//                            @Override
//                            protected String getLabelComponentCssClass() {
//                                return TEXT_MUTED;
//                            }
//
//                            @Override
//                            protected String getComponentCssClass() {
//                                return super.getComponentCssClass() + " gap-2";
//                            }
//                        };
//                    }
//
//                    @Contract(pure = true)
//                    @Override
//                    protected @NotNull Component getValueComponent(String id) {
//                        Label label = new Label(id, "0");
//                        label.setOutputMarkupId(true);
//                        label.add(AttributeModifier.append(CLASS_CSS, TEXT_MUTED));
//                        return label;
//                    }
//
//                };
//                solvedValueLabelPanel.setOutputMarkupId(true);
//                cardBodyComponent.add(solvedValueLabelPanel);
//                return cardBodyComponent;
//            }
//        };
//        statusHeader.setOutputMarkupId(true);
//        statusHeader.add(AttributeModifier.append(CLASS_CSS, FLEX_SHRINK_GROW)); /* col-6 pl-0 */
//        cardBodyComponent.add(statusHeader);

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
        double valueInliers = 0;
        if (clusterInliers != 0 && processedObjectCount != 0) {
            valueInliers = clusterInliers * 100 / (double) processedObjectCount;
        }

        double valueOutliers = 0;
        if (clusterOtliers != 0 && processedObjectCount != 0) {
            valueOutliers = clusterOtliers * 100 / (double) processedObjectCount;
        }
        progressBars.add(new ProgressBar(valueInliers, ProgressBar.State.INFO));
        progressBars.add(new ProgressBar(valueOutliers, ProgressBar.State.WARNING));

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
                        /*components.add(AttributeModifier.append(CLASS_CSS, "pt-3 pl-2 pr-2"));*/
                        return components;
                    }

                    @Contract(pure = true)
                    @Override
                    protected @NotNull String getContainerLegendCssClass() {
                        return "d-flex flex-wrap justify-content-between pt-2 pb-0 px-0";
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
                                        return " fa fa-circle text-info fa-2xs align-middle";
                                    }

                                    @Override
                                    protected String getIconComponentCssStyle() {
                                        return "font-size:8px;margin-bottom:2px;";
                                    }

                                    @Override
                                    protected String getLabelComponentCssClass() {
                                        return "txt-toned";
                                    }

                                    @Override
                                    protected String getComponentCssClass() {
                                        return super.getComponentCssClass() + "mb-1 gap-2";
                                    }
                                };
                            }

                            @Contract("_ -> new")
                            @Override
                            protected @NotNull Component getValueComponent(String id) {
                                Label label = new Label(id, clusterInliers);
                                label.add(AttributeModifier.append(CLASS_CSS, "d-flex pl-3 m-0 lh-1 text-bold txt-toned"));
                                label.add(AttributeModifier.append(STYLE_CSS, "font-size:18px"));
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
                                        return "fa fa-circle text-warning fa-2xs align-middle";
                                    }

                                    @Override
                                    protected String getIconComponentCssStyle() {
                                        return "font-size:8px;margin-bottom:2px;";
                                    }

                                    @Override
                                    protected String getLabelComponentCssClass() {
                                        return "txt-toned";
                                    }

                                    @Override
                                    protected String getComponentCssClass() {
                                        return super.getComponentCssClass() + "mb-1 gap-2";
                                    }
                                };
                            }

                            @Contract("_ -> new")
                            @Override
                            protected @NotNull Component getValueComponent(String id) {
                                Label label = new Label(id, finalClusterOtliers);
                                label.add(AttributeModifier.append(CLASS_CSS, "d-flex pl-3 m-0 lh-1 text-bold txt-toned"));
                                label.add(AttributeModifier.append(STYLE_CSS, "font-size:18px"));
                                return label;
                            }
                        };
                        inProgress.setOutputMarkupId(true);
                        view.add(inProgress);

                        return view;

                    }
                };

                panel.setOutputMarkupId(true);
                panel.add(AttributeModifier.append(CLASS_CSS, "col-12"));
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
                        label.add(AttributeModifier.append(CLASS_CSS, TEXT_MUTED));
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
                        label.add(AttributeModifier.append(CLASS_CSS, TEXT_MUTED));
                        return label;
                    }
                };
                partitionPanel.setOutputMarkupId(true);
                cardBodyComponent.add(partitionPanel);
                return cardBodyComponent;
            }
        };

        distributionHeader.add(AttributeModifier.append(CLASS_CSS, FLEX_SHRINK_GROW));

        distributionHeader.setOutputMarkupId(true);
        cardBodyComponent.add(distributionHeader);
        container.add(cardBodyComponent);
    }

    private void initOutlierPart(@NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisSessionType session,
            Task task,
            OperationResult result,
            RoleAnalysisSessionStatisticType sessionStatistic,
            WebMarkupContainer container,
            LoadableModel<AnalysisInfoWidgetDto> analysisInfoWidgetDto) {

        initOutlierPartNew(roleAnalysisService, session, task, result, sessionStatistic, container);

        IconWithLabel titlePanel = new IconWithLabel(ID_CARD_TITLE, createStringResource("RoleAnalysisOutlierAnalysisAspectsPanel.widget.characteristics.bestOutlierDetails")) {
            @Contract(pure = true)
            @Override
            protected @NotNull String getIconCssClass() {
                return GuiStyleConstants.CLASS_ICON_OUTLIER + " fa-sm";
            }
        };
        titlePanel.setOutputMarkupId(true);
        container.add(titlePanel);

        initOutlierPanel(container, session, analysisInfoWidgetDto);

    }

    private @NotNull AjaxCompositedIconSubmitButton buildExplorePatternButton(DetectedPattern pattern) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                GuiStyleConstants.CLASS_ICON_SEARCH, IconCssStyle.IN_ROW_STYLE);
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
        explorePatternButton.add(AttributeModifier.append(CLASS_CSS, "ml-auto btn btn-link btn-sm p-0"));
        explorePatternButton.setOutputMarkupId(true);
        return explorePatternButton;
    }

    private void explorePatternPerform(@NotNull DetectedPattern pattern) {
        PageParameters parameters = new PageParameters();
        String clusterOid = pattern.getClusterRef().getOid();
        parameters.add(OnePageParameterEncoder.PARAMETER, clusterOid);
        parameters.add(PANEL_ID, "clusterDetails");
        parameters.add(PARAM_DETECTED_PATER_ID, pattern.getId());
        StringValue fullTableSetting = getPageBase().getPageParameters().get(PARAM_TABLE_SETTING);
        if (fullTableSetting != null && fullTableSetting.toString() != null) {
            parameters.add(PARAM_TABLE_SETTING, fullTableSetting.toString());
        }

        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                .getObjectDetailsPage(RoleAnalysisClusterType.class);
        getPageBase().navigateToNext(detailsPageClass, parameters);
    }

    public void initOutlierPanel(WebMarkupContainer container, RoleAnalysisSessionType session, LoadableModel<AnalysisInfoWidgetDto> topSessionOutlier) {

        WebMarkupContainer panelContainer = new WebMarkupContainer(ID_PANEL_CONTAINER);
        panelContainer.setOutputMarkupId(true);
        panelContainer.add(AttributeModifier.replace(CLASS_CSS, "card-body p-2 pt-3"));
        container.add(panelContainer);

        String sessionOid = session.getOid();

        LoadableModel<RoleAnalysisOutlierPartitionType> outlierPartitionTypeLoadableModel = new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected RoleAnalysisOutlierPartitionType load() {
                RoleAnalysisOutlierType topOutliers = topSessionOutlier.getObject().getTopOutliers();
                if (topOutliers == null) {
                    return null;
                }
                RoleAnalysisOutlierPartitionType outlierPartition = null;
                List<RoleAnalysisOutlierPartitionType> outlierPartitions = topOutliers.getPartition();
                for (RoleAnalysisOutlierPartitionType outlierPartitionNext : outlierPartitions) {
                    ObjectReferenceType partitionTargetSessionRef = outlierPartitionNext.getTargetSessionRef();
                    if (partitionTargetSessionRef.getOid().equals(sessionOid)) {
                        outlierPartition = outlierPartitionNext;
                        break;
                    }
                }
                return outlierPartition;
            }
        };

        RoleAnalysisPartitionOverviewPanel panel = buildTopoutlierPanel(topSessionOutlier, outlierPartitionTypeLoadableModel);

        panel.setOutputMarkupId(true);
        panelContainer.add(panel);
    }

    private static @NotNull RoleAnalysisPartitionOverviewPanel buildTopoutlierPanel(LoadableModel<AnalysisInfoWidgetDto> topSessionOutlier, LoadableModel<RoleAnalysisOutlierPartitionType> outlierPartitionTypeLoadableModel) {
        RoleAnalysisPartitionOverviewPanel panel = new RoleAnalysisPartitionOverviewPanel(ID_PANEL,
                outlierPartitionTypeLoadableModel, Model.of(topSessionOutlier.getObject().getTopOutliers())) {

            @Override
            protected @NotNull String replaceWidgetCssClass() {
                return "col-12 col-xl-6 p-2";
            }
        };
        return panel;
    }

    private static void emptyPanel(String idPanel, @NotNull WebMarkupContainer container) {
        Label label = new Label(idPanel, "No data available");
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
            List<RoleAnalysisOutlierPartitionType> outlierPartitions = outlier.getPartition();

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
            throw new SystemException("Couldn't count anomalies", e);
        }
        return assignmentAnomalyCount;
    }

}

