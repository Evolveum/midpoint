/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.message2;

import org.apache.wicket.Component;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessagesModel;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * @author lazyman
 */
public class FeedbackListView extends ListView<FeedbackMessage> {

    public FeedbackListView(String id, Component component) {
        super(id);
        setDefaultModel(new FeedbackMessagesModel(component));
    }

    @Override
    protected void populateItem(ListItem<FeedbackMessage> item) {
        final FeedbackMessage message = item.getModelObject();
        message.markRendered();

        Panel panel = new FeedbackAlertMessage("message", item.getModel());
        item.add(panel);
    }
}
