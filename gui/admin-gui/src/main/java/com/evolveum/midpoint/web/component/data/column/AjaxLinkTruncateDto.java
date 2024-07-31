/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;

import java.io.Serializable;

public class AjaxLinkTruncateDto implements Serializable {

    public static final String F_NAME = "name";
    public static final String F_ICON = "icon";
    public static final String F_MODE = "mode";

    private String name;
    private CompositedIcon icon;
    private RoleAnalysisOperationMode mode;

   public AjaxLinkTruncateDto(String name, CompositedIcon icon, RoleAnalysisOperationMode mode) {
        this.name = name;
        this.icon = icon;
        this.mode = mode;
    }

    public String getName() {
        return name;
    }

    public CompositedIcon getIcon() {
        return icon;
    }

    public RoleAnalysisOperationMode getMode() {
        return mode;
   }
}
