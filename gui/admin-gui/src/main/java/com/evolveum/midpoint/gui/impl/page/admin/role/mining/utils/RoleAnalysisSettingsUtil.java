/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcedureType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class RoleAnalysisSettingsUtil {

    private RoleAnalysisSettingsUtil() {
    }

    public static @NotNull String getRoleAnalysisMode(@NotNull RoleAnalysisOptionType analysisOption) {
        RoleAnalysisCategoryType analysisCategory = analysisOption.getAnalysisCategory();
        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();

        return Character.
                toUpperCase(processMode.value().charAt(0))
                + processMode.value().substring(1)
                + "/"
                + getAnalysisCategoryTitle(analysisCategory);
    }

    public static @NotNull String getRoleAnalysisTypeMode(@NotNull RoleAnalysisOptionType analysisOption) {
        RoleAnalysisProcedureType procedureType = analysisOption.getAnalysisProcedureType();

        if (procedureType == null) {
            return "N/A";
        }

        if (procedureType == RoleAnalysisProcedureType.ROLE_MINING) {
            return "Role mining";
        } else {
            return "Outlier detection";
        }
    }

    @Contract(pure = true)
    public static @NotNull String getAnalysisCategoryTitle(RoleAnalysisCategoryType category) {
        if (category == null) {
            return "N/A";
        }

        return switch (category) {
            case OUTLIERS_DEPARTMENT, DEPARTMENT -> "department";
            case ATTRIBUTE_BASED -> "attribute";
            case BALANCED -> "balanced";
            case EXACT -> "exact";
            case EXPLORATION -> "exploration";
            case ADVANCED -> "advanced";
            case BIRTHRIGHT -> "birthright";
        };
    }
}
