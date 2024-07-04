package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier;

import com.evolveum.midpoint.common.mining.objects.analysis.AttributeAnalysisStructure;
import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.statistic.ClusterStatistic;
import com.evolveum.midpoint.common.mining.utils.values.*;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.collect.ListMultimap;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.LOGGER;
import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutliersDetectionUtil.*;
import static com.evolveum.midpoint.model.impl.mining.utils.RoleAnalysisAlgorithmUtils.resolveAttributeStatistics;

//TODO
public class ClusteringOutlierDetectionUtils {

    public static @NotNull RoleAnalysisClusterType createTemporaryCluster(
            @NotNull RoleAnalysisSessionType session,
            @NotNull List<String> clusterMembersOids) {
        RoleAnalysisClusterType cluster = new RoleAnalysisClusterType();

        RoleAnalysisDetectionOptionType detectionOption = prepareDetectionOptions(session);
        cluster.setDetectionOption(detectionOption);

        for (String element : clusterMembersOids) {
            cluster.getMember().add(new ObjectReferenceType().oid(element).type(UserType.COMPLEX_TYPE));
        }

        return cluster;
    }

    @NotNull
    public static MiningOperationChunk prepareTemporaryOperationChunk(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType tempCluster, @NotNull Task task,
            @NotNull OperationResult result) {
        DisplayValueOption displayValueOption = new DisplayValueOption();
        displayValueOption.setProcessMode(RoleAnalysisProcessModeType.USER);
        displayValueOption.setChunkMode(RoleAnalysisChunkMode.EXPAND);
        displayValueOption.setSortMode(RoleAnalysisSortMode.JACCARD);
        displayValueOption.setChunkAction(RoleAnalysisChunkAction.EXPLORE_DETECTION);

        RoleAnalysisSortMode sortMode = displayValueOption.getSortMode();
        if (sortMode == null) {
            displayValueOption.setSortMode(RoleAnalysisSortMode.NONE);
        }

        return roleAnalysisService.prepareMiningStructure(
                tempCluster, displayValueOption, RoleAnalysisProcessModeType.USER, result, task);
    }

    public static void processClusterAttributeAnalysis(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType temporaryCluster,
            @NotNull List<MiningRoleTypeChunk> miningRoleTypeChunks,
            @Nullable List<RoleAnalysisAttributeDef> userAnalysisAttributeDef,
            @Nullable List<RoleAnalysisAttributeDef> roleAnalysisAttributeDef,
            @NotNull Task task,
            @NotNull OperationResult result) {

        Set<PrismObject<UserType>> clusterUsers;
        Set<PrismObject<RoleType>> clusterRoles;

        List<String> rolesOidsSet = new ArrayList<>();
        for (MiningRoleTypeChunk roleTypeChunk : miningRoleTypeChunks) {
            List<String> members = roleTypeChunk.getMembers();
            rolesOidsSet.addAll(members);
        }

        List<ObjectReferenceType> membersOidsSet = temporaryCluster.getMember();
        clusterUsers = membersOidsSet.stream().map(ref -> roleAnalysisService
                        .cacheUserTypeObject(new HashMap<>(), ref.getOid(), task, result, null))
                .filter(Objects::nonNull).collect(Collectors.toSet());

        clusterRoles = rolesOidsSet.stream().map(oid -> roleAnalysisService
                        .cacheRoleTypeObject(new HashMap<>(), oid, task, result, null))
                .filter(Objects::nonNull).collect(Collectors.toSet());

        Double userDensity = 0.0;
        Double roleDensity = 0.0;
        List<AttributeAnalysisStructure> userAttributeAnalysisStructures = null;
        if (userAnalysisAttributeDef != null) {
            userAttributeAnalysisStructures = roleAnalysisService
                    .userTypeAttributeAnalysis(clusterUsers, userDensity, task, result, userAnalysisAttributeDef);
        }
        List<AttributeAnalysisStructure> roleAttributeAnalysisStructures = null;
        if (roleAnalysisAttributeDef != null) {
            roleAttributeAnalysisStructures = roleAnalysisService
                    .roleTypeAttributeAnalysis(clusterRoles, roleDensity, task, result, roleAnalysisAttributeDef);
        }

        AnalysisClusterStatisticType roleAnalysisClusterStatisticType = new AnalysisClusterStatisticType();

        ClusterStatistic clusterStatistic = new ClusterStatistic();
        clusterStatistic.setUserAttributeAnalysisStructures(userAttributeAnalysisStructures);
        clusterStatistic.setRoleAttributeAnalysisStructures(roleAttributeAnalysisStructures);
        resolveAttributeStatistics(clusterStatistic, roleAnalysisClusterStatisticType);
        temporaryCluster.setClusterStatistics(roleAnalysisClusterStatisticType);
    }

