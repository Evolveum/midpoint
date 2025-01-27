/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel;

import com.evolveum.midpoint.common.outlier.OutlierExplanationResolver;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.RoleAnalysisDetectedAnomalyTable.SORT_ANOMALY_SCORE;

public class AnomalyObjectDto implements Serializable {

    transient RoleAnalysisOutlierType outlier;

    transient Map<String, AnomalyPartitionMap> roleAnomalyMap = new HashMap<>();
    transient AnomalyTableCategory category;
    transient boolean isPartitionCountVisible;

    public record AnomalyPartitionMap(DetectedAnomalyResult anomalyResult,
                                      RoleAnalysisOutlierPartitionType associatedPartition,
                                      List<OutlierExplanationResolver.ExplanationResult> explanation,
                                      int partitionCount,
                                      double anomalyScore) {
    }

    public enum AnomalyTableCategory implements Serializable {

        PARTITION_ANOMALY,
        OUTLIER_OVERVIEW;

        AnomalyTableCategory() {
        }
    }

    public AnomalyObjectDto(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisOutlierType outlier,
            @Nullable RoleAnalysisOutlierPartitionType partition,
            boolean isPartitionCountVisible,
            @NotNull Task task,
            @NotNull OperationResult result) {
        this.isPartitionCountVisible = isPartitionCountVisible;
        intiModels(roleAnalysisService, outlier, partition, task, result);
    }

    public void intiModels(
            @NotNull RoleAnalysisService roleAnalysisService,
            RoleAnalysisOutlierType outlier,
            RoleAnalysisOutlierPartitionType partition,
            @NotNull Task task,
            @NotNull OperationResult result) {
        this.outlier = outlier;
        Map<String, Integer> countPartitionsMap = countPartitions(outlier);
        if (partition != null) {
            this.category = AnomalyTableCategory.PARTITION_ANOMALY;
            initPartitionModel(roleAnalysisService, partition, countPartitionsMap, task, result);
        } else {
            this.category = AnomalyTableCategory.OUTLIER_OVERVIEW;
            initOutlierAnomaliesBasedTopScore(roleAnalysisService, outlier, countPartitionsMap, task, result);
        }
    }

    public Map<String, Integer> countPartitions(@NotNull RoleAnalysisOutlierType outlier) {
        Map<String, Integer> partitionCountMap = new HashMap<>();

        List<RoleAnalysisOutlierPartitionType> partition = outlier.getPartition();
        partition.forEach(partitionType -> {
            List<DetectedAnomalyResult> detectedAnomalyResult = partitionType.getDetectedAnomalyResult();
            detectedAnomalyResult.forEach(detectedAnomalyResult1 -> {
                ObjectReferenceType targetObjectRef = detectedAnomalyResult1.getTargetObjectRef();
                if (targetObjectRef != null) {
                    String oid = targetObjectRef.getOid();
                    partitionCountMap.put(oid, partitionCountMap.getOrDefault(oid, 0) + 1);
                }
            });
        });
        return partitionCountMap;
    }

    public void initPartitionModel(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisOutlierPartitionType partition,
            @NotNull Map<String, Integer> countPartitionsMap,
            @NotNull Task task,
            @NotNull OperationResult result) {
        List<DetectedAnomalyResult> detectedAnomalyResultList = partition.getDetectedAnomalyResult();
        for (DetectedAnomalyResult detectedAnomalyResult : detectedAnomalyResultList) {
            updateAnomalyRecordIfNeeded(roleAnalysisService, partition, detectedAnomalyResult, countPartitionsMap, task, result);
        }
    }

    private void updateAnomalyRecordIfNeeded(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisOutlierPartitionType partition,
            @NotNull DetectedAnomalyResult detectedAnomalyResult,
            @NotNull Map<String, Integer> countPartitionsMap,
            @NotNull Task task,
            @NotNull OperationResult result) {

        ObjectReferenceType targetObjectRef = detectedAnomalyResult.getTargetObjectRef();
        DetectedAnomalyStatistics statistics = detectedAnomalyResult.getStatistics();
        if (targetObjectRef == null || statistics == null) {
            return;
        }

        Double confidence = statistics.getConfidence();
        if (confidence == null) {
            confidence = 0.0;
        }

        String associatedRoleOid = targetObjectRef.getOid();
        AnomalyPartitionMap anomalyPartitionMap = roleAnomalyMap.get(associatedRoleOid);

        List<OutlierExplanationResolver.ExplanationResult> explanationResults = roleAnalysisService
                .explainOutlierAnomalyAccess(detectedAnomalyResult, task, result);

        if (anomalyPartitionMap == null || anomalyPartitionMap.anomalyScore() < confidence) {
            Integer partitionsCount = countPartitionsMap.getOrDefault(associatedRoleOid, 0);
            roleAnomalyMap.put(
                    associatedRoleOid,
                    new AnomalyPartitionMap(detectedAnomalyResult, partition, explanationResults, partitionsCount, confidence));
        }
    }

