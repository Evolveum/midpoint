/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.IResource;

import java.io.Serializable;

public class StatisticBoxDto implements Serializable {

    IModel<DisplayType> displayModel;
    IModel<IResource> messageImageResourceModel;

    public StatisticBoxDto(IModel<DisplayType> displayModel, IModel<IResource> messageImageResourceModel) {
        this.messageImageResourceModel = messageImageResourceModel;
        this.displayModel = displayModel;
    }

    public IResource getMessageImageResource() {
        return messageImageResourceModel != null ? messageImageResourceModel.getObject() : null;
    }

    public String getBoxTitle() {
        return displayModel != null ? GuiDisplayTypeUtil.getTranslatedLabel(displayModel.getObject()) : "";
    }

    public String getBoxDescription() {
        return displayModel != null ? GuiDisplayTypeUtil.getHelp(displayModel.getObject()) : "";
    }

    public String getBoxImageCss() {
        return displayModel != null ? GuiDisplayTypeUtil.getIconCssClass(displayModel.getObject()) : "";
    }

}
