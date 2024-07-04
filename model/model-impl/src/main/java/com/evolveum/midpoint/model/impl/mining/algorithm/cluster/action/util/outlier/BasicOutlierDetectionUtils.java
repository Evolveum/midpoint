package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.utils.values.FrequencyItem;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.common.mining.utils.values.ZScoreData;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutliersDetectionUtil.*;

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
            HashMap<String, RoleAnalysisOutlierType> map,
            Double similarityThreshold) {
        List<MiningRoleTypeChunk> miningRoleTypeChunks = miningOperationChunk.getMiningRoleTypeChunks(
                RoleAnalysisSortMode.NONE);

        ZScoreData zScoreData = roleAnalysisService.resolveOutliersZScore(miningRoleTypeChunks, range, sensitivity);

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

                    if (userOutliers.getDuplicatedRoleAssignment() == null
                            || userOutliers.getDuplicatedRoleAssignment().isEmpty()) {
                        Set<ObjectReferenceType> duplicateAssignment = resolveUserDuplicateAssignment(
                                roleAnalysisService, userTypeObject.asObjectable(), task, result);
                        userOutliers.getDuplicatedRoleAssignment().addAll(duplicateAssignment);
                    }

                    userOutliers.setOutlierNoiseCategory(RoleAnalysisOutlierNoiseCategoryType.PART_OF_CLUSTER);

                    userOutliers.setSimilarObjectsThreshold(similarityThreshold);

                    //TODO should we use alg for precise similarity?
                    userOutliers.setSimilarObjects(cluster.getMember().size());
                    Double membershipDensity = cluster.getClusterStatistics().getMembershipDensity();
                    userOutliers.setSimilarObjectsDensity(membershipDensity);

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
                        userOutliers.setAttributeAnalysis(attributeAnalysis);
                    }

                    double assignmentFrequencyConfidence = calculateOutlierRoleAssignmentFrequencyConfidence(
                            userTypeObject, countOfRoles);
                    userOutliers.setOutlierAssignmentFrequencyConfidence(assignmentFrequencyConfidence);

                    detectAndLoadPatternAnalysis(userOutliers, miningRoleTypeChunks);

                    double outlierConfidenceBasedAssignment = 0;
                    for (RoleAnalysisOutlierDescriptionType prepareRoleOutlier : prepareRoleOutliers) {
                        detectAndLoadPatternAnalysis(miningRoleTypeChunk, user, prepareRoleOutlier, miningRoleTypeChunks);
                        double confidence = calculateAssignmentAnomalyConfidence(roleAnalysisService, session, userTypeObject, prepareRoleOutlier, task, result);
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
    }

    public static void resolveRoleModeOutliers(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull MiningOperationChunk miningOperationChunk,
            RangeType range,
            Double sensitivity,
            ObjectReferenceType clusterRef,
            ObjectReferenceType sessionRef,
            HashMap<String, RoleAnalysisOutlierType> map) {
        List<MiningUserTypeChunk> miningUserTypeChunks = miningOperationChunk.getMiningUserTypeChunks(
                RoleAnalysisSortMode.NONE);

        ZScoreData zScoreData = roleAnalysisService.resolveOutliersZScore(miningUserTypeChunks, range, sensitivity);

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

}
