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

import com.evolveum.midpoint.web.component.util.LoadableModel;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class FeedbackMessagePanel extends Panel {

    public FeedbackMessagePanel(String id, IModel<FeedbackMessage> message) {
        super(id);

        Label label = new Label("message", message);
        label.add(new AttributeAppender("class", createModel(message), " "));
        add(label);

        add(new EmptyPanel("details"));
    }

    private IModel<String> createModel(final IModel<FeedbackMessage> model) {
        return new LoadableModel<String>(false) {

            @Override
            protected String load() {
                FeedbackMessage message = model.getObject();
                switch (message.getLevel()) {
                    case FeedbackMessage.INFO:
                    case FeedbackMessage.SUCCESS:
                        return "messages-topSucc";
                    case FeedbackMessage.ERROR:
                    case FeedbackMessage.FATAL:
                        return "messages-topError";
                    case FeedbackMessage.UNDEFINED:
                    case FeedbackMessage.DEBUG:
                    case FeedbackMessage.WARNING:
                    default:
                        return "messages-topWarn";
                }
            }
        };
    }
}
