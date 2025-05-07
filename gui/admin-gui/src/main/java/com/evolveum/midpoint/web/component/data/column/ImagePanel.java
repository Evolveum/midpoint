/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.ExternalImage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author lazyman
 */
public class ImagePanel extends BasePanel<DisplayType> {

    //image can be defined either with css class or with image file source; therefore we need to use 2 different tags for each case
    private static final String ID_IMAGE = "image";
    private static final String ID_IMAGE_SRC = "imageSrc";
    private IconRole iconRole = IconRole.IMAGE;

    public enum IconRole {
        IMAGE("img"),
        BUTTON("button");

        private final String value;

        IconRole(String value) {
            this.value = value;
        }

        String getValue() {
            return value;
        }
    }

    public ImagePanel(String id, IModel<String> iconClassModel, IModel<String> titleModel) {
        super(id, new Model<>());
        DisplayType iconDisplayData = new DisplayType();
        IconType icon = new IconType();
        icon.setCssClass(iconClassModel != null ? iconClassModel.getObject() : null);
        iconDisplayData.setIcon(icon);

        PolyStringType title = new PolyStringType(titleModel != null ? titleModel.getObject() : null);
        iconDisplayData.setTooltip(title);

        getModel().setObject(iconDisplayData);
    }

    public ImagePanel(String id, IModel<DisplayType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        Label image = new Label(ID_IMAGE);
        image.add(AttributeModifier.replace("class", new PropertyModel<>(getModel(), "icon.cssClass")));
        DisplayType displayBean = getModelObject();
        if (displayBean != null) {
            String toolTip = LocalizationUtil.translatePolyString(displayBean.getTooltip());
            image.add(AttributeModifier.replace("title", toolTip));
            image.add(AttributeModifier.replace("aria-label", toolTip));
        }
        image.add(AttributeAppender.append("style", () -> StringUtils.isNotBlank(getColor()) ? "color: " + getColor() + ";" : ""));
        image.setOutputMarkupId(true);
        image.add(new VisibleBehaviour(() -> getModelObject() != null && getModelObject().getIcon() != null && StringUtils.isNotEmpty(getModelObject().getIcon().getCssClass())));

        image.add(AttributeAppender.append("role", iconRole.getValue()));
        image.add(AttributeAppender.append("tabindex", 0));

        add(image);

        ExternalImage customLogoImgSrc = new ExternalImage(ID_IMAGE_SRC,
                WebComponentUtil.getIconUrlModel(getModelObject() != null ? getModelObject().getIcon() : null));
        customLogoImgSrc.setOutputMarkupId(true);
        customLogoImgSrc.add(new VisibleBehaviour(() -> getModelObject() != null && getModelObject().getIcon() != null && StringUtils.isNotEmpty(getModelObject().getIcon().getImageUrl())));
        add(customLogoImgSrc);
    }

    public void setIconRole(IconRole role) {
        this.iconRole = role;
    }

    private String getColor() {
        if (getModelObject() == null) {
            return null;
        }

        IconType icon = getModelObject().getIcon();
        if (icon == null) {
            return null;
        }

        return GuiDisplayTypeUtil.removeStringAfterSemicolon(icon.getColor());
    }
}
