/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.clustering;

import com.evolveum.midpoint.common.mining.objects.analysis.cache.AttributeAnalysisCache;
import com.evolveum.midpoint.common.mining.objects.analysis.cache.ObjectCategorisationCache;
import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

import org.jetbrains.annotations.NotNull;

/**
 * Implements the standard category clustering operation for role analysis.
 * This class is responsible for executing the clustering operation.
 */
public class StandardClustering implements Clusterable {

    @Override
    public void executeClustering(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull ModelService modelService,
            @NotNull RoleAnalysisSessionType session,
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull AttributeAnalysisCache attributeAnalysisCache,
            @NotNull ObjectCategorisationCache objectCategorisationCache, @NotNull Task task,
            @NotNull OperationResult result) {
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();

        if (processMode != null) {
            switch (processMode) {
                case ROLE -> new RoleBasedClustering()
                        .executeClustering(roleAnalysisService, modelService, session, handler, attributeAnalysisCache, objectCategorisationCache, task, result);
                case USER -> new UserBasedClustering().
                        executeClustering(roleAnalysisService, modelService, session, handler, attributeAnalysisCache, objectCategorisationCache, task, result);
            }
        } else {
            throw new IllegalStateException("Missing process mode in analysis option");
        }
    }

}
