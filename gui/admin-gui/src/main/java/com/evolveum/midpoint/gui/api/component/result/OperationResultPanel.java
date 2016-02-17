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

package com.evolveum.midpoint.gui.api.component.result;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.LoadableModel;

/**
 * @author katkav
 */
public class OperationResultPanel extends BasePanel<OpResult> {

	private static final String ID_MESSAGE = "message";

	static final String OPERATION_RESOURCE_KEY_PREFIX = "operation.";

	private static final Trace LOGGER = TraceManager.getTrace(OperationResultPanel.class);

	public OperationResultPanel(String id, IModel<OpResult> model, boolean children) {
		super(id, model);

		initLayout();
	}

	protected void initLayout() {

		WebMarkupContainer detailsBox = new WebMarkupContainer("detailsBox");
		detailsBox.setOutputMarkupId(true);
		detailsBox.add(AttributeModifier.append("class", createHeaderCss()));
		add(detailsBox);

		initHeader(detailsBox);
		initDetails(detailsBox);
		// todo "show more" link only for operation result messages
	}

	private void initHeader(WebMarkupContainer box) {
		WebMarkupContainer iconType = new WebMarkupContainer("iconType");
		iconType.setOutputMarkupId(true);
		iconType.add(new AttributeAppender("class", new AbstractReadOnlyModel() {
			@Override
			public Object getObject() {
				StringBuilder sb = new StringBuilder();

				OpResult message = getModelObject();

				switch (message.getStatus()) {
				case IN_PROGRESS:
				case NOT_APPLICABLE:
					sb.append(" fa-info");
					break;
				case SUCCESS:
					sb.append(" fa-check");
					break;
				case PARTIAL_ERROR:
				case FATAL_ERROR:
					sb.append(" fa-ban");
					break;
				case UNKNOWN:
				case WARNING:
				case HANDLED_ERROR:
				default:
					sb.append(" fa-warning");
				}

				return sb.toString();
			}
		}));

		box.add(iconType);
		

		Label message = createMessage();

		AjaxLink showMore = new AjaxLink(ID_MESSAGE) {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				OpResult result = OperationResultPanel.this.getModel().getObject();
				result.setShowMore(!result.isShowMore());
				target.add(OperationResultPanel.this);
			}
		};

		showMore.add(message);
		box.add(showMore);

