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
public interface IconCssStyle {

    BottomLeftIconCssStyle BOTTOM_LEFT_STYLE = new BottomLeftIconCssStyle();
    BottomRightIconCssStyle BOTTOM_RIGHT_STYLE = new BottomRightIconCssStyle();
    BottomLeftForColumnIconCssStyle BOTTOM_LEFT_FOR_COLUMN_STYLE = new BottomLeftForColumnIconCssStyle();
    BottomRightForColumnIconCssStyle BOTTOM_RIGHT_FOR_COLUMN_STYLE = new BottomRightForColumnIconCssStyle();
    CenterIconCssStyle CENTER_STYLE = new CenterIconCssStyle();
    CenterForColumnIconCssStyle CENTER_FOR_COLUMN_STYLE = new CenterForColumnIconCssStyle();
    CenterWithRightShiftCssStyle CENTER_STYLE_WITH_RIGHT_SHIFT = new CenterWithRightShiftCssStyle();
    InRowIconCssStyle IN_ROW_STYLE = new InRowIconCssStyle();
    TopLeftIconCssStyle TOP_LEFT_STYLE = new TopLeftIconCssStyle();
    TopRightIconCssStyle TOP_RIGHT_STYLE = new TopRightIconCssStyle();
    TopRightForColumnIconCssStyle TOP_RIGHT_FOR_COLUMN_STYLE = new TopRightForColumnIconCssStyle();

    String getBasicCssClass();

    String getLayerCssClass();
}
