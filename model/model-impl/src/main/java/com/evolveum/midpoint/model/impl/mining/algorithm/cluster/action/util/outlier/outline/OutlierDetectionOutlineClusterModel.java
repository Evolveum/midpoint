/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.outline;

import com.evolveum.midpoint.common.mining.objects.analysis.AttributeAnalysisStructure;
import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.analysis.cache.AttributeAnalysisCache;
import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.statistic.ClusterStatistic;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChunkAction;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChunkMode;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.common.mining.utils.values.ZScoreData;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.collect.ListMultimap;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.model.impl.mining.RoleAnalysisServiceImpl.resolveJaccardCloseObjectResult;
import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutliersDetectionUtil.prepareDetectionOptions;
import static com.evolveum.midpoint.model.impl.mining.utils.RoleAnalysisAlgorithmUtils.resolveAttributeStatistics;

public class OutlierDetectionOutlineClusterModel {

    RoleAnalysisClusterType analysisCluster;
    ObjectReferenceType analyzedObjectRef;
    PrismObject<UserType> userObject;
    @NotNull OutlierDetectionOutlineModel outlineModel;
    List<MiningRoleTypeChunk> miningRoleTypeChunks;
    boolean isSuitableForDetection;
    double minThreshold = 0.5;
    double minThresholdForTotalOutlier = 0.5;
    ZScoreData zScoreData;
    MutableDouble usedFrequency;
    double similarityThreshold;

    OutlierNoiseCategoryType noiseCategory = OutlierNoiseCategoryType.OVERAL_NOISE;
    OutlierClusterCategoryType outlierCategory = OutlierClusterCategoryType.OUTER_OUTLIER;

    public OutlierDetectionOutlineClusterModel(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull OutlierDetectionOutlineModel outlineModel,
            @NotNull AttributeAnalysisCache userAnalysisCache,
            @NotNull ObjectReferenceType analyzedObjectRef,
            @NotNull Task task,
            @NotNull OperationResult result) {
        this.outlineModel = outlineModel;
        this.analyzedObjectRef = analyzedObjectRef;

        String description = analyzedObjectRef.getDescription();
        if (description != null && !description.equals("unknown")) {
            noiseCategory = OutlierNoiseCategoryType.fromValue(description);
        }

        prepareDetectionModel(roleAnalysisService, outlineModel, userAnalysisCache, analyzedObjectRef, task, result);
    }

    public void prepareDetectionModel(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull OutlierDetectionOutlineModel outlineModel,
            @NotNull AttributeAnalysisCache userAnalysisCache,
            @NotNull ObjectReferenceType analyzedObjectRef,
            @NotNull Task task,
            @NotNull OperationResult result) {

        ListMultimap<List<String>, String> chunkMap = outlineModel.getChunkMap();
        List<String> outliersClusterMembers = outlineModel.getOutliersClusterMembers();
        int minMembersCount = outlineModel.minMembersCount;
        String memberOid = analyzedObjectRef.getOid();
        this.usedFrequency = new MutableDouble(minThreshold);

        this.userObject = roleAnalysisService.getUserTypeObject(memberOid, task, result);
        if (this.userObject == null) {
            this.isSuitableForDetection = false;
            return;
        }

        List<String> jaccardCloseObject = prepareJaccardCloseObjects(roleAnalysisService,
                analyzedObjectRef,
                chunkMap,
                outliersClusterMembers,
                minMembersCount,
                task,
                result);
        if (jaccardCloseObject == null || jaccardCloseObject.isEmpty()) {
            this.isSuitableForDetection = false;
            return;
        } else {
            this.isSuitableForDetection = true;
        }
        RoleAnalysisSessionType session = outlineModel.getSession();
        RoleAnalysisClusterType tempCluster = createTemporaryCluster(session, jaccardCloseObject);

        MiningOperationChunk tempMiningOperationChunk = prepareTemporaryOperationChunk(
                roleAnalysisService,
                tempCluster,
                task,
                result);

        List<MiningRoleTypeChunk> tmpMiningRoleTypeChunks = tempMiningOperationChunk.getMiningRoleTypeChunks(
                RoleAnalysisSortMode.NONE);

        List<MiningUserTypeChunk> tmpMiningUserTypeChunks = tempMiningOperationChunk.getMiningUserTypeChunks(
                RoleAnalysisSortMode.NONE);

        this.miningRoleTypeChunks = tmpMiningRoleTypeChunks;

        List<RoleAnalysisAttributeDef> userAnalysisAttributeDef = outlineModel.getUserAnalysisAttributeDef();
        List<RoleAnalysisAttributeDef> roleAnalysisAttributeDef = outlineModel.getRoleAnalysisAttributeDef();
        processClusterAttributeAnalysis(roleAnalysisService, tempCluster,
                userAnalysisAttributeDef, roleAnalysisAttributeDef, userAnalysisCache, task, result);

        MutableDouble clusterDensity = new MutableDouble(0);
        MutableDouble clusterRolesCount = new MutableDouble(0);
        calculateTemporaryClusterDensityAndRolesCount(clusterDensity, clusterRolesCount,
                tmpMiningRoleTypeChunks, tmpMiningUserTypeChunks);
        double density = clusterDensity.doubleValue();
        int countOfRoles = clusterRolesCount.intValue();
        AnalysisClusterStatisticType clusterStatistics = tempCluster.getClusterStatistics();
        clusterStatistics.setMembershipDensity(density);
        clusterStatistics.setRolesCount(countOfRoles);

        RoleAnalysisDetectionOptionType detectionOption = outlineModel.getDetectionOption();
        RangeType standardDeviation = detectionOption.getStandardDeviation();
        Double frequencyThreshold = detectionOption.getFrequencyThreshold();
        Double sensitivity = detectionOption.getSensitivity();
        this.zScoreData = roleAnalysisService.resolveOutliersZScore(
                tmpMiningRoleTypeChunks, standardDeviation, sensitivity, frequencyThreshold);
        this.similarityThreshold = usedFrequency.doubleValue() * 100;
    }

