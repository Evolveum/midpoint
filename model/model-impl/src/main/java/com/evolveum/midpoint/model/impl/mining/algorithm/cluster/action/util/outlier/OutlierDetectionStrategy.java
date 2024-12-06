package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier;

import com.evolveum.midpoint.common.mining.objects.analysis.cache.AttributeAnalysisCache;

import com.evolveum.midpoint.common.mining.objects.analysis.cache.ObjectCategorisationCache;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

public interface OutlierDetectionStrategy {

    void executeAnalysis(@NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull RoleAnalysisSessionType session,
            @NotNull AttributeAnalysisCache userAnalysisCache,
            @NotNull ObjectCategorisationCache objectCategorisationCache,
            @NotNull Task task,
            @NotNull OperationResult result);
}
