/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.detection;

import static com.evolveum.midpoint.model.impl.mining.algorithm.detection.DefaultPatternResolver.loadTopPatterns;
import static com.evolveum.midpoint.model.impl.mining.utils.RoleAnalysisObjectUtils.*;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.objects.detection.DetectionOption;
import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.mining.algorithm.BaseAction;
import com.evolveum.midpoint.model.impl.mining.algorithm.chunk.PrepareChunkStructure;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

public class DetectionActionExecutor extends BaseAction {

    private final DetectionOperation detectionType;
    private final RoleAnalysisProgressIncrement handler = new RoleAnalysisProgressIncrement("Pattern Detection", 6, this::incrementProgress);
    private final String clusterOid;
    private final ModelService modelService;

    /** BEWARE! Do not create subresults from this value. Just to avoid confusion. */
    private final OperationResult result;
    private final Task task;
    private static final Trace LOGGER = TraceManager.getTrace(DetectionActionExecutor.class);


    public DetectionActionExecutor(
            @NotNull AbstractActivityRun<?, ?, ?> activityRun,
            String clusterOid,
            OperationResult result) {
        super(activityRun);
        this.detectionType = new PatternResolver();
        this.clusterOid = clusterOid;
        this.modelService = ModelBeans.get().modelService;
        this.result = result;
        this.task = activityRun.getRunningTask();
    }

    public void executeDetectionProcess() {
        handler.enterNewStep("Load Data");
        handler.setActive(true);
        handler.setOperationCountToProcess(1);
        PrismObject<RoleAnalysisClusterType> clusterPrismObject = getClusterTypeObject(modelService, clusterOid, task, result);
        if (clusterPrismObject == null) {
            LOGGER.error("Failed to resolve RoleAnalysisClusterType from UUID: {}", clusterOid);
            return;
        }

        RoleAnalysisClusterType cluster = clusterPrismObject.asObjectable();

        ObjectReferenceType roleAnalysisSessionRef = cluster.getRoleAnalysisSessionRef();

        String sessionOid = roleAnalysisSessionRef.getOid();
        PrismObject<RoleAnalysisSessionType> sessionTypeObject = getSessionTypeObject(modelService, sessionOid, task, result
        );

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

        if (detectedPatterns != null && !detectedPatterns.isEmpty()) {
            detectedPatterns = loadTopPatterns(detectedPatterns);
            replaceRoleAnalysisClusterDetectionPattern(modelService, clusterOid,
                    detectedPatterns, task, result
            );
        }

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

    public RoleAnalysisProgressIncrement getHandler() {
        return handler;
    }

}
