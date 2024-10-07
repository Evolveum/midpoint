/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.outlier.panel;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.LOGGER;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChunkAction;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChunkMode;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.menu.listGroup.ListGroupMenuItem;
import com.evolveum.midpoint.gui.impl.component.menu.listGroup.MenuItemLinkPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisWidgetsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.WidgetItemModel;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.RoleAnalysisObjectDto;
import com.evolveum.midpoint.web.component.data.RoleAnalysisTable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

public class OutlierClusterItemPanel<T extends Serializable>
        extends BasePanel<ListGroupMenuItem<T>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_LINK = "link";

    private final IModel<RoleAnalysisOutlierPartitionType> partitionModel;
    private final IModel<RoleAnalysisOutlierType> outlierModel;

    public OutlierClusterItemPanel(@NotNull String id,
            @NotNull IModel<ListGroupMenuItem<T>> model,
            @NotNull IModel<RoleAnalysisOutlierPartitionType> selectionModel,
            @NotNull IModel<RoleAnalysisOutlierType> outlierModel) {
        super(id, model);

        this.partitionModel = selectionModel;
        this.outlierModel = outlierModel;
        initLayout();
    }

    private void initLayout() {
        add(AttributeModifier.append(CLASS_CSS, () -> getModelObject().isOpen() ? "open" : null));
        MenuItemLinkPanel<?> link = new MenuItemLinkPanel<>(ID_LINK, getModel(), 0) {
            @Override
            protected boolean isChevronLinkVisible() {
                return false;
            }

            @SuppressWarnings("rawtypes")
            @Override
            protected void onClickPerformed(AjaxRequestTarget target, ListGroupMenuItem item) {
                OutlierClusterItemPanel.this.onClickPerformed(target, getDetailsPanelComponent());
            }
        };
        add(link);
    }

    protected void onClickPerformed(@NotNull AjaxRequestTarget target, @NotNull Component panelComponent) {
        dispatchComponent(target, panelComponent);
    }

    private void dispatchComponent(@NotNull AjaxRequestTarget target, @NotNull Component component) {
        component.replaceWith(buildDetailsPanel(component.getId()));
        target.add(getDetailsPanelComponent());
    }

    private @NotNull Component buildDetailsPanel(@NotNull String id) {
        RoleAnalysisWidgetsPanel detailsPanel = loadDetailsPanel(id);
        detailsPanel.setOutputMarkupId(true);
        return detailsPanel;
    }

    @NotNull
    private RoleAnalysisWidgetsPanel loadDetailsPanel(@NotNull String id) {

        return new RoleAnalysisWidgetsPanel(id, loadDetailsModel()) {
            @Override
            protected @NotNull Component getPanelComponent(String id1) {
                DisplayValueOption displayValueOption = new DisplayValueOption();
                RoleAnalysisClusterType cluster = prepareTemporaryCluster(displayValueOption);
                if (cluster == null) {
                    return super.getPanelComponent(id1);
                }

                RoleAnalysisTable<MiningUserTypeChunk, MiningRoleTypeChunk> table = loadTable(id1, cluster);
                table.setOutputMarkupId(true);
                return table;
            }
        };
    }

    //TODO this is temporary solution for testing
    private @Nullable RoleAnalysisClusterType prepareTemporaryCluster(DisplayValueOption displayValueOption) {
        RoleAnalysisOutlierType outlier = getOutlierModel().getObject();
        RoleAnalysisOutlierPartitionType partition = getPartitionModel().getObject();
        RoleAnalysisPartitionAnalysisType partitionAnalysis = partition.getPartitionAnalysis();
        RoleAnalysisOutlierSimilarObjectsAnalysisResult similarObjectAnalysis = partitionAnalysis.getSimilarObjectAnalysis();
        if (similarObjectAnalysis == null) {
            return null;
        }
        List<ObjectReferenceType> similarObjects = similarObjectAnalysis.getSimilarObjects();
        Set<String> similarObjectOids = similarObjects.stream().map(ObjectReferenceType::getOid).collect(Collectors.toSet());
        String sessionOid = partition.getTargetSessionRef().getOid();
        String userOid = outlier.getTargetObjectRef().getOid();

        PageBase pageBase = getPageBase();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        Task task = pageBase.createSimpleTask("Idk");
        OperationResult result = task.getResult();

        PrismObject<RoleAnalysisSessionType> sessionObject = roleAnalysisService.getSessionTypeObject(sessionOid, task, result);

        if (sessionObject == null) {
            LOGGER.error("Session object is null");
            return null;
        }

        RoleAnalysisSessionType session = sessionObject.asObjectable();
        RoleAnalysisDetectionOptionType defaultDetectionOption = session.getDefaultDetectionOption();

        double minFrequency = 2;
        double maxFrequency = 2;

        if (defaultDetectionOption != null && defaultDetectionOption.getStandardDeviation() != null) {
            RangeType frequencyRange = defaultDetectionOption.getStandardDeviation();
            if (frequencyRange.getMin() != null) {
                minFrequency = frequencyRange.getMin().intValue();
            }
            if (frequencyRange.getMax() != null) {
                maxFrequency = frequencyRange.getMax().intValue();
            }
        }

        displayValueOption.setProcessMode(RoleAnalysisProcessModeType.USER);
        displayValueOption.setChunkMode(RoleAnalysisChunkMode.EXPAND);
        displayValueOption.setSortMode(RoleAnalysisSortMode.JACCARD);
        displayValueOption.setChunkAction(RoleAnalysisChunkAction.EXPLORE_DETECTION);
        RoleAnalysisClusterType cluster = new RoleAnalysisClusterType();
        for (String element : similarObjectOids) {
            cluster.getMember().add(new ObjectReferenceType()
                    .oid(element).type(UserType.COMPLEX_TYPE));
        }

        RoleAnalysisDetectionOptionType detectionOption = new RoleAnalysisDetectionOptionType();
        detectionOption.setStandardDeviation(new RangeType().min(minFrequency).max(maxFrequency));
        cluster.setDetectionOption(detectionOption);

        MiningOperationChunk miningOperationChunk = roleAnalysisService.prepareBasicChunkStructure(cluster, null,
                displayValueOption, RoleAnalysisProcessModeType.USER, null, result, task);

        RangeType standardDeviation = detectionOption.getFrequencyRange();
        Double sensitivity = detectionOption.getSensitivity();
        Double frequencyThreshold = detectionOption.getFrequencyThreshold();

        RoleAnalysisSortMode sortMode = displayValueOption.getSortMode();
        if (sortMode == null) {
            displayValueOption.setSortMode(RoleAnalysisSortMode.NONE);
            sortMode = RoleAnalysisSortMode.NONE;
        }

        List<MiningRoleTypeChunk> roles = miningOperationChunk.getMiningRoleTypeChunks(sortMode);

        if (standardDeviation != null) {
            roleAnalysisService.resolveOutliersZScore(roles, standardDeviation, sensitivity, frequencyThreshold);
        }

        cluster.setRoleAnalysisSessionRef(
                new ObjectReferenceType()
                        .type(RoleAnalysisSessionType.COMPLEX_TYPE)
                        .oid(sessionOid)
                        .targetName(session.getName()));
        cluster.setClusterStatistics(new AnalysisClusterStatisticType()
                .rolesCount(roles.size())
                .usersCount(similarObjectOids.size()));

        cluster.setDescription(userOid);
        return cluster;
    }

    @NotNull
    private RoleAnalysisTable<MiningUserTypeChunk, MiningRoleTypeChunk> loadTable(
            String id,
            @NotNull RoleAnalysisClusterType cluster) {

        LoadableModel<RoleAnalysisObjectDto> miningOperationChunk = new LoadableModel<>(false) {

            @Contract(" -> new")
            @Override
            protected @NotNull RoleAnalysisObjectDto load() {
                return new RoleAnalysisObjectDto(cluster, new ArrayList<>(), 0, getPageBase());

            }
        };

        RoleAnalysisTable<MiningUserTypeChunk, MiningRoleTypeChunk> table = new RoleAnalysisTable<>(
                id,
                miningOperationChunk);

        table.setOutputMarkupId(true);
        return table;
    }

    protected @NotNull Component getDetailsPanelComponent() {
        return getPageBase().get("form").get("panel");
    }

    public IModel<RoleAnalysisOutlierPartitionType> getPartitionModel() {
        return partitionModel;
    }

    public IModel<RoleAnalysisOutlierType> getOutlierModel() {
        return outlierModel;
    }

    private @NotNull IModel<List<WidgetItemModel>> loadDetailsModel() {
        RoleAnalysisOutlierPartitionType partition = getPartitionModel().getObject();
        RoleAnalysisPartitionAnalysisType partitionAnalysis = partition.getPartitionAnalysis();
        RoleAnalysisOutlierSimilarObjectsAnalysisResult similarObjectAnalysis = partitionAnalysis.getSimilarObjectAnalysis();

        if (similarObjectAnalysis == null) {
            return Model.ofList(List.of());
        }
        List<WidgetItemModel> detailsModel = List.of(
                new WidgetItemModel(createStringResource(""),
                        Model.of("")) {
                    @Override
                    public Component createValueComponent(String id) {
                        Label label = new Label(id, similarObjectAnalysis.getSimilarObjectsCount());
                        label.add(AttributeModifier.append(CLASS_CSS, " h4"));
                        return label;
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        return new LabelWithHelpPanel(id, createStringResource("Similar objects")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisOutlierType.anomalyCount.help");
                            }
                        };
                    }
                },

                new WidgetItemModel(createStringResource(""),
                        Model.of("")) {
                    @Override
                    public Component createValueComponent(String id) {
                        Double similarObjectsDensity = similarObjectAnalysis.getSimilarObjectsDensity();
                        if (similarObjectsDensity == null) {
                            similarObjectsDensity = 0.0;
                        }
                        BigDecimal value = BigDecimal.valueOf(similarObjectsDensity);
                        value = value.setScale(2, RoundingMode.HALF_UP);
                        double doubleValue = value.doubleValue();
                        Label label = new Label(id, doubleValue + "%");
                        label.add(AttributeModifier.append(CLASS_CSS, " h4"));
                        return label;
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        return new LabelWithHelpPanel(id,
                                createStringResource("Density")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisOutlierType.density.help");
                            }
                        };
                    }
                },

                new WidgetItemModel(createStringResource(""),
                        Model.of("Sort")) {
                    @Override
                    public Component createValueComponent(String id) {
                        Double outlierAssignmentFrequencyConfidence = partitionAnalysis.getOutlierAssignmentFrequencyConfidence();
                        if (outlierAssignmentFrequencyConfidence == null) {
                            outlierAssignmentFrequencyConfidence = 0.0;
                        }
                        BigDecimal value = BigDecimal.valueOf(outlierAssignmentFrequencyConfidence);
                        value = value.setScale(2, RoundingMode.HALF_UP);
                        double doubleValue = value.doubleValue();
                        Label label = new Label(id, doubleValue + "%");
                        label.add(AttributeModifier.append(CLASS_CSS, " h4"));
                        return label;
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        return new LabelWithHelpPanel(id, Model.of("Assignments frequency")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisOutlierType.anomalyAverageConfidence.help");
                            }
                        };
                    }
                },

                new WidgetItemModel(createStringResource(""),
                        Model.of("Chart")) {
                    @Override
                    public Component createValueComponent(String id) {
                        Label label = new Label(id, similarObjectAnalysis.getSimilarObjectsThreshold() + "%");
                        label.add(AttributeModifier.append(CLASS_CSS, " h4"));
                        return label;
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        return new LabelWithHelpPanel(id, Model.of("Similarity threshold")) {
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

}
