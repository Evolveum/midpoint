/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel;

import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A Data Transfer Object representing a role analysis partition details and its associated outlier.
 *
 * <p>Used in session outlier partition tables.</p>
 */
public class PartitionObjectDto implements Serializable {

    public static final String F_ASSOCIATED_OUTLIER_NAME = "associatedOutlierName";
    public static final String F_OUTLIER_PARTITION_SCORE = "outlierPartitionScore";
    public static final String F_ANOMALY_ACCESS_COUNT = "anomalyAccessCount";
    public static final String F_CLUSTER_LOCATION_NAME = "clusterLocationName";

    String associatedOutlierName;
    String clusterLocationName;

    double outlierPartitionScore;

    double anomalyAccessCount;

    transient RoleAnalysisOutlierPartitionType partition;
    transient RoleAnalysisOutlierType outlier;

    public PartitionObjectDto(@NotNull RoleAnalysisOutlierType outlier, @NotNull RoleAnalysisOutlierPartitionType partition) {
        this.outlier = outlier;
        this.partition = partition;
        this.associatedOutlierName = outlier.getName().getOrig();
        this.outlierPartitionScore = partition.getPartitionAnalysis().getOverallConfidence();
        List<DetectedAnomalyResult> detectedAnomalyResult = partition.getDetectedAnomalyResult();
        this.anomalyAccessCount = detectedAnomalyResult != null ? detectedAnomalyResult.size() : 0;
        ObjectReferenceType clusterRef = partition.getClusterRef();
        if (clusterRef != null) {
            PolyStringType targetName = clusterRef.getTargetName();
            this.clusterLocationName = targetName != null ? targetName.getOrig() : "N/A";
        }
    }

    /**
     * Constructs a new {@code PartitionObjectDto} list instance
     *
     * @param roleAnalysisService The service used to retrieve role analysis data.
     * @param session The role analysis session associated with the outlier partitions.
     * @param limit The maximum number of partitions to retrieve.
     * @param task The task in which the operation is performed.
     * @param result The operation result.
     * @return A list of PartitionObjectDto representing the outlier partitions details and their associated outliers.
     */
    public static List<PartitionObjectDto> buildPartitionObjectList(@NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisSessionType session,
            Integer limit,
            Task task,
            OperationResult result) {
        Map<RoleAnalysisOutlierPartitionType, RoleAnalysisOutlierType> sessionOutlierPartitionsMap = roleAnalysisService
                .getSessionOutlierPartitionsMap(session.getOid(), limit, true, task, result);

        return sessionOutlierPartitionsMap.entrySet().stream()
                .map(entry -> new PartitionObjectDto(entry.getValue(), entry.getKey()))
                .collect(Collectors.toList());
    }

    public String getAssociatedOutlierName() {
        return associatedOutlierName;
    }

    public double getOutlierPartitionScore() {
        return outlierPartitionScore;
    }

    public double getAnomalyAccessCount() {
        return anomalyAccessCount;
    }

    public RoleAnalysisOutlierPartitionType getPartition() {
        return partition;
    }

    public RoleAnalysisOutlierType getOutlier() {
        return outlier;
    }

    public String getClusterLocationName() {
        return clusterLocationName;
    }
}
