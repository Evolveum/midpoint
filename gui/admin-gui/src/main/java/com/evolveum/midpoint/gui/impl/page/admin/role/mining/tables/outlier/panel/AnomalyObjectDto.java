/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel;

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

public class AnomalyObjectDto implements Serializable {

    transient RoleAnalysisOutlierType outlier;

    transient Map<String, AnomalyPartitionMap> roleAnomalyMap = new HashMap<>();
    transient AnomalyTableCategory category;
    transient boolean isPartitionCountVisible;

    public record AnomalyPartitionMap(DetectedAnomalyResult anomalyResult,
                                      RoleAnalysisOutlierPartitionType associatedPartition,
                                      List<OutlierDetectionExplanationType> explanation,
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
            @NotNull RoleAnalysisOutlierType outlier,
            @Nullable RoleAnalysisOutlierPartitionType partition,
            boolean isPartitionCountVisible) {
        this.isPartitionCountVisible = isPartitionCountVisible;
        intiModels(outlier, partition);
    }

    public void intiModels(RoleAnalysisOutlierType outlier, RoleAnalysisOutlierPartitionType partition) {
        this.outlier = outlier;
        Map<String, Integer> countPartitionsMap = countPartitions(outlier);
        if (partition != null) {
            this.category = AnomalyTableCategory.PARTITION_ANOMALY;
            initPartitionModel(partition, countPartitionsMap);
        } else {
            this.category = AnomalyTableCategory.OUTLIER_OVERVIEW;
            initOutlierAnomaliesBasedTopScore(outlier, countPartitionsMap);
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

    public void initPartitionModel(@NotNull RoleAnalysisOutlierPartitionType partition, @NotNull Map<String, Integer> countPartitionsMap) {
        List<DetectedAnomalyResult> detectedAnomalyResultList = partition.getDetectedAnomalyResult();
        for (DetectedAnomalyResult detectedAnomalyResult : detectedAnomalyResultList) {
            updateAnomalyRecordIfNeeded(partition, detectedAnomalyResult, countPartitionsMap);
        }
    }

    private void updateAnomalyRecordIfNeeded(
            @NotNull RoleAnalysisOutlierPartitionType partition,
            @NotNull DetectedAnomalyResult detectedAnomalyResult,
            @NotNull Map<String, Integer> countPartitionsMap) {

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

        List<OutlierDetectionExplanationType> explanation = detectedAnomalyResult.getExplanation();

        if (anomalyPartitionMap == null || anomalyPartitionMap.anomalyScore() < confidence) {
            Integer partitionsCount = countPartitionsMap.getOrDefault(associatedRoleOid, 0);
            roleAnomalyMap.put(
                    associatedRoleOid,
                    new AnomalyPartitionMap(detectedAnomalyResult, partition, explanation, partitionsCount, confidence));
        }
    }

    public void initOutlierAnomaliesBasedTopScore(
            @NotNull RoleAnalysisOutlierType outlier,
            @NotNull Map<String, Integer> countPartitionsMap) {

        List<RoleAnalysisOutlierPartitionType> partitions = outlier.getPartition();
        if (partitions.isEmpty()) {
            return;
        }
        for (RoleAnalysisOutlierPartitionType partition : partitions) {
            partition.getDetectedAnomalyResult().forEach(detectedAnomalyResult
                    -> updateAnomalyRecordIfNeeded(partition, detectedAnomalyResult, countPartitionsMap));
        }
    }

    protected @NotNull SelectableBeanObjectDataProvider<RoleType> buildProvider(
            @NotNull Component component,
            @NotNull PageBase pageBase,
            @NotNull Task task,
            @NotNull OperationResult result) {

        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        List<RoleType> roles = new ArrayList<>();

        loadRolesFromAnomalyOidSet(roleAnalysisService, roles, task, result);

        return new SelectableBeanObjectDataProvider<>(component, Set.of()) {

            @SuppressWarnings("rawtypes")
            @Override
            protected List<RoleType> searchObjects(
                    Class type,
                    ObjectQuery query,
                    Collection collection,
                    Task task,
                    OperationResult result) {
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
                return roles.size();
            }
        };
    }

    private void loadRolesFromAnomalyOidSet(
            RoleAnalysisService roleAnalysisService,
            List<RoleType> roles,
            Task task,
            OperationResult result) {
        roleAnomalyMap.keySet().forEach(oid -> {
            PrismObject<RoleType> rolePrismObject = roleAnalysisService.getRoleTypeObject(oid, task, result);
            if (rolePrismObject != null) {
                roles.add(rolePrismObject.asObjectable());
            }
        });
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

    public List<OutlierDetectionExplanationType> getExplanation(String oid) {
        return roleAnomalyMap.get(oid).explanation();
    }

    public DetectedAnomalyResult getAnomalyResult(String oid) {
        return roleAnomalyMap.get(oid).anomalyResult();
    }
}
