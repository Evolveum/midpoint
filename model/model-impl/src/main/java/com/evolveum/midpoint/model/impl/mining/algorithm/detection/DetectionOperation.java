/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.detection;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningBaseTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.objects.detection.PatternDetectionOption;
import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

/**
 * The `DetectionOperation` interface defines the operations required for performing pattern detection
 * within the role analysis process. Implementations of this interface are responsible for detecting
 * patterns within the analyzed data based on user-based or role-based detection criteria.
 */
public interface DetectionOperation {

    /**
     * Performs pattern detection using the provided mining role type chunks, detection options,
     * progress increment handler and process mode.
     *
     * @param processMode The mode specifying whether the process is user-based or role-based.
     * @param miningBaseTypeChunks The mining structure type chunks to analyze.
     * @param detectionOption The detection options to configure the detection process.
     * @param handler The progress increment handler for tracking the detection process.
     * @return A list of detected patterns based on provided detection criteria.
     */
    @NotNull <T extends MiningBaseTypeChunk> List<DetectedPattern> performDetection(
            @NotNull RoleAnalysisProcessModeType processMode,
            @NotNull List<T> miningBaseTypeChunks,
            @NotNull PatternDetectionOption detectionOption,
            @NotNull RoleAnalysisProgressIncrement handler);

}
