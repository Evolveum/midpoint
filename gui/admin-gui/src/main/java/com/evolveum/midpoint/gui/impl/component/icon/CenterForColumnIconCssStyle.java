/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.icon;

/**
 * @author skublik
 */
public class CenterForColumnIconCssStyle implements LayeredIconCssStyle {

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
        return "center-layer-for-column";
    }

    @Override
    public String getStrokeLayerCssClass() {
        return "center-icon-stroke-layer";
    }

}
