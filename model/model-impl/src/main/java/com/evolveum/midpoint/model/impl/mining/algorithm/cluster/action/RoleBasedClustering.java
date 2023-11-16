/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action;

import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism.*;
import com.evolveum.midpoint.model.impl.mining.utils.RoleAnalysisAlgorithmUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.ClusteringUtils.prepareDataPoints;

/**
 * Implements clustering of roles based process mode.
 * This class is responsible for executing the clustering operation.
 */
public class RoleBasedClustering implements Clusterable {

    /**
     * Executes the clustering operation for role analysis.
     *
     * @param roleAnalysisService The role analysis service for performing operations.
     * @param modelService The model service for performing operations.
     * @param session The role analysis session object to be processed.
     * @param handler The progress increment handler for tracking the execution progress.
     * @param task The task being executed.
     * @param result The operation result to record the outcome.
     * @return A list of PrismObject instances representing the role analysis clusters.
     */
    @Override
    public List<PrismObject<RoleAnalysisClusterType>> executeClustering(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull ModelService modelService,
            @NotNull RoleAnalysisSessionType session,
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull Task task,
            @NotNull OperationResult result) {

        RoleAnalysisSessionOptionType sessionOptionType = session.getRoleModeOptions();

        int minUserOccupancy = sessionOptionType.getPropertiesRange().getMin().intValue();
        int maxUserOccupancy = sessionOptionType.getPropertiesRange().getMax().intValue();

        handler.enterNewStep("Load Data");
        handler.setOperationCountToProcess(1);
        ListMultimap<List<String>, String> chunkMap = loadRoleBasedMultimapData(
                modelService, minUserOccupancy, maxUserOccupancy, sessionOptionType.getQuery(), task, result);
        handler.iterateActualStatus();

        if (chunkMap.isEmpty()) {
            return null;
        }

        handler.enterNewStep("Prepare Data");
        handler.setOperationCountToProcess(1);
        List<DataPoint> dataPoints = prepareDataPoints(chunkMap);
        handler.iterateActualStatus();

        double similarityThreshold = sessionOptionType.getSimilarityThreshold();
        double similarityDifference = 1 - (similarityThreshold / 100);

        if (similarityDifference == 0.00) {
            return new RoleAnalysisAlgorithmUtils().processExactMatch(
                    roleAnalysisService, dataPoints, session, handler, task, result);
        }

        int minUsersOverlap = sessionOptionType.getMinPropertiesOverlap();
        int minRolesCount = sessionOptionType.getMinMembersCount();

        DistanceMeasure distanceMeasure = new JaccardDistancesMeasure(minUsersOverlap);
        DensityBasedClustering<DataPoint> dbscan = new DensityBasedClustering<>(
                similarityDifference, minRolesCount, distanceMeasure, minUsersOverlap);

        List<Cluster<DataPoint>> clusters = dbscan.cluster(dataPoints, handler);

        return new RoleAnalysisAlgorithmUtils().processClusters(roleAnalysisService, dataPoints, clusters, session,
                handler, task, result);

    }

    @NotNull
    private ListMultimap<List<String>, String> loadRoleBasedMultimapData(@NotNull ModelService modelService,
            int minProperties,
            int maxProperties,
            @Nullable SearchFilterType userQuery,
            @NotNull Task task,
            @NotNull OperationResult result) {

        Set<String> existingRolesOidsSet = ClusteringUtils.getExistingRolesOidsSet(modelService, task, result);

        //role //user
        ListMultimap<String, String> roleToUserMap = ClusteringUtils
                .getRoleBasedRoleToUserMap(modelService, userQuery, existingRolesOidsSet, task, result);

        //user //role
        return ClusteringUtils.getRoleBasedUserToRoleMap(minProperties, maxProperties, roleToUserMap);
    }

}
