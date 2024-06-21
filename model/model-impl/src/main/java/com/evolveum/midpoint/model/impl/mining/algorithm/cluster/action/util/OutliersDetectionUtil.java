/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidAssignment;
import static com.evolveum.midpoint.common.mining.utils.algorithm.JaccardSorter.jacquardSimilarity;
import static com.evolveum.midpoint.model.impl.mining.utils.RoleAnalysisAlgorithmUtils.resolveAttributeStatistics;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortString;

import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.common.mining.objects.analysis.AttributeAnalysisStructure;
import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.common.mining.objects.statistic.ClusterStatistic;
import com.evolveum.midpoint.common.mining.utils.values.*;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.object.SimpleHeatPattern;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.util.exception.SystemException;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectionOption;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

//TODO this is experimental
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

                        //TODO should we use alg for precise similarity?
                        userOutliers.setSimilarObjects(cluster.getMember().size());
                        Double membershipDensity = cluster.getClusterStatistics().getMembershipDensity();
                        userOutliers.setSimilarObjectsDensity(membershipDensity);

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

    public static @NotNull Collection<RoleAnalysisOutlierType> executeOuterOutliersAnalysis(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull RoleAnalysisSessionType session,
            @NotNull Task task,
            @NotNull OperationResult result) {
        ModelService modelService = roleAnalysisService.getModelService();

        UserAnalysisSessionOptionType userModeOptions = session.getUserModeOptions();
        Integer minMembersCount = userModeOptions.getMinMembersCount();
        HashMap<String, RoleAnalysisOutlierType> map = new HashMap<>();

        ObjectReferenceType clusterRef = new ObjectReferenceType()
                .oid(cluster.getOid())
                .type(RoleAnalysisClusterType.COMPLEX_TYPE);

        ObjectReferenceType sessionRef = new ObjectReferenceType()
                .oid(session.getOid())
                .type(RoleAnalysisSessionType.COMPLEX_TYPE);

        List<ObjectReferenceType> member = cluster.getMember();
        List<String> outliersMembers = new ArrayList<>();
        for (ObjectReferenceType objectReferenceType : member) {
            String memberOid = objectReferenceType.getOid();
            outliersMembers.add(memberOid);
        }

        double minThreshold = 0.5;
        for (ObjectReferenceType objectReferenceType : member) {
            String memberOid = objectReferenceType.getOid();

            Double jaccardFrequencyMetric = getJaccardFrequencyMetric(memberOid, roleAnalysisService,
                    modelService, null, task, result, outliersMembers, minThreshold, minMembersCount);

            List<PrismObject<UserType>> jaccardCloseObject = getJaccardCloseObject(memberOid, roleAnalysisService,
                    modelService, null, task, result, outliersMembers, jaccardFrequencyMetric);

            if (jaccardCloseObject.isEmpty()) {
                continue;
            }

            DisplayValueOption displayValueOption = new DisplayValueOption();
            displayValueOption.setProcessMode(RoleAnalysisProcessModeType.USER);
            displayValueOption.setChunkMode(RoleAnalysisChunkMode.EXPAND);
            displayValueOption.setSortMode(RoleAnalysisSortMode.JACCARD);
            displayValueOption.setChunkAction(RoleAnalysisChunkAction.EXPLORE_DETECTION);
            RoleAnalysisClusterType tempCluster = new RoleAnalysisClusterType();
            for (PrismObject<UserType> element : jaccardCloseObject) {
                tempCluster.getMember().add(new ObjectReferenceType()
                        .oid(element.getOid()).type(UserType.COMPLEX_TYPE).targetName(element.getName().getOrig()));
            }

            RoleAnalysisDetectionOptionType detectionOption = new RoleAnalysisDetectionOptionType();
            detectionOption.setFrequencyRange(new RangeType().min(2.0).max(2.0));
            tempCluster.setDetectionOption(detectionOption);

            MiningOperationChunk tempMiningOperationChunk = roleAnalysisService.prepareMiningStructure(tempCluster, displayValueOption,
                    RoleAnalysisProcessModeType.USER, result, task);

            RangeType frequencyRange = detectionOption.getFrequencyRange();

            RoleAnalysisSortMode sortMode = displayValueOption.getSortMode();
            if (sortMode == null) {
                displayValueOption.setSortMode(RoleAnalysisSortMode.NONE);
                sortMode = RoleAnalysisSortMode.NONE;
            }

            List<MiningRoleTypeChunk> miningRoleTypeChunks = tempMiningOperationChunk.getMiningRoleTypeChunks(
                    RoleAnalysisSortMode.NONE);

            List<MiningUserTypeChunk> miningUserTypeChunks = tempMiningOperationChunk.getMiningUserTypeChunks(
                    RoleAnalysisSortMode.NONE);

            int allPossibleRelation = miningRoleTypeChunks.size() * miningUserTypeChunks.size();

            int totalAssignPropertiesRelation = 0;
            for (MiningRoleTypeChunk miningRoleTypeChunk : miningRoleTypeChunks) {
                List<String> properties = miningRoleTypeChunk.getProperties();
                totalAssignPropertiesRelation += properties.size();
            }

            double density = Math.min((totalAssignPropertiesRelation / (double) allPossibleRelation) * 100, 100);


            List<RoleAnalysisAttributeDef> userAnalysisAttributeDef = roleAnalysisService.resolveAnalysisAttributes(session, UserType.COMPLEX_TYPE);
            List<RoleAnalysisAttributeDef> roleAnalysisAttributeDef = roleAnalysisService.resolveAnalysisAttributes(session, RoleType.COMPLEX_TYPE);

            Set<PrismObject<UserType>> clusterUsers;
            Set<PrismObject<RoleType>> clusterRoles;

            List<ObjectReferenceType> membersOidsSet = tempCluster.getMember();
            clusterUsers = membersOidsSet.stream().map(ref -> roleAnalysisService
                            .cacheUserTypeObject(new HashMap<>(), ref.getOid(), task, result, null))
                    .filter(Objects::nonNull).collect(Collectors.toSet());

            List<String> rolesOidsSet = new ArrayList<>();
            for (MiningRoleTypeChunk roleTypeChunk : miningRoleTypeChunks) {
                List<String> members = roleTypeChunk.getMembers();
                rolesOidsSet.addAll(members);
            }

            clusterRoles = rolesOidsSet.stream().map(oid -> roleAnalysisService
                            .cacheRoleTypeObject(new HashMap<>(), oid, task, result, null))
                    .filter(Objects::nonNull).collect(Collectors.toSet());

            Double userDensity = 0.0;
            Double roleDensity = 0.0;
            List<AttributeAnalysisStructure> userAttributeAnalysisStructures = roleAnalysisService
                    .userTypeAttributeAnalysis(clusterUsers, userDensity, task, result, userAnalysisAttributeDef);
            List<AttributeAnalysisStructure> roleAttributeAnalysisStructures = roleAnalysisService
                    .roleTypeAttributeAnalysis(clusterRoles, roleDensity, task, result, roleAnalysisAttributeDef);

            AnalysisClusterStatisticType roleAnalysisClusterStatisticType = new AnalysisClusterStatisticType();

            ClusterStatistic clusterStatistic = new ClusterStatistic();
            clusterStatistic.setUserAttributeAnalysisStructures(userAttributeAnalysisStructures);
            clusterStatistic.setRoleAttributeAnalysisStructures(roleAttributeAnalysisStructures);
            resolveAttributeStatistics(clusterStatistic, roleAnalysisClusterStatisticType);
            tempCluster.setClusterStatistics(roleAnalysisClusterStatisticType);
            ZScoreData zScoreData = roleAnalysisService.resolveOutliersZScore(miningRoleTypeChunks, frequencyRange.getMin(), frequencyRange.getMax());

            int countOfRoles = 0;
            for (MiningRoleTypeChunk miningRoleTypeChunk : miningRoleTypeChunks) {
                countOfRoles += miningRoleTypeChunk.getRoles().size();
            }

            //this is row miningRoleTypeChunk
            for (MiningRoleTypeChunk miningRoleTypeChunk : miningRoleTypeChunks) {

                FrequencyItem frequencyItem = miningRoleTypeChunk.getFrequencyItem();
                if (!frequencyItem.getStatus().equals(FrequencyItem.Status.INCLUDE) && miningRoleTypeChunk.getProperties().contains(memberOid)) {

                    List<String> roles = miningRoleTypeChunk.getMembers();

                    List<RoleAnalysisOutlierDescriptionType> prepareRoleOutliers = new ArrayList<>();

                    roles.forEach(role -> {
                        RoleAnalysisOutlierDescriptionType outlierDescription = new RoleAnalysisOutlierDescriptionType();
                        outlierDescription.setCategory(OutlierCategory.OUTER_OUTLIER);
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

                    PrismObject<UserType> userTypeObject = roleAnalysisService.getUserTypeObject(memberOid, task, result);
                    if (userTypeObject == null) {
                        continue;
                    }

                    RoleAnalysisOutlierType userOutliers = map.get(memberOid);

                    if (userOutliers == null) {
                        userOutliers = new RoleAnalysisOutlierType();
                        userOutliers.setTargetObjectRef(new ObjectReferenceType().oid(memberOid).type(UserType.COMPLEX_TYPE));
                    }

                    userOutliers.setSimilarObjects(jaccardCloseObject.size());
                    userOutliers.setSimilarObjectsDensity(density);


                    List<RoleAnalysisAttributeDef> attributesForUserAnalysis = roleAnalysisService.resolveAnalysisAttributes(
                            session, UserType.COMPLEX_TYPE);

                    AnalysisClusterStatisticType clusterStatistics = tempCluster.getClusterStatistics();
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
                        detectAndLoadPatternAnalysis(miningRoleTypeChunk, memberOid, prepareRoleOutlier, miningRoleTypeChunks);
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

                        //TODO STORE ALSO ATTRIBUTE ABOVE 80%
                        double confidence = (distributionConfidenceDiff + patternConfidenceDiff + itemFactorConfidenceDiff
                                + roleMemberConfidenceDiff + coverageConfidenceDiff) / 5;
                        outlierConfidenceBasedAssignment += confidence;
                        prepareRoleOutlier.setConfidence(confidence);
                        userOutliers.getResult().add(prepareRoleOutlier.clone());
                    }

                    double averageItemFactor = getAverageItemFactor(compareAttributeResult);

                    outlierConfidenceBasedAssignment = outlierConfidenceBasedAssignment / prepareRoleOutliers.size();
                    userOutliers.setOutlierPropertyConfidence(outlierConfidenceBasedAssignment);

                    Double membershipDensity = userOutliers.getSimilarObjectsDensity();
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

                    map.put(memberOid, userOutliers);

                }
            }

        }

        return map.values();
    }

    public static List<PrismObject<UserType>> getJaccardCloseObject(String userOid,
            RoleAnalysisService roleAnalysisService,
            ModelService modelService,
            ObjectQuery query,
            Task task,
            OperationResult result,
            List<String> outliersMembers, Double jaccardFrequencyMetric) {

        List<PrismObject<UserType>> elements = new ArrayList<>();
        PrismObject<UserType> userTypeObject = roleAnalysisService.getUserTypeObject(userOid, task, result);
        if (userTypeObject == null) {
            return elements;
        }
        elements.add(userTypeObject);

        List<String> similarUserOidSet = new ArrayList<>();

        UserType userObject = userTypeObject.asObjectable();
        List<String> userRolesToCompare = getRolesOidAssignment(userObject);

        //TODO store assignments and user oid? whats about heap? Search takes a lot of time
        //TODO it is not nessesary to search all users, and is not nessesary to search twice

        ResultHandler<UserType> resultHandler2 = (user, lResult) -> {
            try {
                //TODO think about this ignore outliers?
                // Whats if we want to compare with them?
                // Whats if cluster is not generated because missing one object?
                // Im not sure about it
                if (!outliersMembers.contains(user.getOid()) && !user.getOid().equals(userOid)) {
                    List<String> rolesOidAssignment = getRolesOidAssignment(user.asObjectable());
                    double jacquardSimilarity = jacquardSimilarity(userRolesToCompare, rolesOidAssignment);
                    if (jacquardSimilarity >= jaccardFrequencyMetric) {
                        elements.add(user);
                        similarUserOidSet.add(user.getOid());
                    }
                }
            } catch (Exception e) {
                String errorMessage = "Cannot resolve role members: " + toShortString(user.asObjectable())
                        + ": " + e.getMessage();
                throw new SystemException(errorMessage, e);
            }

            return true;
        };

        try {
            modelService.searchObjectsIterative(UserType.class, query, resultHandler2, null,
                    task, result);
        } catch (Exception ex) {
            throw new RuntimeException("Cannot resolve role members: " + userOid + ": " + ex.getMessage(), ex);
        } finally {
            result.recomputeStatus();
        }

        return elements;
    }

    public static Double getJaccardFrequencyMetric(String userOid,
            @NotNull RoleAnalysisService roleAnalysisService,
            ModelService modelService,
            ObjectQuery query,
            Task task,
            OperationResult result,
            List<String> outliersMembers,
            double minThreshold,
            int minMembers) {

        List<PrismObject<UserType>> elements = new ArrayList<>();
        PrismObject<UserType> userTypeObject = roleAnalysisService.getUserTypeObject(userOid, task, result);
        if (userTypeObject == null) {
            return 1.0;
        }
        elements.add(userTypeObject);

        Map<Double, Integer> similarityStats = new TreeMap<>(Collections.reverseOrder());

        UserType userObject = userTypeObject.asObjectable();
        List<String> userRolesToCompare = getRolesOidAssignment(userObject);

        //TODO store assignments and user oid? whats about heap? Search takes a lot of time
        //TODO it is not nessesary to search all users, and is not nessesary to search twice
        ResultHandler<UserType> resultHandler = (user, lResult) -> {
            try {
                //TODO think about this ignore outliers?
                // Whats if we want to compare with them?
                // Whats if cluster is not generated because missing one object?
                // Im not sure about it
                if (!outliersMembers.contains(user.getOid()) && !user.getOid().equals(userOid)) {
                    List<String> rolesOidAssignment = getRolesOidAssignment(user.asObjectable());
                    double jacquardSimilarity = jacquardSimilarity(userRolesToCompare, rolesOidAssignment);
                    jacquardSimilarity = Math.floor(jacquardSimilarity * 10) / 10.0;
                    if (jacquardSimilarity >= minThreshold) {
                        if (similarityStats.containsKey(jacquardSimilarity)) {
                            similarityStats.put(jacquardSimilarity, similarityStats.get(jacquardSimilarity) + 1);
                        } else {
                            similarityStats.put(jacquardSimilarity, 1);
                        }
                    }
                }
            } catch (Exception e) {
                String errorMessage = "Cannot resolve role members: " + toShortString(user.asObjectable())
                        + ": " + e.getMessage();
                throw new SystemException(errorMessage, e);
            }

            return true;
        };

        try {
            modelService.searchObjectsIterative(UserType.class, query, resultHandler, null,
                    task, result);
        } catch (Exception ex) {
            throw new RuntimeException("Cannot resolve role members: " + userOid + ": " + ex.getMessage(), ex);
        } finally {
            result.recomputeStatus();
        }

        for (Map.Entry<Double, Integer> entry : similarityStats.entrySet()) {
            Integer value = entry.getValue();
            if (value >= minMembers) {
                return entry.getKey();
            }
        }

        return 1.0;
    }
}
