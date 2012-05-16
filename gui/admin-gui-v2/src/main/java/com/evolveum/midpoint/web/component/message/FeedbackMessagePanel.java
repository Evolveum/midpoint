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
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
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

        WebMarkupContainer content = new WebMarkupContainer("content");
        if (message.getObject().getMessage() instanceof OpResult) {
            ListView<OpResult> subresults = new ListView<OpResult>("subresults", createSubresultsModel(message)) {

                @Override
                protected void populateItem(ListItem<OpResult> item) {
                    item.add(new AttributeAppender("class",
                            OperationResultPanel.createMessageLiClass(item.getModel()), " "));
                    item.add(new OperationResultPanel("subresult", item.getModel()));
                }
            };
            content.add(subresults);
            content.add(new AttributeAppender("class", new LoadableModel<String>(false) {

                @Override
                protected String load() {
                    return getDetailsCss(new PropertyModel<OpResult>(message, "message"));
                }
            }, " "));
        } else {
            content.setVisible(false);
        }
        content.setMarkupId(label.getMarkupId() + "_content");
        add(content);

        content.add(new Label("operation", new LoadableModel<String>() {

            @Override
            protected String load() {
                OpResult result = (OpResult) message.getObject().getMessage();

                String resourceKey = OperationResultPanel.OPERATION_RESOURCE_KEY_PREFIX + result.getOperation();
                return getPage().getString(resourceKey, null, resourceKey);
            }
        }));

        ListView<Param> params = new ListView<Param>("params",
                OperationResultPanel.createParamsModel(new PropertyModel<OpResult>(message, "message"))) {

            @Override
            protected void populateItem(ListItem<Param> item) {
                item.add(new Label("paramName", new PropertyModel<Object>(item.getModel(), "name")));
                item.add(new Label("paramValue", new PropertyModel<Object>(item.getModel(), "value")));
            }
        };
        content.add(params);

        initExceptionLayout(content, message);
    }

    private void initExceptionLayout(WebMarkupContainer content, final IModel<FeedbackMessage> message) {
        WebMarkupContainer exception = new WebMarkupContainer("exception") {

            @Override
            public boolean isVisible() {
                FeedbackMessage fMessage = message.getObject();
                if (!(fMessage.getMessage() instanceof OpResult)) {
                    return false;
                }
                OpResult result = (OpResult) fMessage.getMessage();
                return StringUtils.isNotEmpty(result.getExceptionMessage())
                        || StringUtils.isNotEmpty(result.getExceptionsStackTrace());
            }
        };
        content.add(exception);
        exception.add(new MultiLineLabel("exceptionMessage", new PropertyModel<String>(message, "message.exceptionMessage")));

        WebMarkupContainer errorStack = new WebMarkupContainer("errorStack");
        errorStack.setOutputMarkupId(true);
        exception.add(errorStack);

        WebMarkupContainer errorStackContent = new WebMarkupContainer("errorStackContent");
        errorStackContent.setMarkupId(errorStack.getMarkupId() + "_content");
        exception.add(errorStackContent);

        errorStackContent.add(new MultiLineLabel("exceptionStack",
                new PropertyModel<String>(message, "message.exceptionsStackTrace")));
    }

    private String getDetailsCss(final IModel<OpResult> model) {
        OpResult result = model.getObject();
        if (result == null || result.getStatus() == null) {
            return "messages-warn-content";
        }

        switch (result.getStatus()) {
            case FATAL_ERROR:
            case PARTIAL_ERROR:
                return "messages-error-content";
            case IN_PROGRESS:
            case NOT_APPLICABLE:
                return "messages-info-content";
            case SUCCESS:
                return "messages-succ-content";
            case UNKNOWN:
            case WARNING:
            default:
                return "messages-warn-content";
        }
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
        if (!(message.getMessage() instanceof OpResult)) {
            return message.getMessage().toString();
        }

        OpResult result = (OpResult) message.getMessage();

        if (!StringUtils.isEmpty(result.getMessage())) {
            return result.getMessage();
        }

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
