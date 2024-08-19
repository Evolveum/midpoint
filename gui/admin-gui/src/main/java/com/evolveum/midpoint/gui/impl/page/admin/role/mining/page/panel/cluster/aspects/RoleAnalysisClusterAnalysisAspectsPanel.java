/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.aspects;

import static com.evolveum.midpoint.common.mining.utils.ExtractPatternUtils.transformDefaultPattern;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.RoleAnalysisAspectsWebUtils.getClusterWidgetModelOutliers;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.RoleAnalysisAspectsWebUtils.getClusterWidgetModelPatterns;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_TABLE_SETTING;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel.generateUserOutlierResultModel;

import java.io.Serial;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBarPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.*;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.widgets.component.RoleAnalysisIdentifyWidgetPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.widgets.model.IdentifyWidgetItem;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.RoleAnalysisDistributionProgressPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.RoleAnalysisOutlierDashboardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.RoleAnalysisValueLabelPanel;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.model.InfoBoxModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.*;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "clusterOverview", defaultContainerPath = "empty")
@PanelInstance(identifier = "clusterOverview",
        applicableForType = RoleAnalysisClusterType.class,
        defaultPanel = true,
        display = @PanelDisplay(
                label = "RoleAnalysis.overview.panel",
                icon = GuiStyleConstants.CLASS_LINE_CHART_ICON,
                order = 10))
