/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier;

import static com.evolveum.midpoint.common.mining.utils.ExtractPatternUtils.prepareDetectedPattern;
import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.*;

import java.util.*;

import com.evolveum.midpoint.common.mining.objects.statistic.UserAccessDistribution;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutlierAttributeResolver.UnusualAttributeValueResult;

import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.explanation.ExplanationUtil;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import com.google.common.collect.ListMultimap;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.analysis.cache.AttributeAnalysisCache;
import com.evolveum.midpoint.common.mining.objects.analysis.cache.RoleMemberCountCache;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.objects.detection.PatternDetectionOption;
import com.evolveum.midpoint.common.mining.utils.values.FrequencyItem;
import com.evolveum.midpoint.common.mining.utils.values.ZScoreData;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.context.OutlierPatternResolver;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.object.SimpleHeatPattern;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

//TODO major multiple thinks is processed multiple times
// (Create structure for caching these data, NOTE: use also clustering process there is multiple mapped structures that can be used)
public class OutliersDetectionUtil {

    private OutliersDetectionUtil() {
    }

    public static void updateOrImportOutlierObject(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisSessionType session,
            @NotNull String userOid,
            @NotNull RoleAnalysisOutlierPartitionType partition,
            @NotNull AttributeAnalysisCache userAnalysisCache,
            @NotNull Task task,
            @NotNull OperationResult result) {
        RoleAnalysisDetectionOptionType detectionOption = session.getDefaultDetectionOption();
        Double sensitivity = detectionOption.getSensitivity();
        if (sensitivity == null) {
            sensitivity = 0.8;
        }
        double requiredConfidence = roleAnalysisService.calculateOutlierConfidenceRequired(sensitivity);

        //TODO temporary solution // move outside from *
        requiredConfidence = requiredConfidence * 100;

        RoleAnalysisPartitionAnalysisType partitionAnalysis = partition.getPartitionAnalysis();
        //TODO whats if cluster is < requiredConfidence and clusterAnomalyObjectsConfidence > requiredConfidence,
        // should we inform manager about specific outlier type?
//        Double clusterConfidence = partitionAnalysis.getOverallConfidence();
//        Double clusterAnomalyObjectsConfidence = partition.getPartitionAnalysis().getAnomalyObjectsConfidence();
//        if (clusterConfidence == null
//                || (clusterConfidence < requiredConfidence
//                && clusterAnomalyObjectsConfidence < requiredConfidence)) {
//            return;
//        }

        Double partitionOverallConfidence = partitionAnalysis.getOverallConfidence();
        if (partitionOverallConfidence == null || partitionOverallConfidence < requiredConfidence) {
            return;
        }

        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();

        List<RoleAnalysisAttributeDef> userAnalysisAttributeDef = roleAnalysisService
                .resolveAnalysisAttributes(session, UserType.COMPLEX_TYPE);
        List<RoleAnalysisAttributeDef> roleAnalysisAttributeDef = roleAnalysisService
                .resolveAnalysisAttributes(session, RoleType.COMPLEX_TYPE);

        RoleAnalysisPatternAnalysis patternAnalysis = partitionAnalysis.getPatternAnalysis();
        if (patternAnalysis != null) {
            RoleAnalysisDetectionPatternType topDetectedPattern = patternAnalysis.getTopDetectedPattern();
            if (topDetectedPattern != null) {
                roleAnalysisService.resolveDetectedPatternsAttributesCached(Collections.singletonList(topDetectedPattern), userExistCache,
                        roleExistCache, userAnalysisCache, roleAnalysisAttributeDef, userAnalysisAttributeDef, task, result);
            }
        }

        List<DetectedAnomalyResult> detectedAnomalyResults = partition.getDetectedAnomalyResult();

        detectedAnomalyResults.forEach(detectedAnomalyResult -> {
            RoleAnalysisPatternAnalysis pattern = detectedAnomalyResult.getStatistics().getPatternAnalysis();
            if (pattern != null) {
                RoleAnalysisDetectionPatternType topDetectedPattern = pattern.getTopDetectedPattern();
                if (topDetectedPattern != null) {
                    roleAnalysisService.resolveDetectedPatternsAttributesCached(Collections.singletonList(topDetectedPattern), userExistCache,
                            roleExistCache, userAnalysisCache, roleAnalysisAttributeDef, userAnalysisAttributeDef, task, result);
                }
            }
        });

        //TODO to *

        importOrExtendOutlier(roleAnalysisService, userOid, partition, task, result);
    }

