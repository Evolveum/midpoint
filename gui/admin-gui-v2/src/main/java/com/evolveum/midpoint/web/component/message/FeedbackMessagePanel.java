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
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class FeedbackMessagePanel extends Panel {

    public FeedbackMessagePanel(String id, IModel<FeedbackMessage> message) {
        super(id);

        initLayout(message);
    }

    private void initLayout(final IModel<FeedbackMessage> message) {
        Label label = new Label("message", new LoadableModel<String>(false) {

            @Override
            protected String load() {
                return getTopMessage(message);
            }
        });

        label.add(new AttributeAppender("class", new LoadableModel<String>() {
            @Override
            protected String load() {
                return getLabelCss(message);
            }
        }, " "));
        label.setOutputMarkupId(true);
        add(label);

        WebMarkupContainer details;
        if (message.getObject().getMessage() instanceof OpResult) {
            ListView<OpResult> subresults = new ListView<OpResult>("subresults",
                    createSubresultsModel(message)) {

                @Override
                protected void populateItem(ListItem<OpResult> item) {
                    item.add(new OperationResultPanel("subresult", item.getModel()));
                }
            };
            details = subresults;
        } else {
            details = new EmptyPanel("content");
        }
        details.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                FeedbackMessage msg = message.getObject();
                if (msg.getMessage() instanceof OpResult) {
                    return true;
                }

                return false;
            }
        });
        details.setMarkupId(label.getMarkupId() + "_content");
        add(details);
    }

    private IModel<List<OpResult>> createSubresultsModel(final IModel<FeedbackMessage> model) {
        return new LoadableModel<List<OpResult>>(false) {

            @Override
            protected List<OpResult> load() {
                FeedbackMessage message = model.getObject();
                Serializable serializable = message.getMessage();
                if (!(serializable instanceof OpResult)) {
                    return new ArrayList<OpResult>();
                }

                OpResult result = (OpResult) serializable;
                return result.getSubresults();
            }
        };
    }

    private String getTopMessage(final IModel<FeedbackMessage> model) {
        FeedbackMessage message = model.getObject();
        if (message.getMessage() instanceof OpResult) {
            OpResult result = (OpResult) message.getMessage();

            if (StringUtils.isEmpty(result.getMessage())) {
                String resourceKey;
                switch (result.getStatus()) {
                    case FATAL_ERROR:
                        resourceKey = "feedbackMessagePanel.message.fatalError";
                        break;
                    case IN_PROGRESS:
                        resourceKey = "feedbackMessagePanel.message.inProgress";
                        break;
                    case NOT_APPLICABLE:
                        resourceKey = "feedbackMessagePanel.message.notApplicable";
                        break;
                    case WARNING:
                        resourceKey = "feedbackMessagePanel.message.warn";
                        break;
                    case PARTIAL_ERROR:
                        resourceKey = "feedbackMessagePanel.message.partialError";
                        break;
                    case SUCCESS:
                        resourceKey = "feedbackMessagePanel.message.success";
                        break;
                    case UNKNOWN:
                    default:
                        resourceKey = "feedbackMessagePanel.message.unknown";
                }

                return new StringResourceModel(resourceKey, this, null).getString();
            }

            return result.getMessage();
        }

        return message.getMessage().toString();

    }

    private String getLabelCss(final IModel<FeedbackMessage> model) {
        FeedbackMessage message = model.getObject();
        switch (message.getLevel()) {
            case FeedbackMessage.INFO:
                return "messages-topInfo";
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
}
