/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.icon;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;
import org.apache.commons.lang.StringUtils;

/**
 * @author skublik
 */
public class CompositedIcon implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String basicIcon;
	private List<IconType> layerIcons;
	private String basicIconHtmlColor = "";
	private String title = "";
	
	
	public CompositedIcon(String basicIcon, List<IconType> layerIcons, String basicIconHtmlColor, String title){
		this.basicIcon = basicIcon;
		this.layerIcons = layerIcons;
		this.basicIconHtmlColor = basicIconHtmlColor;
		this.title = title;
	}
	
	public String getBasicIcon() {
		return basicIcon;
	}
	
	public List<IconType> getLayerIcons() {
		return layerIcons;
	}

	public String getBasicIconHtmlColor() {
		return basicIconHtmlColor;
	}

	public boolean hasLayerIcons(){
		return getLayerIcons() != null && !getLayerIcons().isEmpty();
	}

	public boolean hasBasicIcon() {
		return StringUtils.isNotEmpty(getBasicIcon());
	}

	public boolean hasBasicIconHtmlColor() {
		return StringUtils.isNotEmpty(getBasicIconHtmlColor());
	}
	
	public String getTitle() {
		return title;
	}
}
