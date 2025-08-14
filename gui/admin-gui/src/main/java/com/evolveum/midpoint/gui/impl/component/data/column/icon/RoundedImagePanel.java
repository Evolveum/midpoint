/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.data.column.icon;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.IResource;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RoundedImagePanel extends BasePanel<DisplayType> {

    private static final long serialVersionUID = 1L;

    private static final String ID_ICON = "icon";
    private static final String ID_CSS_ICON = "cssIcon";
    private static final String ID_IMAGE = "image";

    private IModel<IResource> preferredImage;

    public RoundedImagePanel(String id, IModel<DisplayType> model, IModel<IResource> preferredImage) {
        super(id, model);

        this.preferredImage = preferredImage;

        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(new VisibleBehaviour(() -> isCssIconVisible()));
        icon.add(AttributeAppender.append("class", getCssClassesIconContainer()));
        add(icon);

        Label cssIcon = new Label(ID_CSS_ICON);
        cssIcon.add(AttributeAppender.append("class", () -> getCssIcon()));
        cssIcon.add(AttributeAppender.append("style", () -> getIconColorStyle()));
        icon.add(cssIcon);

        NonCachingImage image = new NonCachingImage(ID_IMAGE, preferredImage);
        image.add(new VisibleBehaviour(() -> preferredImage.getObject() != null));
        image.add(AttributeAppender.append("alt", getAlternativeTextForImage()));
        add(image);
    }

    protected String getAlternativeTextForImage() {
        return null;
    }

    protected String getCssClassesIconContainer() {
        return null;
    }

    private IconType getIcon() {
        DisplayType display = getModelObject();
        if (display == null) {
            return null;
        }

        return display.getIcon();
    }

    private String getCssIcon() {
        IconType icon = getIcon();
        return icon != null ? icon.getCssClass() : null;
    }

    private String getIconColorStyle() {
        IconType icon = getIcon();
        if(icon != null) {
            String color = icon.getColor();
            if (StringUtils.isNotEmpty(color)) {
                return "color:" + GuiDisplayTypeUtil.removeStringAfterSemicolon(color) + ";";
            }
        }
        return null;
    }

    private boolean isCssIconVisible() {
        if (preferredImage != null && preferredImage.getObject() != null) {
            return false;
        }

        return StringUtils.isNotEmpty(getCssIcon());
    }
}