public class RoleAnalysisClusterAnalysisAspectsPanel extends AbstractObjectMainPanel<RoleAnalysisClusterType, ObjectDetailsModels<RoleAnalysisClusterType>> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_HEADER_ITEMS = "header-items";
    private static final String ID_PANEL = "panelId";
    private static final String ID_PATTERNS = "patterns";

    public RoleAnalysisClusterAnalysisAspectsPanel(String id, ObjectDetailsModels<RoleAnalysisClusterType> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    protected void initLayout() {

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        ObjectDetailsModels<RoleAnalysisClusterType> objectDetailsModels = getObjectDetailsModels();
        RoleAnalysisClusterType cluster = objectDetailsModels.getObjectType();
        ObjectReferenceType targetSessionRef = cluster.getRoleAnalysisSessionRef();
        PageBase pageBase = RoleAnalysisClusterAnalysisAspectsPanel.this.getPageBase();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        Task task = pageBase.createSimpleTask("Load session type object");

        PrismObject<RoleAnalysisSessionType> sessionTypeObject = roleAnalysisService
                .getSessionTypeObject(targetSessionRef.getOid(), task, task.getResult());
        if (sessionTypeObject == null) {
            Label label = new Label(ID_PANEL, "No session found");
            container.add(label);
            EmptyPanel emptyPanel = new EmptyPanel(ID_PATTERNS);
            container.add(emptyPanel);
            return;
        }

        RoleAnalysisSessionType session = sessionTypeObject.asObjectable();
        RoleAnalysisCategoryType analysisCategory = session.getAnalysisOption().getAnalysisCategory();

        if (RoleAnalysisCategoryType.OUTLIERS.equals(analysisCategory)) {
            initInfoOutlierPanel(container);
//            initOutlierAnalysisHeaderPanel(container);
            initOutlierPartNew(container);
        } else {
            initInfoPatternPanel(container);
            initMiningPartNew(roleAnalysisService, task.getResult(), container);
        }

        AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();

        if (clusterStatistics != null) {
            RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = clusterStatistics.getUserAttributeAnalysisResult();
            RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResult = clusterStatistics.getRoleAttributeAnalysisResult();
            RoleAnalysisAttributePanel roleAnalysisAttributePanel = new RoleAnalysisAttributePanel(ID_PANEL,
                    createStringResource("RoleAnalysis.aspect.overview.page.title.clustering.attribute.analysis"),
                    roleAttributeAnalysisResult, userAttributeAnalysisResult) {
                @Override
                protected @NotNull String getChartContainerStyle() {
                    return "height:25vh;";
                }
            };
            roleAnalysisAttributePanel.setOutputMarkupId(true);
            container.add(roleAnalysisAttributePanel);
        } else {
            Label label = new Label(ID_PANEL, createStringResource("RoleAnalysis.aspect.overview.page.title.no.data.available"));
            label.setOutputMarkupId(true);
            container.add(label);
        }
    }

    protected void initMiningPartNew(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull OperationResult result,
            @NotNull WebMarkupContainer container) {

        RepeatingView cardBodyComponent = new RepeatingView(ID_HEADER_ITEMS);
        cardBodyComponent.setOutputMarkupId(true);
        container.add(cardBodyComponent);

        ObjectDetailsModels<RoleAnalysisClusterType> objectDetailsModels = getObjectDetailsModels();
        RoleAnalysisClusterType cluster = objectDetailsModels.getObjectType();
        AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();
        List<ObjectReferenceType> resolvedPattern = cluster.getResolvedPattern();

        List<RoleAnalysisCandidateRoleType> candidateRoles = cluster.getCandidateRoles();

        Double detectedReductionMetric = clusterStatistics.getDetectedReductionMetric();
        if (detectedReductionMetric == null) {
            detectedReductionMetric = 0.0;
        }

        int candidateRolesCount = candidateRoles.size();
        int resolvedPatternCount = resolvedPattern.size();
        //TODO check why is there empty ObjectReferenceType
        if (resolvedPattern.size() == 1) {
            ObjectReferenceType objectReferenceType = resolvedPattern.get(0);
            if (objectReferenceType == null || objectReferenceType.getOid() == null) {
                resolvedPatternCount = 0;
            }
        }

        Integer rolesCount = clusterStatistics.getRolesCount();
        Integer usersCount = clusterStatistics.getUsersCount();
        if (rolesCount == null) {
            rolesCount = 0;
        }

        if (usersCount == null) {
            usersCount = 0;
        }

        int totalAssignmentRoleToUser = roleAnalysisService.countUserOwnedRoleAssignment(result);
        double percentagePartReduction = 0;
        if (detectedReductionMetric != 0 && totalAssignmentRoleToUser != 0) {
            percentagePartReduction = (detectedReductionMetric / totalAssignmentRoleToUser) * 100;
            BigDecimal bd = new BigDecimal(percentagePartReduction).setScale(2, RoundingMode.HALF_UP);
            percentagePartReduction = bd.doubleValue();
        }

        int processedObjectCount = rolesCount + usersCount;

        int finalResolvedPatternCount = resolvedPatternCount;
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
                if (candidateRolesCount > 0 || finalResolvedPatternCount > 0) {
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
                        return new IconWithLabel(id, createStringResource("RoleAnalysis.aspect.overview.page.title.candidate.roles")) {
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
                        Label label = new Label(id, candidateRolesCount);
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
                        return new IconWithLabel(id, createStringResource(
                                "RoleAnalysis.aspect.overview.page.title.resolved.suggestions")) {
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

        List<ProgressBar> progressBars = new ArrayList<>();

        progressBars.add(new ProgressBar(rolesCount * 100 / (double) processedObjectCount, ProgressBar.State.SUCCESS));
        progressBars.add(new ProgressBar(usersCount * 100 / (double) processedObjectCount, ProgressBar.State.DANGER));

        Integer finalRolesCount = rolesCount;
        Integer finalUsersCount = usersCount;
        Double finalDetectedReductionMetric = detectedReductionMetric;
        double finalPercentagePartReduction = percentagePartReduction;
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

            @SuppressWarnings("rawtypes")
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
                                        createStringResource("RoleAnalysis.aspect.overview.page.title.cluster.roles")) {
                                    @Override
                                    protected String getIconCssClass() {
                                        return "fa fa-circle text-success fa-2xs";
                                    }

                                    @Override
                                    protected String getLabelComponentCssClass() {
                                        return "text-success";
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
                                Label label = new Label(id, finalRolesCount);
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
                                        createStringResource("RoleAnalysis.aspect.overview.page.title.cluster.users")) {
                                    @Override
                                    protected String getIconCssClass() {
                                        return "fa fa-circle text-danger fa-2xs";
                                    }

                                    @Override
                                    protected String getLabelComponentCssClass() {
                                        return "text-danger";
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
                                Label label = new Label(id, finalUsersCount);
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
                        Label label = new Label(id, finalDetectedReductionMetric);
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
                        Label label = new Label(id, finalPercentagePartReduction + "%");
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
            @NotNull WebMarkupContainer container) {

        RepeatingView cardBodyComponent = new RepeatingView(ID_HEADER_ITEMS);
        cardBodyComponent.setOutputMarkupId(true);
        container.add(cardBodyComponent);

        ObjectDetailsModels<RoleAnalysisClusterType> objectDetailsModels = getObjectDetailsModels();
        RoleAnalysisClusterType cluster = objectDetailsModels.getObjectType();
        AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();

        List<RoleAnalysisOutlierPartitionType> allPartitions = getAllPartitions(cluster.getOid(), getPageBase());
        int outlierCount = 0;
        int anomalyCount = 0;

        if (allPartitions != null) {
            outlierCount = allPartitions.size();
            for (RoleAnalysisOutlierPartitionType allPartition : allPartitions) {
                List<DetectedAnomalyResult> detectedAnomalyResult = allPartition.getDetectedAnomalyResult();
                if (detectedAnomalyResult != null) {
                    anomalyCount += detectedAnomalyResult.size();
                }
            }
        }

        Integer rolesCount = clusterStatistics.getRolesCount();
        Integer usersCount = clusterStatistics.getUsersCount();
        if (rolesCount == null) {
            rolesCount = 0;
        }

        if (usersCount == null) {
            usersCount = 0;
        }

        int processedObjectCount = rolesCount + usersCount;

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

        List<ProgressBar> progressBars = new ArrayList<>();

        progressBars.add(new ProgressBar(rolesCount * 100 / (double) processedObjectCount, ProgressBar.State.SUCCESS));
        progressBars.add(new ProgressBar(usersCount * 100 / (double) processedObjectCount, ProgressBar.State.DANGER));

        Integer finalRolesCount = rolesCount;
        Integer finalUsersCount = usersCount;
        int finalOutlierCount = outlierCount;
        int finalAnomalyCount = anomalyCount;
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

            @SuppressWarnings("rawtypes")
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
                                        createStringResource("RoleAnalysis.aspect.overview.page.title.cluster.roles")) {
                                    @Override
                                    protected String getIconCssClass() {
                                        return "fa fa-circle text-success fa-2xs";
                                    }

                                    @Override
                                    protected String getLabelComponentCssClass() {
                                        return "text-success";
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
                                Label label = new Label(id, finalRolesCount);
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
                                        createStringResource("RoleAnalysis.aspect.overview.page.title.cluster.users")) {
                                    @Override
                                    protected String getIconCssClass() {
                                        return "fa fa-circle text-danger fa-2xs";
                                    }

                                    @Override
                                    protected String getLabelComponentCssClass() {
                                        return "text-danger";
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
                                Label label = new Label(id, finalUsersCount);
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
                panel.add(AttributeAppender.append("class", "col-12 pl-0"));
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
                                createStringResource("RoleAnalysisOutlierAnalysisAspectsPanel.widget.characteristics.outliers")) {
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
                        Label label = new Label(id, finalOutlierCount);
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
                                createStringResource("RoleAnalysisOutlierAnalysisAspectsPanel.widget.characteristics.anomalies")) {
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
                        Label label = new Label(id, finalAnomalyCount);
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

    private void initOutlierAnalysisHeaderPanel(WebMarkupContainer container) {

        RepeatingView headerItems = new RepeatingView(ID_HEADER_ITEMS);
        headerItems.setOutputMarkupId(true);
        container.add(headerItems);

        ObjectDetailsModels<RoleAnalysisClusterType> objectDetailsModels = getObjectDetailsModels();
        RoleAnalysisClusterType cluster = objectDetailsModels.getObjectType();
        AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();

        InfoBoxModel infoBoxResolvedPatterns = new InfoBoxModel(GuiStyleConstants.CLASS_DETECTED_PATTERN_ICON + " text-white",
                "User outliers",
                String.valueOf(outliersCount),
                100,
                "Number of user outlier for cluster");

        RoleAnalysisInfoBox resolvedPatternLabel = new RoleAnalysisInfoBox(headerItems.newChildId(), Model.of(infoBoxResolvedPatterns)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-primary";
            }

        };
        resolvedPatternLabel.add(AttributeModifier.replace("class", "col-md-6"));
        resolvedPatternLabel.setOutputMarkupId(true);
        headerItems.add(resolvedPatternLabel);

        InfoBoxModel infoBoxCandidateRoles = new InfoBoxModel(GuiStyleConstants.CLASS_CANDIDATE_ROLE_ICON + " text-white",
                "Assignments anomaly",
                String.valueOf(anomalyAssignmentCount),
                100,
                "Number of assignment anomaly for cluster");

        RoleAnalysisInfoBox candidateRolesLabel = new RoleAnalysisInfoBox(headerItems.newChildId(), Model.of(infoBoxCandidateRoles)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-primary";
            }
        };
        candidateRolesLabel.add(AttributeModifier.replace("class", "col-md-6"));
        candidateRolesLabel.setOutputMarkupId(true);
        headerItems.add(candidateRolesLabel);

        InfoBoxModel infoBoxRoles = new InfoBoxModel(GuiStyleConstants.CLASS_OBJECT_ROLE_ICON + " text-white",
                "Roles",
                String.valueOf(clusterStatistics.getRolesCount()),
                100,
                "Number of roles in the cluster");

        RoleAnalysisInfoBox rolesLabel = new RoleAnalysisInfoBox(headerItems.newChildId(), Model.of(infoBoxRoles)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-primary";
            }
        };
        rolesLabel.add(AttributeModifier.replace("class", "col-md-6"));
        rolesLabel.setOutputMarkupId(true);
        headerItems.add(rolesLabel);

        InfoBoxModel infoBoxUsers = new InfoBoxModel(GuiStyleConstants.CLASS_OBJECT_USER_ICON + " text-white",
                "Users",
                String.valueOf(clusterStatistics.getUsersCount()),
                100,
                "Number of users in the cluster");

        RoleAnalysisInfoBox usersLabel = new RoleAnalysisInfoBox(headerItems.newChildId(), Model.of(infoBoxUsers)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-primary";
            }
        };
        usersLabel.add(AttributeModifier.replace("class", "col-md-6"));
        usersLabel.setOutputMarkupId(true);
        headerItems.add(usersLabel);

    }

    int outliersCount = 0;
    int anomalyAssignmentCount = 0;

    protected List<RoleAnalysisOutlierPartitionType> getAllPartitions(String clusterOid, PageBase pageBase) {
        ModelService modelService = pageBase.getModelService();
        Task task = pageBase.createSimpleTask("loadRoleAnalysisInfo");
        OperationResult result = task.getResult();
        List<RoleAnalysisOutlierPartitionType> searchResultList = new ArrayList<>();
        ResultHandler<RoleAnalysisOutlierType> resultHandler = (outlier, lResult) -> {

            RoleAnalysisOutlierType outlierObject = outlier.asObjectable();
            List<RoleAnalysisOutlierPartitionType> outlierPartitions = outlierObject.getOutlierPartitions();
            for (RoleAnalysisOutlierPartitionType outlierPartition : outlierPartitions) {
                ObjectReferenceType targetClusterRef = outlierPartition.getTargetClusterRef();
                String oid = targetClusterRef.getOid();
                if (clusterOid.equals(oid)) {
                    searchResultList.add(outlierPartition);
                    break;
                }
            }

            return true;
        };

        try {
            modelService.searchObjectsIterative(RoleAnalysisOutlierType.class, null, resultHandler,
                    null, task, result);
        } catch (Exception ex) {
            throw new RuntimeException("Couldn't search outliers", ex);
        }
        return searchResultList;
    }


    private void initInfoOutlierPanelOld(WebMarkupContainer container) {
        RoleAnalysisItemPanel roleAnalysisInfoPatternPanel = new RoleAnalysisItemPanel(ID_PATTERNS,
                createStringResource("RoleAnalysis.aspect.overview.page.title.discovered.cluster.outliers")) {
            @Override
            protected void addItem(RepeatingView repeatingView) {
                RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();
                PageBase pageBase = RoleAnalysisClusterAnalysisAspectsPanel.this.getPageBase();
                ModelService modelService = pageBase.getModelService();
                Task task = pageBase.createSimpleTask("loadRoleAnalysisInfo");
                RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
                OperationResult result = task.getResult();
                //TODO replace after db schema change
//                SearchResultList<PrismObject<RoleAnalysisOutlierType>> searchResultList;
//                try {
//                    searchResultList = modelService
//                            .searchObjects(RoleAnalysisOutlierType.class, getQuery(cluster), null, task, result);
//                } catch (SchemaException | ObjectNotFoundException | SecurityViolationException |
//                        CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
//                    throw new RuntimeException(e);
//                }

                //TODO remove hack after db schema change
                List<PrismObject<RoleAnalysisOutlierType>> searchResultList = new ArrayList<>();
                String clusterOid = cluster.getOid();
                ResultHandler<RoleAnalysisOutlierType> resultHandler = (outlier, lResult) -> {

                    RoleAnalysisOutlierType outlierObject = outlier.asObjectable();
                    List<RoleAnalysisOutlierPartitionType> outlierPartitions = outlierObject.getOutlierPartitions();
                    for (RoleAnalysisOutlierPartitionType outlierPartition : outlierPartitions) {
                        ObjectReferenceType targetClusterRef = outlierPartition.getTargetClusterRef();
                        String oid = targetClusterRef.getOid();
                        if (clusterOid.equals(oid)) {
                            searchResultList.add(outlier);
                            break;
                        }
                    }

                    return true;
                };

                try {
                    modelService.searchObjectsIterative(RoleAnalysisOutlierType.class, null, resultHandler,
                            null, task, result);
                } catch (Exception ex) {
                    throw new RuntimeException("Couldn't search outliers", ex);
                }

                if (searchResultList == null || searchResultList.isEmpty()) {
                    return;
                }

                outliersCount = searchResultList.size();
                for (int i = 0; i < searchResultList.size(); i++) {
                    PrismObject<RoleAnalysisOutlierType> outlierTypePrismObject = searchResultList.get(i);
                    RoleAnalysisOutlierType outlierObject = outlierTypePrismObject.asObjectable();
                    List<RoleAnalysisOutlierPartitionType> outlierPartitions = outlierObject.getOutlierPartitions();
                    for (RoleAnalysisOutlierPartitionType outlierPartition : outlierPartitions) {
                        RoleAnalysisPartitionAnalysisType partitionAnalysis = outlierPartition.getPartitionAnalysis();

                        List<DetectedAnomalyResult> outlierStatResult = outlierPartition.getDetectedAnomalyResult();
                        anomalyAssignmentCount = outlierStatResult.size();
                        Double clusterConfidence = partitionAnalysis.getOverallConfidence();
                        String formattedConfidence = String.format("%.2f", clusterConfidence);
                        String label;

                        ObjectReferenceType targetClusterRef = outlierPartition.getTargetClusterRef();
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
                                OutlierObjectModel outlierObjectModel = null;

                                PageBase pageBase = getPageBase();
                                RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
                                Task task = pageBase.createSimpleTask("loadOutlierDetails");
                                ObjectReferenceType targetSessionRef = outlierPartition.getTargetSessionRef();
                                PrismObject<RoleAnalysisSessionType> sessionTypeObject = roleAnalysisService
                                        .getSessionTypeObject(targetSessionRef.getOid(), task, task.getResult());
                                if (sessionTypeObject == null) {
                                    return;
                                }
                                RoleAnalysisSessionType sessionType = sessionTypeObject.asObjectable();
                                RoleAnalysisProcessModeType processMode = sessionType.getAnalysisOption().getProcessMode();

                                ObjectReferenceType targetClusterRef = outlierPartition.getTargetClusterRef();
                                PrismObject<RoleAnalysisClusterType> clusterTypeObject = roleAnalysisService
                                        .getClusterTypeObject(targetClusterRef.getOid(), task, task.getResult());
                                if (clusterTypeObject == null) {
                                    return;
                                }
                                RoleAnalysisClusterType cluster = clusterTypeObject.asObjectable();
                                if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
                                    outlierObjectModel = generateUserOutlierResultModel(
                                            roleAnalysisService, outlierObject, task, task.getResult(), outlierPartition, getPageBase());
                                } else {
                                    //TODO
                                }

                                if (outlierObjectModel == null) {
                                    return;
                                }
                                String outlierName = outlierObjectModel.getOutlierName();
                                double outlierConfidence = outlierObjectModel.getOutlierConfidence();
                                String outlierDescription = outlierObjectModel.getOutlierDescription();
                                String timeCreated = outlierObjectModel.getTimeCreated();

                                OutlierObjectModel finalOutlierObjectModel = outlierObjectModel;
                                OutlierResultPanel detailsPanel = new OutlierResultPanel(
                                        ((PageBase) getPage()).getMainPopupBodyId(),
                                        createStringResource("RoleAnalysis.aspect.overview.page.title.outlier.details")) {

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
                                        finalOutlierObjectModel.getOutlierItemModels()
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
            }

            @Contract(pure = true)
            @Override
            public @NotNull String getCardBodyCssClass() {
                return " overflow-auto ";
            }

            @Contract(pure = true)
            @Override
            public @NotNull String replaceCardCssClass() {
                return "card p-0";
            }

            @Contract(pure = true)
            @Override
            public @NotNull String getCardBodyStyle() {
                return " height:34vh;";
            }

            @Contract(pure = true)
            @Override
            public @NotNull String replaceBtnToolCssClass() {
                return " position-relative  ml-auto btn btn-primary btn-sm";
            }
        };
        roleAnalysisInfoPatternPanel.setOutputMarkupId(true);
        container.add(roleAnalysisInfoPatternPanel);
    }

    private void initInfoOutlierPanel(@NotNull WebMarkupContainer container) {
        RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();
        IModel<List<IdentifyWidgetItem>> modelPatterns = getClusterWidgetModelOutliers(cluster, getPageBase());

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
                ObjectDetailsModels<RoleAnalysisClusterType> objectDetailsModels = RoleAnalysisClusterAnalysisAspectsPanel.this
                        .getObjectDetailsModels();
                String oid = objectDetailsModels.getObjectType().getOid();
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, oid);
                parameters.add("panelId", "outlierPanel");

                Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                        .getObjectDetailsPage(RoleAnalysisClusterType.class);
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
                return "col-12 pl-0 pr-0";
            }
        };
        panel.setOutputMarkupId(true);
        container.add(panel);
    }

    private void initInfoPatternPanel(@NotNull WebMarkupContainer container) {

        OperationResult result = new OperationResult("loadTopClusterPatterns");
        IModel<List<IdentifyWidgetItem>> modelPatterns = getClusterWidgetModelPatterns(getObjectDetailsModels().getObjectType(),
                result, getPageBase(), 5);
        RoleAnalysisIdentifyWidgetPanel panel = new RoleAnalysisIdentifyWidgetPanel(ID_PATTERNS,
                createStringResource("Pattern.suggestions.title"), modelPatterns) {

            @Override
            protected Component getBodyHeaderPanel(String id) {
                WebMarkupContainer panel = new WebMarkupContainer(id);
                panel.setOutputMarkupId(true);
                return panel;
            }

            @Override
            protected void onClickFooter(AjaxRequestTarget target) {
                ObjectDetailsModels<RoleAnalysisClusterType> objectDetailsModels = RoleAnalysisClusterAnalysisAspectsPanel.this
                        .getObjectDetailsModels();
                String oid = objectDetailsModels.getObjectType().getOid();
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, oid);
                parameters.add("panelId", "detectedPattern");

                Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                        .getObjectDetailsPage(RoleAnalysisClusterType.class);
                getPageBase().navigateToNext(detailsPageClass, parameters);
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

}

