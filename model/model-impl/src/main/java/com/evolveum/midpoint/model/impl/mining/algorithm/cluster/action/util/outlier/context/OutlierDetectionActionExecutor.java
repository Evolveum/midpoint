/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.context;

import com.evolveum.midpoint.common.mining.objects.analysis.AttributePathResult;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.BasicOutlierDetectionStrategy;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.ClusteringOutlierDetectionStrategy;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutlierDetectionStrategyResolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.Map;

//TODO
public class OutlierDetectionActionExecutor {

    private final RoleAnalysisService roleAnalysisService;

    public OutlierDetectionActionExecutor(RoleAnalysisService roleAnalysisService) {
        this.roleAnalysisService = roleAnalysisService;
    }

    public void executeOutlierDetection(
            @NotNull RoleAnalysisClusterType cluster,
            @Nullable RoleAnalysisSessionType session,
            @NotNull RoleAnalysisOptionType analysisOption,
            @NotNull Map<String, Map<String, AttributePathResult>> userAnalysisCache,
            @NotNull Task task,
            @NotNull OperationResult result) {
        if (session != null && analysisOption.getAnalysisCategory().equals(RoleAnalysisCategoryType.OUTLIERS)) {
            RoleAnalysisDetectionOptionType detectionOption = session.getDefaultDetectionOption();
            Double min = detectionOption.getFrequencyRange().getMin();
            if (min == null) {
                detectionOption.getFrequencyRange().setMin(0.01);
            }

            OutlierDetectionStrategyResolver detectionExecutionUtil = resolveDetectionStrategy(cluster, session);

            detectionExecutionUtil.executeDetection(roleAnalysisService, cluster, session, userAnalysisCache, task, result);
        }

    }

    @NotNull
    private static OutlierDetectionStrategyResolver resolveDetectionStrategy(
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull RoleAnalysisSessionType session) {
        UserAnalysisSessionOptionType userModeOptions = session.getUserModeOptions();
        Boolean detailedAnalysis = false;
        if (userModeOptions != null) {
            detailedAnalysis = userModeOptions.getDetailedAnalysis();
        }

        OutlierDetectionStrategyResolver detectionExecutionUtil;
        if (cluster.getCategory().equals(RoleAnalysisClusterCategory.OUTLIERS) && detailedAnalysis) {
            detectionExecutionUtil = new OutlierDetectionStrategyResolver(new ClusteringOutlierDetectionStrategy());
        } else {
            detectionExecutionUtil = new OutlierDetectionStrategyResolver(new BasicOutlierDetectionStrategy());

        }
        return detectionExecutionUtil;
    }

}
