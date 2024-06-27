/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidAssignment;

import java.util.*;

import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.object.SimpleHeatPattern;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectionOption;
import com.evolveum.midpoint.common.mining.utils.values.FrequencyItem;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.common.mining.utils.values.ZScoreData;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
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
            List<MiningRoleTypeChunk> miningRoleTypeChunks = miningOperationChunk.getMiningRoleTypeChunks(
                    RoleAnalysisSortMode.NONE);

            ZScoreData zScoreData = roleAnalysisService.resolveOutliersZScore(miningRoleTypeChunks, minFrequency, maxFrequency);

            int countOfRoles = 0;
            for (MiningRoleTypeChunk miningRoleTypeChunk : miningRoleTypeChunks) {
                countOfRoles += miningRoleTypeChunk.getRoles().size();
            }

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

                        outlierDescription.setCreateTimestamp(
                                XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis()));

                        double confidence = roleAnalysisService.calculateZScoreConfidence(miningRoleTypeChunk, zScoreData);
                        outlierDescription.setConfidenceDeviation(confidence);
                        outlierDescription.setFrequency(frequencyItem.getFrequency());

                        outlierDescription.setCluster(clusterRef.clone());

                        outlierDescription.setSession(sessionRef.clone());

                        prepareRoleOutliers.add(outlierDescription);
                    });

                    for (String user : users) {
                        PrismObject<UserType> userTypeObject = roleAnalysisService.getUserTypeObject(user, task, result);
                        if (userTypeObject == null) {
                            continue;
                        }

                        RoleAnalysisOutlierType userOutliers = map.get(user);

                        if (userOutliers == null) {
                            userOutliers = new RoleAnalysisOutlierType();
                            userOutliers.setTargetObjectRef(new ObjectReferenceType().oid(user).type(UserType.COMPLEX_TYPE));
                        }

                        List<RoleAnalysisAttributeDef> attributesForUserAnalysis = roleAnalysisService.resolveAnalysisAttributes(
                                session, UserType.COMPLEX_TYPE);

                        AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();
                        RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = clusterStatistics.getUserAttributeAnalysisResult();

                        RoleAnalysisAttributeAnalysisResult userAttributes = roleAnalysisService
                                .resolveUserAttributes(userTypeObject, attributesForUserAnalysis);

                        RoleAnalysisAttributeAnalysisResult compareAttributeResult = roleAnalysisService
                                .resolveSimilarAspect(userAttributes, userAttributeAnalysisResult);

                        AttributeAnalysis attributeAnalysis = new AttributeAnalysis();
                        attributeAnalysis.setUserAttributeAnalysisResult(userAttributeAnalysisResult);
                        attributeAnalysis.setUserClusterCompare(compareAttributeResult);
                        userOutliers.setAttributeAnalysis(attributeAnalysis);

                        double assignmentFrequencyConfidence = calculateOutlierRoleAssignmentFrequencyConfidence(
                                userTypeObject, countOfRoles);
                        userOutliers.setOutlierAssignmentFrequencyConfidence(assignmentFrequencyConfidence);

                        detectAndLoadPatternAnalysis(userOutliers, miningRoleTypeChunks);

                        double outlierConfidenceBasedAssignment = 0;
                        for (RoleAnalysisOutlierDescriptionType prepareRoleOutlier : prepareRoleOutliers) {
                            detectAndLoadPatternAnalysis(miningRoleTypeChunk, user, prepareRoleOutlier, miningRoleTypeChunks);
                            double itemFactorConfidence = calculateItemFactorConfidence(
                                    session, prepareRoleOutlier, userTypeObject, roleAnalysisService, task, result);
                            double distributionConfidence = prepareRoleOutlier.getConfidenceDeviation();
                            double patternConfidence = prepareRoleOutlier.getPatternInfo().getConfidence();
                            double roleMemberConfidence = calculateRoleCoverageConfidence(
                                    prepareRoleOutlier, roleAnalysisService, task, result);
                            double coverageConfidence = calculateOutlierPropertyCoverageConfidence(prepareRoleOutlier);

                            double distributionConfidenceDiff = distributionConfidence * 100;
                            double patternConfidenceDiff = 100 - patternConfidence;
                            double itemFactorConfidenceDiff = 100 - itemFactorConfidence;
                            double roleMemberConfidenceDiff = 100 - roleMemberConfidence;
                            double coverageConfidenceDiff = 100 - coverageConfidence;

                            double confidence = (distributionConfidenceDiff + patternConfidenceDiff + itemFactorConfidenceDiff
                                    + roleMemberConfidenceDiff + coverageConfidenceDiff) / 5;
                            outlierConfidenceBasedAssignment += confidence;
                            prepareRoleOutlier.setConfidence(confidence);
                            userOutliers.getResult().add(prepareRoleOutlier.clone());
                        }

                        double averageItemFactor = getAverageItemFactor(compareAttributeResult);

                        outlierConfidenceBasedAssignment = outlierConfidenceBasedAssignment / prepareRoleOutliers.size();
                        userOutliers.setOutlierPropertyConfidence(outlierConfidenceBasedAssignment);

                        Double membershipDensity = clusterStatistics.getMembershipDensity();
                        Double outlierPatternConfidence = userOutliers.getPatternInfo().getConfidence();
                        double clusterConfidence = outlierConfidenceBasedAssignment
                                + assignmentFrequencyConfidence
                                + outlierPatternConfidence
                                + averageItemFactor;

                        if (membershipDensity != null) {
                            clusterConfidence += membershipDensity;
                        }

                        clusterConfidence = clusterConfidence / 5;
                        userOutliers.setClusterConfidence(clusterConfidence);

                        map.put(user, userOutliers);
                    }

                }
            }

        } else if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            List<MiningUserTypeChunk> miningUserTypeChunks = miningOperationChunk.getMiningUserTypeChunks(
                    RoleAnalysisSortMode.NONE);

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

                        outlierDescription.setConfidenceDeviation(confidence);

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

    private static double getAverageItemFactor(RoleAnalysisAttributeAnalysisResult compareAttributeResult) {
        double averageItemFactor = 0;
        if (compareAttributeResult != null) {
            List<RoleAnalysisAttributeAnalysis> attributeAnalysisCompare = compareAttributeResult.getAttributeAnalysis();
            for (RoleAnalysisAttributeAnalysis attribute : attributeAnalysisCompare) {
                Double density = attribute.getDensity();
                if (density != null) {
                    averageItemFactor += density;
                }
            }

            averageItemFactor = averageItemFactor / attributeAnalysisCompare.size();
        }
        return averageItemFactor;
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
        double topPatternCoverage = ((double) topPatternRelation / clusterRelations) * 100;

        RoleAnalysisPatternInfo patternInfo = new RoleAnalysisPatternInfo();
        patternInfo.setConfidence(topPatternCoverage);
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
        double topPatternCoverage = ((double) topPatternRelation / clusterRelations) * 100;
        patternInfo.setConfidence(topPatternCoverage);
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

    public static double calculateItemFactorConfidence(
            @NotNull RoleAnalysisSessionType session,
            @NotNull RoleAnalysisOutlierDescriptionType outlierResult,
            @NotNull PrismObject<UserType> userTypeObject,
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull Task task,
            @NotNull OperationResult result) {

        ObjectReferenceType targetObjectRef = outlierResult.getObject();
        PrismObject<RoleType> roleTypeObject = roleAnalysisService.getRoleTypeObject(targetObjectRef.getOid(), task, result);
        if (roleTypeObject == null) {
            return 0;
        }
        List<RoleAnalysisAttributeDef> attributesForUserAnalysis = roleAnalysisService.resolveAnalysisAttributes(
                session, UserType.COMPLEX_TYPE);
        if (attributesForUserAnalysis == null || attributesForUserAnalysis.isEmpty()) {
            return 0;
        }
        RoleAnalysisAttributeAnalysisResult roleAnalysisAttributeAnalysisResult = roleAnalysisService
                .resolveRoleMembersAttribute(roleTypeObject.getOid(), task, result, attributesForUserAnalysis);

        RoleAnalysisAttributeAnalysisResult userAttributes = roleAnalysisService.resolveUserAttributes(
                userTypeObject, attributesForUserAnalysis);

        RoleAnalysisAttributeAnalysisResult compareAttributeResult = roleAnalysisService
                .resolveSimilarAspect(userAttributes, roleAnalysisAttributeAnalysisResult);

        double averageItemsOccurs = 0;
        assert compareAttributeResult != null;
        List<RoleAnalysisAttributeAnalysis> attributeAnalysis = compareAttributeResult.getAttributeAnalysis();
        for (RoleAnalysisAttributeAnalysis analysis : attributeAnalysis) {
            Double density = analysis.getDensity();
            if (density != null) {
                averageItemsOccurs += density;
            }
        }
        outlierResult.setItemFactorConfidence(averageItemsOccurs / attributeAnalysis.size());
        return averageItemsOccurs / attributeAnalysis.size();
    }

    public static double calculateRoleCoverageConfidence(@NotNull RoleAnalysisOutlierDescriptionType outlierResult,
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull Task task,
            @NotNull OperationResult result) {
        ObjectReferenceType targetObjectRef = outlierResult.getObject();
        int roleMemberCount;
        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        roleAnalysisService.extractUserTypeMembers(
                userExistCache, null,
                new HashSet<>(Collections.singleton(targetObjectRef.getOid())),
                task, result);
        roleMemberCount = userExistCache.size();

        int userCountInRepo = roleAnalysisService.countObjects(UserType.class, null, null, task, result);

        double memberPercentageRepo = (((double) roleMemberCount / userCountInRepo) * 100);
        outlierResult.setMemberCoverageConfidence(memberPercentageRepo);
        return memberPercentageRepo;
    }

    public static double calculateOutlierPropertyCoverageConfidence(@NotNull RoleAnalysisOutlierDescriptionType outlierResult) {
        double occurInCluster = outlierResult.getFrequency() * 100;
        outlierResult.setOutlierCoverageConfidence(occurInCluster);
        return occurInCluster;
    }

    public static double calculateOutlierRoleAssignmentFrequencyConfidence(@NotNull PrismObject<UserType> prismUser,
            int allRolesForGroup) {
        List<String> rolesOidAssignment = getRolesOidAssignment(prismUser.asObjectable());
        int userRolesCount = rolesOidAssignment.size();
        return ((double) userRolesCount / allRolesForGroup) * 100;
    }
}