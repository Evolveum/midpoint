/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.*;

import java.io.Serial;
import java.util.List;

public class ChatPanel extends BasePanel<List<ChatMessageItem>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_HEADER_IMAGE = "headerImage";
    private static final String ID_HEADER_TITLE = "headerTitle";
    private static final String ID_MESSAGE_CONTAINER = "messageContainer";
    private static final String ID_MESSAGE_TITLE = "messageTitle";
    private static final String ID_MESSAGE_TEXT = "messageText";
    private static final String ID_MESSAGE_DESCRIPTION = "messageDescription";
    private static final String ID_MESSAGE_IMAGE = "messageImage";

    IModel<DisplayType> titleModel;

    public ChatPanel(String id, IModel<DisplayType> titleModel, IModel<List<ChatMessageItem>> model) {
        super(id, model);
        this.titleModel = titleModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {
        WebComponent headerImage = new WebComponent(ID_HEADER_IMAGE);
        headerImage.setOutputMarkupId(true);
        headerImage.add(AttributeAppender.append("class", getHeaderImageClass()));
        headerImage.add(new VisibleBehaviour(() -> getHeaderImageClass() != null));
        add(headerImage);

        Label headerTitle = new Label(ID_HEADER_TITLE, getHeaderTitleModel());
        headerTitle.setOutputMarkupId(true);
        add(headerTitle);

        initMessagesPanel();
    }

    private String getHeaderImageClass() {
        return GuiDisplayTypeUtil.getIconCssClass(titleModel.getObject());
    }

    private IModel<String> getHeaderTitleModel() {
        return () -> GuiDisplayTypeUtil.getTranslatedLabel(titleModel.getObject());
    }

    private void initMessagesPanel() {
        ListView<ChatMessageItem> messageContainer = new ListView<ChatMessageItem>(ID_MESSAGE_CONTAINER, getModel()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<ChatMessageItem> item) {
                ChatMessageItem message = item.getModelObject();

                Label messageTitle = new Label(ID_MESSAGE_TITLE, message.getMessageTitle());
                item.add(messageTitle);

                Label messageText = new Label(ID_MESSAGE_TEXT, message.getMessageText());
                messageText.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(message.getMessageText())));
                item.add(messageText);

                Label messageDescription = new Label(ID_MESSAGE_DESCRIPTION, message.getMessageDescription());
                messageDescription.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(message.getMessageDescription())));
                item.add(messageDescription);

                Component messageImage = initMessageImageComponent(message);
                item.add(messageImage);
            }

            private Component initMessageImageComponent(ChatMessageItem message) {
                IResource messageImageResource = message.getMessageImageResource();
                Component messageImagePanel = WebComponentUtil.createPhotoOrDefaultImagePanel(ID_MESSAGE_IMAGE, messageImageResource,
                        new IconType().cssClass(message.getMessageImageCss()));
                messageImagePanel.add(AttributeAppender.append("style", "font-size: 40px;"));
                return messageImagePanel;
            }
        };
        messageContainer.setOutputMarkupId(true);
        add(messageContainer);

    }
}
