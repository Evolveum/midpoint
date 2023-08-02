/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.ClusterAlgorithmUtils.loadIntersections;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.loadDetectionOption;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.detection.DetectionAction;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object.DetectionOption;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.PrepareChunkStructure;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object.ClusterOptions;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object.ClusterStatistic;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.detection.DetectedPattern;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningOperationChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningRoleTypeChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningUserTypeChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class DefaultPatternResolver {

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

    public List<RoleAnalysisDetectionType> loadPattern(
            @NotNull ClusterOptions clusterOptions,
            @NotNull ClusterStatistic clusterStatistic,
            @NotNull RoleAnalysisClusterType clusterType,
            @NotNull PageBase pageBase,
            @NotNull OperationResult result) {

        List<RoleAnalysisDetectionType> roleAnalysisClusterDetectionTypeList = new ArrayList<>();
        if (clusterOptions.getSimilarity() == 1) {

            RoleAnalysisDetectionType roleAnalysisClusterDetectionType = new RoleAnalysisDetectionType();
            roleAnalysisClusterDetectionType.setSearchMode(clusterOptions.getSearchMode());

            ObjectReferenceType objectReferenceType;
            Set<ObjectReferenceType> properties = clusterStatistic.getPropertiesRef();
            for (ObjectReferenceType propertiesRef : properties) {
                objectReferenceType = new ObjectReferenceType();
                objectReferenceType.setOid(propertiesRef.getOid());
                objectReferenceType.setType(propertiesComplexType);
                roleAnalysisClusterDetectionType.getPropertiesOccupation().add(objectReferenceType);
            }

            Set<ObjectReferenceType> members = clusterStatistic.getMembersRef();
            for (ObjectReferenceType processedObjectOid : members) {
                objectReferenceType = new ObjectReferenceType();
                objectReferenceType.setOid(processedObjectOid.getOid());
                objectReferenceType.setType(processedObjectComplexType);
                roleAnalysisClusterDetectionType.getMemberOccupation().add(objectReferenceType);
            }

            int propertiesCount = properties.size();
            int membersCount = members.size();

            roleAnalysisClusterDetectionType.setClusterMetric((double) propertiesCount * membersCount);
            roleAnalysisClusterDetectionTypeList.add(roleAnalysisClusterDetectionType);
        } else {
            List<RoleAnalysisDetectionType> clusterDetectionTypeList = resolveDefaultIntersection(clusterOptions,
                    clusterType, pageBase, result);
            roleAnalysisClusterDetectionTypeList.addAll(clusterDetectionTypeList);

        }

        return roleAnalysisClusterDetectionTypeList;
    }

    String state = "START";

    private List<RoleAnalysisDetectionType> resolveDefaultIntersection(
            @NotNull ClusterOptions clusterOptions,
            @NotNull RoleAnalysisClusterType clusterType,
            @NotNull PageBase pageBase, @NotNull OperationResult operationResult) {

        List<DetectedPattern> possibleBusinessRole;
        RoleAnalysisProcessModeType mode = clusterOptions.getMode();
        RoleAnalysisSearchModeType searchMode = clusterOptions.getSearchMode();

        MiningOperationChunk miningOperationChunk = new PrepareChunkStructure().executeOperation(clusterType, false,
                roleAnalysisProcessModeType,
                pageBase, operationResult, state);
        List<MiningRoleTypeChunk> miningRoleTypeChunks = miningOperationChunk.getMiningRoleTypeChunks(
                ClusterObjectUtils.SORT.NONE);
        List<MiningUserTypeChunk> miningUserTypeChunks = miningOperationChunk.getMiningUserTypeChunks(
                ClusterObjectUtils.SORT.NONE);

        DetectionOption roleAnalysisSessionDetectionOptionType = loadDetectionOption(clusterOptions);

        possibleBusinessRole = new DetectionAction(roleAnalysisSessionDetectionOptionType).executeDetection(miningRoleTypeChunks, miningUserTypeChunks, mode);

        return loadIntersections(possibleBusinessRole, searchMode, processedObjectComplexType, propertiesComplexType);
    }

}