    public static void importOrExtendOutlier(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull String userOid,
            @NotNull RoleAnalysisOutlierPartitionType partition,
            @NotNull Task task,
            @NotNull OperationResult result) {
        PrismObject<RoleAnalysisOutlierType> outlierPrismObject = roleAnalysisService.searchOutlierObjectByUserOid(
                userOid, task, result);

        ExplanationUtil.uploadOutlierExplanation(roleAnalysisService, userOid, partition, task, result);

        if (outlierPrismObject == null) {
            PrismObject<UserType> userPrismObject = roleAnalysisService.getUserTypeObject(userOid, task, result);
            if (userPrismObject == null) {
                return;
            }

            PolyStringType name = userPrismObject.asObjectable().getName();

            RoleAnalysisOutlierType roleAnalysisOutlierType = new RoleAnalysisOutlierType();
            roleAnalysisOutlierType.setObjectRef(new ObjectReferenceType()
                    .oid(userOid)
                    .type(UserType.COMPLEX_TYPE)
                    .targetName(name.getOrig()));
            roleAnalysisOutlierType.getPartition().add(partition);
            roleAnalysisOutlierType.setAnomalyObjectsConfidence(partition.getPartitionAnalysis().getAnomalyObjectsConfidence());
            roleAnalysisOutlierType.setOverallConfidence(partition.getPartitionAnalysis().getOverallConfidence());
            //TODO when update? every partition?
            resolveUserDuplicateAssignment(roleAnalysisService, roleAnalysisOutlierType, userOid, task, result);
            roleAnalysisOutlierType.getExplanation().addAll(CloneUtil.cloneCollectionMembers(partition.getExplanation()));
            roleAnalysisService.resolveOutliers(roleAnalysisOutlierType, task, result);
        } else {
            RoleAnalysisOutlierType roleAnalysisOutlierType = outlierPrismObject.asObjectable();
            List<RoleAnalysisOutlierPartitionType> outlierPartitions = roleAnalysisOutlierType.getPartition();
            //TODO just temporary confidence
            double overallConfidence = 0;
            double anomalyObjectsConfidence = 0;
            RoleAnalysisPartitionAnalysisType newPartitionAnalysis = partition.getPartitionAnalysis();
            Double newPartitionOverallConfidence = newPartitionAnalysis.getOverallConfidence();
            List<OutlierDetectionExplanationType> newOutlierExplanation = partition.getExplanation();
            for (RoleAnalysisOutlierPartitionType outlierPartition : outlierPartitions) {
                RoleAnalysisPartitionAnalysisType partitionAnalysis = outlierPartition.getPartitionAnalysis();
                Double partitionOveralConfidence = partitionAnalysis.getOverallConfidence();
                overallConfidence += partitionOveralConfidence;
                anomalyObjectsConfidence += partitionAnalysis.getAnomalyObjectsConfidence();

                if (partitionOveralConfidence >= newPartitionOverallConfidence) {
                    newPartitionOverallConfidence = partitionOveralConfidence;
                    newOutlierExplanation = outlierPartition.getExplanation();
                }
            }
            overallConfidence += partition.getPartitionAnalysis().getOverallConfidence();
            anomalyObjectsConfidence += partition.getPartitionAnalysis().getAnomalyObjectsConfidence();

            overallConfidence = overallConfidence / (outlierPartitions.size() + 1);
            anomalyObjectsConfidence = anomalyObjectsConfidence / (outlierPartitions.size() + 1);
            roleAnalysisService.addOutlierPartition(
                    roleAnalysisOutlierType.getOid(), partition, newOutlierExplanation, overallConfidence, anomalyObjectsConfidence, result);
        }
    }

    private static void calculateAndLoadUnusualRoleMembersAttributeAnalysis(@NotNull DetectedAnomalyStatistics statistics) {
        AttributeAnalysis attributeAnalysis1 = statistics.getAttributeAnalysis();
        RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResult = attributeAnalysis1.getRoleAttributeAnalysisResult();
        if (roleAttributeAnalysisResult != null) {
            OutlierAttributeResolver attributeResolver = new OutlierAttributeResolver(0.2);
            List<RoleAnalysisAttributeAnalysis> attributeAnalysis = roleAttributeAnalysisResult.getAttributeAnalysis();
            List<UnusualAttributeValueResult> unusualAttributeValueResults = attributeResolver.resolveUnusualAttributes(attributeAnalysis, attributeAnalysis);
            loadUnusualAttributesAndValues(roleAttributeAnalysisResult, unusualAttributeValueResults);
        }
    }

    private static List<UnusualAttributeValueResult> calculateUnusualAttributeResults(
            @NotNull DetectedAnomalyResult anomalyResult,
            @NotNull PrismObject<UserType> userTypeObject,
            @Nullable List<RoleAnalysisAttributeDef> attributesForUserAnalysis,
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull AttributeAnalysisCache userAnalysisCache,
            @NotNull Task task,
            @NotNull OperationResult result
    ) {
        ObjectReferenceType targetObjectRef = anomalyResult.getTargetObjectRef();
        PrismObject<RoleType> roleTypeObject = roleAnalysisService.getRoleTypeObject(targetObjectRef.getOid(), task, result);
        if (roleTypeObject == null) {
            return Collections.emptyList();
        }
        if (attributesForUserAnalysis == null || attributesForUserAnalysis.isEmpty()) {
            return Collections.emptyList();
        }
        RoleAnalysisAttributeAnalysisResult roleMemberAttributeAnalysisResult = getRoleMemberAnalysis(roleTypeObject, userAnalysisCache, roleAnalysisService, attributesForUserAnalysis, task, result);
        RoleAnalysisAttributeAnalysisResult userAttributes = getUserAttributeAnalysis(userTypeObject, userAnalysisCache, roleAnalysisService, attributesForUserAnalysis);

        OutlierAttributeResolver attributeResolver = new OutlierAttributeResolver(0.2);
        List<RoleAnalysisAttributeAnalysis> roleMemberAttributeDetails = roleMemberAttributeAnalysisResult.getAttributeAnalysis();
        List<RoleAnalysisAttributeAnalysis> userAttributeDetails = userAttributes.getAttributeAnalysis();

        return attributeResolver.resolveUnusualAttributes(roleMemberAttributeDetails, userAttributeDetails);
    }

