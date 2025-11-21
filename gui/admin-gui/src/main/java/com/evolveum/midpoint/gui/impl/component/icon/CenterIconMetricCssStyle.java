/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.icon;

public class CenterIconMetricCssStyle implements LayeredIconCssStyle {

    @Override
    public String getBasicCssClass() {
        return "icon-basic-transparent";
    }

    @Override
    public String getBasicLayerCssClass() {
        return "icon-metric-layer";
    }

    @Override
    public String getLayerCssClass() {
        return "center-layer";
    }

    @Override
    public String getStrokeLayerCssClass() {
        return "center-icon-stroke-layer";
    }

}
