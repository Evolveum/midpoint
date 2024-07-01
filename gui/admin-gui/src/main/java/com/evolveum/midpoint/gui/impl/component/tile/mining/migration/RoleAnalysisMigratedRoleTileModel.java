/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile.mining.migration;

import java.io.Serializable;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

public class RoleAnalysisMigratedRoleTileModel<T extends Serializable> extends Tile<T> {

    String icon;
    String name;
    String createDate;
    String inducementsCount;
    String membersCount;
    String processMode;
    PageBase pageBase;
    String status;
    RoleType role;
    String clusterOid;

    public RoleAnalysisMigratedRoleTileModel(String icon, String title) {
        super(icon, title);
    }

    public RoleAnalysisMigratedRoleTileModel(
            @NotNull RoleType role,
            @NotNull PageBase pageBase,
            @NotNull String processMode,
            @NotNull String clusterOid) {
        this.icon = GuiStyleConstants.CLASS_CANDIDATE_ROLE_ICON;
        this.clusterOid = clusterOid;
        this.name = role.getName().getOrig();
        this.role = role;
        this.pageBase = pageBase;
        this.createDate = resolveDateAndTime(role);
        this.inducementsCount = getRoleInducementsCount(role);
        this.membersCount = getRoleAssignmentCount(role, pageBase);
        this.processMode = processMode;
        this.status = resolveStatus(role).name();
    }

    private Enum<ActivationStatusType> resolveStatus(@NotNull RoleType role) {
        return role.getActivation().getEffectiveStatus();
    }

    private String getRoleAssignmentCount(@NotNull RoleType role, @NotNull PageBase pageBase) {
        Task task = pageBase.createSimpleTask("countRoleMembers");
        OperationResult result = task.getResult();

        Integer membersCount = pageBase.getRoleAnalysisService()
                .countUserTypeMembers(null, role.getOid(),
                        task, result);
        return String.valueOf(membersCount);
    }

    private @NotNull String getRoleInducementsCount(@NotNull RoleType role) {
        return String.valueOf(role.getInducement().size());
    }

    private @NotNull String resolveDateAndTime(@NotNull RoleType role) {

        if(role.getMetadata() == null || role.getMetadata().getCreateTimestamp() == null) {
            return "";
        }

        XMLGregorianCalendar createTimestamp = role.getMetadata().getCreateTimestamp();
        int year = createTimestamp.getYear();
        int month = createTimestamp.getMonth();
        int day = createTimestamp.getDay();
        int hours = createTimestamp.getHour();
        int minutes = createTimestamp.getMinute();

        String dateString = String.format("%04d:%02d:%02d", year, month, day);

        String amPm = (hours < 12) ? "AM" : "PM";
        hours = hours % 12;
        if (hours == 0) {
            hours = 12;
        }
        String timeString = String.format("%02d:%02d %s", hours, minutes, amPm);

        return dateString + ", " + timeString;
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

    public String getProcessMode() {
        return processMode;
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

    public String getClusterOid() {
        return clusterOid;
    }

}
