/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.clustering;

import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.ClusteringUtils.*;
import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.object.RoleAnalysisAttributeDefConvert.generateMatchingRulesList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.evolveum.midpoint.common.mining.objects.analysis.cache.AttributeAnalysisCache;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism.*;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.object.RoleAnalysisAttributeDefConvert;
import com.evolveum.midpoint.model.impl.mining.utils.RoleAnalysisAlgorithmUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * Implements clustering of advanced category.
 * This class is responsible for executing the clustering operation.
 */
public class AdvancedClustering implements Clusterable {

    public static final Trace LOGGER = TraceManager.getTrace(AdvancedClustering.class);

    @Override
    public @NotNull List<PrismObject<RoleAnalysisClusterType>> executeClustering(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull ModelService modelService,
            @NotNull RoleAnalysisSessionType session,
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull AttributeAnalysisCache attributeAnalysisCache,
            @NotNull Task task,
            @NotNull OperationResult result) {

        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();

        if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return executeRoleBasedAdvancedClustering(roleAnalysisService, modelService, session, attributeAnalysisCache, handler, task, result);
        } else {
            return executeUserBasedAdvancedClustering(roleAnalysisService, modelService, session, attributeAnalysisCache, handler, task, result);
        }
    }

    public @NotNull List<PrismObject<RoleAnalysisClusterType>> executeRoleBasedAdvancedClustering(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull ModelService modelService,
            @NotNull RoleAnalysisSessionType session,
            @NotNull AttributeAnalysisCache attributeAnalysisCache,
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull Task task,
            @NotNull OperationResult result) {
        RoleAnalysisSessionOptionType sessionOptionType = session.getRoleModeOptions();
        Boolean isIndirect = sessionOptionType.isIsIndirect();
        int minUserOccupancy = sessionOptionType.getPropertiesRange().getMin().intValue();
        int maxUserOccupancy = sessionOptionType.getPropertiesRange().getMax().intValue();
        int minUsersOverlap = sessionOptionType.getMinPropertiesOverlap();
        int minRolesCount = sessionOptionType.getMinMembersCount();
        double similarityThreshold = sessionOptionType.getSimilarityThreshold();
        double similarityDifference = 1 - (similarityThreshold / 100);
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();

        List<RoleAnalysisAttributeDefConvert> roleAnalysisAttributeDefConverts = generateMatchingRulesList(
                sessionOptionType.getClusteringAttributeSetting(),
                RoleAnalysisProcessModeType.ROLE);

        SearchFilterType query = sessionOptionType.getQuery();

        List<DataPoint> dataPoints = loadInitialData(modelService, roleAnalysisService, handler, isIndirect,
                RoleAnalysisProcessModeType.ROLE, roleAnalysisAttributeDefConverts,
                minUserOccupancy, maxUserOccupancy, query, task, result);

        if (dataPoints.isEmpty()) {
            LOGGER.warn("No data to process.");
            return new ArrayList<>();
        }

        DistanceMeasure distanceMeasure = new JaccardDistancesMeasure(
                minUsersOverlap, new HashSet<>(roleAnalysisAttributeDefConverts), 0);

        boolean ruleExist = !roleAnalysisAttributeDefConverts.isEmpty() && roleAnalysisAttributeDefConverts.get(0).getRoleAnalysisItemDef() != null;

        ClusteringMode clusteringMode = getClusteringMode(analysisOption, ruleExist);
        DensityBasedClustering<DataPoint> dbscan = new DensityBasedClustering<>(
                similarityDifference, minRolesCount, distanceMeasure, minUsersOverlap, clusteringMode);

        List<Cluster<DataPoint>> clusters = dbscan.cluster(dataPoints, handler);

        return new RoleAnalysisAlgorithmUtils().processClusters(roleAnalysisService, dataPoints, clusters, session,
                attributeAnalysisCache, handler, task, result);
    }

    public @NotNull List<PrismObject<RoleAnalysisClusterType>> executeUserBasedAdvancedClustering(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull ModelService modelService,
            @NotNull RoleAnalysisSessionType session,
            @NotNull AttributeAnalysisCache attributeAnalysisCache,
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull Task task,
            @NotNull OperationResult result) {
        UserAnalysisSessionOptionType sessionOptionType = session.getUserModeOptions();
        Boolean isIndirect = sessionOptionType.isIsIndirect();
        int minRolesOccupancy = sessionOptionType.getPropertiesRange().getMin().intValue();
        int maxRolesOccupancy = sessionOptionType.getPropertiesRange().getMax().intValue();
        double similarityThreshold = sessionOptionType.getSimilarityThreshold();
        double similarityDifference = 1 - (similarityThreshold / 100);
        int minRolesOverlap = sessionOptionType.getMinPropertiesOverlap();
        int minUsersCount = sessionOptionType.getMinMembersCount();
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();

        List<RoleAnalysisAttributeDefConvert> roleAnalysisAttributeDefConverts = generateMatchingRulesList(
                sessionOptionType.getClusteringAttributeSetting(),
                RoleAnalysisProcessModeType.USER);

        SearchFilterType query = sessionOptionType.getQuery();

        List<DataPoint> dataPoints = loadInitialData(modelService, roleAnalysisService, handler,
                isIndirect, RoleAnalysisProcessModeType.USER, roleAnalysisAttributeDefConverts,
                minRolesOccupancy, maxRolesOccupancy, query, task, result);

        if (dataPoints.isEmpty()) {
            LOGGER.info("No data to process.");
            return new ArrayList<>();
        }

        DistanceMeasure distanceMeasure = new JaccardDistancesMeasure(
                minRolesOverlap, new HashSet<>(roleAnalysisAttributeDefConverts), 0);

        boolean ruleExist = !roleAnalysisAttributeDefConverts.isEmpty() && roleAnalysisAttributeDefConverts.get(0).getRoleAnalysisItemDef() != null;
        ClusteringMode clusteringMode = getClusteringMode(analysisOption, ruleExist);

        DensityBasedClustering<DataPoint> dbscan = new DensityBasedClustering<>(
                similarityDifference, minUsersCount, distanceMeasure, minRolesOverlap, clusteringMode);

        List<Cluster<DataPoint>> clusters = dbscan.cluster(dataPoints, handler);

        return new RoleAnalysisAlgorithmUtils().processClusters(roleAnalysisService, dataPoints, clusters, session,
                attributeAnalysisCache, handler, task, result);
    }

    private @NotNull List<DataPoint> loadInitialData(
            @NotNull ModelService modelService,
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisProgressIncrement handler,
            Boolean isIndirect, @NotNull RoleAnalysisProcessModeType processMode,
            @NotNull List<RoleAnalysisAttributeDefConvert> roleAnalysisAttributeDefConverts,
            int minProperties,
            int maxProperties,
            @Nullable SearchFilterType userQuery,
            @NotNull Task task,
            @NotNull OperationResult result) {

        handler.enterNewStep(LOAD_DATA_STEP);
        handler.setOperationCountToProcess(1);

        ListMultimap<List<String>, String> chunkMap;
        if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            chunkMap = loadRoleModeData(modelService, isIndirect, minProperties, maxProperties, userQuery, task, result);
        } else {
            chunkMap = loadUserModeData(modelService, isIndirect, minProperties, maxProperties, userQuery, task, result);
        }
        handler.iterateActualStatus();

        if (chunkMap.isEmpty()) {
            LOGGER.warn("No data to process.");
            return new ArrayList<>();
        }

        handler.enterNewStep(PREPARING_DATA_POINTS_STEP);
        handler.setOperationCountToProcess(1);

        List<DataPoint> dataPoints;
        if (roleAnalysisAttributeDefConverts.isEmpty() || roleAnalysisAttributeDefConverts.get(0).getRoleAnalysisItemDef() == null) {
            dataPoints = prepareDataPoints(chunkMap);
        } else {
            if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
                dataPoints = prepareDataPointsRoleModeRules(chunkMap, roleAnalysisService, roleAnalysisAttributeDefConverts, task);
            } else {
                dataPoints = prepareDataPointsUserModeRules(chunkMap, roleAnalysisService, roleAnalysisAttributeDefConverts, task);
            }
        }
        handler.iterateActualStatus();
        return dataPoints;
    }

    @NotNull
    public ListMultimap<List<String>, String> loadUserModeData(
            @NotNull ModelService modelService,
            @NotNull Boolean isIndirect,
            int minRolesOccupancy,
            int maxRolesOccupancy,
            @Nullable SearchFilterType sessionOptionType,
            @NotNull Task task,
            @NotNull OperationResult result) {

        if (isIndirect) {
            return loadUserBasedMembershipMultimapData(
                    modelService, minRolesOccupancy, maxRolesOccupancy, sessionOptionType, task, result);
        }

        return loadUserBasedMultimapData(modelService, minRolesOccupancy,
                maxRolesOccupancy, sessionOptionType, task, result);
    }

    @NotNull
    public ListMultimap<List<String>, String> loadRoleModeData(
            @NotNull ModelService modelService,
            Boolean isIndirect, int minUserOccupancy,
            int maxUserOccupancy,
            @Nullable SearchFilterType sessionOptionType,
            @NotNull Task task,
            @NotNull OperationResult result) {

        if (isIndirect) {
            return loadRoleBasedMembershipMultimapData(
                    modelService, minUserOccupancy, maxUserOccupancy, sessionOptionType, task, result);
        }

        return loadRoleBasedMultimapData(
                modelService, minUserOccupancy, maxUserOccupancy, sessionOptionType, task, result);
    }

    @NotNull
    private static ClusteringMode getClusteringMode(@NotNull RoleAnalysisOptionType analysisOption, boolean ruleExist) {
        RoleAnalysisCategoryType analysisCategory = analysisOption.getAnalysisCategory();
        RoleAnalysisProcedureType analysisProcedureType = analysisOption.getAnalysisProcedureType();
        if (!ruleExist) {
            if (analysisCategory.equals(RoleAnalysisCategoryType.DEPARTMENT)) {
                return ClusteringMode.UNBALANCED;
            }

            return ClusteringMode.BALANCED;
        } else {

            if(analysisProcedureType.equals(RoleAnalysisProcedureType.OUTLIER_DETECTION)
            || analysisCategory.equals(RoleAnalysisCategoryType.ATTRIBUTE_BASED)) {
                return ClusteringMode.BALANCED_RULES_OUTLIER;
            }

            if (analysisCategory.equals(RoleAnalysisCategoryType.DEPARTMENT)) {
                return ClusteringMode.UNBALANCED_RULES;
            }

            return ClusteringMode.BALANCED_RULES;
        }
    }
}
