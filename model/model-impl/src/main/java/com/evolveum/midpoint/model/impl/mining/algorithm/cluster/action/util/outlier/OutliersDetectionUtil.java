/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidAssignment;
import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidInducement;

import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectionOption;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.object.SimpleHeatPattern;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

//TODO
public class OutliersDetectionUtil {

    static double calculateAssignmentAnomalyConfidence(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisSessionType session,
            PrismObject<UserType> userTypeObject,
            RoleAnalysisOutlierDescriptionType prepareRoleOutlier,
            @NotNull Task task,
            OperationResult result) {
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

        return (distributionConfidenceDiff + patternConfidenceDiff + itemFactorConfidenceDiff
                + roleMemberConfidenceDiff + coverageConfidenceDiff) / 5;
    }

    static double getAverageItemFactor(@Nullable RoleAnalysisAttributeAnalysisResult compareAttributeResult) {

        if (compareAttributeResult == null) {
            return 0;
        }

        double averageItemFactor = 0;
        List<RoleAnalysisAttributeAnalysis> attributeAnalysisCompare = compareAttributeResult.getAttributeAnalysis();
        for (RoleAnalysisAttributeAnalysis attribute : attributeAnalysisCompare) {
            Double density = attribute.getDensity();
            if (density != null) {
                averageItemFactor += density;
            }
        }

        averageItemFactor = averageItemFactor / attributeAnalysisCompare.size();
        return averageItemFactor;
    }

    //TODO this is just for USER MODE! Implement Role (Experimental)
    static void detectAndLoadPatternAnalysis(
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
    static void detectAndLoadPatternAnalysis(
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

    private static double calculateItemFactorConfidence(
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

    public static double calculateRoleCoverageConfidence(
            @NotNull RoleAnalysisOutlierDescriptionType outlierResult,
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

    @NotNull
    static RoleAnalysisDetectionOptionType prepareDetectionOptions(@NotNull RoleAnalysisSessionType session) {
        RoleAnalysisDetectionOptionType defaultDetectionOption = session.getDefaultDetectionOption();
        double minFrequency = 2;
        double maxFrequency = 2;

        if (defaultDetectionOption != null) {
            if (defaultDetectionOption.getFrequencyRange() != null) {
                RangeType frequencyRange = defaultDetectionOption.getFrequencyRange();
                if (frequencyRange.getMin() != null) {
                    minFrequency = frequencyRange.getMin().intValue();
                }
                if (frequencyRange.getMax() != null) {
                    maxFrequency = frequencyRange.getMax().intValue();
                }
            }
        }

        RoleAnalysisDetectionOptionType detectionOption = new RoleAnalysisDetectionOptionType();
        detectionOption.setFrequencyRange(new RangeType().min(minFrequency).max(maxFrequency));
        return detectionOption;
    }

    static @NotNull Set<ObjectReferenceType> resolveUserDuplicateAssignment(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull UserType user,
            @NotNull Task task,
            @NotNull OperationResult result) {
        List<String> rolesOidAssignment = getRolesOidAssignment(user);
        Set<ObjectReferenceType> duplicatedRoleAssignments = new HashSet<>();
        for (String roleOid : rolesOidAssignment) {
            PrismObject<RoleType> roleAssignment = roleAnalysisService.getRoleTypeObject(roleOid, task, result);
            if (roleAssignment != null) {
                List<String> rolesOidInducement = getRolesOidInducement(roleAssignment.asObjectable());
                for (String roleOidInducement : rolesOidInducement) {
                    if (rolesOidAssignment.contains(roleOidInducement)) {
                        duplicatedRoleAssignments.add(new ObjectReferenceType().oid(roleOidInducement).type(RoleType.COMPLEX_TYPE));
                    }
                }
            }
        }
        return duplicatedRoleAssignments;
    }

}
