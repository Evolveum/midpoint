/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.component.message;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessagesModel;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;

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
        final WebMarkupContainer ul = new WebMarkupContainer("ul") {

            @Override
            public boolean isVisible() {
                return hasMessages();
            }
        };
        add(ul);

        FeedbackListView li = new FeedbackListView("li", this);
        ul.add(li);
        
        AjaxLink close = new AjaxLink("close") {
        	
        	@Override
        	public void onClick(AjaxRequestTarget target) {
        		ul.setVisible(false);
//        		target.add(ul);
        	}
		};
		
		add(close);
    }

    public boolean hasMessages() {
        return hasMessages(FeedbackMessage.UNDEFINED);
    }

    private FeedbackListView getFeedbackListView() {
        return (FeedbackListView) get("ul:li");
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

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.render(JavaScriptHeaderItem.forReference(
                new PackageResourceReference(MainFeedback.class, "MainFeedback.js")));
        response.render(OnDomReadyHeaderItem.forScript("initMessages()"));
    }

    public final void setFilter(IFeedbackMessageFilter filter) {
        FeedbackMessagesModel model = (FeedbackMessagesModel) getFeedbackListView().getDefaultModel();
        model.setFilter(filter);
    }
}
