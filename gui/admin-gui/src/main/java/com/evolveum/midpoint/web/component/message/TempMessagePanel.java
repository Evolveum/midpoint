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

import com.evolveum.midpoint.gui.api.component.result.Context;
import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.component.result.Param;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.LoadableModel;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
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
public class TempMessagePanel extends Panel {

    public TempMessagePanel(String id, IModel<FeedbackMessage> message) {
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
        label.add(new AttributeModifier("title", new LoadableModel<String>() {

			@Override
			protected String load() {
				return getString("tempMessagePanel.message." + FeedbackMessagePanel.createMessageTooltip(message));
			}
		}));
        add(label);

        WebMarkupContainer content = new WebMarkupContainer("content");
        if (message.getObject().getMessage() instanceof OpResult) {
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
        content.add(new AttributeModifier("title", new LoadableModel<String>() {

			@Override
			protected String load() {
				return getString("tempMessagePanel.message." + FeedbackMessagePanel.createMessageTooltip(message));
			}
		}));
        add(content);
        
        WebMarkupContainer operationPanel = new WebMarkupContainer("operationPanel");
        content.add(operationPanel);
        
        operationPanel.add(new Label("operation", new LoadableModel<String>() {

            @Override
            protected String load() {
                OpResult result = (OpResult) message.getObject().getMessage();

                String resourceKey = OperationResultPanel.OPERATION_RESOURCE_KEY_PREFIX + result.getOperation();
                return getPage().getString(resourceKey, null, resourceKey);
            }
        }));
        
        WebMarkupContainer countPanel = new WebMarkupContainer("countPanel");
    	countPanel.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                OpResult result = (OpResult) message.getObject().getMessage();
                return result.getCount() > 1;
            }
        });
    	countPanel.add(new Label("count", new PropertyModel<String>(message, "message.count")));
        operationPanel.add(countPanel);

        ListView<Param> params = new ListView<Param>("params",
                OperationResultPanel.createParamsModel(new PropertyModel<OpResult>(message, "message"))) {

            @Override
            protected void populateItem(ListItem<Param> item) {
                item.add(new Label("paramName", new PropertyModel<Object>(item.getModel(), "name")));
                item.add(new Label("paramValue", new PropertyModel<Object>(item.getModel(), "value")));
            }
        };
        content.add(params);
        
        ListView<Context> contexts = new ListView<Context>("contexts",
       		 OperationResultPanel.createContextsModel(new PropertyModel<OpResult>(message, "message"))) {
			@Override
			protected void populateItem(ListItem<Context> item) {
				item.add(new Label("contextName", new PropertyModel<Object>(item.getModel(), "name")));
               item.add(new Label("contextValue", new PropertyModel<Object>(item.getModel(), "value")));
			}
       };
       content.add(contexts);
       
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
            case HANDLED_ERROR:
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
                resourceKey = "tempMessagePanel.message.fatalError";
                break;
            case IN_PROGRESS:
                resourceKey = "tempMessagePanel.message.inProgress";
                break;
            case NOT_APPLICABLE:
                resourceKey = "tempMessagePanel.message.notApplicable";
                break;
            case WARNING:
                resourceKey = "tempMessagePanel.message.warn";
                break;
            case PARTIAL_ERROR:
                resourceKey = "tempMessagePanel.message.partialError";
                break;
            case SUCCESS:
                resourceKey = "tempMessagePanel.message.success";
                break;
            case HANDLED_ERROR:
                resourceKey = "tempMessagePanel.message.expectedError";
                break;
            case UNKNOWN:
            default:
                resourceKey = "tempMessagePanel.message.unknown";
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
