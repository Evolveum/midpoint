/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.message;

import java.io.Serial;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessagesModel;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

public class SimpleFeedbackPanel extends BasePanel<List<FeedbackMessage>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_MESSAGES = "messages";
    private static final String ID_MESSAGE = "message";

    public SimpleFeedbackPanel(String id, IFeedbackMessageFilter filter) {
        super(id, null);

        if (filter != null) {
            getModel().setFilter(filter);
        }
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    @Override
    public FeedbackMessagesModel getModel() {
        return (FeedbackMessagesModel) super.getModel();
    }

    @Override
    public FeedbackMessagesModel createModel() {
        return new FeedbackMessagesModel(this);
    }

    private void initLayout() {
        ListView<FeedbackMessage> messages = new ListView<>(ID_MESSAGES, getModel()) {

            @Override
            protected void populateItem(org.apache.wicket.markup.html.list.ListItem<FeedbackMessage> item) {
                item.add(createMessagePanel(ID_MESSAGE, item.getModel()));
            }
        };
        add(messages);
    }

    private Component createMessagePanel(String id, IModel<FeedbackMessage> model) {
        Label message = new Label(id, () -> model.getObject().getMessage());
        message.add(new VisibleBehaviour(() -> model.getObject().getMessage() != null));
        message.add(AttributeAppender.append("class", () -> {

            int level = model.getObject().getLevel();
            return switch (level) {
                case FeedbackMessage.ERROR, FeedbackMessage.WARNING, FeedbackMessage.FATAL -> "invalid-feedback";
                default -> "valid-feedback";
            };
        }));
        return message;
    }

    public static void addSimpleFeedbackAppender(FormComponent<?> component) {
        component.add(AttributeAppender.append("class", () -> {
            if (!component.isValid()) {
                return "is-invalid";
            }

            if (component.getModelObject() == null) {
                return null;
            }

            return "is-valid";
        }));
    }
}