    public void initOutlierAnomaliesBasedTopScore(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisOutlierType outlier,
            @NotNull Map<String, Integer> countPartitionsMap,
            @NotNull Task task,
            @NotNull OperationResult result) {

        List<RoleAnalysisOutlierPartitionType> partitions = outlier.getPartition();
        if (partitions.isEmpty()) {
            return;
        }
        for (RoleAnalysisOutlierPartitionType partition : partitions) {
            partition.getDetectedAnomalyResult().forEach(detectedAnomalyResult
                    -> updateAnomalyRecordIfNeeded(roleAnalysisService, partition, detectedAnomalyResult, countPartitionsMap, task, result));
        }
    }

    protected @NotNull SelectableBeanObjectDataProvider<RoleType> buildProvider(
            @NotNull Component component,
            @NotNull PageBase pageBase) {

        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();

        return new SelectableBeanObjectDataProvider<>(component, Set.of()) {
            private List<RoleType> roles = new ArrayList<>();

            @SuppressWarnings("rawtypes")
            @Override
            protected List<RoleType> searchObjects(
                    Class type,
                    ObjectQuery query,
                    Collection collection,
                    Task task,
                    OperationResult result) {

                sortByNameIfNeeded(getSort().getProperty(), getSort().isAscending(), roles);

                Integer offset = query.getPaging().getOffset();
                Integer maxSize = query.getPaging().getMaxSize();
                return roles.subList(offset, Math.min(offset + maxSize, roles.size()));
            }

            @Override
            protected Integer countObjects(
                    Class<RoleType> type,
                    ObjectQuery query,
                    Collection<SelectorOptions<GetOperationOptions>> currentOptions,
                    Task task,
                    OperationResult result) {

                String property = getSort().getProperty();
                boolean ascending = getSort().isAscending();

                roles = loadRolesFromAnomalyOidSet(roleAnalysisService, task, property, ascending, result);
                sortByNameIfNeeded(property, ascending, roles);
                return roles.size();
            }
        };
    }

    private static void sortByNameIfNeeded(String property, boolean ascending, List<RoleType> roles) {
        if (property.equals(ObjectType.F_NAME.getLocalPart())) {
            roles.sort((a, b) -> {
                int compare = a.getName().getOrig().compareTo(b.getName().getOrig());
                return ascending ? compare : -compare;
            });
        }
    }

    private @NotNull List<RoleType> loadRolesFromAnomalyOidSet(
            RoleAnalysisService roleAnalysisService,
            Task task,
            String property,
            boolean ascending,
            OperationResult result) {
        List<Map.Entry<String, AnomalyPartitionMap>> toSort = new ArrayList<>(roleAnomalyMap.entrySet());

        if (property.equals(SORT_ANOMALY_SCORE) && ascending) {
            toSort.sort((a, b) -> {
                int compare = Double.compare(b.getValue().anomalyScore(), a.getValue().anomalyScore());
                return -compare;
            });
        } else {
            toSort.sort((a, b) -> Double.compare(b.getValue().anomalyScore(), a.getValue().anomalyScore()));
        }

        List<RoleType> list = new ArrayList<>();
        for (Map.Entry<String, AnomalyPartitionMap> entry : toSort) {
            PrismObject<RoleType> rolePrismObject = roleAnalysisService.getRoleTypeObject(entry.getKey(), task, result);
            if (rolePrismObject != null) {
                RoleType roleObject = rolePrismObject.asObjectable();
                list.add(roleObject);
            }
        }
        return list;
    }

    public boolean isPartitionCountVisible() {
        return false;
    }

    public AnomalyPartitionMap getAnomalyPartitionMap(String oid) {
        return roleAnomalyMap.get(oid);
    }

    public RoleAnalysisOutlierType getOutlier() {
        return outlier;
    }

    public AnomalyTableCategory getCategory() {
        return category;
    }

    public int getPartitionCount(String oid) {
        return roleAnomalyMap.get(oid).partitionCount();
    }

    public double getAnomalyScore(String oid) {
        return roleAnomalyMap.get(oid).anomalyScore();
    }

    public List<OutlierExplanationResolver.ExplanationResult> getExplanation(String oid) {
        return roleAnomalyMap.get(oid).explanation();
    }

    public DetectedAnomalyResult getAnomalyResult(String oid) {
        return roleAnomalyMap.get(oid).anomalyResult();
    }
}
