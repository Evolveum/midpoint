/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util;

import java.util.*;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisAttributeDefUtils.getAttributesForUserAnalysis;

public class OutliersDetectionUtil {

    public static @Nullable RoleAnalysisAttributeAnalysisResult resolveSimilarAspect(
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull RoleAnalysisAttributeAnalysisResult outlierCandidateAttributeAnalysisResult) {
        Objects.requireNonNull(cluster);
        Objects.requireNonNull(outlierCandidateAttributeAnalysisResult);

        List<RoleAnalysisAttributeAnalysis> outlierAttributeAnalysis = outlierCandidateAttributeAnalysisResult.getAttributeAnalysis();
        if (outlierAttributeAnalysis.isEmpty()) {
            return null;
        }

        AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();
        RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = clusterStatistics.getUserAttributeAnalysisResult();
        if (userAttributeAnalysisResult == null) {
            return null;
        }
        List<RoleAnalysisAttributeAnalysis> attributeAnalysis = userAttributeAnalysisResult.getAttributeAnalysis();
        if (attributeAnalysis.isEmpty()) {
            return null;
        }

        RoleAnalysisAttributeAnalysisResult outlierAttributeAnalysisResult = new RoleAnalysisAttributeAnalysisResult();

        for (RoleAnalysisAttributeAnalysis clusterAnalysis : attributeAnalysis) {
            String clusterItemPath = clusterAnalysis.getItemPath();
            Double clusterDensity = clusterAnalysis.getDensity();
            Set<String> outlierValues = extractCorrespondingOutlierValues(outlierCandidateAttributeAnalysisResult, clusterItemPath);
            if (outlierValues == null) {
                continue;
            }

            RoleAnalysisAttributeAnalysis correspondingAttributeAnalysis = new RoleAnalysisAttributeAnalysis();
            correspondingAttributeAnalysis.setItemPath(clusterItemPath);
            correspondingAttributeAnalysis.setDensity(clusterDensity);

            List<RoleAnalysisAttributeStatistics> attributeStatistics = clusterAnalysis.getAttributeStatistics();
            for (RoleAnalysisAttributeStatistics attributeStatistic : attributeStatistics) {
                String clusterAttributeValue = attributeStatistic.getAttributeValue();
                if (outlierValues.contains(clusterAttributeValue)) {
                    correspondingAttributeAnalysis.getAttributeStatistics().add(attributeStatistic);
                }
            }

            outlierAttributeAnalysisResult.getAttributeAnalysis().add(correspondingAttributeAnalysis);
        }

        return outlierAttributeAnalysisResult;
    }

    public static @Nullable RoleAnalysisAttributeAnalysisResult resolveSimilarAspect(
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull PrismObject<UserType> prismUser) {
        Objects.requireNonNull(cluster);
        Objects.requireNonNull(prismUser);

        RoleAnalysisAttributeAnalysisResult outlierCandidateAttributeAnalysisResult = new RoleAnalysisAttributeAnalysisResult();

        List<RoleAnalysisAttributeDef> itemDef = getAttributesForUserAnalysis();

        for (RoleAnalysisAttributeDef item : itemDef) {
            RoleAnalysisAttributeAnalysis roleAnalysisAttributeAnalysis = new RoleAnalysisAttributeAnalysis();
            roleAnalysisAttributeAnalysis.setItemPath(item.getPath().toString());
            List<RoleAnalysisAttributeStatistics> attributeStatistics = roleAnalysisAttributeAnalysis.getAttributeStatistics();

            ItemPath path = item.getPath();
            boolean isContainer = item.isContainer();

            if (isContainer) {
                Set<String> values = item.resolveMultiValueItem(prismUser, path);
                for (String value : values) {
                    RoleAnalysisAttributeStatistics attributeStatistic = new RoleAnalysisAttributeStatistics();
                    attributeStatistic.setAttributeValue(value);
                    attributeStatistics.add(attributeStatistic);
                }
            } else {
                String value = item.resolveSingleValueItem(prismUser, path);
                if (value != null) {
                    RoleAnalysisAttributeStatistics attributeStatistic = new RoleAnalysisAttributeStatistics();
                    attributeStatistic.setAttributeValue(value);
                    attributeStatistics.add(attributeStatistic);
                }
            }
            outlierCandidateAttributeAnalysisResult.getAttributeAnalysis().add(roleAnalysisAttributeAnalysis.clone());
        }

        List<RoleAnalysisAttributeAnalysis> outlierAttributeAnalysis = outlierCandidateAttributeAnalysisResult.getAttributeAnalysis();
        if (outlierAttributeAnalysis.isEmpty()) {
            return null;
        }

        AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();
        RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = clusterStatistics.getUserAttributeAnalysisResult();
        if (userAttributeAnalysisResult == null) {
            return null;
        }
        List<RoleAnalysisAttributeAnalysis> attributeAnalysis = userAttributeAnalysisResult.getAttributeAnalysis();
        if (attributeAnalysis.isEmpty()) {
            return null;
        }

        RoleAnalysisAttributeAnalysisResult outlierAttributeAnalysisResult = new RoleAnalysisAttributeAnalysisResult();

        for (RoleAnalysisAttributeAnalysis clusterAnalysis : attributeAnalysis) {
            String clusterItemPath = clusterAnalysis.getItemPath();
            Double clusterDensity = clusterAnalysis.getDensity();
            Set<String> outlierValues = extractCorrespondingOutlierValues(outlierCandidateAttributeAnalysisResult, clusterItemPath);
            if (outlierValues == null) {
                continue;
            }

            RoleAnalysisAttributeAnalysis correspondingAttributeAnalysis = new RoleAnalysisAttributeAnalysis();
            correspondingAttributeAnalysis.setItemPath(clusterItemPath);
            correspondingAttributeAnalysis.setDensity(clusterDensity);

            List<RoleAnalysisAttributeStatistics> attributeStatistics = clusterAnalysis.getAttributeStatistics();
            for (RoleAnalysisAttributeStatistics attributeStatistic : attributeStatistics) {
                String clusterAttributeValue = attributeStatistic.getAttributeValue();
                if (outlierValues.contains(clusterAttributeValue)) {
                    correspondingAttributeAnalysis.getAttributeStatistics().add(attributeStatistic);
                }
            }

            outlierAttributeAnalysisResult.getAttributeAnalysis().add(correspondingAttributeAnalysis);
        }

        return outlierAttributeAnalysisResult;
    }

