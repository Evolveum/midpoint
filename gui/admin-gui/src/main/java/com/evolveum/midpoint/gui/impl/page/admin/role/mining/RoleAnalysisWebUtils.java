/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import org.jetbrains.annotations.NotNull;

public class RoleAnalysisWebUtils {

    public static final String TITLE_CSS = "title";
    public static final String CLASS_CSS = "class";
    public static final String STYLE_CSS = "style";

    public static final String TEXT_MUTED = "text-muted";
    public static final String TEXT_TRUNCATE = "text-truncate";
    public static final String FONT_WEIGHT_BOLD = "font-weight-bold";

    public static final String PANEL_ID = "panel";

    private RoleAnalysisWebUtils() {
    }

    public static String getRoleAssignmentCount(@NotNull RoleType role, @NotNull PageBase pageBase) {
        Task task = pageBase.createSimpleTask("countRoleMembers");
        OperationResult result = task.getResult();

        Integer roleMembersCount = pageBase.getRoleAnalysisService()
                .countUserTypeMembers(null, role.getOid(),
                        task, result);
        return String.valueOf(roleMembersCount);
    }

    public static @NotNull String getRoleInducementsCount(@NotNull RoleType role) {
        return String.valueOf(role.getInducement().size());
    }
}
