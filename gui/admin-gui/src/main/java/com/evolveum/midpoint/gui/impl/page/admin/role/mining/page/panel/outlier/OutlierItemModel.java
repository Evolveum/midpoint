/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