    //TODO it does more than just anomaly confidence calculation. Refactor, split, rename.
    public static double calculateAssignmentAnomalyConfidence(
            @NotNull RoleAnalysisService roleAnalysisService,
            @Nullable List<RoleAnalysisAttributeDef> attributesForUserAnalysis,
            PrismObject<UserType> userTypeObject,
            int numberOfAllUsersInRepo,
            @NotNull DetectedAnomalyResult anomalyResult,
            @NotNull AttributeAnalysisCache userAnalysisCache,
            @NotNull Task task,
            @NotNull OperationResult result) {

        DetectedAnomalyStatistics statistics = anomalyResult.getStatistics();

        long startTime = System.currentTimeMillis();
        double itemFactorConfidence = calculateItemFactorConfidence(
                anomalyResult, userTypeObject, attributesForUserAnalysis, roleAnalysisService, userAnalysisCache, task, result);
        long endTime = System.currentTimeMillis();
        LOGGER.debug("ITEM FACTOR CONFIDENCE: Item factor confidence calculation time in ms: {}", (endTime - startTime));

        List<UnusualAttributeValueResult> unusualAttributeResults = calculateUnusualAttributeResults(
                anomalyResult, userTypeObject, attributesForUserAnalysis, roleAnalysisService, userAnalysisCache, task, result
        );

        calculateAndLoadUnusualRoleMembersAttributeAnalysis(statistics);

        loadUnusualDetectedAnomalyAttributeStatistics(statistics, unusualAttributeResults);

        double distributionConfidence = statistics.getConfidenceDeviation();
        double patternConfidence = getPatternConfidence(statistics.getPatternAnalysis());

        double roleMemberConfidence = calculateRoleCoverageConfidence(
                anomalyResult, userAnalysisCache.getRoleMemberCountCache(), roleAnalysisService, numberOfAllUsersInRepo, task, result);
        double coverageConfidence = calculateOutlierPropertyCoverageConfidence(anomalyResult);

        //TODO disable distributionConfidenceDiff
        double distributionConfidenceDiff = distributionConfidence * 100;
        double patternConfidenceDiff = 100 - patternConfidence;
        double itemFactorConfidenceDiff = 100 - itemFactorConfidence;
        double roleMemberConfidenceDiff = 100 - roleMemberConfidence;
        double coverageConfidenceDiff = 100 - coverageConfidence;

        //TODO TBD ((patternConfidenceDiff + itemFactorConfidenceDiff
        //                + roleMemberConfidenceDiff + coverageConfidenceDiff) / 4) * outlierClusterConfidence;
        return (distributionConfidenceDiff + patternConfidenceDiff + itemFactorConfidenceDiff
                + roleMemberConfidenceDiff + coverageConfidenceDiff) / 5;
    }

    private static void loadUnusualDetectedAnomalyAttributeStatistics(
            @Nullable DetectedAnomalyStatistics statistics,
            @Nullable List<UnusualAttributeValueResult> unusualAttributeResults) {

        if (statistics == null || unusualAttributeResults == null) {
            return;
        }

        AttributeAnalysis attributeAnalysis = statistics.getAttributeAnalysis();
        RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = attributeAnalysis.getUserAttributeAnalysisResult();
        RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResult = attributeAnalysis.getRoleAttributeAnalysisResult();
        RoleAnalysisAttributeAnalysisResult userRoleMembersCompare = attributeAnalysis.getUserRoleMembersCompare();

        if (userAttributeAnalysisResult != null) {
            loadUnusualAttributesAndValues(userAttributeAnalysisResult, unusualAttributeResults);
        }

        if (userRoleMembersCompare != null) {
            loadUnusualAttributesAndValues(userRoleMembersCompare, unusualAttributeResults);
        }
    }

    /**
     * Loads unusual attributes and values to the attribute analysis container.
     *
     * @param attributeAnalysisContainer The attribute analysis container for mapping.
     * @param unusualAttributeResults The list of unusual attribute results to process.
     */
    private static void loadUnusualAttributesAndValues(
            @NotNull RoleAnalysisAttributeAnalysisResult attributeAnalysisContainer,
            @NotNull List<UnusualAttributeValueResult> unusualAttributeResults) {

        List<RoleAnalysisAttributeAnalysis> userAttributeAnalysisResult = attributeAnalysisContainer.getAttributeAnalysis();

        for (UnusualAttributeValueResult unusualAttributeResult : unusualAttributeResults) {
            ItemPathType path = unusualAttributeResult.path();

            RoleAnalysisAttributeAnalysis attributeAnalysisDetail = userAttributeAnalysisResult.stream()
                    .filter(a -> a.getItemPath().equals(path))
                    .findFirst()
                    .orElse(null);
            if (attributeAnalysisDetail == null) {
                continue;
            }
            List<RoleAnalysisAttributeStatistics> attributeStatistics = attributeAnalysisDetail.getAttributeStatistics();
            List<OutlierAttributeResolver.UnusualSingleValueDetail> unusualSingleValueDetails = unusualAttributeResult.partialResults();
            for (RoleAnalysisAttributeStatistics attributeStatistic : attributeStatistics) {
                if (attributeStatistic.getAttributeValue() == null) {
                    continue;
                }

                attributeStatistic.setIsUnusual(unusualSingleValueDetails.stream()
                        .anyMatch(u -> u.value().equals(attributeStatistic.getAttributeValue()) && u.isUnusual()));
            }

            attributeAnalysisDetail.isUnusual(unusualAttributeResult.isUnusual());
        }
    }

