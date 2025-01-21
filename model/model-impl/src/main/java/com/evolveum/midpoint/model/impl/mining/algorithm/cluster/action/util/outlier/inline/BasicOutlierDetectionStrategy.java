/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.inline;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.common.mining.objects.analysis.cache.ObjectCategorisationCache;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.analysis.cache.AttributeAnalysisCache;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.utils.values.FrequencyItem;
import com.evolveum.midpoint.common.mining.utils.values.ZScoreData;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutlierAnalyzeModel;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutlierDetectionStrategy;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutliersDetectionUtil.*;

//TODO clean/remove duplicates implement role mode

//com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.inline.BasicOutlierDetectionStrategy
public class BasicOutlierDetectionStrategy implements OutlierDetectionStrategy {

    private static final Trace LOGGER = TraceManager.getTrace(BasicOutlierDetectionStrategy.class);

    @Override
    public void executeAnalysis(@NotNull RoleAnalysisService roleAnalysisService,
                                @NotNull RoleAnalysisClusterType cluster,
                                @NotNull RoleAnalysisSessionType session,
                                @NotNull AttributeAnalysisCache userAnalysisCache,
                                @NotNull ObjectCategorisationCache objectCategorisationCache, @NotNull Task task,
                                @NotNull OperationResult result) {

        OutlierDetectionBasicModel model = new OutlierDetectionBasicModel(
                roleAnalysisService, session, cluster, task, result);
        //TODO role mode
        outlierAnalysisProcess(
                roleAnalysisService, model, userAnalysisCache, task, result);

    }

    /**
     * NOTE just in case of user clustering mode.
     */
    private void outlierAnalysisProcess(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull OutlierDetectionBasicModel model,
            @NotNull AttributeAnalysisCache analysisCache,
            @NotNull Task task,
            @NotNull OperationResult result) {

        RoleAnalysisSessionType session = model.getSession();
        int userCountInRepo = model.getUserCountInRepo();
        List<RoleAnalysisAttributeDef> attributesForUserAnalysis = model.getAttributesForUserAnalysis();
        ZScoreData zScoreData = model.getZScoreData();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = model.getMiningRoleTypeChunks();
        ListMultimap<String, DetectedAnomalyResult> userRoleMap = ArrayListMultimap.create();

        long startTime = System.currentTimeMillis();

        for (MiningRoleTypeChunk miningRoleTypeChunk : miningRoleTypeChunks) {
            FrequencyItem frequencyItem = miningRoleTypeChunk.getFrequencyItem();
            FrequencyItem.Status status = frequencyItem.getStatus();
            if (!status.equals(FrequencyItem.Status.INCLUDE)) {
                List<String> members = miningRoleTypeChunk.getProperties();
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

        long endTime = System.currentTimeMillis();
        double totalProcessingTime = (endTime - startTime) / 1000.0;
        LOGGER.debug("ALL ANOMALY/CHUNK: TOTAL PROCESSING TIME: {} seconds", totalProcessingTime);

        startTime = System.currentTimeMillis();
        Set<String> keySet = userRoleMap.keySet();

        for (String memberOid : keySet) {
            PrismObject<UserType> userObject = roleAnalysisService.getUserTypeObject(memberOid, task, result);
            Collection<DetectedAnomalyResult> detectedAnomalyResults = userRoleMap.get(memberOid);
            if (userObject == null || detectedAnomalyResults.isEmpty()) {
                continue;
            }

            ObjectReferenceType analyzedObjectRef = new ObjectReferenceType()
                    .oid(memberOid)
                    .type(UserType.COMPLEX_TYPE);

            OutlierAnalyzeModel outlierAnalyzeModel = new OutlierAnalyzeModel(model, userObject, analyzedObjectRef);
            RoleAnalysisOutlierPartitionType partition = analyzeAndResolveOutlierObject(roleAnalysisService,
                    analysisCache,
                    outlierAnalyzeModel,
                    detectedAnomalyResults,
                    task,
                    result);

            updateOrImportOutlierObject(roleAnalysisService, session, memberOid, partition, analysisCache, task, result);
        }

        endTime = System.currentTimeMillis();
        totalProcessingTime = (endTime - startTime) / 1000.0;
        LOGGER.debug("ALL USER CANDIDATE OUTLIER KEYSET: TOTAL PROCESSING TIME: {} seconds", totalProcessingTime);
    }

}