    private static @Nullable Set<String> extractCorrespondingOutlierValues(
            @NotNull RoleAnalysisAttributeAnalysisResult outlierCandidateAttributeAnalysisResult, String itemPath) {
        List<RoleAnalysisAttributeAnalysis> outlier = outlierCandidateAttributeAnalysisResult.getAttributeAnalysis();
        for (RoleAnalysisAttributeAnalysis outlierAttribute : outlier) {
            if (outlierAttribute.getItemPath().equals(itemPath)) {
                Set<String> outlierValues = new HashSet<>();
                for (RoleAnalysisAttributeStatistics attributeStatistic : outlierAttribute.getAttributeStatistics()) {
                    outlierValues.add(attributeStatistic.getAttributeValue());
                }
                return outlierValues;
            }
        }
        return null;
    }

    public static Collection<RoleAnalysisOutlierType> executeOutliersAnalysis(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull RoleAnalysisSessionType session,
            @NotNull RoleAnalysisOptionType analysisOption,
            Double minFrequency,
            @NotNull Task task,
            @NotNull OperationResult result) {

        if (minFrequency == null) {
            minFrequency = 0.0;
        }
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

        //TODO role mode
        if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
            List<MiningRoleTypeChunk> miningRoleTypeChunks = miningOperationChunk.getMiningRoleTypeChunks(RoleAnalysisSortMode.NONE);

            for (MiningRoleTypeChunk miningRoleTypeChunk : miningRoleTypeChunks) {

                double frequency = miningRoleTypeChunk.getFrequency();
                if (frequency * 100 < minFrequency) {
                    List<String> roles = miningRoleTypeChunk.getMembers();
                    List<String> users = miningRoleTypeChunk.getProperties();

                    List<RoleAnalysisOutlierDescriptionType> prepareRoleOutliers = new ArrayList<>();

                    roles.forEach(role -> {
                        RoleAnalysisOutlierDescriptionType outlierDescription = new RoleAnalysisOutlierDescriptionType();
                        outlierDescription.setCategory(OutlierCategory.INNER_OUTLIER);
                        outlierDescription.setObject(new ObjectReferenceType().
                                oid(role)
                                .type(RoleType.COMPLEX_TYPE));

                        outlierDescription.setConfidence(1 - frequency);

                        outlierDescription.setCluster(clusterRef.clone());

                        outlierDescription.setSession(sessionRef.clone());

                        prepareRoleOutliers.add(outlierDescription);
                    });

                    for (String user : users) {
                        RoleAnalysisOutlierType userOutliers = map.get(user);

                        if (userOutliers == null) {
                            userOutliers = new RoleAnalysisOutlierType();
                            userOutliers.setTargetObjectRef(new ObjectReferenceType().oid(user).type(UserType.COMPLEX_TYPE));
                            for (RoleAnalysisOutlierDescriptionType prepareRoleOutlier : prepareRoleOutliers) {
                                userOutliers.getResult().add(prepareRoleOutlier.clone());
                            }
                            map.put(user, userOutliers);
                        } else {
                            for (RoleAnalysisOutlierDescriptionType prepareRoleOutlier : prepareRoleOutliers) {
                                userOutliers.getResult().add(prepareRoleOutlier.clone());
                            }
                        }

                    }

                }
            }

        } else if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            List<MiningUserTypeChunk> miningUserTypeChunks = miningOperationChunk.getMiningUserTypeChunks(RoleAnalysisSortMode.NONE);

            for (MiningUserTypeChunk miningUserTypeChunk : miningUserTypeChunks) {

                double frequency = miningUserTypeChunk.getFrequency();
                if (frequency * 100 < minFrequency) {
                    List<String> roles = miningUserTypeChunk.getProperties();
                    List<String> users = miningUserTypeChunk.getMembers();

                    List<RoleAnalysisOutlierDescriptionType> prepareUserOutliers = new ArrayList<>();

                    users.forEach(user -> {
                        RoleAnalysisOutlierDescriptionType outlierDescription = new RoleAnalysisOutlierDescriptionType();
                        outlierDescription.setCategory(OutlierCategory.INNER_OUTLIER);
                        outlierDescription.setObject(new ObjectReferenceType().
                                oid(user)
                                .type(UserType.COMPLEX_TYPE));

                        outlierDescription.setConfidence(1 - frequency);

                        outlierDescription.setCluster(clusterRef.clone());

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
}
