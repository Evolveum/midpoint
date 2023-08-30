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
import com.evolveum.midpoint.common.mining.objects.handler.Handler;

import java.util.List;



public interface DetectionOperation {

    List<DetectedPattern> performUserBasedDetection(List<MiningRoleTypeChunk> miningRoleTypeChunks,
            DetectionOption roleAnalysisDetectionOptionType, Handler handler);

    List<DetectedPattern> performRoleBasedDetection(List<MiningUserTypeChunk> miningRoleTypeChunks,
            DetectionOption roleAnalysisDetectionOptionType, Handler handler);

}
