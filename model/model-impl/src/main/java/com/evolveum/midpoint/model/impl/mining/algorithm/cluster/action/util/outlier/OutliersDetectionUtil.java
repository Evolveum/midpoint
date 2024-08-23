/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier;

import java.util.*;

import com.evolveum.midpoint.common.mining.objects.analysis.cache.AttributeAnalysisCache;
import com.evolveum.midpoint.common.mining.objects.analysis.cache.RoleMemberCountCache;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.context.OutlierPatternResolver;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

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

import static com.evolveum.midpoint.common.mining.utils.ExtractPatternUtils.prepareDetectedPattern;
import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.*;

//TODO major multiple thinks is processed multiple times
// (Create structure for caching these data, NOTE: use also clustering process there is multiple mapped structures that can be used)
public class OutliersDetectionUtil {

    public static void updateOrImportOutlierObject(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisSessionType session,
            @NotNull String userOid,
            @NotNull RoleAnalysisOutlierPartitionType partition,
            @NotNull Task task,
            @NotNull OperationResult result) {

        RoleAnalysisDetectionOptionType detectionOption = session.getDefaultDetectionOption();
        Double sensitivity = detectionOption.getSensitivity();
        double requiredConfidence = roleAnalysisService.calculateOutlierConfidenceRequired(sensitivity);

        //TODO temporary solution
        requiredConfidence = requiredConfidence * 100;

        Double clusterConfidence = partition.getPartitionAnalysis().getOverallConfidence();
        Double clusterAnomalyObjectsConfidence = partition.getPartitionAnalysis().getAnomalyObjectsConfidence();
        if (clusterConfidence == null
                || clusterConfidence < requiredConfidence
                || clusterAnomalyObjectsConfidence < requiredConfidence) {
            return;
        }

        PrismObject<RoleAnalysisOutlierType> outlierObject = roleAnalysisService.searchOutlierObjectByUserOidClusters(
                userOid, task, result);

        if (outlierObject == null) {
            PrismObject<UserType> userPrismObject = roleAnalysisService.getUserTypeObject(userOid, task, result);
            if (userPrismObject == null) {
                return;
            }

            PolyStringType name = userPrismObject.asObjectable().getName();

            RoleAnalysisOutlierType roleAnalysisOutlierType = new RoleAnalysisOutlierType();
            roleAnalysisOutlierType.setTargetObjectRef(new ObjectReferenceType()
                    .oid(userOid)
                    .type(UserType.COMPLEX_TYPE)
                    .targetName(name.getOrig()));
            roleAnalysisOutlierType.getOutlierPartitions().add(partition);
            roleAnalysisOutlierType.setAnomalyObjectsConfidence(partition.getPartitionAnalysis().getAnomalyObjectsConfidence());
            roleAnalysisOutlierType.setOverallConfidence(partition.getPartitionAnalysis().getOverallConfidence());
            //TODO when update? every partition?
            resolveUserDuplicateAssignment(roleAnalysisService, roleAnalysisOutlierType, userOid, task, result);
            roleAnalysisService.resolveOutliers(roleAnalysisOutlierType, task, result);
        } else {
            RoleAnalysisOutlierType roleAnalysisOutlierType = outlierObject.asObjectable();
            List<RoleAnalysisOutlierPartitionType> outlierPartitions = roleAnalysisOutlierType.getOutlierPartitions();
            //TODO just temporary confidence
            double overallConfidence = 0;
            double anomalyObjectsConfidence = 0;
            for (RoleAnalysisOutlierPartitionType outlierPartition : outlierPartitions) {
                overallConfidence += outlierPartition.getPartitionAnalysis().getOverallConfidence();
                anomalyObjectsConfidence += outlierPartition.getPartitionAnalysis().getAnomalyObjectsConfidence();
            }
            overallConfidence += partition.getPartitionAnalysis().getOverallConfidence();
            anomalyObjectsConfidence += partition.getPartitionAnalysis().getAnomalyObjectsConfidence();

            overallConfidence = overallConfidence / (outlierPartitions.size() + 1);
            anomalyObjectsConfidence = anomalyObjectsConfidence / (outlierPartitions.size() + 1);
            roleAnalysisService.addOutlierPartition(
                    roleAnalysisOutlierType.getOid(), partition, overallConfidence, anomalyObjectsConfidence, result);
        }
    }

