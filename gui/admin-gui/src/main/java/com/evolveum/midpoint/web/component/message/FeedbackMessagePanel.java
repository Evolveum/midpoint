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
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class FeedbackMessagePanel extends Panel {

    private String xml = null;

    public FeedbackMessagePanel(String id, IModel<FeedbackMessage> message) {
        super(id);

        initLayout(message);
    }

    private void initLayout(final IModel<FeedbackMessage> message) {
        WebMarkupContainer messageContainer = new WebMarkupContainer("messageContainer");
        messageContainer.setOutputMarkupId(true);
        messageContainer.add(new AttributeAppender("class", new LoadableModel<String>() {
            @Override
            protected String load() {
                return getLabelCss(message);
            }
        }, " "));
        messageContainer.add(new AttributeModifier("title", new LoadableModel<String>() {

            @Override
            protected String load() {
                return getString("feedbackMessagePanel.message." + createMessageTooltip(message));
            }
        }));
        add(messageContainer);
        Label label = new Label("message", new LoadableModel<String>(false) {

            @Override
            protected String load() {
                return getTopMessage(message);
            }
        });
        messageContainer.add(label);
        WebMarkupContainer topExceptionContainer = new WebMarkupContainer("topExceptionContainer");
        messageContainer.add(topExceptionContainer);
        WebMarkupContainer content = new WebMarkupContainer("content");
        if (message.getObject().getMessage() instanceof OpResult) {

            OpResult result = (OpResult) message.getObject().getMessage();
            xml = result.getXml();
            export(content, new Model<String>(xml));

            ListView<OpResult> subresults = new ListView<OpResult>("subresults",
                    createSubresultsModel(message)) {

                @Override
                protected void populateItem(final ListItem<OpResult> item) {
                    item.add(new AttributeAppender("class", OperationResultPanel.createMessageLiClass(item
                            .getModel()), " "));
                    item.add(new AttributeModifier("title", new LoadableModel<String>() {

                        @Override
                        protected String load() {
                            return getString("feedbackMessagePanel.message."
                                    + OperationResultPanel.createMessageTooltip(item.getModel()).getObject());
                        }
                    }));
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
            topExceptionContainer.setVisibilityAllowed(false);
        }
        content.setMarkupId(messageContainer.getMarkupId() + "_content");
        add(content);

        WebMarkupContainer operationPanel = new WebMarkupContainer("operationPanel");
        topExceptionContainer.add(operationPanel);

        operationPanel.add(new Label("operation", new LoadableModel<String>() {

            @Override
            protected String load() {
                OpResult result = (OpResult) message.getObject().getMessage();

                String resourceKey = OperationResultPanel.OPERATION_RESOURCE_KEY_PREFIX
                        + result.getOperation();
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
        topExceptionContainer.add(params);

        ListView<Context> contexts = new ListView<Context>("contexts",
                OperationResultPanel.createContextsModel(new PropertyModel<OpResult>(message, "message"))) {
            @Override
            protected void populateItem(ListItem<Context> item) {
                item.add(new Label("contextName", new PropertyModel<Object>(item.getModel(), "name")));
                item.add(new Label("contextValue", new PropertyModel<Object>(item.getModel(), "value")));
            }
        };
        topExceptionContainer.add(contexts);

		/*
         * WebMarkupContainer countLi = new WebMarkupContainer("countLi");
		 * countLi.add(new VisibleEnableBehaviour() {
		 * 
		 * @Override public boolean isVisible() { OpResult result = (OpResult)
		 * message.getObject().getMessage(); return result.getCount() > 1; } });
		 * content.add(countLi); countLi.add(new Label("count", new
		 * PropertyModel<String>(message, "message.count")));
		 */

        initExceptionLayout(content, topExceptionContainer, message);

        content.add(new Label("collapseAll", new LoadableModel<String>() {

            @Override
            protected String load() {
                return getString("feedbackMessagePanel.collapseAll");
            }
        }));
        content.add(new Label("expandAll", new LoadableModel<String>() {

            @Override
            protected String load() {
                return getString("feedbackMessagePanel.expandAll");
            }
        }));
    }

    private IModel<String> getXml() {
        return new LoadableModel<String>() {
            @Override
            protected String load() {
                return xml;
            }
        };
    }

    private void initExceptionLayout(WebMarkupContainer content, WebMarkupContainer topExceptionContainer,
                                     final IModel<FeedbackMessage> message) {
        WebMarkupContainer exception = new WebMarkupContainer("exception") {

            @Override
            public boolean isVisible() {
                return isExceptionVisible(message);
            }
        };
        topExceptionContainer.add(exception);
        exception.add(new MultiLineLabel("exceptionMessage", new PropertyModel<String>(message,
                "message.exceptionMessage")));

        WebMarkupContainer errorStackContainer = new WebMarkupContainer("errorStackContainer") {
            @Override
            public boolean isVisible() {
                return isExceptionVisible(message);
            }
        };
        content.add(errorStackContainer);

        WebMarkupContainer errorStack = new WebMarkupContainer("errorStack");
        errorStack.setOutputMarkupId(true);
        errorStackContainer.add(errorStack);

        // export(errorStackContainer, new PropertyModel<String>(message,
        // "message.exceptionsStackTrace"));

        WebMarkupContainer errorStackContent = new WebMarkupContainer("errorStackContent");
        errorStackContent.setMarkupId(errorStack.getMarkupId() + "_content");
        errorStackContainer.add(errorStackContent);

        errorStackContent.add(new MultiLineLabel("exceptionStack", new PropertyModel<String>(message,
                "message.exceptionsStackTrace")));
    }

    private boolean isExceptionVisible(final IModel<FeedbackMessage> message) {
        FeedbackMessage fMessage = message.getObject();
        if (!(fMessage.getMessage() instanceof OpResult)) {
            return false;
        }
        OpResult result = (OpResult) fMessage.getMessage();
        return StringUtils.isNotEmpty(result.getExceptionMessage())
                || StringUtils.isNotEmpty(result.getExceptionsStackTrace());

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
            case HANDLED_ERROR:
                return "messages-exp-content";
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
            case HANDLED_ERROR:
                resourceKey = "feedbackMessagePanel.message.expectedError";
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

    static String createMessageTooltip(final IModel<FeedbackMessage> model) {
        FeedbackMessage message = model.getObject();
        switch (message.getLevel()) {
            case FeedbackMessage.INFO:
                return "info";
            case FeedbackMessage.SUCCESS:
                return "success";
            case FeedbackMessage.ERROR:
                return "partialError";
            case FeedbackMessage.FATAL:
                return "fatalError";
            case FeedbackMessage.UNDEFINED:
                return "undefined";
            case FeedbackMessage.DEBUG:
                return "debug";
            case FeedbackMessage.WARNING:
            default:
                return "warn";
        }
    }

    public void export(final WebMarkupContainer content, final IModel<String> xml) {
        Label export = new Label("export", new LoadableModel<String>() {

            @Override
            protected String load() {
                return getString("feedbackMessagePanel.export");
            }
        });
        content.add(export);
        xml.setObject(xml.getObject().replace("\"", "'").replace("<", "&lt;").replace(">", "&gt;").replace(" ", "&nbsp;"));
        xml.setObject(xml.getObject().replace("\n", "<br>"));
        export.add(new AttributeAppender("onClick", "initXml(\"" + xml.getObject() + "\")"));

    }

//	private LoadableDetachableModel<File> createFileModel(final IModel<String> model) {
//		return new LoadableDetachableModel<File>() {
//			@Override
//			protected File load() {
//				File tempFile;
//				try {
//					MidPointApplication application = getMidpointApplication();
//					WebApplicationConfiguration config = application.getWebApplicationConfiguration();
//					File folder = new File(config.getImportFolder());
//
//					if (!folder.exists() || !folder.isDirectory()) {
//						folder.mkdir();
//					}
//					tempFile = new File(folder, "MessageXml.xml");
//
//					if (tempFile.exists()) {
//						tempFile.delete();
//					}
//
//					tempFile.createNewFile();
//					InputStream data = new ByteArrayInputStream("some data".getBytes());
//					Files.writeTo(tempFile, data);
//
//				} catch (IOException ex) {
//					throw new RuntimeException(ex);
//				}
//				getSession().setAttribute("exportingMessage", true);
//				return tempFile;
//			}
//		};
//	}
}
