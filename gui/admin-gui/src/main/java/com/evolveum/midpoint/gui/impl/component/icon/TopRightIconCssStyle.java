/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.icon;

/**
 * Created by honchar
 */
public class TopRightIconCssStyle implements LayeredIconCssStyle {

    @Override
    public String getBasicCssClass() {
        return "icon-basic-transparent";
    }

    @Override
    public String getBasicLayerCssClass() {
        return "icon-basic-layer";
    }

    @Override
    public String getLayerCssClass() {
        return "top-right-layer";
    }

    @Override
    public String getStrokeLayerCssClass() {
        return "icon-stroke-layer";
    }

}

