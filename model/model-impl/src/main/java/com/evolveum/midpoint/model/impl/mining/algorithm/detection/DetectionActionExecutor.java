/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.detection;

import static com.evolveum.midpoint.model.impl.mining.algorithm.detection.DefaultPatternResolver.loadTopPatterns;

import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.objects.detection.PatternDetectionOption;
import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.mining.algorithm.BaseAction;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * The `DetectionActionExecutor` class is responsible for executing the pattern detection process
 * within the role analysis. It processes a specific cluster and identifies patterns based on
 * the configured detection options and the session details.
 * <p>
 * This class is a crucial part of the role analysis workflow, helping to identify patterns within
 * the analyzed data for better decision-making regarding role and user assignments.
 */
public class DetectionActionExecutor extends BaseAction {

    private final DetectionOperation detectionType;
    private final RoleAnalysisProgressIncrement handler = new RoleAnalysisProgressIncrement("Pattern Detection: "
            + "DetectionActionExecutor", 6, this::incrementProgress);
    private final String clusterOid;

    /** BEWARE! Do not create subresults from this value. Just to avoid confusion. */
    private final OperationResult result;
    private final Task task;
    private final RoleAnalysisService roleAnalysisService;
    private static final Trace LOGGER = TraceManager.getTrace(DetectionActionExecutor.class);

    public DetectionActionExecutor(
            @NotNull AbstractActivityRun<?, ?, ?> activityRun,
            @NotNull String clusterOid,
            @NotNull OperationResult result) {
        super(activityRun);
        this.detectionType = new PatternResolver();
        this.clusterOid = clusterOid;
        this.result = result;
        this.task = activityRun.getRunningTask();
        this.roleAnalysisService = ModelBeans.get().roleAnalysisService;
    }

    /**
     * Executes the pattern detection process within the role analysis for a specific cluster.
     * This method retrieves cluster and session information, prepares data, and performs pattern detection.
     */
    public void executeDetectionProcess() {
        handler.enterNewStep("Load Data");
        handler.setActive(true);
        handler.setOperationCountToProcess(1);
        PrismObject<RoleAnalysisClusterType> clusterPrismObject = roleAnalysisService
                .getClusterTypeObject(clusterOid, task, result);
        if (clusterPrismObject == null) {
            LOGGER.error("Failed to resolve RoleAnalysisClusterType from UUID: {}", clusterOid);
            return;
        }

        RoleAnalysisClusterType cluster = clusterPrismObject.asObjectable();

        ObjectReferenceType roleAnalysisSessionRef = cluster.getRoleAnalysisSessionRef();

        String sessionOid = roleAnalysisSessionRef.getOid();
        PrismObject<RoleAnalysisSessionType> sessionTypeObject = roleAnalysisService
                .getSessionTypeObject(sessionOid, task, result);

        if (sessionTypeObject == null) {
            LOGGER.error("Failed to resolve RoleAnalysisSessionType from UUID: {}", sessionOid);
            return;
        }
        RoleAnalysisSessionType session = sessionTypeObject.asObjectable();
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();

        SearchFilterType userSearchFilter = null;
        if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            RoleAnalysisSessionOptionType roleModeOptions = session.getRoleModeOptions();
            if (roleModeOptions != null) {
                userSearchFilter = roleModeOptions.getUserSearchFilter();
            }
        } else if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
            UserAnalysisSessionOptionType userModeOptions = session.getUserModeOptions();
            if (userModeOptions != null) {
                userSearchFilter = userModeOptions.getUserSearchFilter();
            }
        }

        MiningOperationChunk miningOperationChunk = roleAnalysisService.prepareCompressedMiningStructure(cluster, userSearchFilter,
                true, processMode, result, task);

        List<MiningRoleTypeChunk> miningRoleTypeChunks = miningOperationChunk.getMiningRoleTypeChunks(RoleAnalysisSortMode.NONE);
        List<MiningUserTypeChunk> miningUserTypeChunks = miningOperationChunk.getMiningUserTypeChunks(RoleAnalysisSortMode.NONE);
        handler.iterateActualStatus();

        PatternDetectionOption detectionOption = new PatternDetectionOption(cluster);
        List<DetectedPattern> detectedPatterns = executeDetection(miningRoleTypeChunks, miningUserTypeChunks,
                processMode, detectionOption);

        if (detectedPatterns != null && !detectedPatterns.isEmpty()) {
            detectedPatterns = loadTopPatterns(detectedPatterns);
            roleAnalysisService.anylseAttributesAndReplaceDetectionPattern(clusterOid,
                    detectedPatterns, task, result
            );
        }

    }

    private List<DetectedPattern> executeDetection(@NotNull List<MiningRoleTypeChunk> miningRoleTypeChunks,
            @NotNull List<MiningUserTypeChunk> miningUserTypeChunks,
            @NotNull RoleAnalysisProcessModeType mode,
            @NotNull PatternDetectionOption detectionOption) {
        if (mode.equals(RoleAnalysisProcessModeType.USER)) {
            return detectionType.performDetection(mode, miningRoleTypeChunks, detectionOption, handler);
        } else if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return detectionType.performDetection(mode, miningUserTypeChunks, detectionOption, handler);
        }
        return null;
    }

    public RoleAnalysisProgressIncrement getHandler() {
        return handler;
    }

}
