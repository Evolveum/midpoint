/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.icon;

/**
 * Created by honchar
 */
public class CenterWithRightShiftCssStyle implements LayeredIconCssStyle {

    @Override
    public String getBasicCssClass() {
        return "icon-basic-transparent";
    }

    @Override
    public String getBasicLayerCssClass() {
        return "center-with-right-shift-layer";
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

