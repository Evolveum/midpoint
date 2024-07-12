package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier;

import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutliersDetectionUtil.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.prism.util.CloneUtil;

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
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

//TODO
public class BasicOutlierDetectionUtils {

    public static void resolveUserModeOutliers(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull RoleAnalysisSessionType session,
            @NotNull Task task,
            @NotNull MiningOperationChunk miningOperationChunk,
            RangeType range,
            Double sensitivity,
            ObjectReferenceType clusterRef,
            ObjectReferenceType sessionRef,
            OperationResult result,
            Double similarityThreshold) {
        List<MiningRoleTypeChunk> miningRoleTypeChunks = miningOperationChunk.getMiningRoleTypeChunks(
                RoleAnalysisSortMode.NONE);

        ZScoreData zScoreData = roleAnalysisService.resolveOutliersZScore(miningRoleTypeChunks, range, sensitivity);

        int countOfRoles = 0;
        for (MiningRoleTypeChunk miningRoleTypeChunk : miningRoleTypeChunks) {
            countOfRoles += miningRoleTypeChunk.getRoles().size();
        }

        ListMultimap<String, DetectedAnomalyResult> userRoleMap = ArrayListMultimap.create();

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
                        RoleAnalysisPatternAnalysis patternAnalysis = detectAndLoadPatternAnalysis(
                                miningRoleTypeChunk, user, miningRoleTypeChunks);
                        statistics.setPatternAnalysis(patternAnalysis);
                        double anomalyConfidence = calculateAssignmentAnomalyConfidence(
                                roleAnalysisService, session, userTypeObject, anomalyResult, task, result);
                        statistics.setConfidence(anomalyConfidence);
                        userRoleMap.put(user, anomalyResult);
                    }

                });

            }
        }


        for (Map.Entry<String, DetectedAnomalyResult> entry : userRoleMap.entries()) {
            String userOid = entry.getKey();
            List<DetectedAnomalyResult> detectedAnomalyResults = userRoleMap.get(userOid);

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

            List<RoleAnalysisAttributeDef> attributesForUserAnalysis = roleAnalysisService.resolveAnalysisAttributes(
                    session, UserType.COMPLEX_TYPE);

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

            updateOutlierObjectInRepo(roleAnalysisService, session, userOid, partitionType, task, result);
        }

    }

    public static void updateOutlierObjectInRepo(
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
            RoleAnalysisOutlierType roleAnalysisOutlierType = new RoleAnalysisOutlierType();
            roleAnalysisOutlierType.setTargetObjectRef(new ObjectReferenceType().oid(userOid).type(UserType.COMPLEX_TYPE));
            roleAnalysisOutlierType.getOutlierPartitions().add(partition);
            roleAnalysisOutlierType.setAnomalyObjectsConfidence(partition.getPartitionAnalysis().getAnomalyObjectsConfidence());
            roleAnalysisOutlierType.setOverallConfidence(partition.getPartitionAnalysis().getOverallConfidence());
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
            roleAnalysisService.updateOutlierObject(
                    roleAnalysisOutlierType.getOid(), partition, overallConfidence, anomalyObjectsConfidence, result);
        }
    }

    //TODO temporary disabled
//    public static void resolveRoleModeOutliers(
//            @NotNull RoleAnalysisService roleAnalysisService,
//            @NotNull MiningOperationChunk miningOperationChunk,
//            RangeType range,
//            Double sensitivity,
//            ObjectReferenceType clusterRef,
//            ObjectReferenceType sessionRef,
//            HashMap<String, RoleAnalysisOutlierType> map) {
//        List<MiningUserTypeChunk> miningUserTypeChunks = miningOperationChunk.getMiningUserTypeChunks(
//                RoleAnalysisSortMode.NONE);
//
//        ZScoreData zScoreData = roleAnalysisService.resolveOutliersZScore(miningUserTypeChunks, range, sensitivity);
//
//        for (MiningUserTypeChunk miningUserTypeChunk : miningUserTypeChunks) {
//
//            FrequencyItem frequencyItem = miningUserTypeChunk.getFrequencyItem();
//
//            //TODO Z score
//            if (!frequencyItem.getStatus().equals(FrequencyItem.Status.INCLUDE)) {
//                List<String> roles = miningUserTypeChunk.getProperties();
//                List<String> users = miningUserTypeChunk.getMembers();
//
//                List<RoleAnalysisOutlierDescriptionType> prepareUserOutliers = new ArrayList<>();
//
//                users.forEach(user -> {
//                    RoleAnalysisOutlierDescriptionType outlierDescription = new RoleAnalysisOutlierDescriptionType();
//                    outlierDescription.setCategory(OutlierCategory.INNER_OUTLIER);
//                    outlierDescription.setObject(new ObjectReferenceType().
//                            oid(user)
//                            .type(UserType.COMPLEX_TYPE));
//
//                    double confidence = roleAnalysisService.calculateZScoreConfidence(miningUserTypeChunk, zScoreData);
//
//                    outlierDescription.setConfidenceDeviation(confidence);
//
//                    outlierDescription.setCluster(clusterRef.clone());
//                    outlierDescription.setFrequency(frequencyItem.getFrequency());
//
//                    outlierDescription.setSession(sessionRef.clone());
//
//                    prepareUserOutliers.add(outlierDescription);
//                });
//
//                for (String role : roles) {
//                    RoleAnalysisOutlierType roleOutliers = map.get(role);
//
//                    if (roleOutliers == null) {
//                        roleOutliers = new RoleAnalysisOutlierType();
//                        roleOutliers.setTargetObjectRef(new ObjectReferenceType().oid(role).type(RoleType.COMPLEX_TYPE));
//                        for (RoleAnalysisOutlierDescriptionType prepareRoleOutlier : prepareUserOutliers) {
//                            roleOutliers.getResult().add(prepareRoleOutlier.clone());
//                        }
//                        map.put(role, roleOutliers);
//                    } else {
//                        for (RoleAnalysisOutlierDescriptionType prepareRoleOutlier : prepareUserOutliers) {
//                            roleOutliers.getResult().add(prepareRoleOutlier.clone());
//                        }
//                    }
//
//                }
//
//            }
//        }
//    }

}
