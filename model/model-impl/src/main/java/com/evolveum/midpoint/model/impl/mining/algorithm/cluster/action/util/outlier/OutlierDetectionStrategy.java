package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier;

import com.evolveum.midpoint.common.mining.objects.analysis.AttributePathResult;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

import java.util.Map;

public interface OutlierDetectionStrategy {

    void executeAnalysis(@NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull RoleAnalysisSessionType session,
            @NotNull Map<String, Map<String, AttributePathResult>> userAnalysisCache,
            @NotNull Task task,
            @NotNull OperationResult result);
}
