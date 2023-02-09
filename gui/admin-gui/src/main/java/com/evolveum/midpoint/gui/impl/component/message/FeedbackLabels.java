/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.message;

import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessagesModel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

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
                Label label = new Label(ID_MESSAGE, new PropertyModel<>(item.getModel(), "message"));
                label.add(AttributeAppender.append("class", getColorClass(item.getModel().getObject().getLevelAsString())));
                item.add(label);
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

    private String getColorClass(String levelAsString) {
        switch(levelAsString) {
            case "INFO":
                return "text-info";
            case "WARNING":
                return "text-warning";
            case "SUCCESS":
                return "text-success";
            case "ERRO":
            case "FATAL":
                return "text-danger";
        }
        return "text-secondary";
    }

    protected ListView<FeedbackMessage> getFeedbackListView() {
        return (ListView<FeedbackMessage>) get(ID_LIST);
    }

}