    public static void calculateTemporaryClusterDensityAndRolesCount(
            @NotNull MutableDouble clusterDensity,
            @NotNull MutableDouble clusterRolesCount,
            @NotNull List<MiningRoleTypeChunk> miningRoleTypeChunks,
            @NotNull List<MiningUserTypeChunk> miningUserTypeChunks) {
        int allPossibleRelation = miningRoleTypeChunks.size() * miningUserTypeChunks.size();
        int totalAssignPropertiesRelation = 0;
        int rolesCount = 0;
        for (MiningRoleTypeChunk miningRoleTypeChunk : miningRoleTypeChunks) {
            List<String> properties = miningRoleTypeChunk.getProperties();
            rolesCount += miningRoleTypeChunk.getRoles().size();
            totalAssignPropertiesRelation += properties.size();
        }
        double density = Math.min((totalAssignPropertiesRelation / (double) allPossibleRelation) * 100, 100);
        clusterDensity.setValue(density);
        clusterRolesCount.setValue(rolesCount);
    }

    @Nullable
    public static List<String> prepareJaccardCloseObjects(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull Task task,
            @NotNull String memberOid,
            @NotNull ListMultimap<List<String>, String> chunkMap,
            @NotNull MutableDouble usedFrequency,
            @NotNull List<String> outliersMembers,
            double minThreshold,
            @NotNull Integer minMembersCount,
            @NotNull OperationResult result) {
        List<String> jaccardCloseObject = roleAnalysisService.findJaccardCloseObject(
                memberOid,
                chunkMap,
                usedFrequency,
                outliersMembers, minThreshold, minMembersCount, task,
                result);

        if (jaccardCloseObject.isEmpty()) {
            return null;
        }

        jaccardCloseObject.add(memberOid);
        return jaccardCloseObject;
    }

