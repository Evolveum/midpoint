package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisPartitionOverviewPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.widgets.model.IdentifyWidgetItem;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisDetectedPatternDetailsPopup;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

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

import static com.evolveum.midpoint.common.mining.utils.ExtractPatternUtils.transformDefaultPattern;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_DETECTED_PATER_ID;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_TABLE_SETTING;

public class RoleAnalysisAspectsWebUtils {

    public static @NotNull IModel<List<IdentifyWidgetItem>> getClusterWidgetModelOutliers(
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull PageBase pageBase) {

        List<RoleAnalysisOutlierType> outliers = loadTopClusterOutliers(cluster.getOid(), pageBase);
        List<IdentifyWidgetItem> detailsModel = new ArrayList<>();
        String targetPartitionOid = cluster.getOid();
        PolyStringType clusterName = cluster.getName();
        boolean isCluster = true;

        return loadOutlierWidgetModels(pageBase, outliers, isCluster, targetPartitionOid, clusterName, detailsModel);
    }

    @NotNull
    private static IModel<List<IdentifyWidgetItem>> loadOutlierWidgetModels(
            @NotNull PageBase pageBase,
            @NotNull List<RoleAnalysisOutlierType> outliers,
            boolean isCluster,
            @NotNull String targetPartitionOid,
            @NotNull PolyStringType clusterName,
            @NotNull List<IdentifyWidgetItem> detailsModel) {
        for (RoleAnalysisOutlierType topFiveOutlier : outliers) {

            Double overallConfidence = 0.0;
            Set<String> anomalies = new HashSet<>();
            List<RoleAnalysisOutlierPartitionType> outlierPartitions = topFiveOutlier.getOutlierPartitions();
            RoleAnalysisOutlierPartitionType clusterPartition = null;
            for (RoleAnalysisOutlierPartitionType outlierPartition : outlierPartitions) {
                String oid;
                if(isCluster) {
                    oid = outlierPartition.getTargetClusterRef().getOid();
                } else {
                    oid = outlierPartition.getTargetSessionRef().getOid();
                }
                if (oid.equals(targetPartitionOid)) {
                    overallConfidence = outlierPartition.getPartitionAnalysis().getOverallConfidence();
                    if (overallConfidence == null) {
                        overallConfidence = 0.0;
                    }

                    clusterPartition = outlierPartition;

                    List<DetectedAnomalyResult> detectedAnomalyResult = outlierPartition.getDetectedAnomalyResult();
                    for (DetectedAnomalyResult detectedAnomaly : detectedAnomalyResult) {
                        if (detectedAnomaly.getTargetObjectRef() != null) {
                            anomalies.add(detectedAnomaly.getTargetObjectRef().getOid());
                        }
                    }
                }
            }

            BigDecimal bd = new BigDecimal(overallConfidence);
            bd = bd.setScale(2, RoundingMode.HALF_UP);
            overallConfidence = bd.doubleValue();
            String description = anomalies.size() + " anomalies were detected in " + clusterName;
            RoleAnalysisOutlierPartitionType finalClusterPartition = clusterPartition;
            IdentifyWidgetItem identifyWidgetItem = new IdentifyWidgetItem(
                    IdentifyWidgetItem.ComponentType.OUTLIER,
                    Model.of(GuiStyleConstants.CLASS_ICON_OUTLIER),
                    Model.of(topFiveOutlier.getName().getOrig()),
                    Model.of(description),
                    Model.of(overallConfidence + "%"),
                    Model.of("name")) {
                @Override
                public void onActionComponentClick(AjaxRequestTarget target) {
                    RoleAnalysisPartitionOverviewPanel panel = new RoleAnalysisPartitionOverviewPanel(
                            pageBase.getMainPopupBodyId(),
                            Model.of(finalClusterPartition), Model.of(topFiveOutlier)) {
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
                public Component createTitleComponent(String id) {
                    AjaxLinkPanel linkPanel = new AjaxLinkPanel(id, Model.of(topFiveOutlier.getName())) {
                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            PageParameters parameters = new PageParameters();
                            String outlierOid = topFiveOutlier.getOid();
                            parameters.add(OnePageParameterEncoder.PARAMETER, outlierOid);
                            Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                                    .getObjectDetailsPage(RoleAnalysisOutlierType.class);
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

    public static @NotNull IModel<List<IdentifyWidgetItem>> getSessionWidgetModelOutliers(
            @NotNull RoleAnalysisSessionType session,
            @NotNull PageBase pageBase) {

        String sessionOid = session.getOid();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        Task task = pageBase.createSimpleTask("Get top session outliers");
        OperationResult result = task.getResult();
        List<RoleAnalysisOutlierType> topSessionOutliers = roleAnalysisService.getSessionOutliers(sessionOid, task, result);
        List<RoleAnalysisOutlierType> outliers = topSessionOutliers.subList(0, Math.min(topSessionOutliers.size(), 5));

        List<IdentifyWidgetItem> detailsModel = new ArrayList<>();
        String targetPartitionOid = session.getOid();
        PolyStringType sessionName = session.getName();
        boolean isCluster = false;

        return loadOutlierWidgetModels(pageBase, outliers, isCluster, targetPartitionOid, sessionName, detailsModel);

    }

    public static @NotNull IModel<List<IdentifyWidgetItem>> getClusterWidgetModelPatterns(
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull OperationResult result,
            @NotNull PageBase pageBase,
            @Nullable Integer maxPatternsToShow) {

        List<DetectedPattern> topPatters = transformDefaultPattern(cluster);
        topPatters.sort(Comparator.comparing(DetectedPattern::getMetric).reversed());

        if (maxPatternsToShow != null) {
            topPatters = topPatters.subList(0, Math.min(topPatters.size(), maxPatternsToShow));
        }

        return preparePatternWidgetsModel(result, pageBase, topPatters);
    }

    public static @NotNull IModel<List<IdentifyWidgetItem>> getSessionWidgetModelPatterns(
            @NotNull RoleAnalysisSessionType session,
            @NotNull OperationResult result,
            @NotNull PageBase pageBase,
            @Nullable Integer maxPatternsToShow) {

        List<DetectedPattern> topPatters = getTopSessionPatterns(session, pageBase);
        topPatters.sort(Comparator.comparing(DetectedPattern::getMetric).reversed());

        if (maxPatternsToShow != null) {
            topPatters = topPatters.subList(0, Math.min(topPatters.size(), maxPatternsToShow));
        }

        return preparePatternWidgetsModel(result, pageBase, topPatters);
    }

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
                BigDecimal bd = new BigDecimal(percentagePart);
                bd = bd.setScale(2, RoundingMode.HALF_UP);
                percentagePart = bd.doubleValue();
            }
            String formattedReductionFactorConfidence = String.format("%.0f", percentagePart);
            double itemsConfidence = pattern.getItemsConfidence();
            String formattedItemConfidence = String.format("%.1f", itemsConfidence);
            String description =
                    relationsMetric +
                            "x relationships with a confidence of  " +
                            formattedItemConfidence + "%";

            String name = "Role suggestion #" + (i + 1);
            IdentifyWidgetItem identifyWidgetItem = new IdentifyWidgetItem(
                    IdentifyWidgetItem.ComponentType.PATTERN,
                    Model.of(GuiStyleConstants.CLASS_DETECTED_PATTERN_ICON),
                    Model.of(),
                    Model.of(description),
                    Model.of(formattedReductionFactorConfidence + "%"),
                    Model.of("name")) {

                public Component createValueTitleComponent(String id) {
                    Label label = new Label(id,
                            pageBase.createStringResource("RoleAnalysisIdentifyWidgetPanel.value.title.system.reduction"));
                    label.setOutputMarkupId(true);
                    label.add(new VisibleBehaviour(() -> getValue() != null));
                    return label;
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
                    AjaxLinkPanel linkPanel = new AjaxLinkPanel(id, Model.of(name)) {
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

    public static @NotNull List<DetectedPattern> getTopSessionPatterns(
            @NotNull RoleAnalysisSessionType session,
            @NotNull PageBase pageBase) {
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();

        Task task = pageBase.createSimpleTask("getTopPatterns");
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
        topDetectedPatterns.sort(Comparator.comparing(DetectedPattern::getMetric).reversed());
        return topDetectedPatterns;
    }

    private static @NotNull List<RoleAnalysisOutlierType> loadTopClusterOutliers(
            @NotNull String clusterOid,
            @NotNull PageBase pageBase) {
        Task task = pageBase.createSimpleTask("loadRoleAnalysisInfo");
        ModelService modelService = pageBase.getModelService();
        OperationResult result = task.getResult();

        List<RoleAnalysisOutlierType> searchResultList = new ArrayList<>();
        ResultHandler<RoleAnalysisOutlierType> resultHandler = (outlier, lResult) -> {

            RoleAnalysisOutlierType outlierObject = outlier.asObjectable();
            List<RoleAnalysisOutlierPartitionType> outlierPartitions = outlierObject.getOutlierPartitions();
            for (RoleAnalysisOutlierPartitionType outlierPartition : outlierPartitions) {
                ObjectReferenceType targetClusterRef = outlierPartition.getTargetClusterRef();
                String oid = targetClusterRef.getOid();
                if (clusterOid.equals(oid)) {
                    Double overallConfidence = outlierPartition.getPartitionAnalysis().getOverallConfidence();
                    //sort by confidence
                    outlierObject.setOverallConfidence(overallConfidence); // tmp
                    searchResultList.add(outlier.asObjectable());
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

        searchResultList.sort(Comparator.comparing(RoleAnalysisOutlierType::getOverallConfidence).reversed());
        return searchResultList.subList(0, Math.min(searchResultList.size(), 5));
    }
}
