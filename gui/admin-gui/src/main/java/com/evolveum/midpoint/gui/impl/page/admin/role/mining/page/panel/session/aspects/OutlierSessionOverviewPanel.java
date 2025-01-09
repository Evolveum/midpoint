/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.aspects;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.*;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBarPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.MetricValuePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
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
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "sessionOutlierOverView", defaultContainerPath = "empty")
@PanelInstance(identifier = "sessionOutlierOverView",
        applicableForType = RoleAnalysisSessionType.class,
        defaultPanel = true,
        display = @PanelDisplay(
                label = "RoleAnalysis.overview.panel",
                icon = GuiStyleConstants.CLASS_LINE_CHART_ICON,
                order = 10))
public class OutlierSessionOverviewPanel extends AbstractObjectMainPanel<RoleAnalysisSessionType, ObjectDetailsModels<RoleAnalysisSessionType>> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_HEADER_ITEMS = "header-items";
    private static final String ID_TABLE = "table";

    private static final String FLEX_SHRINK_GROW = "flex-shrink-1 flex-grow-1 p-0";

    int userOutlierCount = 0;
    int assignmentAnomalyCount = 0;
    int topConfidence = 0;

    public OutlierSessionOverviewPanel(String id, ObjectDetailsModels<RoleAnalysisSessionType> model, ContainerPanelConfigurationType config) {
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
        Task task = getPageBase().createSimpleTask("Get top session outliers");
        OperationResult result = task.getResult();

        initOutlierPartNew(roleAnalysisService, session, task, result, sessionStatistic, container);

        RoleAnalysisViewAllPanel<String> outlierOverviewTable = buildOutlierOverviewTable(session);
        container.add(outlierOverviewTable);

    }

    private @NotNull RoleAnalysisViewAllPanel<String> buildOutlierOverviewTable(RoleAnalysisSessionType session) {
        RoleAnalysisViewAllPanel<String> accessPanel = new RoleAnalysisViewAllPanel<>(ID_TABLE,
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
                        OutlierSessionOverviewPanel.this.getPageBase(),
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

