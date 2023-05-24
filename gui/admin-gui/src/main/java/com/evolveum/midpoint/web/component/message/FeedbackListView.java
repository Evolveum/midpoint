/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.message;

import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessagesModel;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.component.result.OperationResultPanel;

import java.io.Serializable;

/**
 * @author lazyman
 * @author katkav
 */
public class FeedbackListView extends ListView<FeedbackMessage> {

    private static final long serialVersionUID = 1L;

    public FeedbackListView(String id, Component component) {
        super(id);
        setDefaultModel(new FeedbackMessagesModel(component));
    }

    @Override
    protected void populateItem(final ListItem<FeedbackMessage> item) {

        final FeedbackMessage message = item.getModelObject();

        if (message.getMessage() instanceof OpResult) {
            final OpResult opResult = (OpResult) message.getMessage();
            OperationResultPanel panel = new OperationResultPanel("message", Model.of(opResult)) {

                private static final long serialVersionUID = 1L;

                @Override
                public void close(AjaxRequestTarget target, boolean parent) {
                    super.close(target, false);
                    message.markRendered();
                }

                protected void onAfterRender() {
                    opResult.setAlreadyShown(true);
                    super.onAfterRender();
                }
            };
            panel.add(new VisibleBehaviour(() -> opResult != null && !opResult.isAlreadyShown()));

            panel.setOutputMarkupId(true);
            item.add(panel);
        } else {

            message.markRendered();

            IModel<MessagePanel.MessagePanelType> type = () -> {
                FeedbackMessage result = item.getModelObject();

                if (result == null) {
                    return MessagePanel.MessagePanelType.INFO;
                }

                switch (result.getLevel()) {
                    case FeedbackMessage.INFO:
                    case FeedbackMessage.DEBUG:
                        return MessagePanel.MessagePanelType.INFO;
                    case FeedbackMessage.SUCCESS:
                        return MessagePanel.MessagePanelType.SUCCESS;
                    case FeedbackMessage.ERROR:
                    case FeedbackMessage.FATAL:
                        return MessagePanel.MessagePanelType.ERROR;
                    case FeedbackMessage.UNDEFINED:
                    case FeedbackMessage.WARNING:
                    default:
                        return MessagePanel.MessagePanelType.WARN;
                }
            };

            MessagePanel messagePanel = new MessagePanel("message", type, (IModel<Serializable>) () -> item.getModelObject().getMessage()) {

                private static final long serialVersionUID = 1L;

                @Override
                public void close(AjaxRequestTarget target) {
                    super.close(target);
                    message.markRendered();
                }
            };
            messagePanel.setOutputMarkupId(true);
            item.add(messagePanel);
        }
    }
}
