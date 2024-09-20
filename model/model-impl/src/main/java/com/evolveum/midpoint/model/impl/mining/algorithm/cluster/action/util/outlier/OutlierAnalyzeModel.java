/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.inline.OutlierDetectionBasicModel;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.outline.OutlierDetectionOutlineClusterModel;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.outline.OutlierDetectionOutlineModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class OutlierAnalyzeModel {

    RoleAnalysisClusterType analysisCluster;
    ObjectReferenceType analyzedObjectRef;
    RoleAnalysisSessionType session;
    ObjectReferenceType clusterRef;
    ObjectReferenceType sessionRef;
    double similarityThreshold;
    PrismObject<UserType> userObject;
    List<MiningRoleTypeChunk> miningRoleTypeChunks;
    List<RoleAnalysisAttributeDef> attributesForUserAnalysis;
    OutlierNoiseCategoryType noiseCategory;
    OutlierClusterCategoryType outlierCategory;

    public OutlierAnalyzeModel(@NotNull OutlierDetectionOutlineClusterModel clusterModel){
        this.analysisCluster = clusterModel.getAnalysisCluster();
        this.analyzedObjectRef = clusterModel.getAnalyzedObjectRef();
        this.userObject = clusterModel.getUserObject();

        OutlierDetectionOutlineModel outlineModel = clusterModel.getOutlineModel();
        this.session = clusterModel.getOutlineModel().getSession();
        this.clusterRef = outlineModel.getClusterRef();
        this.sessionRef = outlineModel.getSessionRef();
        this.similarityThreshold = clusterModel.getSimilarityThreshold();
        this.miningRoleTypeChunks = clusterModel.getMiningRoleTypeChunks();
        this.attributesForUserAnalysis = outlineModel.getUserAnalysisAttributeDef();
        this.noiseCategory = clusterModel.getNoiseCategory();
        this.outlierCategory = clusterModel.getOutlierCategory();
    }

    public OutlierAnalyzeModel(@NotNull OutlierDetectionBasicModel basicModel,
            @NotNull PrismObject<UserType> userObject,
            @NotNull ObjectReferenceType analyzedObjectRef){
        this.analysisCluster = basicModel.getCluster();
        this.analyzedObjectRef = analyzedObjectRef;
        this.userObject = userObject;

        this.session = basicModel.getSession();
        this.clusterRef = basicModel.getClusterRef();
        this.sessionRef = basicModel.getSessionRef();
        this.similarityThreshold = basicModel.getSimilarityThreshold();
        this.miningRoleTypeChunks = basicModel.getMiningRoleTypeChunks();
        this.attributesForUserAnalysis = basicModel.getAttributesForUserAnalysis();
        this.noiseCategory = basicModel.getNoiseCategory();
        this.outlierCategory = basicModel.getOutlierCategory();
    }

    public RoleAnalysisClusterType getAnalysisCluster() {
        return analysisCluster;
    }

    public ObjectReferenceType getAnalyzedObjectRef() {
        return analyzedObjectRef;
    }

    public RoleAnalysisSessionType getSession() {
        return session;
    }

    public ObjectReferenceType getClusterRef() {
        return clusterRef;
    }

    public ObjectReferenceType getSessionRef() {
        return sessionRef;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public PrismObject<UserType> getUserObject() {
        return userObject;
    }

    public List<MiningRoleTypeChunk> getMiningRoleTypeChunks() {
        return miningRoleTypeChunks;
    }

    public List<RoleAnalysisAttributeDef> getAttributesForUserAnalysis() {
        return attributesForUserAnalysis;
    }

    public OutlierNoiseCategoryType getNoiseCategory() {
        return noiseCategory;
    }

    public OutlierClusterCategoryType getOutlierCategory() {
        return outlierCategory;
    }
}
