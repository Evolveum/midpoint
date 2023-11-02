/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.detection;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.objects.detection.DetectionOption;
import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;

import java.util.List;

/**
 * The `DetectionOperation` interface defines the operations required for performing pattern detection
 * within the role analysis process. Implementations of this interface are responsible for detecting
 * patterns within the analyzed data based on user-based or role-based detection criteria.
 */
public interface DetectionOperation {


    /**
     * Performs user-based pattern detection using the provided mining role type chunks, detection options,
     * and a progress increment handler.
     *
     * @param miningRoleTypeChunks           The mining role type chunks to analyze.
     * @param roleAnalysisDetectionOptionType The detection options to configure the detection process.
     * @param handler                        The progress increment handler for tracking the detection process.
     * @return A list of detected patterns based on user-based detection criteria.
     */
    List<DetectedPattern> performUserBasedDetection(List<MiningRoleTypeChunk> miningRoleTypeChunks,
            DetectionOption roleAnalysisDetectionOptionType, RoleAnalysisProgressIncrement handler);

    /**
     * Performs role-based pattern detection using the provided mining user type chunks, detection options,
     * and a progress increment handler.
     *
     * @param miningUserTypeChunks           The mining user type chunks to analyze.
     * @param roleAnalysisDetectionOptionType The detection options to configure the detection process.
     * @param handler                        The progress increment handler for tracking the detection process.
     * @return A list of detected patterns based on role-based detection criteria.
     */
    List<DetectedPattern> performRoleBasedDetection(List<MiningUserTypeChunk> miningUserTypeChunks,
            DetectionOption roleAnalysisDetectionOptionType, RoleAnalysisProgressIncrement handler);

}
