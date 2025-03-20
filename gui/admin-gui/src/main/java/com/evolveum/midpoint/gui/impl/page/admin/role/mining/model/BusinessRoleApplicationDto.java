/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.model;

import java.io.Serializable;
import java.util.Set;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * The BusinessRoleApplicationDto class represents a Data Transfer Object (DTO) that holds
 * information about a specific role, its associated cluster, and a list of BusinessRoleDtos that holds information
 * about a user's delta to a specific role.
 */
//TODO optimize migration and candidate role generation
public class BusinessRoleApplicationDto implements Serializable {

    PrismObject<RoleAnalysisClusterType> cluster;
    PrismObject<RoleType> businessRole;
    boolean isCandidate = false;
    Long patternId;

    transient Set<ObjectReferenceType> userMembers;
    transient Set<ObjectReferenceType> roleInducements;

    public BusinessRoleApplicationDto(
            @NotNull PrismObject<RoleAnalysisClusterType> cluster,
            @NotNull PrismObject<RoleType> businessRole,
            @NotNull Set<ObjectReferenceType> userMembers,
            @NotNull Set<ObjectReferenceType> roleInducements) {
        setDraft(businessRole);
        this.cluster = cluster;
        this.businessRole = businessRole;
        this.userMembers = userMembers;
        this.roleInducements = roleInducements;
    }

    private static void setDraft(@NotNull PrismObject<RoleType> businessRole) {
        RoleType role = businessRole.asObjectable();
        role.setLifecycleState(SchemaConstants.LIFECYCLE_DRAFT);
    }

    public PrismObject<RoleAnalysisClusterType> getCluster() {
        return cluster;
    }

    public void setCluster(PrismObject<RoleAnalysisClusterType> cluster) {
        this.cluster = cluster;
    }

    public PrismObject<RoleType> getBusinessRole() {
        return businessRole;
    }

    public Set<ObjectReferenceType> getUserMembers() {
        return userMembers;
    }

    public void setPatternId(Long patternId) {
        this.patternId = patternId;
    }

    public boolean isCandidate() {
        return isCandidate;
    }

    public void setCandidate(boolean candidate) {
        isCandidate = candidate;
    }

    public Set<ObjectReferenceType> getRoleInducements() {
        return roleInducements;
    }


}
