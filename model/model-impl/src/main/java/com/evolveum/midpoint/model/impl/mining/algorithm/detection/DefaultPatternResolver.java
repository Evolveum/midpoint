/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.detection;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.objects.detection.PatternDetectionOption;
import com.evolveum.midpoint.common.mining.objects.statistic.ClusterStatistic;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * <p>
 * The DefaultPatternResolver class is responsible for detecting and resolving patterns within a role analysis session.
 * It provides methods to load and resolve patterns based on the configured detection options, session details, and cluster statistics.
 * <p>
 * This class is a crucial component of the role analysis process, helping identify patterns within the analyzed data and
 * facilitating decision-making for role and user assignments.
 */
public class DefaultPatternResolver {

    private static final int MAX_PATTERN_INIT = 30;
    private final RoleAnalysisProcessModeType roleAnalysisProcessModeType;
    private final RoleAnalysisService roleAnalysisService;

    /**
     * Constructs a DefaultPatternResolver for a specific role analysis process mode.
     *
     * @param roleAnalysisService The role analysis service for performing operations.
     * @param roleAnalysisProcessModeType The mode specifying whether the process is role-based or user-based.
     */
    public DefaultPatternResolver(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisProcessModeType roleAnalysisProcessModeType) {
        this.roleAnalysisProcessModeType = roleAnalysisProcessModeType;
        this.roleAnalysisService = roleAnalysisService;
    }

    /**
     * Loads and resolves detection patterns based on the session details, cluster statistics, and detection options.
     *
     * @param session The role analysis session.
     * @param clusterStatistic The cluster statistics for a specific cluster.
     * @param clusterType The cluster type to resolve patterns for.
     * @param result The operation result for tracking the operation status.
     * @param task The task associated with the operation.
     * @return A list of resolved RoleAnalysisDetectionPatternType objects representing detection patterns.
     */
    public List<RoleAnalysisDetectionPatternType> loadPattern(
            @NotNull RoleAnalysisSessionType session,
            @NotNull ClusterStatistic clusterStatistic,
            @NotNull RoleAnalysisClusterType clusterType,
            @NotNull OperationResult result,
            @NotNull Task task) {

        List<RoleAnalysisDetectionPatternType> roleAnalysisClusterDetectionTypeList = new ArrayList<>();
        AbstractAnalysisSessionOptionType sessionOption = getSessionOptionType(session);

//        if (sessionOption.getSimilarityThreshold() == 100) {
//
//            RoleAnalysisDetectionPatternType roleAnalysisClusterDetectionType = new RoleAnalysisDetectionPatternType();
//
//            Set<ObjectReferenceType> roles;
//            Set<ObjectReferenceType> users;
//
//            if (roleAnalysisProcessModeType.equals(RoleAnalysisProcessModeType.ROLE)) {
//                users = clusterStatistic.getPropertiesRef();
//                roles = clusterStatistic.getMembersRef();
//            } else {
//                roles = clusterStatistic.getPropertiesRef();
//                users = clusterStatistic.getMembersRef();
//            }
//
//            List<ObjectReferenceType> rolesOccupancy = roleAnalysisClusterDetectionType.getRolesOccupancy();
//            roles.stream().map(ObjectReferenceType::clone).forEach(rolesOccupancy::add);
//
//            List<ObjectReferenceType> userOccupancy = roleAnalysisClusterDetectionType.getUserOccupancy();
//            users.stream().map(ObjectReferenceType::clone).forEach(userOccupancy::add);
//
//            int propertiesCount = roles.size();
//            int membersCount = users.size();
//
//            roleAnalysisClusterDetectionType.setClusterMetric((double) propertiesCount * membersCount);
//            roleAnalysisClusterDetectionTypeList.add(roleAnalysisClusterDetectionType.clone());
//        } else {
//            List<RoleAnalysisDetectionPatternType> clusterDetectionTypeList = resolveDefaultIntersection(session,
//                    clusterType, result, task);
//            roleAnalysisClusterDetectionTypeList.addAll(clusterDetectionTypeList);
//
//        }

        List<RoleAnalysisDetectionPatternType> clusterDetectionTypeList = resolveDefaultIntersection(session,
                clusterType, result, task);
        roleAnalysisClusterDetectionTypeList.addAll(clusterDetectionTypeList);

        return roleAnalysisClusterDetectionTypeList;
    }

    private List<RoleAnalysisDetectionPatternType> resolveDefaultIntersection(
            @NotNull RoleAnalysisSessionType session,
            @NotNull RoleAnalysisClusterType clusterType,
            @NotNull OperationResult operationResult,
            @NotNull Task task) {
        List<DetectedPattern> possibleBusinessRole;
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        RoleAnalysisProcessModeType mode = analysisOption.getProcessMode();

        SearchFilterType filter = null;
        if(mode.equals(RoleAnalysisProcessModeType.ROLE)){
            RoleAnalysisSessionOptionType roleModeOptions = session.getRoleModeOptions();
            if(roleModeOptions != null){
                filter = roleModeOptions.getUserSearchFilter();
            }
        }else if(mode.equals(RoleAnalysisProcessModeType.USER)){
            UserAnalysisSessionOptionType userModeOptions = session.getUserModeOptions();
            if(userModeOptions != null){
                filter = userModeOptions.getUserSearchFilter();
            }
        }

        MiningOperationChunk miningOperationChunk = roleAnalysisService.prepareCompressedMiningStructure(
                clusterType, filter, false, roleAnalysisProcessModeType, operationResult, task);
        List<MiningRoleTypeChunk> miningRoleTypeChunks = miningOperationChunk.getMiningRoleTypeChunks(
                RoleAnalysisSortMode.NONE);
        List<MiningUserTypeChunk> miningUserTypeChunks = miningOperationChunk.getMiningUserTypeChunks(
                RoleAnalysisSortMode.NONE);

        PatternDetectionOption roleAnalysisSessionDetectionOptionType = loadPatternDetectionOption(session.getDefaultDetectionOption());

        possibleBusinessRole = new DefaultDetectionAction(roleAnalysisSessionDetectionOptionType)
                .executeDetection(miningRoleTypeChunks, miningUserTypeChunks, mode);

        List<DetectedPattern> topPatterns = loadTopPatterns(possibleBusinessRole);

        return loadIntersections(topPatterns);
    }

    /**
     * Loads the top detection patterns from a list of detected patterns based on their cluster metric values.
     *
     * @param detectedPatterns The list of detected patterns.
     * @return A list of the top detected patterns.
     */
    public static List<DetectedPattern> loadTopPatterns(@NotNull List<DetectedPattern> detectedPatterns) {
        detectedPatterns.sort(Comparator.comparing(DetectedPattern::getMetric).reversed());

        List<DetectedPattern> topPatterns = new ArrayList<>();

        int index = 0;
        for (DetectedPattern pattern : detectedPatterns) {
            topPatterns.add(pattern);
            index++;
            if (index >= MAX_PATTERN_INIT) {
                break;
            }
        }
        return topPatterns;
    }

}