    @Nullable
    private List<String> prepareJaccardCloseObjects(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull ObjectReferenceType analyzedObjectRef,
            @NotNull ListMultimap<List<String>, String> chunkMap,
            @NotNull List<String> outliersMembers,
            @NotNull Integer minMembersCount,
            @NotNull Task task,
            @NotNull OperationResult result) {
        String memberOid = analyzedObjectRef.getOid();
        ListMultimap<Double, String> similarityStats = roleAnalysisService.findJaccardCloseObject(
                memberOid,
                chunkMap,
                usedFrequency,
                outliersMembers,
                minThresholdForTotalOutlier,
                minMembersCount,
                task,
                result);

        List<String> jaccardCloseObject = resolveJaccardCloseObjectResult(minMembersCount, usedFrequency, similarityStats);

        if (jaccardCloseObject.isEmpty() || usedFrequency.doubleValue() < minThreshold) {
//            //TODO total outlier?
//            List<String> jaccardCloseObjectTotalOutlier = resolveJaccardCloseObjectResult(1, usedFrequency, similarityStats);
//            if (jaccardCloseObjectTotalOutlier.isEmpty() || jaccardCloseObjectTotalOutlier.get(0).equals(memberOid)) {
//                PrismObject<UserType> userTypeObject = roleAnalysisService.getUserTypeObject(memberOid, task, result);
//                if (userTypeObject == null) {
//                    return null;
//                }
//
//                RoleAnalysisOutlierPartitionType partition = prepareTotalOutlierPartition(clusterRef, sessionRef);
//
//                importOrExtendOutlier(roleAnalysisService, memberOid, partition, task, result);
//            }
            return null;
        }

        jaccardCloseObject.add(memberOid);
        return jaccardCloseObject;
    }

    public boolean isSuitableForDetection() {
        return isSuitableForDetection;
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
            @NotNull RoleAnalysisClusterType tempCluster,
            @NotNull Task task,
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

        return roleAnalysisService.prepareBasicChunkStructure(
                tempCluster, null, displayValueOption, RoleAnalysisProcessModeType.USER, null, result, task);
    }

    private void processClusterAttributeAnalysis(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType temporaryCluster,
            @Nullable List<RoleAnalysisAttributeDef> userAnalysisAttributeDef,
            @Nullable List<RoleAnalysisAttributeDef> roleAnalysisAttributeDef,
            @NotNull AttributeAnalysisCache userAnalysisCache,
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
                    .userTypeAttributeAnalysisCached(
                            clusterUsers, userDensity, userAnalysisCache, userAnalysisAttributeDef, task, result);
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
        this.analysisCluster = temporaryCluster;
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

    public RoleAnalysisClusterType getAnalysisCluster() {
        return analysisCluster;
    }

    public ObjectReferenceType getAnalyzedObjectRef() {
        return analyzedObjectRef;
    }

    public @NotNull OutlierDetectionOutlineModel getOutlineModel() {
        return outlineModel;
    }

    public ZScoreData getzScoreData() {
        return zScoreData;
    }

    public MutableDouble getUsedFrequency() {
        return usedFrequency;
    }

    public @NotNull List<MiningRoleTypeChunk> getMiningRoleTypeChunks() {
        return miningRoleTypeChunks;
    }

    public PrismObject<UserType> getUserObject() {
        return userObject;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public OutlierNoiseCategoryType getNoiseCategory() {
        return noiseCategory;
    }

    public OutlierClusterCategoryType getOutlierCategory() {
        return outlierCategory;
    }
}