		AjaxLink showAll = new AjaxLink("showAll") {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				showHideAll(true, OperationResultPanel.this.getModel().getObject(), target);
			}
		};

		box.add(showAll);

		AjaxLink hideAll = new AjaxLink("hideAll") {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				showHideAll(false, OperationResultPanel.this.getModel().getObject(), target);
			}
		};

		box.add(hideAll);

		DownloadLink downloadXml = new DownloadLink("downloadXml", new AbstractReadOnlyModel<File>() {

			@Override
			public File getObject() {
				String home = getPageBase().getMidpointConfiguration().getMidpointHome();
				File f = new File(home, "result");
				DataOutputStream dos;
				try {
					dos = new DataOutputStream(new FileOutputStream(f));

					dos.writeBytes(OperationResultPanel.this.getModel().getObject().getXml());
				} catch (IOException e) {
					LOGGER.error("Could not download result: {}", e.getMessage(), e);
				}

				return f;
			}

		});
		downloadXml.setDeleteAfterDownload(true);
		box.add(downloadXml);
	}

	private Label createMessage() {
		Label message = null;
		if (StringUtils.isNotBlank(getModelObject().getMessage())) {
			PropertyModel<String> messageModel = new PropertyModel<String>(getModel(), "message");
			message = new Label("messageLabel", messageModel);
		} else {
			message = new Label("messageLabel", new LoadableModel<Object>() {

				@Override
				protected Object load() {
					OpResult result = OperationResultPanel.this.getModelObject();
					String resourceKey = OPERATION_RESOURCE_KEY_PREFIX + result.getOperation();
					return getPage().getString(resourceKey, null, resourceKey);
				}
			});
		}

		message.setRenderBodyOnly(true);
		message.setOutputMarkupId(true);
		return message;
	}

	private void initDetails(WebMarkupContainer box) {
		
		final WebMarkupContainer details = new WebMarkupContainer("details", getModel());
		details.setOutputMarkupId(true);
		details.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return getModel().getObject().isShowMore();
			}
		});

		box.add(details);
		
		WebMarkupContainer operationPanel = new WebMarkupContainer("type");
		operationPanel.setOutputMarkupId(true);
		operationPanel.add(new AttributeAppender("class", new LoadableModel<String>() {
			@Override
			protected String load() {
				return getLabelCss(getModel());
			}
		}, " "));
		details.add(operationPanel);

		Label operationLabel = new Label("operationLabel", getString("FeedbackAlertMessageDetails.operation"));
		operationLabel.setOutputMarkupId(true);
		operationPanel.add(operationLabel);

		Label operation = new Label("operation", new LoadableModel<Object>() {

			@Override
			protected Object load() {
				OpResult result = getModelObject();

				String resourceKey = OPERATION_RESOURCE_KEY_PREFIX + result.getOperation();
				return getPage().getString(resourceKey, null, resourceKey);
			}
		});
		operation.setOutputMarkupId(true);
		operationPanel.add(operation);

		Label count = new Label("countLabel", getString("FeedbackAlertMessageDetails.count"));
		count.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				OpResult result = getModelObject();
				return result.getCount() > 1;
			}
		});
		operationPanel.add(count);
		operationPanel.add(initCountPanel(getModel()));

		Label message = new Label("resultMessage", new PropertyModel<String>(getModel(), "message").getObject());// PageBase.new
																											// PropertyModel<String>(model,
																											// "message"));
		message.setOutputMarkupId(true);

		message.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return StringUtils.isNotBlank(getModelObject().getMessage());
			}
		});

		operationPanel.add(message);

		Label messageLabel = new Label("messageLabel", getString("FeedbackAlertMessageDetails.message"));
		messageLabel.setOutputMarkupId(true);

		messageLabel.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return StringUtils.isNotBlank(getModelObject().getMessage());
			}
		});

		operationPanel.add(messageLabel);

		initParams(operationPanel, getModel());
		initContexts(operationPanel, getModel());
		initError(operationPanel, getModel());
	}

	private void initParams(WebMarkupContainer operationContent, final IModel<OpResult> model) {

		Label paramsLabel = new Label("paramsLabel", getString("FeedbackAlertMessageDetails.params"));
		paramsLabel.setOutputMarkupId(true);
		paramsLabel.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return CollectionUtils.isNotEmpty(model.getObject().getParams());
			}
		});

		operationContent.add(paramsLabel);

		ListView<Param> params = new ListView<Param>("params", createParamsModel(model)) {

			@Override
			protected void populateItem(ListItem<Param> item) {
				item.add(new Label("paramName", new PropertyModel<Object>(item.getModel(), "name")));
				item.add(new Label("paramValue", new PropertyModel<Object>(item.getModel(), "value")));
			}
		};
		params.setOutputMarkupId(true);
		params.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return CollectionUtils.isNotEmpty(model.getObject().getParams());
			}
		});

		operationContent.add(params);

		ListView<OpResult> subresults = new ListView<OpResult>("subresults", createSubresultsModel(model)) {

			@Override
			protected void populateItem(final ListItem<OpResult> item) {
				Panel subresult = new OperationResultPanel("subresult", item.getModel(), true);
				subresult.setOutputMarkupId(true);
				item.add(subresult);
			}
		};
		subresults.setOutputMarkupId(true);
		subresults.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return CollectionUtils.isNotEmpty(model.getObject().getSubresults());
			}
		});

		operationContent.add(subresults);

	}

	private void initContexts(WebMarkupContainer operationContent, final IModel<OpResult> model) {

		Label contextsLabel = new Label("contextsLabel", getString("FeedbackAlertMessageDetails.contexts"));
		contextsLabel.setOutputMarkupId(true);
		contextsLabel.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return CollectionUtils.isNotEmpty(model.getObject().getContexts());
			}
		});

		operationContent.add(contextsLabel);

		ListView<Context> contexts = new ListView<Context>("contexts", createContextsModel(model)) {
			@Override
			protected void populateItem(ListItem<Context> item) {
				item.add(new Label("contextName", new PropertyModel<Object>(item.getModel(), "name")));
				item.add(new Label("contextValue", new PropertyModel<Object>(item.getModel(), "value")));
			}
		};
		contexts.setOutputMarkupId(true);
		contexts.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return CollectionUtils.isNotEmpty(model.getObject().getContexts());
			}
		});
		operationContent.add(contexts);
	}

	private void initError(WebMarkupContainer operationPanel, final IModel<OpResult> model) {
		Label errorLabel = new Label("errorLabel", getString("FeedbackAlertMessageDetails.error"));
		errorLabel.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				// return true;
				return StringUtils.isNotBlank(model.getObject().getExceptionsStackTrace());

			}
		});
		errorLabel.setOutputMarkupId(true);
		operationPanel.add(errorLabel);

		Label errorMessage = new Label("errorMessage", new PropertyModel<String>(model, "exceptionMessage"));
		errorMessage.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				// return true;
				return StringUtils.isNotBlank(model.getObject().getExceptionsStackTrace());

			}
		});
		errorMessage.setOutputMarkupId(true);
		operationPanel.add(errorMessage);

		final Label errorStackTrace = new Label("errorStackTrace",
				new PropertyModel<String>(model, "exceptionsStackTrace"));
		errorStackTrace.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				// return true;
				return model.getObject().isShowError();

			}
		});
		errorStackTrace.setOutputMarkupId(true);
		operationPanel.add(errorStackTrace);

		AjaxLink errorStackTraceLink = new AjaxLink("errorStackTraceLink") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				model.getObject().setShowError(!model.getObject().isShowError());
				target.add(OperationResultPanel.this);
			}

		};
		errorStackTraceLink.setOutputMarkupId(true);
		errorStackTraceLink.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return StringUtils.isNotBlank(model.getObject().getExceptionsStackTrace());

			}
		});
		operationPanel.add(errorStackTraceLink);

	}

	private Label initCountPanel(final IModel<OpResult> model) {
		Label count = new Label("count", new PropertyModel<String>(model, "count"));
		count.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				OpResult result = model.getObject();
				return result.getCount() > 1;
			}
		});
		return count;

	}

	private void showHideAll(final boolean show, OpResult object, AjaxRequestTarget target) {

		Visitor visitor = new Visitor() {

			@Override
			public void visit(Visitable visitable) {
				if (!(visitable instanceof OpResult)) {
					return;
				}

				OpResult result = (OpResult) visitable;
				result.setShowMore(show);

			}
		};

		object.accept(visitor);

		target.add(OperationResultPanel.this);

	}

	private IModel<String> createHeaderCss() {

		return new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				OpResult result = getModelObject();

				if (result == null || result.getStatus() == null) {
					return " box-warning";
				}

				switch (result.getStatus()) {
				case IN_PROGRESS:
				case NOT_APPLICABLE:
					return " box-info";
				case SUCCESS:
					return " box-success";
				case FATAL_ERROR:
				case PARTIAL_ERROR:
					return " box-danger";
				case UNKNOWN:
				case HANDLED_ERROR: // TODO:
				case WARNING:
				default:
					return " box-warning";
				}
			}

		};
	}

	static IModel<List<Param>> createParamsModel(final IModel<OpResult> model) {
		return new LoadableModel<List<Param>>(false) {

			@Override
			protected List<Param> load() {
				OpResult result = model.getObject();
				return result.getParams();
			}
		};
	}

	static IModel<List<Context>> createContextsModel(final IModel<OpResult> model) {
		return new LoadableModel<List<Context>>(false) {

			@Override
			protected List<Context> load() {
				OpResult result = model.getObject();
				return result.getContexts();
			}
		};
	}

	private IModel<List<OpResult>> createSubresultsModel(final IModel<OpResult> model) {
		return new LoadableModel<List<OpResult>>(false) {

			@Override
			protected List<OpResult> load() {
				OpResult result = model.getObject();
				List<OpResult> subresults = result.getSubresults();
				if (subresults == null) {
					subresults = new ArrayList<OpResult>();
				}

				return subresults;
			}
		};
	}

	private String getLabelCss(final IModel<OpResult> model) {
		OpResult result = model.getObject();

		if (result == null || result.getStatus() == null) {
			return "messages-warn-content";
		}

		switch (result.getStatus()) {
		case IN_PROGRESS:
		case NOT_APPLICABLE:
			return "left-info";
		case SUCCESS:
			return "left-success";
		case FATAL_ERROR:
		case PARTIAL_ERROR:
			return "left-danger";
		case UNKNOWN:
		case HANDLED_ERROR: // TODO:
		case WARNING:
		default:
			return "left-warning";
		}
	}

	private String getIconCss(final IModel<OpResult> model) {
		OpResult result = model.getObject();

		if (result == null || result.getStatus() == null) {
			return "fa-warning text-warning";
		}

		switch (result.getStatus()) {
		case IN_PROGRESS:
		case NOT_APPLICABLE:
			return "fa-info-circle  text-info";
		case SUCCESS:
			return "fa-check-circle-o text-success";
		case FATAL_ERROR:
		case PARTIAL_ERROR:
			return "fa-times-circle-o text-danger";
		case UNKNOWN:
		case HANDLED_ERROR: // TODO:
		case WARNING:
		default:
			return "fa-warning text-warning";
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

	
}
