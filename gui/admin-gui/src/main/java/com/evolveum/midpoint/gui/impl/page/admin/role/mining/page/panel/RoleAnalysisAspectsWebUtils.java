package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisPartitionOverviewPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.widgets.model.IdentifyWidgetItem;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisDetectedPatternDetailsPopup;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.explainPartition;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_DETECTED_PATER_ID;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_TABLE_SETTING;

public class RoleAnalysisAspectsWebUtils {

    private RoleAnalysisAspectsWebUtils() {
    }

    public static @NotNull IModel<List<IdentifyWidgetItem>> getClusterWidgetModelOutliers(
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull PageBase pageBase) {

        List<IdentifyWidgetItem> detailsModel = new ArrayList<>();
        PolyStringType clusterName = cluster.getName();

        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        String clusterOid = cluster.getOid();
        Task task = pageBase.createSimpleTask("loadOutliers");
        OperationResult result = task.getResult();
        Map<RoleAnalysisOutlierPartitionType, RoleAnalysisOutlierType> clusterTopOutliers = roleAnalysisService
                .getClusterOutlierPartitionsMap(clusterOid, 5, true, null, task, result);

        return loadOutlierWidgetModels(pageBase, clusterTopOutliers, clusterName, detailsModel, task, result);
    }

    @NotNull
    public static IModel<List<IdentifyWidgetItem>> loadOutlierWidgetModels(
            @NotNull PageBase pageBase,
            Map<RoleAnalysisOutlierPartitionType, RoleAnalysisOutlierType> outliers,
            @NotNull PolyStringType clusterName,
            @NotNull List<IdentifyWidgetItem> detailsModel,
            @NotNull Task task,
            @NotNull OperationResult result) {
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();

        for (Map.Entry<RoleAnalysisOutlierPartitionType, RoleAnalysisOutlierType> entry : outliers.entrySet()) {
            RoleAnalysisOutlierPartitionType outlierPartition = entry.getKey();
            RoleAnalysisOutlierType outlierObject = entry.getValue();

            Double partitionOverallConfidence = 0.0;
            RoleAnalysisPartitionAnalysisType partitionAnalysis = outlierPartition.getPartitionAnalysis();
            if (partitionAnalysis != null) {
                partitionOverallConfidence = partitionAnalysis.getOverallConfidence();
            }
            Set<String> anomalies = new HashSet<>();
            loadAnomalySet(outlierPartition, anomalies);

            BigDecimal bd = BigDecimal.valueOf(partitionOverallConfidence);
            bd = bd.setScale(2, RoundingMode.HALF_UP);
            partitionOverallConfidence = bd.doubleValue();
            String formattedConfidence = String.format("%.2f", partitionOverallConfidence);
            @NotNull Model<String> description = explainPartition(
                    roleAnalysisService, outlierPartition, true, task, result);
            IdentifyWidgetItem identifyWidgetItem = new IdentifyWidgetItem(
                    IdentifyWidgetItem.ComponentType.OUTLIER,
                    Model.of(GuiStyleConstants.CLASS_ICON_OUTLIER),
                    Model.of(outlierObject.getName().getOrig()),
                    description,
                    Model.of(formattedConfidence + "%"),
                    Model.of("name")) {
                @Override
                public void onActionComponentClick(AjaxRequestTarget target) {
                    RoleAnalysisPartitionOverviewPanel panel = new RoleAnalysisPartitionOverviewPanel(
                            pageBase.getMainPopupBodyId(),
                            Model.of(outlierPartition), Model.of(outlierObject)) {
                        @Override
                        public IModel<String> getTitle() {
                            return createStringResource(
                                    "RoleAnalysisPartitionOverviewPanel.title.most.impact.partition");
                        }
                    };
                    panel.setOutputMarkupId(true);
                    pageBase.showMainPopup(panel, target);
                }

                @Override
                public @NotNull Component createValueTitleComponent(String id) {
                    Label label = new Label(id, Model.of());
                    label.add(AttributeModifier.append(CLASS_CSS, "fa fa-level-up fa-sm text-danger"));
                    label.add(new VisibleBehaviour(() -> getDescription() != null));
                    label.setOutputMarkupId(true);
                    return label;
                }

                @Override
                public Component createTitleComponent(String id) {
                    AjaxLinkPanel linkPanel = new AjaxLinkPanel(id, Model.of(outlierObject.getName())) {
                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            DetailsPageUtil.dispatchToObjectDetailsPage(outlierObject.asPrismObject(), this);
                        }
                    };
                    linkPanel.setOutputMarkupId(true);
                    return linkPanel;

                }
            };
            detailsModel.add(identifyWidgetItem);
        }

