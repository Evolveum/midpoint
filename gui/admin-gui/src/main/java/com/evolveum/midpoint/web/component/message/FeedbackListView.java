/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.message;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessagesModel;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class FeedbackListView extends ListView<FeedbackMessage> {
	private boolean isTempPanel;

    public FeedbackListView(String id, Component component, boolean isTempPanel) {
        super(id);
        setDefaultModel(new FeedbackMessagesModel(component));
        this.isTempPanel = isTempPanel;
    }

    @Override
    protected void populateItem(ListItem<FeedbackMessage> item) {
        FeedbackMessage message = item.getModelObject();
        message.markRendered();
        Panel panel = null;
        if(isTempPanel){
        	panel = new TempMessagePanel("message", item.getModel());
        } else {
        	panel = new FeedbackMessagePanel("message", item.getModel());
        }
        
        panel.add(new AttributeAppender("class", createModel(item.getModel()), " "));

        item.add(panel);
    }

    private IModel<String> createModel(final IModel<FeedbackMessage> model) {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                FeedbackMessage message = model.getObject();
                switch (message.getLevel()) {
                    case FeedbackMessage.INFO:
                        return "messages-info";
                    case FeedbackMessage.SUCCESS:
                        return "messages-succ";
                    case FeedbackMessage.ERROR:
                    case FeedbackMessage.FATAL:
                        return "messages-error";
                    case FeedbackMessage.UNDEFINED:
                    case FeedbackMessage.DEBUG:
                    case FeedbackMessage.WARNING:
                    default:
                        return "messages-warn";
                }
            }
        };
    }
}
