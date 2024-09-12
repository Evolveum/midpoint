/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.inline;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.common.mining.utils.values.ZScoreData;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.List;

public class OutlierDetectionBasicModel {

    RoleAnalysisSessionType session;
    RoleAnalysisClusterType cluster;
    ObjectReferenceType clusterRef;
    ObjectReferenceType sessionRef;
    MiningOperationChunk miningOperationChunk;
    RoleAnalysisOptionType analysisOption;
    RoleAnalysisProcessModeType processMode;
    RangeType frequencyRange;
    Double sensitivity;
    double similarityThreshold;
    List<RoleAnalysisAttributeDef> attributesForUserAnalysis;
    int userCountInRepo = 0;
    ZScoreData zScoreData;
    List<MiningRoleTypeChunk> miningRoleTypeChunks;
    int countOfRoles;

    OutlierNoiseCategoryType noiseCategory = OutlierNoiseCategoryType.SUITABLE;
    OutlierClusterCategoryType outlierCategory = OutlierClusterCategoryType.INNER_OUTLIER;

    public OutlierDetectionBasicModel(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisSessionType session,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull Task task,
            @NotNull OperationResult result) {
        this.session = session;
        this.cluster = cluster;

        this.analysisOption = session.getAnalysisOption();
        this.processMode = analysisOption.getProcessMode();

        this.miningOperationChunk = roleAnalysisService.prepareCompressedMiningStructure(cluster, null, true,
                processMode, result, task);

        RoleAnalysisDetectionOptionType defaultDetectionOption = session.getDefaultDetectionOption();
        this.frequencyRange = defaultDetectionOption.getFrequencyRange();
        this.sensitivity = defaultDetectionOption.getSensitivity() == null
                ? 0.0
                : defaultDetectionOption.getSensitivity();

        UserAnalysisSessionOptionType userModeOptions = session.getUserModeOptions();
        this.similarityThreshold = userModeOptions.getSimilarityThreshold() == null
                ? 0.0
                : userModeOptions.getSimilarityThreshold();

        this.clusterRef = new ObjectReferenceType()
                .oid(cluster.getOid())
                .type(RoleAnalysisClusterType.COMPLEX_TYPE)
                .targetName(cluster.getName());

        this.sessionRef = new ObjectReferenceType()
                .oid(session.getOid())
                .type(RoleAnalysisSessionType.COMPLEX_TYPE)
                .targetName(session.getName());

        this.attributesForUserAnalysis = roleAnalysisService.resolveAnalysisAttributes(
                session, UserType.COMPLEX_TYPE);

        this.userCountInRepo = roleAnalysisService.countObjects(UserType.class, null, null, task, result);

        this.miningRoleTypeChunks = miningOperationChunk.getMiningRoleTypeChunks(
                RoleAnalysisSortMode.NONE);

        this.countOfRoles = 0;
        for (MiningRoleTypeChunk miningRoleTypeChunk : miningRoleTypeChunks) {
            this.countOfRoles += miningRoleTypeChunk.getRoles().size();
        }

        this.zScoreData = roleAnalysisService.resolveOutliersZScore(miningRoleTypeChunks, frequencyRange, sensitivity);
    }

    public RoleAnalysisProcessModeType getProcessMode() {
        return analysisOption.getProcessMode();
    }

    public MiningOperationChunk getMiningOperationChunk() {
        return miningOperationChunk;
    }

    public RoleAnalysisOptionType getAnalysisOption() {
        return analysisOption;
    }

    public RangeType getFrequencyRange() {
        return frequencyRange;
    }

    public Double getSensitivity() {
        return sensitivity;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public RoleAnalysisSessionType getSession() {
        return session;
    }

    public RoleAnalysisClusterType getCluster() {
        return cluster;
    }

    public ObjectReferenceType getClusterRef() {
        return clusterRef;
    }

    public ObjectReferenceType getSessionRef() {
        return sessionRef;
    }

    public List<RoleAnalysisAttributeDef> getAttributesForUserAnalysis() {
        return attributesForUserAnalysis;
    }

    public int getUserCountInRepo() {
        return userCountInRepo;
    }

    public ZScoreData getZScoreData() {
        return zScoreData;
    }

    public List<MiningRoleTypeChunk> getMiningRoleTypeChunks() {
        return miningRoleTypeChunks;
    }

    public int getCountOfRoles() {
        return countOfRoles;
    }

    public OutlierNoiseCategoryType getNoiseCategory() {
        return noiseCategory;
    }

    public OutlierClusterCategoryType getOutlierCategory() {
        return outlierCategory;
    }
}
