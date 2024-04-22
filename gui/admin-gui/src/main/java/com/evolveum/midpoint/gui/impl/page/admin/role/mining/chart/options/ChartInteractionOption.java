/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.chart.options;

import java.io.Serializable;

//TODO move to chartjs then remove
public class ChartInteractionOption implements Serializable {

    private String mode;
    private Boolean intersect = true;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Boolean getIntersect() {
        return intersect;
    }

    public void setIntersect(Boolean intersect) {
        this.intersect = intersect;
    }
}