    public static double getWeightedItemFactorConfidence(@Nullable RoleAnalysisAttributeAnalysisResult compareAttributeResult) {
        if (compareAttributeResult == null) {
            return 0;
        }

        List<RoleAnalysisAttributeAnalysis> attributeAnalysis = compareAttributeResult.getAttributeAnalysis();
        if (attributeAnalysis.isEmpty()) {
            return 0;
        }

        double totalWeightedDensity = 0.0;
        double totalWeight = 0.0;
        for (RoleAnalysisAttributeAnalysis analysisItem : attributeAnalysis) {
            Double density = analysisItem.getDensity();
            Double weight = analysisItem.getWeight();

            totalWeightedDensity += density * weight;
            totalWeight += weight;
        }

        return totalWeight > 0 ? totalWeightedDensity / totalWeight : 0.0;
    }

    //TODO this is just for USER MODE! Implement Role (Experimental)
    public static @NotNull RoleAnalysisPatternAnalysis detectAndLoadPatternAnalysis(
            @NotNull String userOid,
            @NotNull List<MiningRoleTypeChunk> miningRoleTypeChunks,
            @NotNull RoleAnalysisSessionType session,
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull Task task,
            @NotNull OperationResult result,
            @Nullable List<String> allowedProperties,
            @NotNull AttributeAnalysisCache userAnalysisCache,
            boolean includeAttributeAnalysis) {
        RoleAnalysisPatternAnalysis patternInfo = new RoleAnalysisPatternAnalysis();

        //TODO take from session detection option
        PatternDetectionOption detectionOption = new PatternDetectionOption(
                10, 100, 2, 2);
        List<SimpleHeatPattern> totalRelationOfPatternsForCell = new OutlierPatternResolver()
                .performSingleAnomalyCellDetection(miningRoleTypeChunks, detectionOption,
                        Collections.singletonList(userOid), allowedProperties);

        //TODO simplify until not needed
        int patternCount = totalRelationOfPatternsForCell.size();
        MutableInt totalRelations = new MutableInt(0);
        MutableInt topPatternRelation = new MutableInt(0);
        SimpleHeatPattern topPattern = resolveTopPattern(totalRelationOfPatternsForCell, totalRelations, topPatternRelation);

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
            mapPatternRefs(users, pattern, roles);

            pattern.setReductionCount(detectedPattern.getMetric());

            if (includeAttributeAnalysis) {
                Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
                Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();

                List<RoleAnalysisAttributeDef> userAnalysisAttributeDef = roleAnalysisService
                        .resolveAnalysisAttributes(session, UserType.COMPLEX_TYPE);
                List<RoleAnalysisAttributeDef> roleAnalysisAttributeDef = roleAnalysisService
                        .resolveAnalysisAttributes(session, RoleType.COMPLEX_TYPE);

                //TODO performance test  // 20 - 160ms on test data.
                roleAnalysisService.resolveDetectedPatternsAttributesCached(Collections.singletonList(pattern), userExistCache,
                        roleExistCache, userAnalysisCache, roleAnalysisAttributeDef, userAnalysisAttributeDef, task, result);

            }
            patternInfo.setTopDetectedPattern(pattern);
        }

        int clusterRelations = calculateOveralClusterRelationsCount(miningRoleTypeChunks);

        double topPatternCoverage = 0;
        if (clusterRelations != 0) {
            topPatternCoverage = ((double) topPatternRelation.getValue() / clusterRelations) * 100;
        }

