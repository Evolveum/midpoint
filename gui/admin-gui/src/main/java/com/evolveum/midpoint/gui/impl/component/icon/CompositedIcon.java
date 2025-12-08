/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.icon;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * @author skublik
 */
public class CompositedIcon implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    public static final String F_LAYER_ICONS = "layerIcons";

    private final String basicIcon;
    private final List<LayerIcon> layerIcons;

    private String basicIconHtmlColor = "";
    private String title = "";

    public CompositedIcon(String basicIcon, List<LayerIcon> layerIcons, String basicIconHtmlColor, String title) {
        this.basicIcon = basicIcon;
        this.layerIcons = layerIcons;
        this.basicIconHtmlColor = basicIconHtmlColor;
        this.title = title;
    }

    public String getBasicIcon() {
        return basicIcon != null ? basicIcon.trim() : basicIcon;
    }

    public List<LayerIcon> getLayerIcons() {
        return layerIcons;
    }

    public String getBasicIconHtmlColor() {
        return basicIconHtmlColor;
    }

    public boolean hasLayerIcons() {
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
