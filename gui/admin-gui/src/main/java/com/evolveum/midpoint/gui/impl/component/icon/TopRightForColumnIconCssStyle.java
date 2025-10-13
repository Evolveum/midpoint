/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.icon;

/**
 * @author skublik
 */
public class TopRightForColumnIconCssStyle implements LayeredIconCssStyle {

    @Override
    public String getBasicCssClass() {
        return "icon-basic-transparent-for-column";
    }

    @Override
    public String getBasicLayerCssClass() {
        return "icon-basic-layer-for-column";
    }

    @Override
    public String getLayerCssClass() {
        return "top-right-layer-for-column";
    }

    @Override
    public String getStrokeLayerCssClass() {
        return "icon-stroke-layer";
    }

}

