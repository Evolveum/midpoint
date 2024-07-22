package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier;

import com.evolveum.midpoint.common.mining.objects.analysis.AttributePathResult;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class OutlierDetectionStrategyResolver {

    private final OutlierDetectionStrategy detectionStrategy;

    public OutlierDetectionStrategyResolver(OutlierDetectionStrategy detectionStrategy) {
        this.detectionStrategy = detectionStrategy;
    }

    /**
     * Processes the outliers analysis for the specified role analysis session cluster.
     * This method is used to analyze and import outliers in the role analysis session cluster.
     *
     * @param roleAnalysisService The role analysis service for performing role analysis operations.
     * @param cluster The role analysis cluster to process.
     * @param session The role analysis session.
     * @param userAnalysisCache
     * @param task The current task.
     * @param result The operation result.
     */
    public void executeDetection(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull RoleAnalysisSessionType session,
            @NotNull Map<String, Map<String, AttributePathResult>> userAnalysisCache,
            @NotNull Task task,
            @NotNull OperationResult result) {
        detectionStrategy.executeAnalysis(roleAnalysisService, cluster, session, userAnalysisCache, task, result);
    }

}
