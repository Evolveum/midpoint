/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.message;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessagesModel;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;

import java.util.List;

/**
 * @author lazyman
 */
public class FeedbackAlerts extends Panel implements IFeedback {

    private static final String ID_LIST = "list";

    public FeedbackAlerts(String id) {
        super(id);
        setOutputMarkupId(true);

        initLayout();
    }

    protected void initLayout() {
        FeedbackListView list = new FeedbackListView(ID_LIST, this);
        list.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return hasMessages();
            }
        });
        add(list);
    }

    public boolean hasMessages() {
        return hasMessages(FeedbackMessage.UNDEFINED);
    }

    protected ListView<FeedbackMessage> getFeedbackListView() {
        return (ListView<FeedbackMessage>) get(ID_LIST);
    }

    public final boolean hasMessages(int level) {
        List<FeedbackMessage> messages = getFeedbackListView().getModelObject();
        for (FeedbackMessage msg : messages) {
            if (msg.isLevel(level)) {
                return true;
            }
        }

        return false;
    }

    public final void setFilter(IFeedbackMessageFilter filter) {
        FeedbackMessagesModel model = (FeedbackMessagesModel) getFeedbackListView().getDefaultModel();
        model.setFilter(filter);
    }
}
