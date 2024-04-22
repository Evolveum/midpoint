/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.chart.options;

import java.io.Serializable;

//TODO move to chartjs then remove
public class ChartScaleOption implements Serializable {

    private ChartScaleAxisOption x;
    private ChartScaleAxisOption y;

    public ChartScaleAxisOption getX() {
        return x;
    }

    public void setX(ChartScaleAxisOption x) {
        this.x = x;
    }

    public ChartScaleAxisOption getY() {
        return y;
    }

    public void setY(ChartScaleAxisOption y) {
        this.y = y;
    }

}
