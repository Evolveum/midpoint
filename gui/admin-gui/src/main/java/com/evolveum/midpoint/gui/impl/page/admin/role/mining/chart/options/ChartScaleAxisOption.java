/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.chart.options;

import java.io.Serializable;

//TODO move to chartjs then remove
public class ChartScaleAxisOption implements Serializable {

    Boolean display;
    Boolean stacked;
    ChartTitleOption title;

    public Boolean getStacked() {
        return stacked;
    }

    public void setStacked(Boolean stacked) {
        this.stacked = stacked;
    }

    public Boolean getDisplay() {
        return display;
    }

    public void setDisplay(Boolean display) {
        this.display = display;
    }

    public ChartTitleOption getTitle() {
        return title;
    }

    public void setTitle(ChartTitleOption title) {
        this.title = title;
    }
}
