/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile.mining.migration;

import java.io.Serializable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.resolveDateAndTime;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.getRoleAssignmentCount;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.getRoleInducementsCount;

public class RoleAnalysisMigratedRoleTileModel<T extends Serializable> extends Tile<T> {

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

    public RoleAnalysisMigratedRoleTileModel(String icon, String title) {
        super(icon, title);
    }

    public RoleAnalysisMigratedRoleTileModel(
            @NotNull RoleType role,
            @NotNull PageBase pageBase,
            @NotNull IModel<ObjectReferenceType> clusterRef,
            @NotNull IModel<ObjectReferenceType> sessionOid) {
        this.icon = GuiStyleConstants.CLASS_CANDIDATE_ROLE_ICON;
        this.clusterRef = clusterRef.getObject();
        this.sessionRef = sessionOid.getObject();
        this.name = role.getName().getOrig();
        this.role = role;
        this.pageBase = pageBase;
        this.createDate = resolveDateAndTime(role);
        this.inducementsCount = getRoleInducementsCount(role);
        this.membersCount = getRoleAssignmentCount(role, pageBase);
        this.status = resolveStatus(role).name();
    }

    private Enum<ActivationStatusType> resolveStatus(@NotNull RoleType role) {
        return role.getActivation().getEffectiveStatus();
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

}
