/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.detection;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.objects.detection.PatternDetectionOption;
import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

import org.jetbrains.annotations.NotNull;

/**
 * A class responsible for executing the detection of patterns in role and user mining chunks/role analysis process.
 * The specific detection operation is determined by the provided detection option.
 * Default detection action is used after clustering operation.
 */
public class DefaultDetectionAction implements Serializable {
    private final DetectionOperation detectionType;
    private final RoleAnalysisProgressIncrement handler = new RoleAnalysisProgressIncrement("Pattern Detection: "
            + "DefaultDetectionAction", 6);
    private final PatternDetectionOption detectionOption;

    /**
     * Constructs a DefaultDetectionAction with the specified detection option.
     *
     * @param detectionOption The detection option that defines the specific detection operation.
     */
    public DefaultDetectionAction(@NotNull PatternDetectionOption detectionOption) {
        this.detectionOption = detectionOption;
        detectionType = new PatternResolver();
    }

    /**
     * Executes the pattern detection operation on role or user mining chunks based on the provided mode.
     *
     * @param miningRoleTypeChunks The list of role mining chunks.
     * @param miningUserTypeChunks The list of user mining chunks.
     * @param mode The mode specifying whether the operation is user-based or role-based.
     * @return A list of detected patterns resulting from the detection operation.
     */
    protected List<DetectedPattern> executeDetection(@NotNull List<MiningRoleTypeChunk> miningRoleTypeChunks,
            @NotNull List<MiningUserTypeChunk> miningUserTypeChunks,
            @NotNull RoleAnalysisProcessModeType mode) {
        if (mode.equals(RoleAnalysisProcessModeType.USER)) {
            return detectionType.performDetection(mode, miningRoleTypeChunks, detectionOption, handler);
        } else if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return detectionType.performDetection(mode, miningUserTypeChunks, detectionOption, handler);
        }

        return null;
    }
}
