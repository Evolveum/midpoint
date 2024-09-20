/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile.mining.candidate;

import java.io.Serializable;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.DisplayForLifecycleState;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisCandidateRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.resolveDateAndTime;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.getRoleAssignmentCount;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.getRoleInducementsCount;

public class RoleAnalysisCandidateTileModel<T extends Serializable> extends Tile<T> {

    String icon;
    String name;
    String createDate;
    String inducementsCount;
    String membersCount;
    PageBase pageBase;
    String status;
    RoleType role;
    ObjectReferenceType clusterRef;
    ObjectReferenceType sessionRef;
    RoleAnalysisCandidateRoleType candidateRole;
    Long id;

    public RoleAnalysisCandidateTileModel(String icon, String title) {
        super(icon, title);
    }

    public RoleAnalysisCandidateTileModel(
            @NotNull RoleType role,
            @NotNull PageBase pageBase,
            @NotNull ObjectReferenceType clusterRef,
            @NotNull ObjectReferenceType sessionOid,
            @NotNull RoleAnalysisCandidateRoleType candidateRole) {
        this.icon = GuiStyleConstants.CLASS_CANDIDATE_ROLE_ICON;
        this.name = role.getName().getOrig();
        this.clusterRef = clusterRef;
        this.sessionRef = sessionOid;
        this.role = role;
        this.pageBase = pageBase;
        this.createDate = resolveDateAndTime(role);
        this.inducementsCount = getRoleInducementsCount(role);
        this.membersCount = getRoleAssignmentCount(role, pageBase);
        this.status = resolveStatus(role);
        this.candidateRole = candidateRole;
        this.id = candidateRole.getId();
    }

    private @NotNull String resolveStatus(@NotNull RoleType role) {
        String lifecycleState = role.getLifecycleState();
        if (lifecycleState == null) {
            lifecycleState = DisplayForLifecycleState.ACTIVE.name();
        }
        return lifecycleState;
    }

    @Override
    public String getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }

    public String getCreateDate() {
        return createDate;
    }

    public String getInducementsCount() {
        return inducementsCount;
    }

    public String getMembersCount() {
        return membersCount;
    }

    public PageBase getPageBase() {
        return pageBase;
    }

    public String getStatus() {
        return status;
    }

    public RoleType getRole() {
        return role;
    }

    public ObjectReferenceType getClusterRef() {
        return clusterRef;
    }

    public ObjectReferenceType getSessionRef() {
        return sessionRef;
    }

    public Long getId() {
        return id;
    }

    public RoleAnalysisCandidateRoleType getCandidateRole() {
        return candidateRole;
    }
}
