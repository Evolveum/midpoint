/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier;

import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.BasicOutlierDetectionUtils.resolveRoleModeOutliers;
import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.BasicOutlierDetectionUtils.resolveUserModeOutliers;
import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.ClusteringOutlierDetectionUtils.analyseOutlierClusterMembers;
import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutliersDetectionUtil.prepareDetectionOptions;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;


//TODO
public class OutliersDetectionExecutionUtil {

    public static @NotNull Collection<RoleAnalysisOutlierType> executeBasicOutlierDetection(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull RoleAnalysisSessionType session,
            @NotNull RoleAnalysisOptionType analysisOption,
            @NotNull Task task) {
        //TODO replace result
        OperationResult result = new OperationResult("executeOutliersAnalysis");
        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();

        MiningOperationChunk miningOperationChunk = roleAnalysisService.prepareCompressedMiningStructure(cluster, true,
                processMode, result, task);

        HashMap<String, RoleAnalysisOutlierType> map = new HashMap<>();

        ObjectReferenceType clusterRef = new ObjectReferenceType()
                .oid(cluster.getOid())
                .type(RoleAnalysisClusterType.COMPLEX_TYPE);

        ObjectReferenceType sessionRef = new ObjectReferenceType()
                .oid(session.getOid())
                .type(RoleAnalysisSessionType.COMPLEX_TYPE);

        RangeType frequencyRange = session.getDefaultDetectionOption().getFrequencyRange();
        Double sensitivity = session.getDefaultDetectionOption().getSensitivity();
        UserAnalysisSessionOptionType userModeOptions = session.getUserModeOptions();
        Double similarityThreshold = userModeOptions.getSimilarityThreshold();

        //TODO role mode
        if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
            resolveUserModeOutliers(roleAnalysisService,
                    cluster,
                    session,
                    task,
                    miningOperationChunk,
                    frequencyRange,
                    sensitivity,
                    clusterRef,
                    sessionRef,
                    result,
                    map,
                    similarityThreshold);
        } else if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            resolveRoleModeOutliers(roleAnalysisService,
                    miningOperationChunk,
                    frequencyRange,
                    sensitivity,
                    clusterRef,
                    sessionRef,
                    map);
        }

        return map.values();
    }

    public static @NotNull Collection<RoleAnalysisOutlierType> executeClusteringOutliersDetection(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull RoleAnalysisSessionType session,
            @NotNull Task task) {

        //TODO replace result
        OperationResult result = new OperationResult("executeOuterOutliersAnalysis");
        UserAnalysisSessionOptionType userModeOptions = session.getUserModeOptions();
        Integer minMembersCount = userModeOptions.getMinMembersCount();
        HashMap<String, RoleAnalysisOutlierType> map = new HashMap<>();
        RoleAnalysisDetectionOptionType detectionOption = prepareDetectionOptions(session);

        List<ObjectReferenceType> member = cluster.getMember();
        List<String> outliersMembers = member.stream().map(AbstractReferencable::getOid).collect(Collectors.toList());

        List<RoleAnalysisAttributeDef> userAnalysisAttributeDef = roleAnalysisService.resolveAnalysisAttributes(
                session, UserType.COMPLEX_TYPE);
        List<RoleAnalysisAttributeDef> roleAnalysisAttributeDef = roleAnalysisService.resolveAnalysisAttributes(
                session, RoleType.COMPLEX_TYPE);

        RangeType propertiesRange = userModeOptions.getPropertiesRange();
        ListMultimap<List<String>, String> chunkMap = roleAnalysisService.loadUserForOutlierComparison(
                roleAnalysisService, outliersMembers, propertiesRange.getMin().intValue(), propertiesRange.getMax().intValue(),
                userModeOptions.getQuery(), result, task);
        double minThreshold = 0.5;

        analyseOutlierClusterMembers(roleAnalysisService,
                session,
                cluster,
                task,
                member,
                result,
                minThreshold,
                chunkMap,
                outliersMembers,
                minMembersCount,
                userAnalysisAttributeDef,
                roleAnalysisAttributeDef,
                detectionOption,
                map);

        return map.values();
    }

}