        patternInfo.setConfidence(topPatternCoverage);
        patternInfo.setDetectedPatternCount(patternCount);
        patternInfo.setTopPatternRelation(topPatternRelation.getValue());
        patternInfo.setTotalRelations(totalRelations.getValue());
        patternInfo.setClusterRelations(clusterRelations);
        return patternInfo;
    }

    private static SimpleHeatPattern resolveTopPattern(
            @NotNull List<SimpleHeatPattern> totalRelationOfPatternsForCell,
            @NotNull MutableInt totalRelations,
            @NotNull MutableInt topPatternRelation) {
        int tmpTotalRelations = 0;
        int tmpTopPatternRelation = 0;
        SimpleHeatPattern topPattern = null;
        for (SimpleHeatPattern simpleHeatPattern : totalRelationOfPatternsForCell) {
            int relations = simpleHeatPattern.getTotalRelations();
            tmpTotalRelations += relations;
            if (relations > tmpTopPatternRelation) {
                tmpTopPatternRelation = relations;
                topPattern = simpleHeatPattern;
            }
        }
        totalRelations.setValue(tmpTotalRelations);
        topPatternRelation.setValue(tmpTopPatternRelation);
        return topPattern;
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

    private static RoleAnalysisAttributeAnalysisResult getRoleMemberAnalysis(
            @NotNull PrismObject<RoleType> roleTypeObject,
            @NotNull AttributeAnalysisCache cache,
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull List<RoleAnalysisAttributeDef> attributesForUserAnalysis,
            @NotNull Task task,
            @NotNull OperationResult result
    ) {
        String oid = roleTypeObject.getOid();
        RoleAnalysisAttributeAnalysisResult roleAnalysisAttributeAnalysisResult = cache.getRoleMemberAnalysisCache(oid);
        if (roleAnalysisAttributeAnalysisResult == null) {
            roleAnalysisAttributeAnalysisResult = roleAnalysisService.resolveRoleMembersAttributeCached(oid, cache, task, result, attributesForUserAnalysis);
            cache.putRoleMemberAnalysisCache(oid, roleAnalysisAttributeAnalysisResult);
        }
        return roleAnalysisAttributeAnalysisResult;
    }

    private static RoleAnalysisAttributeAnalysisResult getUserAttributeAnalysis(
            @NotNull PrismObject<UserType> userTypeObject,
            @NotNull AttributeAnalysisCache cache,
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull List<RoleAnalysisAttributeDef> attributesForUserAnalysis
    ) {
        String oid = userTypeObject.getOid();
        RoleAnalysisAttributeAnalysisResult userAttributes = cache.getUserAttributeAnalysisCache(oid);
        if (userAttributes == null) {
            userAttributes = roleAnalysisService.resolveUserAttributes(userTypeObject, attributesForUserAnalysis);
            cache.putUserAttributeAnalysisCache(oid, userAttributes);
        }
        return userAttributes;
    }

    private static double calculateItemFactorConfidence(
            @NotNull DetectedAnomalyResult anomalyResult,
            @NotNull PrismObject<UserType> userTypeObject,
            @Nullable List<RoleAnalysisAttributeDef> attributesForUserAnalysis,
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull AttributeAnalysisCache userAnalysisCache,
            @NotNull Task task,
            @NotNull OperationResult result) {

        ObjectReferenceType targetObjectRef = anomalyResult.getTargetObjectRef();
        PrismObject<RoleType> roleTypeObject = roleAnalysisService.getRoleTypeObject(targetObjectRef.getOid(), task, result);
        if (roleTypeObject == null) {
            return 0;
        }

        if (attributesForUserAnalysis == null || attributesForUserAnalysis.isEmpty()) {
            return 0;
        }

        //TODO this take a lot of time when role is popular. Think about better solution (MAJOR).
        RoleAnalysisAttributeAnalysisResult roleAnalysisAttributeAnalysisResult = getRoleMemberAnalysis(roleTypeObject, userAnalysisCache, roleAnalysisService, attributesForUserAnalysis, task, result);
        RoleAnalysisAttributeAnalysisResult userAttributes = getUserAttributeAnalysis(userTypeObject, userAnalysisCache, roleAnalysisService, attributesForUserAnalysis);

        RoleAnalysisAttributeAnalysisResult compareAttributeResult = roleAnalysisService
                .resolveSimilarAspect(userAttributes, roleAnalysisAttributeAnalysisResult);

        DetectedAnomalyStatistics statistics = anomalyResult.getStatistics();
        AttributeAnalysis attributeAnalysisContainer = new AttributeAnalysis();
        attributeAnalysisContainer.setUserAttributeAnalysisResult(userAttributes);
        attributeAnalysisContainer.setUserRoleMembersCompare(compareAttributeResult);
        attributeAnalysisContainer.setRoleAttributeAnalysisResult(roleAnalysisAttributeAnalysisResult);
        statistics.setAttributeAnalysis(attributeAnalysisContainer);

        double weightedItemFactorConfidence = getWeightedItemFactorConfidence(compareAttributeResult);
        statistics.setItemFactorConfidence(weightedItemFactorConfidence);

        return weightedItemFactorConfidence;
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
        FrequencyType frequencyType = new FrequencyType();
        frequencyType.setPercentageRatio(memberPercentageRepo);
        frequencyType.setValueRatio(roleMemberCount);
        frequencyType.setEntiretyCount(numberOfAllUsersInRepo);

        outlierResult.getStatistics().setMemberCoverageConfidenceStat(frequencyType);
        return memberPercentageRepo;
    }

    public static double calculateOutlierPropertyCoverageConfidence(@NotNull DetectedAnomalyResult outlierResult) {
        DetectedAnomalyStatistics statistics = outlierResult.getStatistics();
        if (statistics == null) {
            return 0;
        }

        double occurInCluster = getFrequencyStat(statistics); //use getGroupFrequency().getPercentageRatio()
        outlierResult.getStatistics().setOutlierCoverageConfidence(occurInCluster);
        return occurInCluster;
    }

    @Deprecated
    private static double getFrequencyStat(@NotNull DetectedAnomalyStatistics statistics){
        FrequencyType groupFrequency = statistics.getGroupFrequency();
        if(groupFrequency == null || groupFrequency.getPercentageRatio() == null){
            Double frequency = statistics.getFrequency();
            return frequency != null ? frequency : 0;
        }else{
            return statistics.getGroupFrequency().getPercentageRatio();
        }
    }

    public static double calculateOutlierRoleAssignmentFrequencyConfidence(@NotNull AttributeAnalysisCache analysisCache, @NotNull PrismObject<UserType> prismUser,
            int allRolesForGroup) {
        ListMultimap<String, String> userMemberCache = analysisCache.getUserMemberCache();
        List<String> rolesOidAssignment = userMemberCache.get(prismUser.getOid());

        if (rolesOidAssignment.isEmpty()) {
            rolesOidAssignment = getRolesOidAssignment(prismUser.asObjectable());
        }
        int userRolesCount = rolesOidAssignment.size();
        return ((double) userRolesCount / allRolesForGroup) * 100;
    }

    @NotNull
    public static RoleAnalysisDetectionOptionType prepareDetectionOptions(@NotNull RoleAnalysisSessionType session) {
        RoleAnalysisDetectionOptionType defaultDetectionOption = session.getDefaultDetectionOption();
        double minFrequency = 2;
        double maxFrequency = 2;
        double frequencyThreshold = 50.0;

        if (defaultDetectionOption != null) {
            if (defaultDetectionOption.getStandardDeviation() != null) {
                RangeType frequencyRange = defaultDetectionOption.getStandardDeviation();
                if (frequencyRange.getMin() != null) {
                    minFrequency = frequencyRange.getMin().intValue();
                }
                if (frequencyRange.getMax() != null) {
                    maxFrequency = frequencyRange.getMax().intValue();
                }
            }
            if (defaultDetectionOption.getFrequencyThreshold() != null) {
                frequencyThreshold = defaultDetectionOption.getFrequencyThreshold();
            }
        }

        RoleAnalysisDetectionOptionType detectionOption = new RoleAnalysisDetectionOptionType();
        detectionOption.setStandardDeviation(new RangeType().min(minFrequency).max(maxFrequency));
        detectionOption.setFrequencyThreshold(frequencyThreshold);
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

        UserAccessDistribution userAccessDistribution = roleAnalysisService.resolveUserAccessDistribution(
                userPrismObject, task, result);

        List<ObjectReferenceType> duplicates = userAccessDistribution.getDuplicates();

        if (duplicates != null) {
            List<ObjectReferenceType> duplicatedRoleAssignment = roleAnalysisOutlierType.getDuplicatedRoleAssignment();
            duplicatedRoleAssignment.addAll(CloneUtil.cloneCollectionMembers(duplicates));
        }
    }

    //TODO TBD partitionAnomaliesConfidence * clusterConfidence
    public static double calculatePartitionOverallConfidence(double clusterConfidence, double partitionAnomaliesConfidence) {
        double overallConfidence = 0;
        double confidenceSum = clusterConfidence + partitionAnomaliesConfidence;
        if (confidenceSum != 0) {
            overallConfidence = confidenceSum / 2;
        }
        return overallConfidence;
    }

    //TODO
    public static double calculatePartitionClusterConfidence(
            double assignmentFrequencyConfidence,
            double outlierPatternConfidence,
            double averageItemFactor,
            double density) {
        double clusterConfidence = assignmentFrequencyConfidence
                + outlierPatternConfidence
                + averageItemFactor;

        clusterConfidence += density;
        if (clusterConfidence != 0) {
            clusterConfidence = clusterConfidence / 4;
        }
        return clusterConfidence;
    }

    public static double calculatePartitionAnomaliesConfidence
            (@NotNull Collection<DetectedAnomalyResult> detectedAnomalyResults) {
        double partitionAnomaliesConfidence = 0;
        for (DetectedAnomalyResult prepareRoleOutlier : detectedAnomalyResults) {
            Double confidence = prepareRoleOutlier.getStatistics().getConfidence();
            partitionAnomaliesConfidence += confidence;
        }
        if (partitionAnomaliesConfidence != 0 && !detectedAnomalyResults.isEmpty()) {
            partitionAnomaliesConfidence = partitionAnomaliesConfidence / detectedAnomalyResults.size();
        }
        return partitionAnomaliesConfidence;
    }

    public static RoleAnalysisOutlierPartitionType analyzeAndResolveOutlierObject(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull AttributeAnalysisCache analysisCache,
            @NotNull OutlierAnalyzeModel analysisModel,
            Collection<DetectedAnomalyResult> detectedAnomalyResults,
            @NotNull Task task,
            @NotNull OperationResult result) {
        RoleAnalysisClusterType analysisCluster = analysisModel.getAnalysisCluster();
        String memberOid = analysisModel.getAnalyzedObjectRef().getOid();
        double similarityThreshold = analysisModel.getSimilarityThreshold();
        PrismObject<UserType> userObject = analysisModel.getUserObject();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = analysisModel.getMiningRoleTypeChunks();

        RoleAnalysisSessionType session = analysisModel.getSession();
        ObjectReferenceType clusterRef = analysisModel.getClusterRef();
        ObjectReferenceType sessionRef = analysisModel.getSessionRef();
        List<RoleAnalysisAttributeDef> attributesForUserAnalysis = analysisModel.getAttributesForUserAnalysis();

        AnalysisClusterStatisticType clusterStatistics1 = analysisCluster.getClusterStatistics();
        int countOfRoles = clusterStatistics1.getRolesCount();

        RoleAnalysisOutlierPartitionType partitionType = new RoleAnalysisOutlierPartitionType();
        partitionType.setClusterRef(clusterRef.clone());
        partitionType.setTargetSessionRef(sessionRef.clone());
        partitionType.setCreateTimestamp(XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis()));

        RoleAnalysisPartitionAnalysisType partitionAnalysis = new RoleAnalysisPartitionAnalysisType();

        OutlierCategoryType outlierCategory = partitionAnalysis.getOutlierCategory();
        if (outlierCategory == null) {
            outlierCategory = new OutlierCategoryType();
            outlierCategory.setOutlierNoiseCategory(analysisModel.getNoiseCategory());
            outlierCategory.setOutlierClusterCategory(analysisModel.getOutlierCategory());
            partitionAnalysis.setOutlierCategory(outlierCategory);
        } else {
            outlierCategory.setOutlierNoiseCategory(analysisModel.getNoiseCategory());
            outlierCategory.setOutlierClusterCategory(analysisModel.getOutlierCategory());
        }
        outlierCategory.setOutlierSpecificCategory(OutlierSpecificCategoryType.ACCESS_NOISE);

        //Resolve similar objects analysis
        RoleAnalysisOutlierSimilarObjectsAnalysisResult similarObjectAnalysis = new RoleAnalysisOutlierSimilarObjectsAnalysisResult();
        similarObjectAnalysis.setSimilarObjectsThreshold(similarityThreshold);
        similarObjectAnalysis.setSimilarObjectsCount(analysisCluster.getMember().size());

        Double density = clusterStatistics1.getMembershipDensity();
        if (density == null) {
            density = 0.0;
        }
        AnalysisClusterStatisticType clusterStatistics = analysisCluster.getClusterStatistics();
        similarObjectAnalysis.setSimilarObjectsDensity(density);
        //TODO store just useful information
        similarObjectAnalysis.setClusterStatistics(clusterStatistics);

        similarObjectAnalysis.getSimilarObjects().addAll(CloneUtil.cloneCollectionMembers(analysisCluster.getMember()));
        partitionAnalysis.setSimilarObjectAnalysis(similarObjectAnalysis);

        RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = clusterStatistics
                .getUserAttributeAnalysisResult();

        RoleAnalysisAttributeAnalysisResult compareAttributeResult = null;
        if (userAttributeAnalysisResult != null && attributesForUserAnalysis != null) {

            RoleAnalysisAttributeAnalysisResult userAttributes = getUserAttributeAnalysis(userObject, analysisCache, roleAnalysisService, attributesForUserAnalysis);

            compareAttributeResult = roleAnalysisService
                    .resolveSimilarAspect(userAttributes, userAttributeAnalysisResult);

            AttributeAnalysis attributeAnalysis = new AttributeAnalysis();
            attributeAnalysis.setUserAttributeAnalysisResult(userAttributeAnalysisResult);
            attributeAnalysis.setUserClusterCompare(compareAttributeResult);
            partitionAnalysis.setAttributeAnalysis(attributeAnalysis);
        }

        double assignmentFrequencyConfidence = calculateOutlierRoleAssignmentFrequencyConfidence(analysisCache,
                userObject, countOfRoles);
        partitionAnalysis.setOutlierAssignmentFrequencyConfidence(assignmentFrequencyConfidence);

        RoleAnalysisPatternAnalysis roleAnalysisPatternInfo = detectAndLoadPatternAnalysis(memberOid, miningRoleTypeChunks,
                session, roleAnalysisService, task, result, null, analysisCache, false);
        partitionAnalysis.setPatternAnalysis(roleAnalysisPatternInfo);

        double partitionAnomaliesConfidence = calculatePartitionAnomaliesConfidence(detectedAnomalyResults);
        partitionAnalysis.setAnomalyObjectsConfidence(partitionAnomaliesConfidence);

        partitionType.getDetectedAnomalyResult().addAll(CloneUtil.cloneCollectionMembers(detectedAnomalyResults));

        double averageItemFactor = getWeightedItemFactorConfidence(compareAttributeResult);

        double outlierPatternConfidence = getPatternConfidence(partitionAnalysis.getPatternAnalysis());

        double clusterConfidence = calculatePartitionClusterConfidence(assignmentFrequencyConfidence,
                outlierPatternConfidence,
                averageItemFactor,
                density);
        partitionAnalysis.setSimilarObjectsConfidence(clusterConfidence);

        double overallConfidence = calculatePartitionOverallConfidence(clusterConfidence, partitionAnomaliesConfidence);
        partitionAnalysis.setOverallConfidence(overallConfidence);

        partitionType.setPartitionAnalysis(partitionAnalysis);

        return partitionType;
    }

    private static double getPatternConfidence(RoleAnalysisPatternAnalysis patternAnalysis) {
        double patternConfidence = 0.0;

        if (patternAnalysis != null && patternAnalysis.getConfidence() != null) {
            patternConfidence = patternAnalysis.getConfidence();
        }
        return patternConfidence;
    }

    public static void resolveOutlierAnomalies(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull AttributeAnalysisCache analysisCache,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull MiningRoleTypeChunk miningRoleTypeChunk,
            @NotNull ZScoreData zScoreData,
            @NotNull FrequencyItem frequencyItem,
            @NotNull List<String> members,
            @NotNull List<MiningRoleTypeChunk> miningRoleTypeChunks,
            @NotNull RoleAnalysisSessionType session,
            @Nullable List<RoleAnalysisAttributeDef> attributesForUserAnalysis,
            int userCountInRepo,
            @NotNull ListMultimap<String, DetectedAnomalyResult> userRoleMap) {
        List<String> roles = miningRoleTypeChunk.getMembers();

        double anomalyDeviationConfidence = roleAnalysisService.calculateZScoreConfidence(miningRoleTypeChunk, zScoreData);
        double anomalyFrequencyConfidence = frequencyItem.getFrequency();
        int memberCount = members.size();
        for (String role : roles) {
            ObjectReferenceType anomalyRef = new ObjectReferenceType()
                    .oid(role)
                    .type(RoleType.COMPLEX_TYPE);

            for (String member : members) {
                PrismObject<UserType> userTypeObject = roleAnalysisService.getUserTypeObject(
                        member, task, result);

                List<String> allowedProperties = miningRoleTypeChunk.getProperties();
                RoleAnalysisPatternAnalysis patternAnalysis = detectAndLoadPatternAnalysis(member, miningRoleTypeChunks, session,
                        roleAnalysisService, task, result, allowedProperties, analysisCache, false);

                DetectedAnomalyResult detectedAnomalyResult = prepareChunkAnomalyResult(anomalyRef,
                        anomalyFrequencyConfidence,
                        anomalyDeviationConfidence,
                        memberCount,
                        patternAnalysis);

                double anomalyConfidence = calculateAssignmentAnomalyConfidence(
                        roleAnalysisService, attributesForUserAnalysis,
                        userTypeObject, userCountInRepo, detectedAnomalyResult, analysisCache, task, result);

                DetectedAnomalyStatistics statistics = detectedAnomalyResult.getStatistics();
                statistics.setConfidence(anomalyConfidence);
                userRoleMap.put(member, detectedAnomalyResult);
            }
        }
    }

    //TODO should we move other statistics computation to this method?

    /**
     * Partially prepares a DetectedAnomalyResult object based on the provided parameters.
     *
     * @param anomalyRef A reference to the anomaly object. This is the object that the anomaly is detected for.
     * @param anomalyFrequencyConfidence The confidence level of the anomaly frequency.
     * @param anomalyDeviationConfidence The confidence level of the anomaly deviation.
     * @param memberCount The number of members in the cluster.
     * @param patternAnalysis The analysis of the pattern associated with the anomaly.
     * @return A DetectedAnomalyResult object that encapsulates the provided parameters along with a timestamp of when the anomaly result was created.
     */
    private static @NotNull DetectedAnomalyResult prepareChunkAnomalyResult(
            @NotNull ObjectReferenceType anomalyRef,
            double anomalyFrequencyConfidence,
            double anomalyDeviationConfidence,
            int memberCount,
            @Nullable RoleAnalysisPatternAnalysis patternAnalysis) {
        DetectedAnomalyResult anomalyResult = new DetectedAnomalyResult();
        anomalyResult.setTargetObjectRef(anomalyRef);
        anomalyResult.setStatistics(new DetectedAnomalyStatistics());
        DetectedAnomalyStatistics statistics = anomalyResult.getStatistics();
        anomalyResult.setCreateTimestamp(
                XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis()));

        statistics.setConfidenceDeviation(anomalyDeviationConfidence);
        FrequencyType frequencyType = new FrequencyType();
        frequencyType.setPercentageRatio(anomalyFrequencyConfidence * 100);
        int valueRatio = (int) (anomalyFrequencyConfidence * memberCount);
        frequencyType.setValueRatio(valueRatio);
        frequencyType.setEntiretyCount(memberCount);
        statistics.setGroupFrequency(frequencyType);
        statistics.setPatternAnalysis(patternAnalysis);
        return anomalyResult;
    }

    public static @NotNull RoleAnalysisOutlierPartitionType prepareTotalOutlierPartition(
            @NotNull ObjectReferenceType clusterRef,
            @NotNull ObjectReferenceType sessionRef,
            double requiredConfidence) {
        RoleAnalysisOutlierPartitionType partition = new RoleAnalysisOutlierPartitionType();
        partition.setClusterRef(clusterRef);
        partition.setTargetSessionRef(sessionRef);
        partition.setCreateTimestamp(XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis()));

        RoleAnalysisPartitionAnalysisType partitionAnalysis = new RoleAnalysisPartitionAnalysisType();
        OutlierCategoryType outlierCategory = new OutlierCategoryType();
        outlierCategory.setOutlierNoiseCategory(OutlierNoiseCategoryType.MEMBERS_NOISE);
        outlierCategory.setOutlierClusterCategory(OutlierClusterCategoryType.OUTER_OUTLIER);
        outlierCategory.setOutlierSpecificCategory(OutlierSpecificCategoryType.UNIQUE_OBJECT);
        partitionAnalysis.setOutlierCategory(outlierCategory);
        partitionAnalysis.setAnomalyObjectsConfidence(0.0);
        partitionAnalysis.setOverallConfidence(requiredConfidence);
        partition.setPartitionAnalysis(partitionAnalysis);
        return partition;
    }
}
