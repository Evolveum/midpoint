/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.icon;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

import org.apache.wicket.model.IModel;

/**
 * @author skublik
 */
public class CompositedIconBuilder {

    private final List<LayerIcon> layerIcons = new ArrayList<>();

    private String basicIcon = "";
    private String colorHtmlValue = "";
    private String title = "";

    public CompositedIcon build() {
        return new CompositedIcon(basicIcon, layerIcons, colorHtmlValue, title);
    }

    private void setBasicIcon(String icon, String style) {
        basicIcon = icon + " " + style;
    }

    private void appendLayerIcon(LayerIcon icon) {
        layerIcons.add(icon);
    }

    private void appendLayerIcon(int index, LayerIcon icon) {
        layerIcons.add(index, icon);
    }

    public CompositedIconBuilder setBasicIcon(String icon, IconCssStyle style) {
        return setBasicIcon(icon, style, "");
    }

    public CompositedIconBuilder setBasicIcon(String icon, IconCssStyle style, String additionalCssClass) {
        additionalCssClass = additionalCssClass + " " + validateInput(icon, style, true);
        if (StringUtils.isEmpty(additionalCssClass.trim())) {
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
        if (StringUtils.isNotEmpty(additionalCssClass)) {
            sb.append(" ").append(additionalCssClass);
        }
        appendLayerIcon(0, new LayerIcon(IconAndStylesUtil.createIconType(sb.toString())));
        return this;
    }

    public <ICS extends IconCssStyle> CompositedIconBuilder setBasicIcon(IconType icon, ICS style) {
        Validate.notNull(icon, "no icon object");
        Validate.notNull(icon.getCssClass(), "no icon class");
        Validate.notNull(style, "no icon style");

        setBasicIcon(icon.getCssClass(), style.getBasicCssClass());
        return this;
    }

    public CompositedIconBuilder appendColorHtmlValue(String colorHtmlValue) {
        this.colorHtmlValue = colorHtmlValue;
        return this;
    }

    public CompositedIconBuilder appendLayerIcon(String icon, LayeredIconCssStyle style) {
        IconType iconType = new IconType();
        iconType.setCssClass(icon);
        return appendLayerIcon(iconType, style);
    }

    public CompositedIconBuilder appendLayerIcon(String icon, CompositedIconCssStyle style, String additionalCssClass) {
        additionalCssClass = additionalCssClass + " " + validateInput(icon, style, false);
        StringBuilder sb = new StringBuilder(icon);
        sb.append(" ");
        if (layerIcons.isEmpty()) {
            sb.append(style.getLayerIconCssClassOfFirstIcon());
        } else {
            sb.append(style.getLayerCssClass());
        }
        if (StringUtils.isNotEmpty(additionalCssClass)) {
            sb.append(" ").append(additionalCssClass);
        }
        appendLayerIcon(new LayerIcon(IconAndStylesUtil.createIconType(sb.toString())));
        return this;
    }

    public CompositedIconBuilder appendLayerIcon(IconType icon, LayeredIconCssStyle style) {
        Validate.notNull(icon, "no icon object");
        Validate.notNull(icon.getCssClass(), "no icon class");
        Validate.notNull(style, "no icon style");

        StringBuilder sb = new StringBuilder(icon.getCssClass());
        sb.append(" ").append(style.getLayerCssClass());
        if (StringUtils.isNotEmpty(icon.getColor())) {
            sb.append(" ").append(icon.getColor()).append(" ");
        }
        String layerIconClass = sb.toString();
        sb.append(" ").append(style.getStrokeLayerCssClass());
        appendLayerIcon(new LayerIcon(IconAndStylesUtil.createIconType(sb.toString(), icon.getColor())));
        appendLayerIcon(new LayerIcon(IconAndStylesUtil.createIconType(layerIconClass, icon.getColor())));
        return this;
    }

    public CompositedIconBuilder appendLayerIcon(IModel<String> labelModel, IconType icon, LayeredIconCssStyle style) {
        Validate.notNull(icon, "no icon object");
        Validate.notNull(labelModel, "no icon label");
        Validate.notNull(style, "no icon style");

        StringBuilder sb = new StringBuilder(style.getLayerCssClass());
        if (StringUtils.isNotEmpty(icon.getColor())) {
            sb.append(" ").append(icon.getColor()).append(" ");
        }
        String layerIconClass = sb.toString();
        sb.append(" ").append(style.getStrokeLayerCssClass());
        appendLayerIcon(new LayerIcon(IconAndStylesUtil.createIconType(sb.toString(), icon.getColor()), labelModel));
        appendLayerIcon(new LayerIcon(IconAndStylesUtil.createIconType(layerIconClass, icon.getColor()), labelModel));
        return this;
    }

    public CompositedIconBuilder appendLayerIcon(IconType icon, IconCssStyle style) {
        Validate.notNull(icon, "no icon object");
        Validate.notNull(icon.getCssClass(), "no icon class");
        Validate.notNull(style, "no icon style");

        StringBuilder sb = new StringBuilder(icon.getCssClass());
        sb.append(" ").append(style.getLayerCssClass());
        if (StringUtils.isNotEmpty(icon.getColor())) {
            sb.append(" ").append(icon.getColor());
        }
        appendLayerIcon(new LayerIcon(IconAndStylesUtil.createIconType(sb.toString(), icon.getColor())));
        return this;
    }

    private String validateInput(String icon, IconCssStyle style, Boolean isBasic) {
//        Validate.notNull(icon, "no icon class");  //TODO: why would we need this to be validated?
        Validate.notNull(style, "no icon style");

        if (icon == null) {
            return "";
        }

        if (isBasic && icon.equals(GuiStyleConstants.EVO_CROW_ICON) && style instanceof LayeredIconCssStyle) {
            return "font-size-130-per";
        }
        return "";
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
