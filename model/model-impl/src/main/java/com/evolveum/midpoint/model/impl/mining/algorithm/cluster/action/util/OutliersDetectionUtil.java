/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util;

import java.util.*;

import com.evolveum.midpoint.common.mining.objects.detection.DetectionOption;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.utils.values.FrequencyItem;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.common.mining.utils.values.ZScoreData;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class OutliersDetectionUtil {

    public static @NotNull Collection<RoleAnalysisOutlierType> executeOutliersAnalysis(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull RoleAnalysisSessionType session,
            @NotNull RoleAnalysisOptionType analysisOption,
            @NotNull Task task,
            @NotNull OperationResult result) {

        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();

        MiningOperationChunk miningOperationChunk = roleAnalysisService.prepareCompressedMiningStructure(cluster, true,
                processMode, result, task);

        HashMap<String, RoleAnalysisOutlierType> map = new HashMap<>();

        ObjectReferenceType clusterRef = new ObjectReferenceType()
                .oid(cluster.getOid())
                .type(RoleAnalysisClusterType.COMPLEX_TYPE);

        ObjectReferenceType sessionRef = new ObjectReferenceType()
                .oid(session.getOid())
                .type(RoleAnalysisSessionType.COMPLEX_TYPE);

        RangeType frequencyRange = session.getDefaultDetectionOption().getFrequencyRange();
        Double minFrequency = frequencyRange.getMin();
        Double maxFrequency = frequencyRange.getMax();

        //TODO role mode
        if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
            List<MiningRoleTypeChunk> miningRoleTypeChunks = miningOperationChunk.getMiningRoleTypeChunks(RoleAnalysisSortMode.NONE);

            ZScoreData zScoreData = roleAnalysisService.resolveOutliersZScore(miningRoleTypeChunks, minFrequency, maxFrequency);

            //this is row miningRoleTypeChunk
            for (MiningRoleTypeChunk miningRoleTypeChunk : miningRoleTypeChunks) {

                FrequencyItem frequencyItem = miningRoleTypeChunk.getFrequencyItem();
                if (!frequencyItem.getStatus().equals(FrequencyItem.Status.INCLUDE)) {

                    List<String> roles = miningRoleTypeChunk.getMembers();
                    List<String> users = miningRoleTypeChunk.getProperties();

                    List<RoleAnalysisOutlierDescriptionType> prepareRoleOutliers = new ArrayList<>();

                    roles.forEach(role -> {
                        RoleAnalysisOutlierDescriptionType outlierDescription = new RoleAnalysisOutlierDescriptionType();
                        outlierDescription.setCategory(OutlierCategory.INNER_OUTLIER);
                        outlierDescription.setObject(new ObjectReferenceType().
                                oid(role)
                                .type(RoleType.COMPLEX_TYPE));

                        outlierDescription.setCreateTimestamp(XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis()));

                        double confidence = roleAnalysisService.calculateZScoreConfidence(miningRoleTypeChunk, zScoreData);
                        outlierDescription.setConfidence(confidence);
                        outlierDescription.setFrequency(frequencyItem.getFrequency());

                        outlierDescription.setCluster(clusterRef.clone());

                        outlierDescription.setSession(sessionRef.clone());

                        prepareRoleOutliers.add(outlierDescription);
                    });

                    for (String user : users) {
                        RoleAnalysisOutlierType userOutliers = map.get(user);

                        if (userOutliers == null) {
                            userOutliers = new RoleAnalysisOutlierType();
                            userOutliers.setTargetObjectRef(new ObjectReferenceType().oid(user).type(UserType.COMPLEX_TYPE));
                            for (RoleAnalysisOutlierDescriptionType prepareRoleOutlier : prepareRoleOutliers) {
                                detectAndLoadPatternAnalysis(miningRoleTypeChunk, user, prepareRoleOutlier, miningRoleTypeChunks);

                                userOutliers.getResult().add(prepareRoleOutlier.clone());
                            }
                            map.put(user, userOutliers);
                        } else {
                            for (RoleAnalysisOutlierDescriptionType prepareRoleOutlier : prepareRoleOutliers) {
                                detectAndLoadPatternAnalysis(miningRoleTypeChunk, user, prepareRoleOutlier, miningRoleTypeChunks);
                                userOutliers.getResult().add(prepareRoleOutlier.clone());
                            }
                        }

                    }

                }
            }

//            TODO this is just for USER MODE! Implement Role (Experimental)
            for (RoleAnalysisOutlierType roleAnalysisOutlierType : map.values()) {
                detectAndLoadPatternAnalysis(roleAnalysisOutlierType, miningRoleTypeChunks);
            }

        } else if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            List<MiningUserTypeChunk> miningUserTypeChunks = miningOperationChunk.getMiningUserTypeChunks(RoleAnalysisSortMode.NONE);

            ZScoreData zScoreData = roleAnalysisService.resolveOutliersZScore(miningUserTypeChunks, minFrequency, maxFrequency);

            for (MiningUserTypeChunk miningUserTypeChunk : miningUserTypeChunks) {

                FrequencyItem frequencyItem = miningUserTypeChunk.getFrequencyItem();

                //TODO Z score
                if (!frequencyItem.getStatus().equals(FrequencyItem.Status.INCLUDE)) {
                    List<String> roles = miningUserTypeChunk.getProperties();
                    List<String> users = miningUserTypeChunk.getMembers();

                    List<RoleAnalysisOutlierDescriptionType> prepareUserOutliers = new ArrayList<>();

                    users.forEach(user -> {
                        RoleAnalysisOutlierDescriptionType outlierDescription = new RoleAnalysisOutlierDescriptionType();
                        outlierDescription.setCategory(OutlierCategory.INNER_OUTLIER);
                        outlierDescription.setObject(new ObjectReferenceType().
                                oid(user)
                                .type(UserType.COMPLEX_TYPE));

                        double confidence = roleAnalysisService.calculateZScoreConfidence(miningUserTypeChunk, zScoreData);

                        outlierDescription.setConfidence(confidence);

                        outlierDescription.setCluster(clusterRef.clone());
                        outlierDescription.setFrequency(frequencyItem.getFrequency());

                        outlierDescription.setSession(sessionRef.clone());

                        prepareUserOutliers.add(outlierDescription);
                    });

                    for (String role : roles) {
                        RoleAnalysisOutlierType roleOutliers = map.get(role);

                        if (roleOutliers == null) {
                            roleOutliers = new RoleAnalysisOutlierType();
                            roleOutliers.setTargetObjectRef(new ObjectReferenceType().oid(role).type(RoleType.COMPLEX_TYPE));
                            for (RoleAnalysisOutlierDescriptionType prepareRoleOutlier : prepareUserOutliers) {
                                roleOutliers.getResult().add(prepareRoleOutlier.clone());
                            }
                            map.put(role, roleOutliers);
                        } else {
                            for (RoleAnalysisOutlierDescriptionType prepareRoleOutlier : prepareUserOutliers) {
                                roleOutliers.getResult().add(prepareRoleOutlier.clone());
                            }
                        }

                    }

                }
            }

        }

        return map.values();
    }

    //TODO this is just for USER MODE! Implement Role (Experimental)
    private static void detectAndLoadPatternAnalysis(
            @NotNull RoleAnalysisOutlierType roleAnalysisOutlierType,
            @NotNull List<MiningRoleTypeChunk> miningRoleTypeChunks) {
        ObjectReferenceType userRef = roleAnalysisOutlierType.getTargetObjectRef();
        String userOid = userRef.getOid();

        DetectionOption detectionOption = new DetectionOption(
                10, 100, 2, 2);
        List<SimpleHeatPattern> totalRelationOfPatternsForCell = new OutlierPatternResolver()
                .performSingleCellDetection(RoleAnalysisProcessModeType.USER, miningRoleTypeChunks, detectionOption,
                        Collections.singletonList(userOid), null);

        int patternCount = totalRelationOfPatternsForCell.size();
        int totalRelations = 0;
        int topPatternRelation = 0;
        for (SimpleHeatPattern simpleHeatPattern : totalRelationOfPatternsForCell) {
            int relations = simpleHeatPattern.getTotalRelations();
            totalRelations += relations;
            if (relations > topPatternRelation) {
                topPatternRelation = relations;
            }
        }

        int clusterRelations = calculateOveralClusterRelationsCount(miningRoleTypeChunks);
        RoleAnalysisPatternInfo patternInfo = new RoleAnalysisPatternInfo();
        patternInfo.setConfidence(0.0);
        patternInfo.setDetectedPatternCount(patternCount);
        patternInfo.setTopPatternRelation(topPatternRelation);
        patternInfo.setTotalRelations(totalRelations);
        patternInfo.setClusterRelations(clusterRelations);
        roleAnalysisOutlierType.setPatternInfo(patternInfo);
    }

    //TODO this is just for USER MODE! Implement Role (Experimental)
    private static void detectAndLoadPatternAnalysis(
            @NotNull MiningRoleTypeChunk miningRoleTypeChunk,
            @NotNull String user,
            @NotNull RoleAnalysisOutlierDescriptionType prepareRoleOutlier,
            @NotNull List<MiningRoleTypeChunk> miningRoleTypeChunks) {
        List<String> allowedProperties = miningRoleTypeChunk.getProperties();
        DetectionOption detectionOption = new DetectionOption(
                10, 100, 2, 2);
        List<SimpleHeatPattern> totalRelationOfPatternsForCell = new OutlierPatternResolver()
                .performSingleCellDetection(RoleAnalysisProcessModeType.USER, miningRoleTypeChunks, detectionOption,
                        Collections.singletonList(user), allowedProperties);

        int patternCount = totalRelationOfPatternsForCell.size();
        int totalRelations = 0;
        int topPatternRelation = 0;
        for (SimpleHeatPattern simpleHeatPattern : totalRelationOfPatternsForCell) {
            int relations = simpleHeatPattern.getTotalRelations();
            totalRelations += relations;
            if (relations > topPatternRelation) {
                topPatternRelation = relations;
            }
        }

        int clusterRelations = calculateOveralClusterRelationsCount(miningRoleTypeChunks);
        RoleAnalysisPatternInfo patternInfo = new RoleAnalysisPatternInfo();
        patternInfo.setConfidence(0.0);
        patternInfo.setDetectedPatternCount(patternCount);
        patternInfo.setTopPatternRelation(topPatternRelation);
        patternInfo.setTotalRelations(totalRelations);
        patternInfo.setClusterRelations(clusterRelations);
        prepareRoleOutlier.setPatternInfo(patternInfo);
    }

    //TODO this is just for USER MODE! Implement Role (Experimental)
    /**
     * Calculate total relations (connections between properties and members) in the cluster.
     */
    public static int calculateOveralClusterRelationsCount(@NotNull List<MiningRoleTypeChunk> miningRoleTypeChunks) {
        int totalRelations = 0;
        for (MiningRoleTypeChunk roleTypeChunk : miningRoleTypeChunks) {
            int propertiesCount = roleTypeChunk.getProperties().size();
            int membersCount = roleTypeChunk.getMembers().size();
            totalRelations += (propertiesCount * membersCount);
        }
        return totalRelations;
    }
}
