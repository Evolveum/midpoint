/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.detection;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object.DetectionOption;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.Handler;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningRoleTypeChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningUserTypeChunk;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

public class DetectionActionExecutor implements Serializable {
    private final DetectionOperation detectionType;

    Handler handler = new Handler("Pattern Detection", 6);

    DetectionOption detectionOption;

    public DetectionActionExecutor(DetectionOption detectionOption) {
        this.detectionOption = detectionOption;
        detectionType = new PatternResolver();
    }

    public List<DetectedPattern> executeDetection(List<MiningRoleTypeChunk> miningRoleTypeChunks,
            List<MiningUserTypeChunk> miningUserTypeChunks, RoleAnalysisProcessModeType mode) {
        if (mode.equals(RoleAnalysisProcessModeType.USER)) {
            return detectionType.performUserBasedDetection(miningRoleTypeChunks, detectionOption, handler);
        } else if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return detectionType.performRoleBasedDetection(miningUserTypeChunks, detectionOption, handler);
        }
        return null;
    }
}
