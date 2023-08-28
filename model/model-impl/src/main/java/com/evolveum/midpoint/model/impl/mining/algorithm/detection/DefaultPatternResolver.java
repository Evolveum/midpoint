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
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.objects.detection.DetectionOption;
import com.evolveum.midpoint.common.mining.objects.statistic.ClusterStatistic;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.mining.algorithm.chunk.PrepareChunkStructure;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class DefaultPatternResolver {

    private static final int MAX_PATTERN_INIT = 10;
    private final RoleAnalysisProcessModeType roleAnalysisProcessModeType;

    public DefaultPatternResolver(@NotNull RoleAnalysisProcessModeType roleAnalysisProcessModeType) {
        this.roleAnalysisProcessModeType = roleAnalysisProcessModeType;
    }

    public List<RoleAnalysisDetectionPatternType> loadPattern(
            RoleAnalysisSessionType session,
            @NotNull ClusterStatistic clusterStatistic,
            @NotNull RoleAnalysisClusterType clusterType,
            @NotNull ModelService modelService,
            @NotNull OperationResult result,
            Task task) {

        List<RoleAnalysisDetectionPatternType> roleAnalysisClusterDetectionTypeList = new ArrayList<>();
        AbstractAnalysisSessionOptionType sessionOption = getSessionOptionType(session);

        if (sessionOption.getSimilarityThreshold() == 100) {

            RoleAnalysisDetectionPatternType roleAnalysisClusterDetectionType = new RoleAnalysisDetectionPatternType();

            Set<ObjectReferenceType> roles;
            Set<ObjectReferenceType> users;

            if (roleAnalysisProcessModeType.equals(RoleAnalysisProcessModeType.ROLE)) {
                users = clusterStatistic.getPropertiesRef();
                roles = clusterStatistic.getMembersRef();
            } else {
                roles = clusterStatistic.getPropertiesRef();
                users = clusterStatistic.getMembersRef();
            }

            ObjectReferenceType objectReferenceType;
            for (ObjectReferenceType propertiesRef : roles) {
                objectReferenceType = new ObjectReferenceType();
                objectReferenceType.setOid(propertiesRef.getOid());
                objectReferenceType.setType(RoleType.COMPLEX_TYPE);
                roleAnalysisClusterDetectionType.getRolesOccupancy().add(objectReferenceType);
            }

            for (ObjectReferenceType processedObjectOid : users) {
                objectReferenceType = new ObjectReferenceType();
                objectReferenceType.setOid(processedObjectOid.getOid());
                objectReferenceType.setType(UserType.COMPLEX_TYPE);
                roleAnalysisClusterDetectionType.getUserOccupancy().add(objectReferenceType);
            }

            int propertiesCount = roles.size();
            int membersCount = users.size();

            roleAnalysisClusterDetectionType.setClusterMetric((double) propertiesCount * membersCount);
            roleAnalysisClusterDetectionTypeList.add(roleAnalysisClusterDetectionType);
        } else {
            List<RoleAnalysisDetectionPatternType> clusterDetectionTypeList = resolveDefaultIntersection(session,
                    clusterType, modelService, result, task);
            roleAnalysisClusterDetectionTypeList.addAll(clusterDetectionTypeList);

        }

        return roleAnalysisClusterDetectionTypeList;
    }

    private List<RoleAnalysisDetectionPatternType> resolveDefaultIntersection(
            RoleAnalysisSessionType session,
            @NotNull RoleAnalysisClusterType clusterType,
            @NotNull ModelService modelService, @NotNull OperationResult operationResult, Task task) {
        List<DetectedPattern> possibleBusinessRole;
        RoleAnalysisProcessModeType mode = session.getProcessMode();

        MiningOperationChunk miningOperationChunk = new PrepareChunkStructure().executeOperation(clusterType, false,
                roleAnalysisProcessModeType,
                modelService, operationResult, task);
        List<MiningRoleTypeChunk> miningRoleTypeChunks = miningOperationChunk.getMiningRoleTypeChunks(
                RoleAnalysisSortMode.NONE);
        List<MiningUserTypeChunk> miningUserTypeChunks = miningOperationChunk.getMiningUserTypeChunks(
                RoleAnalysisSortMode.NONE);

        DetectionOption roleAnalysisSessionDetectionOptionType = loadDetectionOption(session.getDefaultDetectionOption());

        possibleBusinessRole = new DetectionActionExecutor(roleAnalysisSessionDetectionOptionType)
                .executeDetection(miningRoleTypeChunks, miningUserTypeChunks, mode);

        List<DetectedPattern> topPatterns = loadTopPatterns(possibleBusinessRole);

        return loadIntersections(topPatterns);
    }

    public static List<DetectedPattern> loadTopPatterns(List<DetectedPattern> detectedPatterns) {
        detectedPatterns.sort(Comparator.comparing(DetectedPattern::getClusterMetric).reversed());

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