    static double calculateAssignmentAnomalyConfidence(
            @NotNull RoleAnalysisService roleAnalysisService,
            @Nullable List<RoleAnalysisAttributeDef> attributesForUserAnalysis,
            PrismObject<UserType> userTypeObject,
            int numberOfAllUsersInRepo,
            @NotNull DetectedAnomalyResult prepareRoleOutlier,
            @NotNull AttributeAnalysisCache userAnalysisCache,
            @NotNull Task task,
            OperationResult result) {

        DetectedAnomalyStatistics statistics = prepareRoleOutlier.getStatistics();

        long startTime = System.currentTimeMillis();
        double itemFactorConfidence = calculateItemFactorConfidence(
                prepareRoleOutlier, userTypeObject, attributesForUserAnalysis, roleAnalysisService, userAnalysisCache, task, result);
        long endTime = System.currentTimeMillis();
        LOGGER.debug("Item factor confidence calculation time in seconds: {}", (endTime - startTime) / 1000);
        double distributionConfidence = statistics.getConfidenceDeviation();
        double patternConfidence = statistics.getPatternAnalysis().getConfidence();
        double roleMemberConfidence = calculateRoleCoverageConfidence(
                prepareRoleOutlier, userAnalysisCache.getRoleMemberCountCache(), roleAnalysisService, numberOfAllUsersInRepo, task, result);
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

        if (attributeAnalysisCompare.isEmpty() || averageItemFactor == 0) {
            return 0;
        }

        averageItemFactor = averageItemFactor / attributeAnalysisCompare.size();
        return averageItemFactor;
    }

    //TODO this is just for USER MODE! Implement Role (Experimental)
    static @NotNull RoleAnalysisPatternAnalysis detectAndLoadPatternAnalysis(
            @NotNull String userOid,
            @NotNull List<MiningRoleTypeChunk> miningRoleTypeChunks,
            BasicOutlierDetectionStrategy.ProcessingTimes processingTimes,
            @NotNull RoleAnalysisSessionType session,
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull Task task,
            @NotNull OperationResult result,
            @Nullable List<String> allowedProperties) {
        RoleAnalysisPatternAnalysis patternInfo = new RoleAnalysisPatternAnalysis();

        DetectionOption detectionOption = new DetectionOption(
                10, 100, 2, 2);
        List<SimpleHeatPattern> totalRelationOfPatternsForCell = new OutlierPatternResolver()
                .performSingleAnomalyCellDetection(miningRoleTypeChunks, detectionOption,
                        Collections.singletonList(userOid), allowedProperties, processingTimes);

        int patternCount = totalRelationOfPatternsForCell.size();
        int totalRelations = 0;
        int topPatternRelation = 0;
        SimpleHeatPattern topPattern = null;
        for (SimpleHeatPattern simpleHeatPattern : totalRelationOfPatternsForCell) {
            int relations = simpleHeatPattern.getTotalRelations();
            totalRelations += relations;
            if (relations > topPatternRelation) {
                topPatternRelation = relations;
                topPattern = simpleHeatPattern;
            }
        }

        if (topPattern != null) {
            Set<String> patternMembers = new HashSet<>();
            for (MiningRoleTypeChunk miningBaseTypeChunk : miningRoleTypeChunks) {
                List<String> properties = miningBaseTypeChunk.getProperties();
                if (topPattern.isPartOf(new HashSet<>(properties))) {
                    patternMembers.addAll(miningBaseTypeChunk.getMembers());
                }
            }
            DetectedPattern detectedPattern = prepareDetectedPattern(patternMembers, new HashSet<>(topPattern.getPropertiesOids()));

            RoleAnalysisDetectionPatternType pattern = new RoleAnalysisDetectionPatternType();

            Set<String> roles = new HashSet<>(detectedPattern.getRoles());
            Set<String> users = new HashSet<>(detectedPattern.getUsers());

            for (String usersRef : users) {
                pattern.getUserOccupancy().add(
                        new ObjectReferenceType().oid(usersRef).type(UserType.COMPLEX_TYPE));

            }

            for (String rolesRef : roles) {
                pattern.getRolesOccupancy().add(
                        new ObjectReferenceType().oid(rolesRef).type(RoleType.COMPLEX_TYPE)
                );
            }

            pattern.setClusterMetric(detectedPattern.getMetric());

            Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
            Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();

            List<RoleAnalysisAttributeDef> userAnalysisAttributeDef = roleAnalysisService
                    .resolveAnalysisAttributes(session, UserType.COMPLEX_TYPE);
            List<RoleAnalysisAttributeDef> roleAnalysisAttributeDef = roleAnalysisService
                    .resolveAnalysisAttributes(session, RoleType.COMPLEX_TYPE);

            roleAnalysisService.resolveDetectedPatternsAttributes(Collections.singletonList(pattern), userExistCache,
                    roleExistCache, task, result, roleAnalysisAttributeDef, userAnalysisAttributeDef);
            patternInfo.setTopDetectedPattern(pattern);
        }

        int clusterRelations = calculateOveralClusterRelationsCount(miningRoleTypeChunks);
        double topPatternCoverage = ((double) topPatternRelation / clusterRelations) * 100;

        patternInfo.setConfidence(topPatternCoverage);
        patternInfo.setDetectedPatternCount(patternCount);
        patternInfo.setTopPatternRelation(topPatternRelation);
        patternInfo.setTotalRelations(totalRelations);
        patternInfo.setClusterRelations(clusterRelations);
        return patternInfo;
    }

