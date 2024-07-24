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
import com.evolveum.midpoint.common.mining.objects.analysis.cache.AttributeAnalysisCache;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.LOGGER;
import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutliersDetectionUtil.*;
import static com.evolveum.midpoint.model.impl.mining.utils.RoleAnalysisAlgorithmUtils.resolveAttributeStatistics;

//TODO clean/remove duplicates implement role mode
public class ClusteringOutlierDetectionStrategy implements OutlierDetectionStrategy {

    @Override
    public void executeAnalysis(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull RoleAnalysisSessionType session,
            @NotNull AttributeAnalysisCache userAnalysisCache,
            @NotNull Task task,
            @NotNull OperationResult result) {
        UserAnalysisSessionOptionType userModeOptions = session.getUserModeOptions();
        Integer minMembersCount = userModeOptions.getMinMembersCount();
        RoleAnalysisDetectionOptionType detectionOption = prepareDetectionOptions(session);

        List<ObjectReferenceType> member = cluster.getMember();
        List<String> outliersMembers = member.stream().map(AbstractReferencable::getOid).collect(Collectors.toList());

        List<RoleAnalysisAttributeDef> userAnalysisAttributeDef = roleAnalysisService.resolveAnalysisAttributes(
                session, UserType.COMPLEX_TYPE);
        List<RoleAnalysisAttributeDef> roleAnalysisAttributeDef = roleAnalysisService.resolveAnalysisAttributes(
                session, RoleType.COMPLEX_TYPE);

        RangeType propertiesRange = userModeOptions.getPropertiesRange();
        ListMultimap<List<String>, String> chunkMap = roleAnalysisService.loadUserForOutlierComparison(
                roleAnalysisService, outliersMembers, propertiesRange.getMin().intValue(), propertiesRange.getMax().intValue(),
                userModeOptions.getQuery(), result, task);
        double minThreshold = 0.5;

        analyseOutlierClusterMembers(roleAnalysisService,
                session,
                cluster,
                task,
                member,
                result,
                minThreshold,
                chunkMap,
                outliersMembers,
                userAnalysisCache,
                minMembersCount,
                userAnalysisAttributeDef,
                roleAnalysisAttributeDef,
                detectionOption
        );
    }

    /**
     * NOTE just in case of user clustering mode.
     */
    private void outlierAnalysisProcess(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisSessionType session,
            @NotNull Task task,
            ObjectReferenceType analyzedObjectRef,
            @NotNull List<MiningRoleTypeChunk> miningRoleTypeChunks,
            @NotNull AttributeAnalysisCache userAnalysisCache,
            String memberOid,
            ZScoreData zScoreData,
            ObjectReferenceType clusterRef,
            ObjectReferenceType sessionRef,
            OperationResult result,
            MutableDouble usedFrequency,
            RoleAnalysisClusterType tempCluster,
            double density,
            int countOfRoles) {
        List<RoleAnalysisAttributeDef> attributesForUserAnalysis = roleAnalysisService.resolveAnalysisAttributes(
                session, UserType.COMPLEX_TYPE);

        int userCountInRepo = roleAnalysisService.countObjects(UserType.class, null, null, task, result);

        ListMultimap<String, DetectedAnomalyResult> userRoleMap = ArrayListMultimap.create();

        for (MiningRoleTypeChunk miningRoleTypeChunk : miningRoleTypeChunks) {

            FrequencyItem frequencyItem = miningRoleTypeChunk.getFrequencyItem();
            if (!frequencyItem.getStatus().equals(FrequencyItem.Status.INCLUDE)
                    && miningRoleTypeChunk.getProperties().contains(memberOid)) {
                List<String> roles = miningRoleTypeChunk.getMembers();
                List<String> users = miningRoleTypeChunk.getProperties();

                roles.forEach(role -> {
                    DetectedAnomalyResult anomalyResult = new DetectedAnomalyResult();
                    anomalyResult.setTargetObjectRef(new ObjectReferenceType().oid(role).type(RoleType.COMPLEX_TYPE));

                    anomalyResult.setStatistics(new DetectedAnomalyStatistics());
                    DetectedAnomalyStatistics statistics = anomalyResult.getStatistics();
                    anomalyResult.setCreateTimestamp(
                            XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis()));

                    double confidence = roleAnalysisService.calculateZScoreConfidence(miningRoleTypeChunk, zScoreData);
                    statistics.setConfidenceDeviation(confidence);
                    statistics.setFrequency(frequencyItem.getFrequency());

                    for (String user : users) {
                        PrismObject<UserType> userTypeObject = roleAnalysisService.getUserTypeObject(
                                user, task, result);
                        RoleAnalysisPatternAnalysis patternAnalysis = detectAndLoadPatternAnalysis(
                                miningRoleTypeChunk, user, miningRoleTypeChunks);
                        statistics.setPatternAnalysis(patternAnalysis);
                        double anomalyConfidence = calculateAssignmentAnomalyConfidence(
                                roleAnalysisService, attributesForUserAnalysis, userTypeObject, userCountInRepo, anomalyResult,
                                userAnalysisCache, task, result);
                        statistics.setConfidence(anomalyConfidence);
                        userRoleMap.put(user, anomalyResult);
                    }

                });
            }
        }

