/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action;

import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.ClusteringUtils.*;
import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.object.AttributeMatch.generateMatchingRulesList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.object.AttributeMatch;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * Implements clustering of user based process mode.
 * This class is responsible for executing the clustering operation.
 */
public class UserBasedClustering implements Clusterable {

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

        UserAnalysisSessionOptionType sessionOptionType = session.getUserModeOptions();
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        if (analysisOption == null) {
            return null;
        }
        RoleAnalysisCategoryType analysisCategory = analysisOption.getAnalysisCategory();
        if (analysisCategory == null) {
            return null;
        }

        int minRolesOccupancy = sessionOptionType.getPropertiesRange().getMin().intValue();
        int maxRolesOccupancy = sessionOptionType.getPropertiesRange().getMax().intValue();

        handler.enterNewStep("Load Data");
        handler.setOperationCountToProcess(1);
        //roles //users
        ListMultimap<List<String>, String> chunkMap = loadUserBasedMultimapData(modelService, minRolesOccupancy,
                maxRolesOccupancy, sessionOptionType.getQuery(), task, result);
        handler.iterateActualStatus();
        handler.enterNewStep("Prepare Data");
        handler.setOperationCountToProcess(1);

        if (chunkMap.isEmpty()) {
            return null;
        }

        List<DataPoint> dataPoints;
        List<AttributeMatch> attributeMatches = null;
        boolean rule = false;
        if (analysisCategory.equals(RoleAnalysisCategoryType.ADVANCED)) {
            List<RoleAnalysisMatchingRuleType> matchingRule = session.getMatchingRule();
            attributeMatches = generateMatchingRulesList(matchingRule);
            if (attributeMatches.isEmpty()) {
                dataPoints = prepareDataPoints(chunkMap);
            } else {
                rule = true;
                dataPoints = prepareDataPointsUserModerRules(chunkMap, roleAnalysisService, task, attributeMatches);
            }
        } else {
            dataPoints = prepareDataPoints(chunkMap);
        }
        handler.iterateActualStatus();

        double similarityThreshold = sessionOptionType.getSimilarityThreshold();
        double similarityDifference = 1 - (similarityThreshold / 100);

//        if (similarityDifference == 0.00) {
//            return new RoleAnalysisAlgorithmUtils().processExactMatch(roleAnalysisService, dataPoints, session,
//                    handler, task, result);
//        }

        int minRolesOverlap = sessionOptionType.getMinPropertiesOverlap();
        int minUsersCount = sessionOptionType.getMinMembersCount();
        DistanceMeasure distanceMeasure;

        if (rule) {
            distanceMeasure = new JaccardDistancesMeasure(minRolesOverlap, new HashSet<>(attributeMatches));
        } else {
            distanceMeasure = new JaccardDistancesMeasure(minRolesOverlap);
        }

        DensityBasedClustering<DataPoint> dbscan = new DensityBasedClustering<>(similarityDifference,
                minUsersCount, distanceMeasure, minRolesOverlap, rule);
        List<Cluster<DataPoint>> clusters = dbscan.cluster(dataPoints, handler);

        return new RoleAnalysisAlgorithmUtils().processClusters(roleAnalysisService, dataPoints, clusters, session,
                handler, task, result);
    }

    @NotNull
    private ListMultimap<List<String>, String> loadUserBasedMultimapData(@NotNull ModelService modelService,
            int minProperties,
            int maxProperties,
            @Nullable SearchFilterType userQuery,
            @NotNull Task task,
            @NotNull OperationResult result) {

        Set<String> existingRolesOidsSet = ClusteringUtils.getExistingActiveRolesOidsSet(modelService, task, result);

        //role //user
        return ClusteringUtils.getUserBasedRoleToUserMap(modelService, minProperties, maxProperties,
                userQuery, existingRolesOidsSet, task, result
        );
    }

}
