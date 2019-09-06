/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.ExternalImage;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class ImagePanel extends Panel {

    //image can be defined either with css class or with image file source; therefore we need to use 2 different tags for each case
    private static final String ID_IMAGE = "image";
    private static final String ID_IMAGE_SRC = "imageSrc";

    private DisplayType iconDisplayData;

    public ImagePanel(String id, IModel<String> iconClassModel, IModel<String> titleModel) {
        super(id);
        iconDisplayData = new DisplayType();
        IconType icon = new IconType();
        icon.setCssClass(iconClassModel != null ? iconClassModel.getObject() : null);
        iconDisplayData.setIcon(icon);

        PolyStringType title = new PolyStringType(titleModel != null ? titleModel.getObject() : null);
        iconDisplayData.setTooltip(title);
    }

    public ImagePanel(String id, DisplayType iconDisplayData){
        super(id);
        this.iconDisplayData = iconDisplayData == null ? new DisplayType() : iconDisplayData;
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        Label image = new Label(ID_IMAGE);
        image.add(AttributeModifier.replace("class", iconDisplayData.getIcon() != null ? iconDisplayData.getIcon().getCssClass() : ""));
        if (iconDisplayData.getTooltip() != null && StringUtils.isNotEmpty(iconDisplayData.getTooltip().getOrig())) {
            image.add(AttributeModifier.replace("title", iconDisplayData.getTooltip().getOrig()));
        }
        if (iconDisplayData.getIcon() != null && StringUtils.isNotEmpty(iconDisplayData.getIcon().getColor())){
            image.add(AttributeAppender.append("style", "color: " + iconDisplayData.getIcon().getColor() + ";"));
        }
        image.setOutputMarkupId(true);
        image.add(new VisibleBehaviour(() -> iconDisplayData.getIcon() != null && StringUtils.isNotEmpty(iconDisplayData.getIcon().getCssClass())));
        add(image);

        ExternalImage customLogoImgSrc = new ExternalImage(ID_IMAGE_SRC,
                WebComponentUtil.getIconUrlModel(iconDisplayData.getIcon()));
        customLogoImgSrc.setOutputMarkupId(true);
        customLogoImgSrc.add(new VisibleBehaviour(() -> iconDisplayData.getIcon() != null && StringUtils.isNotEmpty(iconDisplayData.getIcon().getImageUrl())));
        add(customLogoImgSrc);
    }
}
