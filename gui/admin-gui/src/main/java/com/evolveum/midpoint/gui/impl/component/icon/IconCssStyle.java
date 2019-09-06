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
	
	public static final BottomLeftIconCssStyle BOTTOM_LEFT_STYLE = new BottomLeftIconCssStyle();
	public static final BottomRightIconCssStyle BOTTOM_RIGHT_STYLE = new BottomRightIconCssStyle();
	public static final BottomLeftForColumnIconCssStyle BOTTOM_LEFT_FOR_COLUMN_STYLE = new BottomLeftForColumnIconCssStyle();
	public static final BottomRightForColumnIconCssStyle BOTTOM_RIGHT_FOR_COLUMN_STYLE = new BottomRightForColumnIconCssStyle();
	public static final CenterIconCssStyle CENTER_STYLE = new CenterIconCssStyle();
	public static final CenterForColumnIconCssStyle CENTER_FOR_COLUMN_STYLE = new CenterForColumnIconCssStyle();
	public static final CenterWithRightShiftCssStyle CENTER_STYLE_WITH_RIGHT_SHIFT = new CenterWithRightShiftCssStyle();
	public static final InRowIconCssStyle IN_ROW_STYLE = new InRowIconCssStyle();
	public static final TopLeftIconCssStyle TOP_LEFT_STYLE = new TopLeftIconCssStyle();
	public static final TopRightIconCssStyle TOP_RIGHT_STYLE = new TopRightIconCssStyle();
	public static final TopRightForColumnIconCssStyle TOP_RIGHT_FOR_COLUMN_STYLE = new TopRightForColumnIconCssStyle();

	public String getBasicCssClass();
	
	public String getLayerCssClass();
}
