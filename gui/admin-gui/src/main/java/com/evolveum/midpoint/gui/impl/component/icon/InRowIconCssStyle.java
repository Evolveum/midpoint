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
