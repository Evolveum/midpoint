/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.context;

import com.evolveum.midpoint.common.mining.objects.analysis.cache.AttributeAnalysisCache;
import com.evolveum.midpoint.common.mining.objects.analysis.cache.ObjectCategorisationCache;
import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.clustering.*;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Resolves the clustering behavior for role analysis.
 */
public class ClusteringBehavioralResolver implements Clusterable {

    public static final Trace LOGGER = TraceManager.getTrace(ClusteringBehavioralResolver.class);

    @Override
    public @NotNull List<PrismObject<RoleAnalysisClusterType>> executeClustering(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull ModelService modelService,
            @NotNull RoleAnalysisSessionType session,
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull AttributeAnalysisCache attributeAnalysisCache,
            @NotNull ObjectCategorisationCache objectCategorisationCache,
            @NotNull Task task,
            @NotNull OperationResult result) {
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        validateNotNull(analysisOption, "analysis option");

        RoleAnalysisCategoryType analysisCategory = analysisOption.getAnalysisCategory();
        validateNotNull(analysisCategory, "analysis category");

        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();
        validateNotNull(processMode, "process mode");

        List<PrismObject<RoleAnalysisClusterType>> clusteringResult = switch (analysisCategory) {
//            case STANDARD, BALANCED, EXACT, EXPLORATION ->
            case BALANCED, EXACT, EXPLORATION, BIRTHRIGHT ->
                    executeStandardClustering(roleAnalysisService, modelService, session, handler,
                            attributeAnalysisCache, objectCategorisationCache, task, result);
            case ADVANCED, DEPARTMENT, ATTRIBUTE_BASED -> {
                RoleAnalysisProcedureType analysisProcedureType = analysisOption.getAnalysisProcedureType();
                if (analysisProcedureType == RoleAnalysisProcedureType.OUTLIER_DETECTION) {
                    yield executeOutlierClustering(roleAnalysisService, modelService, session, handler,
                            attributeAnalysisCache, objectCategorisationCache, task, result);
                }
                yield executeAdvancedClustering(roleAnalysisService, modelService, session, handler,
                        attributeAnalysisCache, objectCategorisationCache, task, result);
            }
            case OUTLIERS_DEPARTMENT -> executeOutlierClustering(roleAnalysisService, modelService, session, handler,
                    attributeAnalysisCache, objectCategorisationCache, task, result);
        };

        validateNotNull(clusteringResult, "clustering result");

        return clusteringResult;
    }

    private @NotNull List<PrismObject<RoleAnalysisClusterType>> executeOutlierClustering(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull ModelService modelService,
            @NotNull RoleAnalysisSessionType session,
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull AttributeAnalysisCache attributeAnalysisCache,
            @NotNull ObjectCategorisationCache objectCategorisationCache,
            @NotNull Task task,
            @NotNull OperationResult result) {
        return new OutlierClustering()
                .executeClustering(roleAnalysisService, modelService, session, handler,
                        attributeAnalysisCache, objectCategorisationCache, task, result);
    }

    private @NotNull List<PrismObject<RoleAnalysisClusterType>> executeStandardClustering(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull ModelService modelService,
            @NotNull RoleAnalysisSessionType session,
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull AttributeAnalysisCache attributeAnalysisCache,
            @NotNull ObjectCategorisationCache objectCategorisationCache,
            @NotNull Task task,
            @NotNull OperationResult result) {
        return new StandardClustering()
                .executeClustering(roleAnalysisService, modelService, session, handler,
                        attributeAnalysisCache, objectCategorisationCache, task, result);
    }

    private @NotNull List<PrismObject<RoleAnalysisClusterType>> executeAdvancedClustering(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull ModelService modelService,
            @NotNull RoleAnalysisSessionType session,
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull AttributeAnalysisCache attributeAnalysisCache,
            @NotNull ObjectCategorisationCache objectCategorisationCache,
            @NotNull Task task,
            @NotNull OperationResult result) {
        return new AdvancedClustering()
                .executeClustering(roleAnalysisService, modelService, session, handler,
                        attributeAnalysisCache, objectCategorisationCache, task, result);
    }

    private void validateNotNull(Object value, String name) {
        if (value == null) {
            LOGGER.error("Missing {}.", name);
            throw new IllegalStateException("Missing " + name);
        }
    }

}
