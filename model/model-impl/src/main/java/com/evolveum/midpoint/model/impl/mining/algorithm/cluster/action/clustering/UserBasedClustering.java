/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.clustering;

import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.ClusteringUtils.*;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism.*;
import com.evolveum.midpoint.model.impl.mining.utils.RoleAnalysisAlgorithmUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserAnalysisSessionOptionType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * Implements clustering of user based process mode.
 * This class is responsible for executing the clustering operation.
 */
public class UserBasedClustering implements Clusterable {

    public static final Trace LOGGER = TraceManager.getTrace(UserBasedClustering.class);


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
    public @NotNull List<PrismObject<RoleAnalysisClusterType>> executeClustering(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull ModelService modelService,
            @NotNull RoleAnalysisSessionType session,
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull Task task,
            @NotNull OperationResult result) {

        UserAnalysisSessionOptionType userModeOptions = session.getUserModeOptions();
        int minRolesOccupancy = userModeOptions.getPropertiesRange().getMin().intValue();
        int maxRolesOccupancy = userModeOptions.getPropertiesRange().getMax().intValue();
        double similarityThreshold = userModeOptions.getSimilarityThreshold();
        double similarityDifference = 1 - (similarityThreshold / 100);
        int minRolesOverlap = userModeOptions.getMinPropertiesOverlap();
        int minUsersCount = userModeOptions.getMinMembersCount();
        Boolean isIndirect = userModeOptions.isIsIndirect();
        SearchFilterType query = userModeOptions.getQuery();

        handler.enterNewStep(LOAD_DATA_STEP);
        handler.setOperationCountToProcess(1);
        //roles //users
        ListMultimap<List<String>, String> chunkMap = loadData(modelService, isIndirect, minRolesOccupancy, maxRolesOccupancy,
                query, result, task
        );

        handler.iterateActualStatus();

        if (chunkMap.isEmpty()) {
            LOGGER.warn("No data to process.");
            return new ArrayList<>();
        }

        handler.enterNewStep(PREPARING_DATA_POINTS_STEP);
        handler.setOperationCountToProcess(1);

        List<DataPoint> dataPoints = prepareDataPoints(chunkMap);
        handler.iterateActualStatus();
        chunkMap.clear();

        DistanceMeasure distanceMeasure = new JaccardDistancesMeasure(minRolesOverlap);
        DensityBasedClustering<DataPoint> dbscan = new DensityBasedClustering<>(similarityDifference,
                minUsersCount, distanceMeasure, minRolesOverlap, ClusteringMode.BALANCED);
        List<Cluster<DataPoint>> clusters = dbscan.cluster(dataPoints, handler);

        return new RoleAnalysisAlgorithmUtils().processClusters(roleAnalysisService, dataPoints, clusters, session,
                handler, task, result);
    }

    @NotNull
    public ListMultimap<List<String>, String> loadData(
            @NotNull ModelService modelService,
            @NotNull Boolean isIndirect,
            int minRolesOccupancy,
            int maxRolesOccupancy,
            @Nullable SearchFilterType query,
            @NotNull OperationResult result,
            @NotNull Task task) {
        if (isIndirect) {
            return loadUserBasedMembershipMultimapData(modelService, minRolesOccupancy,
                    maxRolesOccupancy, query, task, result);
        }
        return loadUserBasedMultimapData(modelService, minRolesOccupancy,
                maxRolesOccupancy, query, task, result);
    }

}
