/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.message;

import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.message.FeedbackListView;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessagesModel;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

/**
 * @author lazyman
 */
public class FeedbackLabels extends FeedbackAlerts {

    private static final String ID_LIST = "list";
    private static final String ID_MESSAGE = "message";

    public FeedbackLabels(String id) {
        super(id);
    }

    protected void initLayout() {
        ListView<FeedbackMessage> list = new ListView<>(ID_LIST, new FeedbackMessagesModel(this)) {
            @Override
            protected void populateItem(ListItem<FeedbackMessage> item) {
                item.add(
                        new Label(ID_MESSAGE, new PropertyModel<>(item.getModel(), "message")));
            }
        };
        list.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return hasMessages();
            }
        });
        add(list);
    }

    protected ListView<FeedbackMessage> getFeedbackListView() {
        return (ListView<FeedbackMessage>) get(ID_LIST);
    }

}
