/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.detection;

import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;
import static com.evolveum.midpoint.model.impl.mining.algorithm.detection.DefaultPatternResolver.loadTopPatterns;
import static com.evolveum.midpoint.model.impl.mining.utils.RoleAnalysisObjectUtils.*;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.objects.detection.DetectionOption;
import com.evolveum.midpoint.common.mining.objects.handler.Handler;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.mining.algorithm.chunk.PrepareChunkStructure;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

public class DetectionActionExecutorNew implements Serializable {
    private final DetectionOperation detectionType;
    Handler handler;
    private final String clusterOid;
    private final ModelService modelService;
    private final OperationResult result;
    private final Task task;

    public DetectionActionExecutorNew(String clusterOid, ModelService modelService, OperationResult result, Task task) {
        this.detectionType = new PatternResolver();
        this.clusterOid = clusterOid;
        this.modelService = modelService;
        this.result = result;
        this.handler = new Handler("Pattern Detection", 6);
        this.task = task;
    }

    public void executeDetectionProcess() {
        handler.setSubTitle("Load Data");
        handler.setActive(true);
        handler.setOperationCountToProcess(1);
        PrismObject<RoleAnalysisClusterType> clusterPrismObject = getClusterTypeObject(modelService, clusterOid, result, task);
        if (clusterPrismObject == null) {
            LOGGER.error("Failed to resolve RoleAnalysisClusterType from UUID: {}", clusterOid);
            return;
        }

        RoleAnalysisClusterType cluster = clusterPrismObject.asObjectable();

        ObjectReferenceType roleAnalysisSessionRef = cluster.getRoleAnalysisSessionRef();

        String sessionOid = roleAnalysisSessionRef.getOid();
        PrismObject<RoleAnalysisSessionType> sessionTypeObject = getSessionTypeObject(modelService, result,
                sessionOid, task);

        if (sessionTypeObject == null) {
            LOGGER.error("Failed to resolve RoleAnalysisSessionType from UUID: {}", sessionOid);
            return;
        }

        RoleAnalysisProcessModeType processMode = sessionTypeObject.asObjectable().getProcessMode();

        MiningOperationChunk miningOperationChunk = new PrepareChunkStructure().executeOperation(cluster, true,
                processMode, modelService, result, task);

        List<MiningRoleTypeChunk> miningRoleTypeChunks = miningOperationChunk.getMiningRoleTypeChunks(RoleAnalysisSortMode.NONE);
        List<MiningUserTypeChunk> miningUserTypeChunks = miningOperationChunk.getMiningUserTypeChunks(RoleAnalysisSortMode.NONE);
        handler.iterateActualStatus();

        DetectionOption detectionOption = new DetectionOption(cluster);
        List<DetectedPattern> detectedPatterns = executeDetection(miningRoleTypeChunks, miningUserTypeChunks,
                processMode, detectionOption);

        if (detectedPatterns != null) {
            detectedPatterns = loadTopPatterns(detectedPatterns);
        }

        replaceRoleAnalysisClusterDetectionPattern(clusterOid, modelService,
                result,
                detectedPatterns, task
        );
    }

    private List<DetectedPattern> executeDetection(List<MiningRoleTypeChunk> miningRoleTypeChunks,
            List<MiningUserTypeChunk> miningUserTypeChunks, RoleAnalysisProcessModeType mode, DetectionOption detectionOption) {
        if (mode.equals(RoleAnalysisProcessModeType.USER)) {
            return detectionType.performUserBasedDetection(miningRoleTypeChunks, detectionOption, handler);
        } else if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return detectionType.performRoleBasedDetection(miningUserTypeChunks, detectionOption, handler);
        }
        return null;
    }

    public Handler getHandler() {
        return handler;
    }

}
