/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.detection;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.*;
import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object.DetectionOption;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.Handler;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningOperationChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningRoleTypeChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningUserTypeChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.chunk.PrepareChunkStructure;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

public class DetectionActionExecutorNew implements Serializable {
    private final DetectionOperation detectionType;
    Handler handler;
    String clusterOid;
    PageBase pageBase;
    OperationResult result;

    public DetectionActionExecutorNew(String clusterOid, PageBase pageBase, OperationResult result) {
        this.detectionType = new PatternResolver();
        this.clusterOid = clusterOid;
        this.pageBase = pageBase;
        this.result = result;
        this.handler = new Handler("Pattern Detection", 6);
    }


    public void executeDetectionProcess() {
        handler.setSubTitle("Load Data");
        handler.setActive(true);
        handler.setOperationCountToProcess(1);
        PrismObject<RoleAnalysisClusterType> clusterPrismObject = getClusterTypeObject(pageBase, result, clusterOid);
        if (clusterPrismObject == null) {
            LOGGER.error("Failed to resolve RoleAnalysisClusterType from UUID: {}", clusterOid);
            return;
        }

        RoleAnalysisClusterType cluster = clusterPrismObject.asObjectable();

        ObjectReferenceType roleAnalysisSessionRef = cluster.getRoleAnalysisSessionRef();

        String sessionOid = roleAnalysisSessionRef.getOid();
        PrismObject<RoleAnalysisSessionType> sessionTypeObject = getSessionTypeObject(pageBase, result,
                sessionOid);

        if (sessionTypeObject == null) {
            LOGGER.error("Failed to resolve RoleAnalysisSessionType from UUID: {}", sessionOid);
            return;
        }

        RoleAnalysisProcessModeType processMode = sessionTypeObject.asObjectable().getProcessMode();

        MiningOperationChunk miningOperationChunk = new PrepareChunkStructure().executeOperation(cluster, true,
                processMode, pageBase, result);

        List<MiningRoleTypeChunk> miningRoleTypeChunks = miningOperationChunk.getMiningRoleTypeChunks(SORT.NONE);
        List<MiningUserTypeChunk> miningUserTypeChunks = miningOperationChunk.getMiningUserTypeChunks(SORT.NONE);
        handler.iterateActualStatus();

        DetectionOption detectionOption = new DetectionOption(cluster);
        List<DetectedPattern> detectedPatterns = executeDetection(miningRoleTypeChunks, miningUserTypeChunks,
                processMode, detectionOption);

        replaceRoleAnalysisClusterDetectionPattern(clusterOid, pageBase,
                result,
                detectedPatterns,
                processMode);
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
