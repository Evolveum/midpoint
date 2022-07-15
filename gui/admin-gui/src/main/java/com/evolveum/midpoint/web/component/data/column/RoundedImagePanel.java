/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.AbstractResource;

import com.evolveum.midpoint.gui.api.component.BasePanel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RoundedImagePanel extends BasePanel<DisplayType> {

    private static final long serialVersionUID = 1L;

    private static final String ID_ICON = "icon";
    private static final String ID_CSS_ICON = "cssIcon";
    private static final String ID_IMAGE = "image";

    private IModel<AbstractResource> preferredImage;

    public RoundedImagePanel(String id, IModel<DisplayType> model, IModel<AbstractResource> preferredImage) {
        super(id, model);

        this.preferredImage = preferredImage;

        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(new VisibleBehaviour(() -> isCssIconVisible()));
        add(icon);

        Label cssIcon = new Label(ID_CSS_ICON);
        cssIcon.add(AttributeAppender.append("class", () -> getCssIcon()));
        icon.add(cssIcon);

        NonCachingImage image = new NonCachingImage(ID_IMAGE, preferredImage);
        image.add(new VisibleBehaviour(() -> preferredImage.getObject() != null));
        add(image);
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

    private boolean isCssIconVisible() {
        return StringUtils.isNotEmpty(getCssIcon());
    }
}
