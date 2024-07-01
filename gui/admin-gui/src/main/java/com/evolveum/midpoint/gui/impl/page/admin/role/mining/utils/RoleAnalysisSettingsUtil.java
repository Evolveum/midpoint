/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

public class RoleAnalysisSettingsUtil {

    public static String getRoleAnalysisMode(RoleAnalysisOptionType analysisOption) {
        RoleAnalysisCategoryType analysisCategory = analysisOption.getAnalysisCategory();
        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();

        String analysisCategoryAsString = analysisCategory == null ? null : analysisCategory.value();
        String capitalizedAnalysisCategory = analysisCategoryAsString == null ? "" :
                Character.toUpperCase(analysisCategoryAsString.charAt(0))
                        + analysisCategoryAsString.substring(1);

        return Character.
                toUpperCase(processMode.value().charAt(0))
                + processMode.value().substring(1)
                + "/"
                + capitalizedAnalysisCategory;
    }
}