        Set<String> keySet = userRoleMap.keySet();
        for (String userOid : keySet) {
            Collection<DetectedAnomalyResult> detectedAnomalyResults = userRoleMap.get(userOid);

            RoleAnalysisOutlierPartitionType partitionType = new RoleAnalysisOutlierPartitionType();
            partitionType.setTargetClusterRef(clusterRef.clone());
            partitionType.setTargetSessionRef(sessionRef.clone());
            partitionType.setCreateTimestamp(XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis()));

            RoleAnalysisPartitionAnalysisType partitionAnalysis = new RoleAnalysisPartitionAnalysisType();

            String description = analyzedObjectRef.getDescription();

            if (description != null && !description.equals("unknown")) {
                RoleAnalysisOutlierNoiseCategoryType roleAnalysisOutlierNoiseCategoryType =
                        RoleAnalysisOutlierNoiseCategoryType.fromValue(description);
                partitionAnalysis.setOutlierNoiseCategory(roleAnalysisOutlierNoiseCategoryType);
            }

            partitionAnalysis.setOutlierCategory(OutlierCategory.OUTER_OUTLIER);

            PrismObject<UserType> userTypeObject = roleAnalysisService.getUserTypeObject(userOid, task, result);
            if (userTypeObject == null) {
                continue;
            }

            //Resolve similar objects analysis
            RoleAnalysisOutlierSimilarObjectsAnalysisResult similarObjectAnalysis = new RoleAnalysisOutlierSimilarObjectsAnalysisResult();
            similarObjectAnalysis.setSimilarObjectsThreshold(usedFrequency.doubleValue() * 100);
            similarObjectAnalysis.setSimilarObjectsCount(tempCluster.getMember().size());

            AnalysisClusterStatisticType clusterStatistics = tempCluster.getClusterStatistics();
            similarObjectAnalysis.setSimilarObjectsDensity(density);
            //TODO store just useful information
            similarObjectAnalysis.setClusterStatistics(clusterStatistics);

            similarObjectAnalysis.getSimilarObjects().addAll(CloneUtil.cloneCollectionMembers(tempCluster.getMember()));
            partitionAnalysis.setSimilarObjectAnalysis(similarObjectAnalysis);

            RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = clusterStatistics
                    .getUserAttributeAnalysisResult();

