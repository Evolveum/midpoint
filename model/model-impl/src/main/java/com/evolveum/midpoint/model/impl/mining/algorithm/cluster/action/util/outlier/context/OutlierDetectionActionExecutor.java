/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.context;

import com.evolveum.midpoint.common.mining.objects.analysis.cache.AttributeAnalysisCache;
import com.evolveum.midpoint.common.mining.objects.analysis.cache.ObjectCategorisationCache;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.inline.BasicOutlierDetectionStrategy;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.outline.OutlineOutlierDetectionStrategy;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutlierDetectionStrategyResolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
            @NotNull AttributeAnalysisCache userAnalysisCache,
            @NotNull ObjectCategorisationCache objectCategorisationCache,
            @NotNull Task task,
            @NotNull OperationResult result) {
        if (session != null && analysisOption.getAnalysisProcedureType().equals(RoleAnalysisProcedureType.OUTLIER_DETECTION)) {
            OutlierDetectionStrategyResolver detectionExecutionUtil = resolveDetectionStrategy(cluster, session);
            detectionExecutionUtil.executeDetection(
                    roleAnalysisService, cluster, session, userAnalysisCache, objectCategorisationCache, task, result);
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
            detectionExecutionUtil = new OutlierDetectionStrategyResolver(new OutlineOutlierDetectionStrategy());
        } else {
            detectionExecutionUtil = new OutlierDetectionStrategyResolver(new BasicOutlierDetectionStrategy());

        }
        return detectionExecutionUtil;
    }

}
