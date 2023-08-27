/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.detection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.objects.detection.DetectionOption;
import com.evolveum.midpoint.common.mining.objects.statistic.ClusterStatistic;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.model.impl.mining.algorithm.chunk.PrepareChunkStructure;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.*;

public class DefaultPatternResolver {

    private static final int MAX_PATTERN_INIT = 10;
    RoleAnalysisProcessModeType roleAnalysisProcessModeType;
    QName processedObjectComplexType;
    QName propertiesComplexType;

    public DefaultPatternResolver(@NotNull RoleAnalysisProcessModeType roleAnalysisProcessModeType) {
        this.roleAnalysisProcessModeType = roleAnalysisProcessModeType;

        if (roleAnalysisProcessModeType.equals(RoleAnalysisProcessModeType.USER)) {
            processedObjectComplexType = UserType.COMPLEX_TYPE;
            propertiesComplexType = RoleType.COMPLEX_TYPE;
        } else {
            processedObjectComplexType = RoleType.COMPLEX_TYPE;
            propertiesComplexType = UserType.COMPLEX_TYPE;
        }
    }

    public List<RoleAnalysisDetectionPatternType> loadPattern(
            RoleAnalysisSessionType session,
            @NotNull ClusterStatistic clusterStatistic,
            @NotNull RoleAnalysisClusterType clusterType,
            @NotNull RepositoryService repoService,
            @NotNull OperationResult result) {

        List<RoleAnalysisDetectionPatternType> roleAnalysisClusterDetectionTypeList = new ArrayList<>();
        AbstractAnalysisSessionOptionType sessionOption = getSessionOptionType(session);

        if (sessionOption.getSimilarityThreshold() == 100) {

            RoleAnalysisDetectionPatternType roleAnalysisClusterDetectionType = new RoleAnalysisDetectionPatternType();

            ObjectReferenceType objectReferenceType;
            Set<ObjectReferenceType> properties = clusterStatistic.getPropertiesRef();
            for (ObjectReferenceType propertiesRef : properties) {
                objectReferenceType = new ObjectReferenceType();
                objectReferenceType.setOid(propertiesRef.getOid());
                objectReferenceType.setType(propertiesComplexType);
                roleAnalysisClusterDetectionType.getRolesOccupancy().add(objectReferenceType);
            }

            Set<ObjectReferenceType> members = clusterStatistic.getMembersRef();
            for (ObjectReferenceType processedObjectOid : members) {
                objectReferenceType = new ObjectReferenceType();
                objectReferenceType.setOid(processedObjectOid.getOid());
                objectReferenceType.setType(processedObjectComplexType);
                roleAnalysisClusterDetectionType.getUserOccupancy().add(objectReferenceType);
            }

            int propertiesCount = properties.size();
            int membersCount = members.size();

            roleAnalysisClusterDetectionType.setClusterMetric((double) propertiesCount * membersCount);
            roleAnalysisClusterDetectionTypeList.add(roleAnalysisClusterDetectionType);
        } else {
            List<RoleAnalysisDetectionPatternType> clusterDetectionTypeList = resolveDefaultIntersection(session,
                    clusterType, repoService, result);
            roleAnalysisClusterDetectionTypeList.addAll(clusterDetectionTypeList);

        }

        return roleAnalysisClusterDetectionTypeList;
    }

    private List<RoleAnalysisDetectionPatternType> resolveDefaultIntersection(
            RoleAnalysisSessionType session,
            @NotNull RoleAnalysisClusterType clusterType,
            @NotNull RepositoryService repoService, @NotNull OperationResult operationResult) {
        List<DetectedPattern> possibleBusinessRole;
        RoleAnalysisProcessModeType mode = session.getProcessMode();

        MiningOperationChunk miningOperationChunk = new PrepareChunkStructure().executeOperation(clusterType, false,
                roleAnalysisProcessModeType,
                repoService, operationResult);
        List<MiningRoleTypeChunk> miningRoleTypeChunks = miningOperationChunk.getMiningRoleTypeChunks(
                RoleAnalysisSortMode.NONE);
        List<MiningUserTypeChunk> miningUserTypeChunks = miningOperationChunk.getMiningUserTypeChunks(
                RoleAnalysisSortMode.NONE);

        DetectionOption roleAnalysisSessionDetectionOptionType = loadDetectionOption(session.getDefaultDetectionOption());

        possibleBusinessRole = new DetectionActionExecutor(roleAnalysisSessionDetectionOptionType)
                .executeDetection(miningRoleTypeChunks, miningUserTypeChunks, mode);

        possibleBusinessRole.sort(Comparator.comparing(DetectedPattern::getClusterMetric).reversed());

        List<DetectedPattern> topPatterns = new ArrayList<>();

        int index = 0;
        for (DetectedPattern pattern : possibleBusinessRole) {
            topPatterns.add(pattern);
            index++;
            if (index >= MAX_PATTERN_INIT) {
                break;
            }
        }

        return loadIntersections(topPatterns, processedObjectComplexType, propertiesComplexType);
    }

}
