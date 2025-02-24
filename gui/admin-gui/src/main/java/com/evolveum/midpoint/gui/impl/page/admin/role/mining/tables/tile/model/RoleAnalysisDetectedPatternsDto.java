/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.model;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;

public class RoleAnalysisDetectedPatternsDto implements Serializable {

    List<DetectedPattern> detectedPatterns;
    int totalRoleToUserAssignments;

    public RoleAnalysisDetectedPatternsDto(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull OperationResult result) {
        initAllRoleSuggestions(roleAnalysisService, result);
        initTotalRoleToUserAssignments(roleAnalysisService, result);
    }

    public RoleAnalysisDetectedPatternsDto(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull OperationResult result) {
        initClusterRoleSuggestions(roleAnalysisService, cluster, result);
        initTotalRoleToUserAssignments(roleAnalysisService, result);
    }

    public RoleAnalysisDetectedPatternsDto(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisSessionType session,
            @NotNull OperationResult result) {
        initSessionRoleSuggestions(roleAnalysisService, session, result);
        initTotalRoleToUserAssignments(roleAnalysisService, result);
    }

    private void initSessionRoleSuggestions(@NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisSessionType session,
            @NotNull OperationResult result) {
        this.detectedPatterns = roleAnalysisService.getSessionRoleSuggestion(session.getOid(), null, true, result);
    }

    private void initClusterRoleSuggestions(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull OperationResult result) {
        this.detectedPatterns = roleAnalysisService.getClusterRoleSuggestions(cluster.getOid(), null, true, result);
    }

    private void initAllRoleSuggestions(@NotNull RoleAnalysisService roleAnalysisService, OperationResult result) {
        this.detectedPatterns = roleAnalysisService.getAllRoleSuggestions(null, true, result);
    }

    private void initTotalRoleToUserAssignments(@NotNull RoleAnalysisService roleAnalysisService, OperationResult result) {
        this.totalRoleToUserAssignments = roleAnalysisService.countUserOwnedRoleAssignment(result);
    }

    public List<DetectedPattern> getDetectedPatterns() {
        return detectedPatterns;
    }

    public int getTotalRoleToUserAssignments() {
        return totalRoleToUserAssignments;
    }
}
