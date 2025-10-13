/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.icon;

/**
 * @author skublik
 */
public class InRowIconCssStyle implements CompositedIconCssStyle {

    @Override
    public String getBasicCssClass() {
        return "";
    }

    @Override
    public String getLayerCssClass() {
        return "in-row-layer";
    }

    @Override
    public String getLayerIconCssClassOfFirstIcon() {
        return "";
    }

}
