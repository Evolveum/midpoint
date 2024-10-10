/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.outline;

import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutliersDetectionUtil.prepareDetectionOptions;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class OutlierDetectionOutlineModel {

    RoleAnalysisSessionType session;
    RoleAnalysisClusterType cluster;
    ObjectReferenceType clusterRef;
    ObjectReferenceType sessionRef;

    RoleAnalysisOptionType analysisOption;
    RangeType frequencyRange;
    Double sensitivity;
    double similarityThreshold;

    List<RoleAnalysisAttributeDef> userAnalysisAttributeDef;
    List<RoleAnalysisAttributeDef> roleAnalysisAttributeDef;
    List<String> outliersClusterMembers;
    ListMultimap<List<String>, String> chunkMap;
    RoleAnalysisDetectionOptionType detectionOption;
    Integer minMembersCount;

    int userCountInRepo;

    public OutlierDetectionOutlineModel(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisSessionType session,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull Task task,
            @NotNull OperationResult result) {
        UserAnalysisSessionOptionType userModeOptions = session.getUserModeOptions();
        this.minMembersCount = userModeOptions.getMinMembersCount();
        this.detectionOption = prepareDetectionOptions(session);

        List<ObjectReferenceType> member = cluster.getMember();
        List<String> list = new ArrayList<>();
        for (ObjectReferenceType objectReferenceType : member) {
            String oid = objectReferenceType.getOid();
            list.add(oid);
        }
        this.outliersClusterMembers = list;

        this.userAnalysisAttributeDef = roleAnalysisService.resolveAnalysisAttributes(
                session, UserType.COMPLEX_TYPE);
        this.roleAnalysisAttributeDef = roleAnalysisService.resolveAnalysisAttributes(
                session, RoleType.COMPLEX_TYPE);

        this.chunkMap = roleAnalysisService.loadUserForOutlierComparison(roleAnalysisService, outliersClusterMembers,
                userModeOptions.getUserSearchFilter(), result, task);
        this.analysisOption = session.getAnalysisOption();
        this.session = session;
        this.cluster = cluster;
        this.clusterRef = new ObjectReferenceType()
                .oid(cluster.getOid())
                .type(RoleAnalysisClusterType.COMPLEX_TYPE)
                .targetName(cluster.getName());
        this.sessionRef = new ObjectReferenceType()
                .oid(session.getOid())
                .type(RoleAnalysisSessionType.COMPLEX_TYPE)
                .targetName(session.getName());

        this.userCountInRepo = roleAnalysisService.countObjects(UserType.class, null, null, task, result);

    }

    public List<RoleAnalysisAttributeDef> getUserAnalysisAttributeDef() {
        return userAnalysisAttributeDef;
    }

    public List<RoleAnalysisAttributeDef> getRoleAnalysisAttributeDef() {
        return roleAnalysisAttributeDef;
    }

    public List<String> getOutliersClusterMembers() {
        return outliersClusterMembers;
    }

    public ListMultimap<List<String>, String> getChunkMap() {
        return chunkMap;
    }

    public RoleAnalysisDetectionOptionType getDetectionOption() {
        return detectionOption;
    }

    public RoleAnalysisProcessModeType getProcessMode() {
        return analysisOption.getProcessMode();
    }

    public RangeType getFrequencyRange() {
        return frequencyRange;
    }

    public Double getSensitivity() {
        return sensitivity;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public RoleAnalysisSessionType getSession() {
        return session;
    }

    public RoleAnalysisClusterType getCluster() {
        return cluster;
    }

    public ObjectReferenceType getClusterRef() {
        return clusterRef;
    }

    public ObjectReferenceType getSessionRef() {
        return sessionRef;
    }

    public RoleAnalysisOptionType getAnalysisOption() {
        return analysisOption;
    }

    public int getUserCountInRepo() {
        return userCountInRepo;
    }

}
