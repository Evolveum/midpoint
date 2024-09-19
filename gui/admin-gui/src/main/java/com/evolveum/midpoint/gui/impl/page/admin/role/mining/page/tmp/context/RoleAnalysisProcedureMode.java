/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.context;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcedureType;

public enum RoleAnalysisProcedureMode implements TileEnum {

    ROLE_MINING("fa fa-cubes",
            "RoleAnalysisCategoryType.ROLE_MINING.description"),
    OUTLIER_DETECTION("fa fa-search",
            "RoleAnalysisCategoryType.OUTLIER_DETECTION.description");

    private final String iconClass;
    private final String descriptionKey;

    RoleAnalysisProcedureMode(String iconClass, String descriptionKey) {
        this.iconClass = iconClass;
        this.descriptionKey = descriptionKey;
    }

    @Override
    public String getIcon() {
        return iconClass;
    }

    public String getDescriptionKey() {
        return descriptionKey;
    }

    public RoleAnalysisProcedureType resolveCategoryMode() {
        return switch (this) {
            case ROLE_MINING -> RoleAnalysisProcedureType.ROLE_MINING;
            case OUTLIER_DETECTION -> RoleAnalysisProcedureType.OUTLIER_DETECTION;
        };
    }
}
