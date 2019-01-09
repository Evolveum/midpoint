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

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;

/**
 * @author skublik
 */
public class CompositedIconBuilder {
	
	private String basicIcon = "";
	private List<String> layerIcons = new ArrayList<String>(); 
	
	public CompositedIcon build() {
		return new CompositedIcon(basicIcon, layerIcons);
	}
	
	private void setBasicIcon(String icon, String style) {
		StringBuilder sb = new StringBuilder(icon);
		sb.append(" ").append(style);
		basicIcon = sb.toString();
	}
	
	private void appendLayerIcon(String icon) {
		layerIcons.add(icon);
	}
	
	private void appendLayerIcon(int index, String icon) {
		layerIcons.add(index, icon);
	}
	
	public CompositedIconBuilder setBasicIcon(String icon, IconCssStyle style) {
		return setBasicIcon(icon, style, "");
	}
	
	public CompositedIconBuilder setBasicIcon(String icon, IconCssStyle style, String additionalCssClass) {
		additionalCssClass = additionalCssClass + " " + validateInput(icon, style, true);
		setBasicIcon(icon, style.getBasicCssClass() + " " + additionalCssClass);
		return this;
	}
	
	public CompositedIconBuilder setBasicIcon(String icon, LayeredIconCssStyle style) {
		return setBasicIcon(icon, style, "");
	}
	
	public CompositedIconBuilder setBasicIcon(String icon, LayeredIconCssStyle style, String additionalCssClass) {
		additionalCssClass = additionalCssClass + " " + validateInput(icon, style, true);
		setBasicIcon(icon, style.getBasicCssClass());
		StringBuilder sb = new StringBuilder(icon);
		sb.append(" ").append(style.getBasicLayerCssClass());
		if(StringUtils.isNotEmpty(additionalCssClass)) {
			sb.append(" ").append(additionalCssClass);
		}
		appendLayerIcon(0, sb.toString());
		return this;
	}
	
	public CompositedIconBuilder appendLayerIcon(String icon, CompositedIconCssStyle style) {
		return appendLayerIcon(icon, style, "");
	}
	
	public CompositedIconBuilder appendLayerIcon(String icon, CompositedIconCssStyle style, String additionalCssClass) {
		additionalCssClass = additionalCssClass + " " + validateInput(icon, style, false);
		StringBuilder sb = new StringBuilder(icon);
		sb.append(" ");
		if(layerIcons.isEmpty()) {
			sb.append(style.getLayerIconCssClassOfFirstIcon());
		} else {
			sb.append(style.getLayerCssClass());
		}
		if(StringUtils.isNotEmpty(additionalCssClass)) {
			sb.append(" ").append(additionalCssClass);
		}
		appendLayerIcon(sb.toString());
		return this;
	}
	
	public CompositedIconBuilder appendLayerIcon(String icon, LayeredIconCssStyle style) {
		return appendLayerIcon(icon, style, "");
	}
	
	public CompositedIconBuilder appendLayerIcon(String icon, LayeredIconCssStyle style, String additionalCssClass) {
		additionalCssClass = additionalCssClass + " " + validateInput(icon, style, false);
		StringBuilder sb = new StringBuilder(icon);
		sb.append(" ").append(style.getLayerCssClass());
		if(StringUtils.isNotEmpty(additionalCssClass)) {
			sb.append(" ").append(additionalCssClass);
		}
		String layerIconClass = sb.toString();
		sb.append(" ").append(style.getStrokeLayerCssClass());
		appendLayerIcon(sb.toString());
		appendLayerIcon(layerIconClass);
		return this;
	}
	
	public CompositedIconBuilder appendLayerIcon(String icon, IconCssStyle style) {
		return appendLayerIcon(icon, style, "");
	}
	
	public CompositedIconBuilder appendLayerIcon(String icon, IconCssStyle style, String additionalCssClass) {
		additionalCssClass = additionalCssClass + " " + validateInput(icon, style, false);
		StringBuilder sb = new StringBuilder(icon);
		sb.append(" ").append(style.getLayerCssClass());
		if(StringUtils.isNotEmpty(additionalCssClass)) {
			sb.append(" ").append(additionalCssClass);
		}
		appendLayerIcon(sb.toString());
		return this;
	}
	
	private String validateInput(String icon, IconCssStyle style, Boolean isBasic) {
		Validate.notNull(icon, "no icon class");
		Validate.notNull(style, "no icon style");
		
		if(isBasic && icon.equals(GuiStyleConstants.EVO_CROW_ICON) && style instanceof LayeredIconCssStyle) {
			return "font-size-130-per";
		}
		return "";
	}
}
