/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier;

import java.io.Serializable;

public class OutlierItemModel implements Serializable {
    private final String value;
    private final String valueDescription;
    private final String icon;

    public OutlierItemModel(String value, String valueDescription, String icon) {
        this.value = value;
        this.valueDescription = valueDescription;
        this.icon = icon;
    }

    public String getValue() {
        return value;
    }

    public String getValueDescription() {
        return valueDescription;
    }

    public String getIcon() {
        return icon;
    }
}
