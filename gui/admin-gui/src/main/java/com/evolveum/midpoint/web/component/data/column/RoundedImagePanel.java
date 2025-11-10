/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.data.column;

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

import java.io.Serial;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RoundedImagePanel extends BasePanel<DisplayType> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_ICON = "icon";
    private static final String ID_CSS_ICON = "cssIcon";
    private static final String ID_IMAGE = "image";

    private final IModel<IResource> preferredImage;

    public RoundedImagePanel(String id, IModel<DisplayType> model, IModel<IResource> preferredImage) {
        super(id, model);

        this.preferredImage = preferredImage;

        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(new VisibleBehaviour(this::isCssIconVisible));
        icon.add(AttributeAppender.append("class", getCssClassesIconContainer()));
        add(icon);

        Label cssIcon = new Label(ID_CSS_ICON);
        cssIcon.add(AttributeAppender.append("class", this::getCssIcon));
        cssIcon.add(AttributeAppender.append("style", this::getIconColorStyle));
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