            RoleAnalysisAttributeAnalysisResult compareAttributeResult = null;
            if (userAttributeAnalysisResult != null && attributesForUserAnalysis != null) {

                RoleAnalysisAttributeAnalysisResult userAttributes = roleAnalysisService
                        .resolveUserAttributes(userTypeObject, attributesForUserAnalysis);

                compareAttributeResult = roleAnalysisService
                        .resolveSimilarAspect(userAttributes, userAttributeAnalysisResult);

                AttributeAnalysis attributeAnalysis = new AttributeAnalysis();
                attributeAnalysis.setUserAttributeAnalysisResult(userAttributeAnalysisResult);
                attributeAnalysis.setUserClusterCompare(compareAttributeResult);
                partitionAnalysis.setAttributeAnalysis(attributeAnalysis);
            }

            double assignmentFrequencyConfidence = calculateOutlierRoleAssignmentFrequencyConfidence(
                    userTypeObject, countOfRoles);
            partitionAnalysis.setOutlierAssignmentFrequencyConfidence(assignmentFrequencyConfidence);

            RoleAnalysisPatternAnalysis roleAnalysisPatternInfo = detectAndLoadPatternAnalysis(userOid, miningRoleTypeChunks);
            partitionAnalysis.setPatternAnalysis(roleAnalysisPatternInfo);

            double outlierConfidenceBasedAssignment = 0;
            for (DetectedAnomalyResult prepareRoleOutlier : detectedAnomalyResults) {
                Double confidence = prepareRoleOutlier.getStatistics().getConfidence();
                outlierConfidenceBasedAssignment += confidence;
            }

            partitionType.getDetectedAnomalyResult().addAll(CloneUtil.cloneCollectionMembers(detectedAnomalyResults));

            double averageItemFactor = getAverageItemFactor(compareAttributeResult);

            outlierConfidenceBasedAssignment = outlierConfidenceBasedAssignment / detectedAnomalyResults.size();
            partitionAnalysis.setAnomalyObjectsConfidence(outlierConfidenceBasedAssignment);

            Double outlierPatternConfidence = partitionAnalysis.getPatternAnalysis().getConfidence();
            double clusterConfidence = outlierConfidenceBasedAssignment
                    + assignmentFrequencyConfidence
                    + outlierPatternConfidence
                    + averageItemFactor;

            clusterConfidence += density;
            clusterConfidence = clusterConfidence / 5;

            partitionAnalysis.setSimilarObjectsConfidence(clusterConfidence);
            //TODO
            partitionAnalysis.setOverallConfidence(clusterConfidence);

            partitionType.setPartitionAnalysis(partitionAnalysis);

            updateOrImportOutlierObject(roleAnalysisService, session, userOid, partitionType, task, result);
        }
    }

    private @NotNull RoleAnalysisClusterType createTemporaryCluster(
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
    private MiningOperationChunk prepareTemporaryOperationChunk(
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

    private void processClusterAttributeAnalysis(
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

    private void calculateTemporaryClusterDensityAndRolesCount(
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
    private List<String> prepareJaccardCloseObjects(
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

    private void analyseOutlierClusterMembers(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisSessionType session,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull Task task,
            @NotNull List<ObjectReferenceType> member,
            OperationResult result,
            double minThreshold,
            ListMultimap<List<String>, String> chunkMap,
            List<String> outliersMembers,
            @NotNull AttributeAnalysisCache userAnalysisCache,
            Integer minMembersCount,
            List<RoleAnalysisAttributeDef> userAnalysisAttributeDef,
            List<RoleAnalysisAttributeDef> roleAnalysisAttributeDef,
            RoleAnalysisDetectionOptionType detectionOption) {

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

            outlierAnalysisProcess(roleAnalysisService,
                    session,
                    task,
                    analyzedObjectRef,
                    miningRoleTypeChunks,
                    userAnalysisCache,
                    memberOid,
                    zScoreData,
                    clusterRef,
                    sessionRef,
                    result,
                    usedFrequency,
                    tempCluster,
                    density,
                    countOfRoles);
        }
    }

}
