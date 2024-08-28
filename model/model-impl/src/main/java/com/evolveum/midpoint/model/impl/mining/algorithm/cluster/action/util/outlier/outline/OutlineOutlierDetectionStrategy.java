/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.outline;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.utils.values.*;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.common.mining.objects.analysis.cache.AttributeAnalysisCache;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutlierAnalyzeModel;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutlierDetectionStrategy;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutliersDetectionUtil.*;

//TODO clean/remove duplicates implement role mode
public class OutlineOutlierDetectionStrategy implements OutlierDetectionStrategy {

    private static final Trace LOGGER = TraceManager.getTrace(OutlineOutlierDetectionStrategy.class);

    @Override
    public void executeAnalysis(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull RoleAnalysisSessionType session,
            @NotNull AttributeAnalysisCache userAnalysisCache,
            @NotNull Task task,
            @NotNull OperationResult result) {
        OutlierDetectionOutlineModel outlineModel = new OutlierDetectionOutlineModel(
                roleAnalysisService, session, cluster, task, result);

        analyseOutlierClusterMembers(roleAnalysisService,
                outlineModel,
                userAnalysisCache,
                task,
                result
        );
    }

    private void analyseOutlierClusterMembers(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull OutlierDetectionOutlineModel outlineModel,
            @NotNull AttributeAnalysisCache userAnalysisCache,
            @NotNull Task task,
            @NotNull OperationResult result) {

        RoleAnalysisClusterType cluster = outlineModel.getCluster();
        List<ObjectReferenceType> member = cluster.getMember();

        for (ObjectReferenceType analyzedObjectRef : member) {
            if (result.isError()) {
                LOGGER.warn("Error during outlier detection for user: {}", result.getMessage());
            }

            OutlierDetectionOutlineClusterModel outlineClusterModel = new OutlierDetectionOutlineClusterModel(
                    roleAnalysisService, outlineModel, userAnalysisCache, analyzedObjectRef, task, result);

            if (!outlineClusterModel.isSuitableForDetection()) {
                continue;
            }

            outlierAnalysisProcess(
                    roleAnalysisService,
                    outlineClusterModel,
                    userAnalysisCache,
                    task,
                    result
            );
        }
    }

    /**
     * NOTE just in case of user clustering mode.
     */
    private void outlierAnalysisProcess(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull OutlierDetectionOutlineClusterModel analysisModel,
            @NotNull AttributeAnalysisCache analysisCache,
            @NotNull Task task,
            @NotNull OperationResult result) {

        ZScoreData zScoreData = analysisModel.getzScoreData();
        ObjectReferenceType analyzedObjectRef = analysisModel.getAnalyzedObjectRef();
        String memberOid = analyzedObjectRef.getOid();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = analysisModel.getMiningRoleTypeChunks();
        OutlierDetectionOutlineModel outlineModel = analysisModel.getOutlineModel();
        RoleAnalysisSessionType session = outlineModel.getSession();
        int userCountInRepo = outlineModel.getUserCountInRepo();

        List<RoleAnalysisAttributeDef> attributesForUserAnalysis = roleAnalysisService.resolveAnalysisAttributes(
                session, UserType.COMPLEX_TYPE);

        ListMultimap<String, DetectedAnomalyResult> userRoleMap = ArrayListMultimap.create();

        List<String> members = new ArrayList<>();
        members.add(memberOid);
        for (MiningRoleTypeChunk miningRoleTypeChunk : miningRoleTypeChunks) {
            FrequencyItem frequencyItem = miningRoleTypeChunk.getFrequencyItem();
            FrequencyItem.Status status = frequencyItem.getStatus();
            List<String> properties = miningRoleTypeChunk.getProperties();
            if (!status.equals(FrequencyItem.Status.INCLUDE) && properties.contains(memberOid)) {
                resolveOutlierAnomalies(roleAnalysisService,
                        analysisCache,
                        task,
                        result,
                        miningRoleTypeChunk,
                        zScoreData,
                        frequencyItem,
                        members,
                        miningRoleTypeChunks,
                        session,
                        attributesForUserAnalysis,
                        userCountInRepo,
                        userRoleMap);
            }
        }

        Collection<DetectedAnomalyResult> detectedAnomalyResults = userRoleMap.get(memberOid);
        if (detectedAnomalyResults.isEmpty()) {
            return;
        }

        OutlierAnalyzeModel outlierAnalyzeModel = new OutlierAnalyzeModel(analysisModel);
        analyzeAndResolveOutlierObject(roleAnalysisService,
                analysisCache,
                outlierAnalyzeModel,
                detectedAnomalyResults,
                task,
                result);
    }
}