    public static void analyzeAnomalyMarkedRoleTypeChunk(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisSessionType session,
            @NotNull Task task,
            ObjectReferenceType analyzedObjectRef,
            @NotNull List<MiningRoleTypeChunk> miningRoleTypeChunks,
            String memberOid,
            ZScoreData zScoreData,
            ObjectReferenceType clusterRef,
            ObjectReferenceType sessionRef,
            OperationResult result,
            HashMap<String, RoleAnalysisOutlierType> map,
            MutableDouble usedFrequency,
            RoleAnalysisClusterType tempCluster,
            List<String> jaccardCloseObject,
            double density,
            int countOfRoles) {
        for (MiningRoleTypeChunk miningRoleTypeChunk : miningRoleTypeChunks) {

            FrequencyItem frequencyItem = miningRoleTypeChunk.getFrequencyItem();
            if (!frequencyItem.getStatus().equals(FrequencyItem.Status.INCLUDE)
                    && miningRoleTypeChunk.getProperties().contains(memberOid)) {

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

                Set<ObjectReferenceType> duplicateAssignment = resolveUserDuplicateAssignment(
                        roleAnalysisService, userTypeObject.asObjectable(), task, result);

                userOutliers.getDuplicatedRoleAssignment().addAll(duplicateAssignment);

                String description = analyzedObjectRef.getDescription();

                if (description != null && !description.equals("unknown")) {
                    RoleAnalysisOutlierNoiseCategoryType roleAnalysisOutlierNoiseCategoryType =
                            RoleAnalysisOutlierNoiseCategoryType.fromValue(description);
                    userOutliers.setOutlierNoiseCategory(roleAnalysisOutlierNoiseCategoryType);
                }

                userOutliers.setSimilarObjectsThreshold(usedFrequency.doubleValue() * 100);
                userOutliers.setClusterStatistics(tempCluster.getClusterStatistics());

                userOutliers.setSimilarObjects(jaccardCloseObject.size());
                userOutliers.setSimilarObjectsDensity(density);

                List<RoleAnalysisAttributeDef> attributesForUserAnalysis = roleAnalysisService.resolveAnalysisAttributes(
                        session, UserType.COMPLEX_TYPE);

                double averageItemFactor = resolveAttributeOutlierStats(roleAnalysisService,
                        tempCluster,
                        attributesForUserAnalysis,
                        userTypeObject,
                        userOutliers);

                double assignmentFrequencyConfidence = calculateOutlierRoleAssignmentFrequencyConfidence(
                        userTypeObject, countOfRoles);

                userOutliers.setOutlierAssignmentFrequencyConfidence(assignmentFrequencyConfidence);

                detectAndLoadPatternAnalysis(userOutliers, miningRoleTypeChunks);

                double outlierConfidenceBasedAssignment = 0;
                for (RoleAnalysisOutlierDescriptionType prepareRoleOutlier : prepareRoleOutliers) {
                    detectAndLoadPatternAnalysis(miningRoleTypeChunk, memberOid, prepareRoleOutlier, miningRoleTypeChunks);
                    double confidence = calculateAssignmentAnomalyConfidence(roleAnalysisService, session, userTypeObject,
                            prepareRoleOutlier, task, result);
                    outlierConfidenceBasedAssignment += confidence;
                    prepareRoleOutlier.setConfidence(confidence);
                    userOutliers.getResult().add(prepareRoleOutlier.clone());
                }

                outlierConfidenceBasedAssignment = outlierConfidenceBasedAssignment / prepareRoleOutliers.size();
                userOutliers.setOutlierPropertyConfidence(outlierConfidenceBasedAssignment);

                resolveOutlierConfidence(userOutliers,
                        outlierConfidenceBasedAssignment,
                        assignmentFrequencyConfidence,
                        averageItemFactor);

                map.put(memberOid, userOutliers);

            }
        }
    }

