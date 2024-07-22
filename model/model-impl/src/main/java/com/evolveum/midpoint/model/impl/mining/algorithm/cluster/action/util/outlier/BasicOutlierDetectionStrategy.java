package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier;

import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutliersDetectionUtil.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.evolveum.midpoint.common.mining.objects.analysis.AttributePathResult;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.utils.values.FrequencyItem;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.common.mining.utils.values.ZScoreData;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

//TODO clean/remove duplicates implement role mode
public class BasicOutlierDetectionStrategy implements OutlierDetectionStrategy {

    private static final Trace LOGGER = TraceManager.getTrace(BasicOutlierDetectionStrategy.class);

    @Override
    public void executeAnalysis(@NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull RoleAnalysisSessionType session,
            @NotNull Map<String, Map<String, AttributePathResult>> userAnalysisCache,
            @NotNull Task task,
            @NotNull OperationResult result) {
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();

        MiningOperationChunk miningOperationChunk = roleAnalysisService.prepareCompressedMiningStructure(cluster, true,
                processMode, result, task);

        ObjectReferenceType clusterRef = new ObjectReferenceType()
                .oid(cluster.getOid())
                .type(RoleAnalysisClusterType.COMPLEX_TYPE)
                .targetName(cluster.getName());

        ObjectReferenceType sessionRef = new ObjectReferenceType()
                .oid(session.getOid())
                .type(RoleAnalysisSessionType.COMPLEX_TYPE)
                .targetName(session.getName());

        RangeType frequencyRange = session.getDefaultDetectionOption().getFrequencyRange();
        Double sensitivity = session.getDefaultDetectionOption().getSensitivity();
        UserAnalysisSessionOptionType userModeOptions = session.getUserModeOptions();
        Double similarityThreshold = userModeOptions.getSimilarityThreshold();

        //TODO role mode
        outlierAnalysisProcess(roleAnalysisService,
                cluster,
                session,
                task,
                miningOperationChunk,
                userAnalysisCache,
                frequencyRange,
                sensitivity,
                clusterRef,
                sessionRef,
                result,
                similarityThreshold);

    }

    /**
     * NOTE just in case of user clustering mode.
     */
    private void outlierAnalysisProcess(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull RoleAnalysisSessionType session,
            @NotNull Task task,
            @NotNull MiningOperationChunk miningOperationChunk,
            @NotNull Map<String, Map<String, AttributePathResult>> userAnalysisCache,
            RangeType range,
            Double sensitivity,
            ObjectReferenceType clusterRef,
            ObjectReferenceType sessionRef,
            OperationResult result,
            Double similarityThreshold) {

        List<RoleAnalysisAttributeDef> attributesForUserAnalysis = roleAnalysisService.resolveAnalysisAttributes(
                session, UserType.COMPLEX_TYPE);

        int userCountInRepo = roleAnalysisService.countObjects(UserType.class, null, null, task, result);

        List<MiningRoleTypeChunk> miningRoleTypeChunks = miningOperationChunk.getMiningRoleTypeChunks(
                RoleAnalysisSortMode.NONE);

        ZScoreData zScoreData = roleAnalysisService.resolveOutliersZScore(miningRoleTypeChunks, range, sensitivity);

        int countOfRoles = 0;
        for (MiningRoleTypeChunk miningRoleTypeChunk : miningRoleTypeChunks) {
            countOfRoles += miningRoleTypeChunk.getRoles().size();
        }

        ListMultimap<String, DetectedAnomalyResult> userRoleMap = ArrayListMultimap.create();
        long startTime1 = System.currentTimeMillis();
        for (MiningRoleTypeChunk miningRoleTypeChunk : miningRoleTypeChunks) {

            FrequencyItem frequencyItem = miningRoleTypeChunk.getFrequencyItem();
            if (!frequencyItem.getStatus().equals(FrequencyItem.Status.INCLUDE)) {
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
                        //TODO
                        RoleAnalysisPatternAnalysis patternAnalysis = detectAndLoadPatternAnalysis(
                                miningRoleTypeChunk, user, miningRoleTypeChunks);
                        statistics.setPatternAnalysis(patternAnalysis);
                        double anomalyConfidence = calculateAssignmentAnomalyConfidence(
                                roleAnalysisService, attributesForUserAnalysis,
                                userTypeObject, userCountInRepo, anomalyResult,userAnalysisCache, task, result);
                        statistics.setConfidence(anomalyConfidence);
                        userRoleMap.put(user, anomalyResult);
                    }

                });

            }
        }
        long endTime = System.currentTimeMillis();
        double totalProcessingTime = (double) (endTime - startTime1) / 1000.0;

        LOGGER.debug("Total processing time for all chunks: " + totalProcessingTime + " seconds");

        startTime1 = System.currentTimeMillis();
        Set<String> keySet = userRoleMap.keySet();
        for (String userOid : keySet) {
            Collection<DetectedAnomalyResult> detectedAnomalyResults = userRoleMap.get(userOid);

            RoleAnalysisOutlierPartitionType partitionType = new RoleAnalysisOutlierPartitionType();
            partitionType.setTargetClusterRef(clusterRef.clone());
            partitionType.setTargetSessionRef(sessionRef.clone());
            partitionType.setCreateTimestamp(XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis()));

            RoleAnalysisPartitionAnalysisType partitionAnalysis = new RoleAnalysisPartitionAnalysisType();

            partitionAnalysis.setOutlierNoiseCategory(RoleAnalysisOutlierNoiseCategoryType.PART_OF_CLUSTER);
            partitionAnalysis.setOutlierCategory(OutlierCategory.INNER_OUTLIER);

            PrismObject<UserType> userTypeObject = roleAnalysisService.getUserTypeObject(userOid, task, result);
            if (userTypeObject == null) {
                continue;
            }

            //Resolve similar objects analysis
            RoleAnalysisOutlierSimilarObjectsAnalysisResult similarObjectAnalysis = new RoleAnalysisOutlierSimilarObjectsAnalysisResult();
            similarObjectAnalysis.setSimilarObjectsThreshold(similarityThreshold);
            similarObjectAnalysis.setSimilarObjectsCount(cluster.getMember().size());

            Double membershipDensity = cluster.getClusterStatistics().getMembershipDensity();
            similarObjectAnalysis.setSimilarObjectsDensity(membershipDensity);
            //TODO store just useful information
            similarObjectAnalysis.setClusterStatistics(cluster.getClusterStatistics());

            similarObjectAnalysis.getSimilarObjects().addAll(CloneUtil.cloneCollectionMembers(cluster.getMember()));
            partitionAnalysis.setSimilarObjectAnalysis(similarObjectAnalysis);

            AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();
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

            if (membershipDensity != null) {
                clusterConfidence += membershipDensity;
            }
            clusterConfidence = clusterConfidence / 5;

            partitionAnalysis.setSimilarObjectsConfidence(clusterConfidence);
            //TODO
            partitionAnalysis.setOverallConfidence(clusterConfidence);

            partitionType.setPartitionAnalysis(partitionAnalysis);

            updateOrImportOutlierObject(roleAnalysisService, session, userOid, partitionType, task, result);
        }
        endTime = System.currentTimeMillis();
        totalProcessingTime = (double) (endTime - startTime1) / 1000.0;
        LOGGER.debug("Total processing time for all user keySet: " + totalProcessingTime + " seconds");
    }
}