    //TODO this is just for USER MODE! (Experimental) Need to be optimized (MAJOR).
//    static @NotNull RoleAnalysisPatternAnalysis detectAndLoadPatternAnalysis(
//            @NotNull MiningRoleTypeChunk miningRoleTypeChunk,
//            @NotNull String user,
//            @NotNull List<MiningRoleTypeChunk> miningRoleTypeChunks,
//            BasicOutlierDetectionStrategy.ProcessingTimes processingTimes) {
//
//        List<String> allowedProperties = miningRoleTypeChunk.getProperties();
//        DetectionOption detectionOption = new DetectionOption(
//                10, 100, 2, 2);
//        List<SimpleHeatPattern> totalRelationOfPatternsForCell = new OutlierPatternResolver()
//                .performSingleAnomalyCellDetection(miningRoleTypeChunks, detectionOption,
//                        Collections.singletonList(user), allowedProperties, processingTimes);
//
//        int patternCount = totalRelationOfPatternsForCell.size();
//        int totalRelations = 0;
//        int topPatternRelation = 0;
//        for (SimpleHeatPattern simpleHeatPattern : totalRelationOfPatternsForCell) {
//            int relations = simpleHeatPattern.getTotalRelations();
//            totalRelations += relations;
//            if (relations > topPatternRelation) {
//                topPatternRelation = relations;
//            }
//        }
//
//        int clusterRelations = calculateOveralClusterRelationsCount(miningRoleTypeChunks);
//        RoleAnalysisPatternAnalysis patternInfo = new RoleAnalysisPatternAnalysis();
//        double topPatternCoverage = ((double) topPatternRelation / clusterRelations) * 100;
//        patternInfo.setConfidence(topPatternCoverage);
//        patternInfo.setDetectedPatternCount(patternCount);
//        patternInfo.setTopPatternRelation(topPatternRelation);
//        patternInfo.setTotalRelations(totalRelations);
//        patternInfo.setClusterRelations(clusterRelations);
//        return patternInfo;
//    }

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
            @NotNull DetectedAnomalyResult outlierResult,
            @NotNull PrismObject<UserType> userTypeObject,
            @Nullable List<RoleAnalysisAttributeDef> attributesForUserAnalysis,
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull AttributeAnalysisCache userAnalysisCache,
            @NotNull Task task,
            @NotNull OperationResult result) {

        ObjectReferenceType targetObjectRef = outlierResult.getTargetObjectRef();
        PrismObject<RoleType> roleTypeObject = roleAnalysisService.getRoleTypeObject(targetObjectRef.getOid(), task, result);
        if (roleTypeObject == null) {
            return 0;
        }

        if (attributesForUserAnalysis == null || attributesForUserAnalysis.isEmpty()) {
            return 0;
        }

        //TODO this take a lot of time when role is popular. Think about better solution (MAJOR).
        RoleAnalysisAttributeAnalysisResult roleAnalysisAttributeAnalysisResult = userAnalysisCache.getRoleMemberAnalysisCache(roleTypeObject.getOid());
        if (roleAnalysisAttributeAnalysisResult == null) {
            roleAnalysisAttributeAnalysisResult = roleAnalysisService
                    .resolveRoleMembersAttributeCached(roleTypeObject.getOid(), userAnalysisCache, task, result, attributesForUserAnalysis);
            userAnalysisCache.putRoleMemberAnalysisCache(roleTypeObject.getOid(), roleAnalysisAttributeAnalysisResult);
        }

        RoleAnalysisAttributeAnalysisResult userAttributes = userAnalysisCache.getUserAttributeAnalysisCache(userTypeObject.getOid());
        if (userAttributes == null) {
            userAttributes = roleAnalysisService.resolveUserAttributes(userTypeObject, attributesForUserAnalysis);
            userAnalysisCache.putUserAttributeAnalysisCache(userTypeObject.getOid(), userAttributes);
        }

        RoleAnalysisAttributeAnalysisResult compareAttributeResult = roleAnalysisService
                .resolveSimilarAspect(userAttributes, roleAnalysisAttributeAnalysisResult);

        DetectedAnomalyStatistics statistics = outlierResult.getStatistics();
        AttributeAnalysis attributeAnalysisContainer = new AttributeAnalysis();
        attributeAnalysisContainer.setUserRoleMembersCompare(compareAttributeResult);
        attributeAnalysisContainer.setRoleAttributeAnalysisResult(roleAnalysisAttributeAnalysisResult);
        statistics.setAttributeAnalysis(attributeAnalysisContainer);

        double averageItemsOccurs = 0;
        assert compareAttributeResult != null;
        List<RoleAnalysisAttributeAnalysis> attributeAnalysis = compareAttributeResult.getAttributeAnalysis();
        for (RoleAnalysisAttributeAnalysis analysis : attributeAnalysis) {
            Double density = analysis.getDensity();
            if (density != null) {
                averageItemsOccurs += density;
            }
        }

        if (averageItemsOccurs == 0 || attributeAnalysis.isEmpty()) {
            return 0;
        }

        statistics.setItemFactorConfidence(averageItemsOccurs / attributeAnalysis.size());

        return averageItemsOccurs / attributeAnalysis.size();
    }

    //TODO test
    public static double calculateRoleCoverageConfidence(
            @NotNull DetectedAnomalyResult outlierResult,
            @NotNull RoleMemberCountCache userAnalysisCache,
            @NotNull RoleAnalysisService roleAnalysisService,
            int numberOfAllUsersInRepo,
            @NotNull Task task,
            @NotNull OperationResult result) {
        ObjectReferenceType targetObjectRef = outlierResult.getTargetObjectRef();
        int roleMemberCount;
        Integer roleCount = userAnalysisCache.get(targetObjectRef.getOid());
        if (roleCount == null) {
            roleMemberCount = roleAnalysisService.countUserTypeMembers(null,
                    new HashSet<>(Collections.singleton(targetObjectRef.getOid())),
                    task, result);
            userAnalysisCache.put(targetObjectRef.getOid(), roleMemberCount);
        } else {
            roleMemberCount = roleCount;
        }
        if (roleMemberCount == 0) {
            return 0;
        }

        double memberPercentageRepo = (((double) roleMemberCount / numberOfAllUsersInRepo) * 100);
        outlierResult.getStatistics().setMemberCoverageConfidence(memberPercentageRepo);
        return memberPercentageRepo;
    }

    public static double calculateOutlierPropertyCoverageConfidence(@NotNull DetectedAnomalyResult outlierResult) {
        double occurInCluster = outlierResult.getStatistics().getFrequency() * 100;
        outlierResult.getStatistics().setOutlierCoverageConfidence(occurInCluster);
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

    //TODO incorrect use membershipRef?
    static void resolveUserDuplicateAssignment(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisOutlierType roleAnalysisOutlierType,
            @NotNull String userOid,
            @NotNull Task task,
            @NotNull OperationResult result) {
        PrismObject<UserType> userPrismObject = roleAnalysisService.getUserTypeObject(userOid, task, result);
        if (userPrismObject == null) {
            return;
        }

        List<ObjectReferenceType> duplicatedRoleAssignment = roleAnalysisOutlierType.getDuplicatedRoleAssignment();
        UserType userObject = userPrismObject.asObjectable();
        List<String> rolesOidAssignment = getRolesOidAssignment(userObject);
        for (String roleOid : rolesOidAssignment) {
            PrismObject<RoleType> roleAssignment = roleAnalysisService.getRoleTypeObject(roleOid, task, result);
            if (roleAssignment != null) {
                List<String> rolesOidInducement = getRolesOidInducement(roleAssignment.asObjectable());
                for (String roleOidInducement : rolesOidInducement) {
                    if (rolesOidAssignment.contains(roleOidInducement)) {
                        ObjectReferenceType ref = new ObjectReferenceType()
                                .oid(roleOidInducement)
                                .type(RoleType.COMPLEX_TYPE);
                        duplicatedRoleAssignment.add(ref);
                    }
                }
            }
        }
    }

}