    public static void analyseOutlierClusterMembers(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisSessionType session,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull Task task,
            @NotNull List<ObjectReferenceType> member,
            OperationResult result,
            double minThreshold,
            ListMultimap<List<String>, String> chunkMap,
            List<String> outliersMembers,
            Integer minMembersCount,
            List<RoleAnalysisAttributeDef> userAnalysisAttributeDef,
            List<RoleAnalysisAttributeDef> roleAnalysisAttributeDef,
            RoleAnalysisDetectionOptionType detectionOption,
            HashMap<String, RoleAnalysisOutlierType> map) {

        ObjectReferenceType clusterRef = new ObjectReferenceType()
                .oid(cluster.getOid())
                .type(RoleAnalysisClusterType.COMPLEX_TYPE);

        ObjectReferenceType sessionRef = new ObjectReferenceType()
                .oid(session.getOid())
                .type(RoleAnalysisSessionType.COMPLEX_TYPE);

        for (ObjectReferenceType analyzedObjectRef : member) {
            String memberOid = analyzedObjectRef.getOid();

            if (result.isError()) {
                LOGGER.warn("Error during outlier detection for user: {}", result.getMessage());
            }

            MutableDouble usedFrequency = new MutableDouble(minThreshold);
            List<String> jaccardCloseObject = prepareJaccardCloseObjects(roleAnalysisService, task, memberOid, chunkMap,
                    usedFrequency, outliersMembers, minThreshold, minMembersCount, result);
            if (jaccardCloseObject == null) {
                continue;
            }

            RoleAnalysisClusterType tempCluster = createTemporaryCluster(session, jaccardCloseObject);

            MiningOperationChunk tempMiningOperationChunk = prepareTemporaryOperationChunk(
                    roleAnalysisService,
                    tempCluster,
                    task,
                    result);

            List<MiningRoleTypeChunk> miningRoleTypeChunks = tempMiningOperationChunk.getMiningRoleTypeChunks(
                    RoleAnalysisSortMode.NONE);

            List<MiningUserTypeChunk> miningUserTypeChunks = tempMiningOperationChunk.getMiningUserTypeChunks(
                    RoleAnalysisSortMode.NONE);

            processClusterAttributeAnalysis(roleAnalysisService, tempCluster, miningRoleTypeChunks,
                    userAnalysisAttributeDef, roleAnalysisAttributeDef, task, result);

            MutableDouble clusterDensity = new MutableDouble(0);
            MutableDouble clusterRolesCount = new MutableDouble(0);
            calculateTemporaryClusterDensityAndRolesCount(clusterDensity, clusterRolesCount,
                    miningRoleTypeChunks, miningUserTypeChunks);
            double density = clusterDensity.doubleValue();
            int countOfRoles = clusterRolesCount.intValue();

            RangeType frequencyRange = detectionOption.getFrequencyRange();
            Double sensitivity = detectionOption.getSensitivity();
            ZScoreData zScoreData = roleAnalysisService.resolveOutliersZScore(
                    miningRoleTypeChunks, frequencyRange, sensitivity);

            analyzeAnomalyMarkedRoleTypeChunk(roleAnalysisService,
                    session,
                    task,
                    analyzedObjectRef,
                    miningRoleTypeChunks,
                    memberOid,
                    zScoreData,
                    clusterRef,
                    sessionRef,
                    result,
                    map,
                    usedFrequency,
                    tempCluster,
                    jaccardCloseObject,
                    density,
                    countOfRoles);
        }
    }

    static double resolveAttributeOutlierStats(@NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType tempCluster,
            List<RoleAnalysisAttributeDef> attributesForUserAnalysis,
            PrismObject<UserType> userTypeObject,
            RoleAnalysisOutlierType userOutliers) {
        AnalysisClusterStatisticType clusterStatistics = tempCluster.getClusterStatistics();
        RoleAnalysisAttributeAnalysisResult compareAttributeResult = null;
        if (clusterStatistics != null
                && clusterStatistics.getUserAttributeAnalysisResult() != null
                && attributesForUserAnalysis != null) {
            RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = clusterStatistics
                    .getUserAttributeAnalysisResult();

            RoleAnalysisAttributeAnalysisResult userAttributes = roleAnalysisService
                    .resolveUserAttributes(userTypeObject, attributesForUserAnalysis);

            compareAttributeResult = roleAnalysisService
                    .resolveSimilarAspect(userAttributes, userAttributeAnalysisResult);

            AttributeAnalysis attributeAnalysis = new AttributeAnalysis();
            attributeAnalysis.setUserAttributeAnalysisResult(userAttributeAnalysisResult);
            attributeAnalysis.setUserClusterCompare(compareAttributeResult);
            userOutliers.setAttributeAnalysis(attributeAnalysis);
        }

        return getAverageItemFactor(compareAttributeResult);
    }

    static void resolveOutlierConfidence(@NotNull RoleAnalysisOutlierType userOutliers,
            double outlierConfidenceBasedAssignment,
            double assignmentFrequencyConfidence,
            double averageItemFactor) {
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
    }

}
