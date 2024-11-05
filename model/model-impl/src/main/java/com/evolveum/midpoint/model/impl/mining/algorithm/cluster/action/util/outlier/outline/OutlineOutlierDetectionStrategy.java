/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.outline;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.analysis.cache.ObjectCategorisationCache;
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
            @NotNull ObjectCategorisationCache objectCategorisationCache,
            @NotNull Task task,
            @NotNull OperationResult result) {
        OutlierDetectionOutlineModel outlineModel = new OutlierDetectionOutlineModel(
                roleAnalysisService, session, cluster, objectCategorisationCache, task, result);

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
        OutlierDetectionOutlineModel outlineModel = analysisModel.getOutlineModel();
        RoleAnalysisSessionType session = outlineModel.getSession();
        ObjectReferenceType analyzedObjectRef = analysisModel.getAnalyzedObjectRef();
        String memberOid = analyzedObjectRef.getOid();
        RoleAnalysisDetectionOptionType detectionOption = session.getDefaultDetectionOption();
        Double sensitivity = detectionOption.getSensitivity();
        double requiredConfidence = roleAnalysisService.calculateOutlierConfidenceRequired(sensitivity) * 100.0;

        if (!analysisModel.isSuitableForDetection()) {
            RoleAnalysisOutlierPartitionType partition = prepareTotalOutlierPartition(
                    outlineModel.getClusterRef(),
                    outlineModel.getSessionRef(),
                    requiredConfidence
            );
            updateOrImportOutlierObject(roleAnalysisService, session, analysisModel.getAnalyzedObjectRef().getOid(), partition, analysisCache, task, result);
            return;
        }

        ZScoreData zScoreData = analysisModel.getzScoreData();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = analysisModel.getMiningRoleTypeChunks();
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

        RoleAnalysisOutlierPartitionType partition;
        if (!detectedAnomalyResults.isEmpty()) {
            //TODO duplicate access?
            OutlierAnalyzeModel outlierAnalyzeModel = new OutlierAnalyzeModel(analysisModel);
            partition = analyzeAndResolveOutlierObject(roleAnalysisService,
                    analysisCache,
                    outlierAnalyzeModel,
                    detectedAnomalyResults,
                    task,
                    result);
            RoleAnalysisPartitionAnalysisType partitionAnalysis = partition.getPartitionAnalysis();
            Double partitionOverallConfidence = partitionAnalysis.getOverallConfidence();
            if (partitionOverallConfidence == null || partitionOverallConfidence < requiredConfidence) {
                partition = prepareTotalOutlierPartition(
                        outlineModel.getClusterRef(),
                        outlineModel.getSessionRef(),
                        requiredConfidence
                );
            }
        } else {
            partition = prepareTotalOutlierPartition(
                    outlineModel.getClusterRef(),
                    outlineModel.getSessionRef(),
                    requiredConfidence
            );
        }

        updateOrImportOutlierObject(roleAnalysisService, session, memberOid, partition, analysisCache, task, result);
    }
}
