/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

	public String getBasicCssClass();
	
	public String getLayerCssClass();
}
