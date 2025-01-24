/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.aspects;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.explainOutlier;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.panel.OutlierPartitionPanel.PARAM_ANOMALY_OID;

import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBarPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.util.AccessMetadataUtil;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.*;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;

import java.io.Serial;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@PanelType(name = "outlierOverView", defaultContainerPath = "empty")
@PanelInstance(identifier = "outlierOverView",
        applicableForType = RoleAnalysisOutlierType.class,
        defaultPanel = true,
        display = @PanelDisplay(
                label = "RoleAnalysis.overview.panel",
                icon = GuiStyleConstants.CLASS_LINE_CHART_ICON,
                order = 10))
public class RoleAnalysisOutlierAnalysisAspectsPanel extends AbstractObjectMainPanel<RoleAnalysisOutlierType, ObjectDetailsModels<RoleAnalysisOutlierType>> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_HEADER_ITEMS = "header-items";
    private static final String ID_ACCESS_PANEL = "accessPanel";

    public RoleAnalysisOutlierAnalysisAspectsPanel(
            @NotNull String id,
            @NotNull ObjectDetailsModels<RoleAnalysisOutlierType> model,
            @NotNull ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        //TODO tbd
        getPageBase().getPageParameters().remove(PARAM_ANOMALY_OID);
    }

    protected void initLayout() {
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        ObjectDetailsModels<RoleAnalysisOutlierType> objectDetailsModels = getObjectDetailsModels();

        PageBase pageBase = getPageBase();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        Task task = pageBase.createSimpleTask("loadOutlierDetails");
        OperationResult result = task.getResult();

        initDashboard(roleAnalysisService, container, task, result);

        initAccessPanel(roleAnalysisService, objectDetailsModels, container, task, result);

    }

    private void initAccessPanel(
            RoleAnalysisService roleAnalysisService,
            @NotNull ObjectDetailsModels<RoleAnalysisOutlierType> objectDetailsModels,
            @NotNull WebMarkupContainer container,
            @NotNull Task task,
            @NotNull OperationResult result) {
        RoleAnalysisViewAllPanel<?> accessPanel = new RoleAnalysisViewAllPanel<>(ID_ACCESS_PANEL,
                createStringResource("RoleAnalysis.aspect.overview.page.title.access.anomalies")) {
            @Contract(pure = true)
            @Override
            protected @NotNull String getIconCssClass() {
                return GuiStyleConstants.CLASS_ICON_ASSIGNMENTS;
            }

            @Contract(" -> new")
            @Override
            protected @NotNull IModel<String> getLinkModel() {
                return createStringResource(
                        "RoleAnalysis.aspect.overview.page.title.view.all.access.anomalies");
            }

            @Override
            protected void onLinkClick(AjaxRequestTarget target) {
                RoleAnalysisOutlierType outlier = getObjectDetailsModels().getObjectType();
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, outlier.getOid());
                parameters.add("panelId", "anomalyAccess");
                Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                        .getObjectDetailsPage(RoleAnalysisOutlierType.class);
                ((PageBase) getPage()).navigateToNext(detailsPageClass, parameters);
            }

            @Override
            protected @NotNull Component getPanelComponent(String id) {
                RoleAnalysisOutlierType outlierObject = objectDetailsModels.getObjectType();
                AnomalyObjectDto dto = new AnomalyObjectDto(
                        roleAnalysisService, outlierObject, null, false, task, result);
                RoleAnalysisDetectedAnomalyTable detectedAnomalyTable = new RoleAnalysisDetectedAnomalyTable(id, Model.of(dto));

                detectedAnomalyTable.setOutputMarkupId(true);
                detectedAnomalyTable.add(AttributeAppender.append("style", "min-height: 400px;"));
                return detectedAnomalyTable;
            }
        };

        accessPanel.setOutputMarkupId(true);
        container.add(accessPanel);
    }

    protected void initDashboard(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull WebMarkupContainer container,
            @NotNull Task task,
            @NotNull OperationResult result) {

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
//            protected String getContainerCssClass() {
//                return super.getContainerCssClass();
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
//                iconWithLabel.add(AttributeAppender.append("class", "badge p-3 my-auto justify-content-center flex-grow-1 flex-shrink-1"));
//                iconWithLabel.add(AttributeAppender.append("style", "background-color: #dcf1f4;"));
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
//                                return "fas fa-sync text-muted";
//                            }
//
//                            @Override
//                            protected String getLabelComponentCssClass() {
//                                return "text-muted";
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
//                        label.add(AttributeAppender.append("class", "text-muted"));
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
//                                return "fas fa-trophy text-muted";
//                            }
//
//                            @Override
//                            protected String getLabelComponentCssClass() {
//                                return "text-muted";
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
//                        label.add(AttributeAppender.append("class", "text-muted"));
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
//        statusHeader.add(AttributeAppender.append("class", "pl-0"));
//        cardBodyComponent.add(statusHeader);

        RoleAnalysisOutlierType outlier = getObjectDetailsModels().getObjectType();
        List<RoleAnalysisOutlierPartitionType> outlierPartitions = outlier.getPartition();
        int partitionCount = outlierPartitions.size();
        Set<String> anomalySet = new HashSet<>();
        for (RoleAnalysisOutlierPartitionType outlierPartition : outlierPartitions) {
            List<DetectedAnomalyResult> detectedAnomalyResult = outlierPartition.getDetectedAnomalyResult();
            for (DetectedAnomalyResult anomalyResult : detectedAnomalyResult) {
                anomalySet.add(anomalyResult.getTargetObjectRef().getOid());
            }
        }
        int anomalyCount = anomalySet.size();

        Model<String> finalExplanationTranslatedModel = explainOutlier(
                roleAnalysisService, outlier, false, task, result);
        RoleAnalysisOutlierDashboardPanel<?> characteristicHeader = new RoleAnalysisOutlierDashboardPanel<>(cardBodyComponent.newChildId(),
                createStringResource("RoleAnalysisOutlierAnalysisAspectsPanel.widget.characteristics")) {
            @Contract(pure = true)
            @Override
            protected @NotNull String getIconCssClass() {
                return "fa fa-lock";
            }

            @Override
            protected @NotNull Component getPanelComponent(String id) {
                IconWithLabel iconWithLabel = new IconWithLabel(id, finalExplanationTranslatedModel) {
                    @Contract(pure = true)
                    @Override
                    protected @NotNull String getIconCssClass() {
                        return "fas fa-chart-line";
                    }

                    @Override
                    protected String getComponentCssStyle() {
                        return "font-size: 20px;";
                    }

                    @Override
                    protected String getComponentCssClass() {
                        return super.getComponentCssClass() + " gap-2";
                    }
                };

                iconWithLabel.add(AttributeAppender.append("class", "badge p-3 my-auto justify-content-center flex-grow-1 flex-shrink-1 bg-transparent-red border border-danger text-danger text-wrap"));
                return iconWithLabel;
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
                        Label label = new Label(id, anomalyCount);
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
                                createStringResource("RoleAnalysisOutlierAnalysisAspectsPanel.widget.characteristics.partitions")) {
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
                        Label label = new Label(id, partitionCount);
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
        characteristicHeader.setOutputMarkupId(true);
        cardBodyComponent.add(characteristicHeader);

        RoleAnalysisOutlierDashboardPanel<?> accessHeader = new RoleAnalysisOutlierDashboardPanel<>(cardBodyComponent.newChildId(),
                createStringResource("RoleAnalysisOutlierAnalysisAspectsPanel.widget.access")) {
            @Contract(pure = true)
            @Override
            protected @NotNull String getIconCssClass() {
                return "fa fa-lock";
            }

            @Override
            protected boolean isFooterVisible() {
                return false;
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getPanelContainerCssClass() {
                return "p-0 h-100 border-top w-100";
            }

            @SuppressWarnings("rawtypes")
            @Override
            protected @NotNull Component getPanelComponent(String id) {

                ObjectDetailsModels<RoleAnalysisOutlierType> objectDetailsModels = RoleAnalysisOutlierAnalysisAspectsPanel
                        .this.getObjectDetailsModels();

                RoleAnalysisOutlierType outlier = objectDetailsModels.getObjectType();

                PageBase pageBase = RoleAnalysisOutlierAnalysisAspectsPanel.this.getPageBase();
                RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
                Task simpleTask = pageBase.createSimpleTask("loadOutlierDetails");
                OperationResult result = simpleTask.getResult();
                PrismObject<UserType> prismUser = roleAnalysisService
                        .getUserTypeObject(outlier.getObjectRef().getOid(), simpleTask, result);

                if (prismUser == null) {
                    return new WebMarkupContainer(id);
                }

                UserType user = prismUser.asObjectable();
                List<ObjectReferenceType> refsToRoles = user.getRoleMembershipRef()
                        .stream()
                        .filter(ref -> QNameUtil.match(ref.getType(), RoleType.COMPLEX_TYPE)) //TODO maybe also check relation?
                        .toList();

                int allAssignmentCount = refsToRoles.size();

                //TODO maybe think about collecting oids so you can later show them
                int directAssignment = 0;
                int indirectAssignment = 0;
                int duplicatedRoleAssignmentCount = 0;

                for (ObjectReferenceType ref : refsToRoles) {
                    List<AssignmentPathMetadataType> metadataPaths = AccessMetadataUtil.computeAssignmentPaths(ref);
                    if (metadataPaths.size() == 1) {
                        List<AssignmentPathSegmentMetadataType> segments = metadataPaths.get(0).getSegment();
                        if (CollectionUtils.isEmpty(segments) || segments.size() == 1) {
                            directAssignment++;
                        } else {
                            indirectAssignment++;
                        }
                    } else {
                        boolean foundDirect = false;
                        boolean foundIndirect = false;
                        for (AssignmentPathMetadataType metadata : metadataPaths) {
                            List<AssignmentPathSegmentMetadataType> segments = metadata.getSegment();
                            if (CollectionUtils.isEmpty(segments) || segments.size() == 1) {
                                foundDirect = true;
                                if (foundIndirect) {
                                    indirectAssignment--;
                                    duplicatedRoleAssignmentCount++;
                                } else {
                                    directAssignment++;
                                }

                            } else {
                                foundIndirect = true;
                                if (foundDirect) {
                                    directAssignment--;
                                    duplicatedRoleAssignmentCount++;
                                } else {
                                    indirectAssignment++;
                                }
                            }
                        }
                    }

                }

                double averageAccessPerUser = getAverageAccessPerUser(roleAnalysisService, result, simpleTask);

                int finalDirectAssignment = directAssignment;
                int finalIndirectAssignment = indirectAssignment;
                int finalDuplicatedRoleAssignmentCount = duplicatedRoleAssignmentCount;
                return new RoleAnalysisAccessDistributionPanel(id) { //TODO create model - this overriding is not very good. might be oneliner when the model is properly created
                    @Override
                    protected String getCount() {
                        return String.valueOf(allAssignmentCount);
                    }

                    @Override
                    protected String getAverageCount() {
                        return String.valueOf(averageAccessPerUser);
                    }

                    @Override
                    protected String getDirectCount() {
                        return String.valueOf(finalDirectAssignment);
                    }

                    @Override
                    protected String getIndirectCount() {
                        return String.valueOf(finalIndirectAssignment);
                    }

                    @Override
                    protected String getDuplicatedCount() {
                        return String.valueOf(finalDuplicatedRoleAssignmentCount);
                    }

                    @Override
                    protected Component getPanelComponent(String id1) {

                        List<ProgressBar> progressBars = new ArrayList<>();
                        addProgressBar(progressBars, ProgressBar.State.SUCCESS, finalDirectAssignment, allAssignmentCount);
                        addProgressBar(progressBars, ProgressBar.State.WARNING, finalIndirectAssignment, allAssignmentCount);
                        addProgressBar(progressBars, ProgressBar.State.DANGER, finalDuplicatedRoleAssignmentCount, allAssignmentCount);

                        ProgressBarPanel components1 = new ProgressBarPanel(id1, new LoadableModel<>() {
                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            protected List<ProgressBar> load() {
                                return progressBars;
                            }
                        });
                        components1.add(AttributeAppender.append("class", "p-0 m-3 justify-content-center"));
                        return components1;
                    }
                };
            }

        };

        accessHeader.setOutputMarkupId(true);
        cardBodyComponent.add(accessHeader);
    }

    private static double getAverageAccessPerUser(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull OperationResult result,
            @NotNull Task simpleTask) {
        int numberOfRoleToUserAssignment = roleAnalysisService.countUserOwnedRoleAssignment(result);

        int finalUsersInSystem = roleAnalysisService.countObjects(UserType.class, null, null, simpleTask, result);

        double averagePerUser = finalUsersInSystem > 0
                ? (double) numberOfRoleToUserAssignment / finalUsersInSystem
                : 0.0;

        BigDecimal averagePerUserRounded = BigDecimal.valueOf(averagePerUser)
                .setScale(2, RoundingMode.HALF_UP);
        averagePerUser = averagePerUserRounded.doubleValue();
        return averagePerUser;
    }

    private void addProgressBar(@NotNull List<ProgressBar> list, @NotNull ProgressBar.State state, int value, int totalValue) {
        //disabled legend
        list.add(new ProgressBar(value * 100 / (double) totalValue, state));
    }

}

