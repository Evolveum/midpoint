/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.clustering;

import com.evolveum.midpoint.common.mining.objects.analysis.cache.AttributeAnalysisCache;
import com.evolveum.midpoint.common.mining.objects.analysis.cache.ObjectCategorisationCache;
import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.model.api.ModelService;

import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.task.api.Task;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

/**
 * Interface for clustering actions in role analysis.
 */
public interface Clusterable {

    /**
     * Execute the clustering action for role analysis.
     *
     * @param roleAnalysisService The role analysis service for performing operations.
     * @param modelService The model service for performing operations.
     * @param session The role analysis session.
     * @param handler The progress increment handler.
     * @param attributeAnalysisCache The cache for attribute analysis.
     * @param objectCategorisationCache The cache for object categorisation.
     * @param task The task being executed.
     * @param result The operation result.
     * @throws IllegalArgumentException If session is null.
     */
    void executeClustering(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull ModelService modelService,
            @NotNull RoleAnalysisSessionType session,
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull AttributeAnalysisCache attributeAnalysisCache,
            @NotNull ObjectCategorisationCache objectCategorisationCache,
            @NotNull Task task,
            @NotNull OperationResult result);
}