        return Model.ofList(detailsModel);
    }

    private static void loadAnomalySet(RoleAnalysisOutlierPartitionType outlierPartition, Set<String> anomalies) {
        List<DetectedAnomalyResult> detectedAnomalyResult = outlierPartition.getDetectedAnomalyResult();
        for (DetectedAnomalyResult detectedAnomaly : detectedAnomalyResult) {
            if (detectedAnomaly.getTargetObjectRef() != null) {
                anomalies.add(detectedAnomaly.getTargetObjectRef().getOid());
            }
        }
    }

    public static @NotNull IModel<List<IdentifyWidgetItem>> getClusterWidgetModelPatterns(
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull OperationResult result,
            @NotNull PageBase pageBase,
            @Nullable Integer maxPatternsToShow) {

        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        List<DetectedPattern> topPatters = roleAnalysisService.getClusterRoleSuggestions(
                cluster.getOid(),
                maxPatternsToShow,
                true,
                result);

        return preparePatternWidgetsModel(result, pageBase, topPatters);
    }

    public static @NotNull IModel<List<IdentifyWidgetItem>> getSessionWidgetModelPatterns(
            @NotNull RoleAnalysisSessionType session,
            @NotNull OperationResult result,
            @NotNull PageBase pageBase,
            @Nullable Integer maxPatternsToShow) {

        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        List<DetectedPattern> topPatters = roleAnalysisService.getSessionRoleSuggestion(
                session.getOid(),
                maxPatternsToShow,
                true,
                result);

        return preparePatternWidgetsModel(result, pageBase, topPatters);
    }

    //TODO localizations
    @NotNull
    private static IModel<List<IdentifyWidgetItem>> preparePatternWidgetsModel(
            @NotNull OperationResult result,
            @NotNull PageBase pageBase,
            @NotNull List<DetectedPattern> topPatters) {
        List<IdentifyWidgetItem> detailsModel = new ArrayList<>();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        int allUserOwnedRoleAssignments = roleAnalysisService.countUserOwnedRoleAssignment(result);
        for (int i = 0; i < topPatters.size(); i++) {
            DetectedPattern pattern = topPatters.get(i);
            double relationsMetric = pattern.getMetric();
            double percentagePart = 0;
            if (relationsMetric != 0 && allUserOwnedRoleAssignments != 0) {
                percentagePart = (relationsMetric / allUserOwnedRoleAssignments) * 100;
                BigDecimal bd = BigDecimal.valueOf(percentagePart);
                bd = bd.setScale(2, RoundingMode.HALF_UP);
                percentagePart = bd.doubleValue();
            }
            String formattedReductionFactorConfidence = String.format("%.2f", percentagePart);
            double itemsConfidence = pattern.getItemsConfidence();
            String formattedItemConfidence = String.format("%.2f", itemsConfidence);
            String description =
                    relationsMetric +
                            "x relationships with a attribute score of  " +
                            formattedItemConfidence + "%";

            String patternName = "Role suggestion #" + (i + 1);
            IdentifyWidgetItem identifyWidgetItem = new IdentifyWidgetItem(
                    IdentifyWidgetItem.ComponentType.PATTERN,
                    Model.of(GuiStyleConstants.CLASS_DETECTED_PATTERN_ICON),
                    Model.of(),
                    Model.of(description),
                    Model.of(formattedReductionFactorConfidence + "%"),
                    Model.of("name")) {

                @Override
                public @NotNull Component createValueTitleComponent(String id) {
                    Label label = new Label(id, Model.of());
                    label.add(AttributeModifier.append(CLASS_CSS, "fa fa-arrow-down fa-sm text-success lh-1"));
                    label.add(new VisibleBehaviour(() -> getDescription() != null));
                    label.setOutputMarkupId(true);
                    return label;
                }

                @Override
                public @NotNull Component createScoreComponent(String id) {
                    Component scoreComponent = super.createScoreComponent(id);
                    scoreComponent.add(AttributeModifier.replace("class", "text-success"));
                    return scoreComponent;
                }

                @Override
                public void onActionComponentClick(AjaxRequestTarget target) {
                    RoleAnalysisDetectedPatternDetailsPopup component = new RoleAnalysisDetectedPatternDetailsPopup(
                            pageBase.getMainPopupBodyId(),
                            Model.of(pattern));
                    pageBase.showMainPopup(component, target);
                }

                @Override
                public Component createTitleComponent(String id) {
                    AjaxLinkPanel linkPanel = new AjaxLinkPanel(id, Model.of(patternName)) {
                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            PageParameters parameters = new PageParameters();
                            String clusterOid = pattern.getClusterRef().getOid();
                            parameters.add(OnePageParameterEncoder.PARAMETER, clusterOid);
                            parameters.add("panelId", "clusterDetails");
                            parameters.add(PARAM_DETECTED_PATER_ID, pattern.getId());
                            StringValue fullTableSetting = pageBase.getPageParameters().get(PARAM_TABLE_SETTING);
                            if (fullTableSetting != null && fullTableSetting.toString() != null) {
                                parameters.add(PARAM_TABLE_SETTING, fullTableSetting.toString());
                            }

                            Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                                    .getObjectDetailsPage(RoleAnalysisClusterType.class);
                            pageBase.navigateToNext(detailsPageClass, parameters);
                        }
                    };
                    linkPanel.setOutputMarkupId(true);
                    return linkPanel;

                }
            };
            detailsModel.add(identifyWidgetItem);
        }
        return Model.ofList(detailsModel);
    }
}
