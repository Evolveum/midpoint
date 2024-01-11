/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * The BusinessRoleApplicationDto class represents a Data Transfer Object (DTO) that holds
 * information about a specific role, its associated cluster, and a list of BusinessRoleDtos that holds information
 * about a user's delta to a specific role.
 */
public class BusinessRoleApplicationDto implements Serializable {

    PrismObject<RoleAnalysisClusterType> cluster;
    PrismObject<RoleType> businessRole;
    List<BusinessRoleDto> businessRoleDtos;
    boolean isCandidate = false;
    Long patternId;
    transient Set<RoleType> candidateRoles;

    public BusinessRoleApplicationDto(
            @NotNull PrismObject<RoleAnalysisClusterType> cluster,
            @NotNull PrismObject<RoleType> businessRole,
            @NotNull List<BusinessRoleDto> businessRoleDtos,
            @NotNull Set<RoleType> candidateRoles) {
        setDraft(businessRole);
        this.cluster = cluster;
        this.businessRole = businessRole;
        this.businessRoleDtos = businessRoleDtos;
        this.candidateRoles = candidateRoles;
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

    public void setBusinessRole(PrismObject<RoleType> businessRole) {
        this.businessRole = businessRole;
    }

    public List<BusinessRoleDto> getBusinessRoleDtos() {
        return businessRoleDtos;
    }

    public void setBusinessRoleDtos(List<BusinessRoleDto> businessRoleDtos) {
        this.businessRoleDtos = businessRoleDtos;
    }

    public Long getPatternId() {
        return patternId;
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

    public Set<RoleType> getCandidateRoles() {
        return candidateRoles;
    }

    public void setCandidateRoles(Set<RoleType> candidateRoles, PageBase pageBase) {
        this.candidateRoles = candidateRoles;

        for (BusinessRoleDto businessRoleDto : businessRoleDtos) {
            businessRoleDto.updateValue(new ArrayList<>(candidateRoles), pageBase);
        }

    }

}
