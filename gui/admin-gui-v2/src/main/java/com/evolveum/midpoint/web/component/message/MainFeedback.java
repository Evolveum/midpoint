/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.message;

import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;

import java.util.List;

/**
 * @author lazyman
 */
public class MainFeedback extends Panel implements IFeedback {

    public MainFeedback(String id) {
        super(id);
        setOutputMarkupId(true);

        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer ul = new WebMarkupContainer("ul") {

            @Override
            public boolean isVisible() {
                return hasMessages();
            }
        };
        add(ul);

        FeedbackListView li = new FeedbackListView("li", this);
        ul.add(li);
    }

    public boolean hasMessages() {
        return hasMessages(FeedbackMessage.UNDEFINED);
    }


    public final boolean hasMessages(int level) {
        FeedbackListView view = (FeedbackListView) get("ul:li");

        List<FeedbackMessage> messages = view.getModelObject();
        for (FeedbackMessage msg : messages) {
            if (msg.isLevel(level)) {
                return true;
            }
        }

        return false;
    }
}
