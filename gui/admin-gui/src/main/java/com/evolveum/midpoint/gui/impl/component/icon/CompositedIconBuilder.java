/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.icon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;

/**
 * @author skublik
 */
public class CompositedIconBuilder {

    private String basicIcon = "";
    private List<IconType> layerIcons = new ArrayList<IconType>();
    private String colorHtmlValue = "";
    private String title = "";

    public CompositedIcon build() {
        return new CompositedIcon(basicIcon, layerIcons, colorHtmlValue, title);
    }

    private void setBasicIcon(String icon, String style) {
        StringBuilder sb = new StringBuilder(icon);
        sb.append(" ").append(style);
        basicIcon = sb.toString();
    }

    private void appendLayerIcon(IconType icon) {
        layerIcons.add(icon);
    }

    private void appendLayerIcon(int index, IconType icon) {
        layerIcons.add(index, icon);
    }

    public CompositedIconBuilder setBasicIcon(String icon, IconCssStyle style) {
        return setBasicIcon(icon, style, "");
    }

    public CompositedIconBuilder setBasicIcon(String icon, IconCssStyle style, String additionalCssClass) {
        additionalCssClass = additionalCssClass + " " + validateInput(icon, style, true);
        if (additionalCssClass == null || StringUtils.isEmpty(additionalCssClass.trim())){
            setBasicIcon(icon, style.getBasicCssClass());
        } else {
            setBasicIcon(icon, style.getBasicCssClass() + " " + additionalCssClass);
        }
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
        appendLayerIcon(0, WebComponentUtil.createIconType(sb.toString()));
        return this;
    }

    public <ICS extends IconCssStyle> CompositedIconBuilder setBasicIcon(IconType icon, ICS style) {
        Validate.notNull(icon, "no icon object");
        Validate.notNull(icon.getCssClass(), "no icon class");
        Validate.notNull(style, "no icon style");

        setBasicIcon(icon.getCssClass(), style.getBasicCssClass());
        return this;
    }

    public CompositedIconBuilder appendColorHtmlValue(String colorHtmlValue){
        this.colorHtmlValue = colorHtmlValue;
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
        appendLayerIcon(WebComponentUtil.createIconType(sb.toString()));
        return this;
    }

    public CompositedIconBuilder appendLayerIcon(IconType icon, LayeredIconCssStyle style) {
        Validate.notNull(icon, "no icon object");
        Validate.notNull(icon.getCssClass(), "no icon class");
        Validate.notNull(style, "no icon style");

        StringBuilder sb = new StringBuilder(icon.getCssClass());
        sb.append(" ").append(style.getLayerCssClass());
        if(StringUtils.isNotEmpty(icon.getColor())) {
            sb.append(" ").append(icon.getColor()).append(" ");
        }
        String layerIconClass = sb.toString();
        sb.append(" ").append(style.getStrokeLayerCssClass());
        appendLayerIcon(WebComponentUtil.createIconType(sb.toString(), icon.getColor()));
        appendLayerIcon(WebComponentUtil.createIconType(layerIconClass, icon.getColor()));
        return this;
    }

    public CompositedIconBuilder appendLayerIcon(IconType icon, IconCssStyle style) {
        Validate.notNull(icon, "no icon object");
        Validate.notNull(icon.getCssClass(), "no icon class");
        Validate.notNull(style, "no icon style");

        StringBuilder sb = new StringBuilder(icon.getCssClass());
        sb.append(" ").append(style.getLayerCssClass());
        if(StringUtils.isNotEmpty(icon.getColor())) {
            sb.append(" ").append(icon.getColor());
        }
        appendLayerIcon(WebComponentUtil.createIconType(sb.toString(), icon.getColor()));
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

    public void setTitle(String title) {
        this.title = title;
    }
}
