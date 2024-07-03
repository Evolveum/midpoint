/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.IResource;

import java.io.Serializable;

public class ChatMessageItem implements Serializable {

    IModel<DisplayType> messageDisplayModel;
    IModel<String> messageTextModel;
    IModel<IResource> messageImageResourceModel;

    public ChatMessageItem(IModel<DisplayType> messageDisplayModel, IModel<String> messageTextModel) {
        this(messageDisplayModel, messageTextModel, null);
    }

    public ChatMessageItem(IModel<DisplayType> messageDisplayModel, IModel<String> messageTextModel,
            IModel<IResource> messageImageResourceModel) {
        this.messageDisplayModel = messageDisplayModel;
        this.messageTextModel = messageTextModel;
        this.messageImageResourceModel = messageImageResourceModel;
    }

    public String getMessageTitle() {
        return messageDisplayModel != null ? GuiDisplayTypeUtil.getTranslatedLabel(messageDisplayModel.getObject()) : "";
    }

    public String getMessageText() {
        return messageTextModel != null ? messageTextModel.getObject() : null;
    }

    public String getMessageDescription() {
        return messageDisplayModel != null ? GuiDisplayTypeUtil.getHelp(messageDisplayModel.getObject()) : "";
    }

    public String getMessageImageCss() {
        return messageDisplayModel != null ? GuiDisplayTypeUtil.getIconCssClass(messageDisplayModel.getObject()) : "";
    }

    public IResource getMessageImageResource() {
        return messageImageResourceModel != null ? messageImageResourceModel.getObject() : null;
    }
}
