/*
 * Copyright (c) 2010-2017 Evolveum
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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
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
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * @author katkav
 */
public class OperationResultPanel extends BasePanel<OpResult> implements Popupable{
	private static final long serialVersionUID = 1L;

	private static final String ID_DETAILS_BOX = "detailsBox";
	private static final String ID_ICON_TYPE = "iconType";
	private static final String ID_MESSAGE = "message";
	private static final String ID_MESSAGE_LABEL = "messageLabel";
	private static final String ID_PARAMS = "params";
	private static final String ID_BACKGROUND_TASK = "backgroundTask";
	private static final String ID_SHOW_ALL = "showAll";
	private static final String ID_HIDE_ALL = "hideAll";
	private static final String ID_ERROR_STACK_TRACE = "errorStackTrace";

	static final String OPERATION_RESOURCE_KEY_PREFIX = "operation.";

	private static final Trace LOGGER = TraceManager.getTrace(OperationResultPanel.class);


	public OperationResultPanel(String id, IModel<OpResult> model, Page parentPage) {
		super(id, model);

		initLayout(parentPage);
	}

	public void initLayout(Page parentPage) {

		WebMarkupContainer detailsBox = new WebMarkupContainer(ID_DETAILS_BOX);
		detailsBox.setOutputMarkupId(true);
		detailsBox.add(AttributeModifier.append("class", createHeaderCss()));
		add(detailsBox);

		initHeader(detailsBox);
		initDetails(detailsBox, parentPage);
	}

	private void initHeader(WebMarkupContainer box) {
		WebMarkupContainer iconType = new WebMarkupContainer(ID_ICON_TYPE);
		iconType.setOutputMarkupId(true);
		iconType.add(new AttributeAppender("class", new AbstractReadOnlyModel<String>() {
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject() {
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
					case FATAL_ERROR:
						sb.append(" fa-ban");
						break;
					case PARTIAL_ERROR:
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

		AjaxLink<String> showMore = new AjaxLink<String>(ID_MESSAGE) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				OpResult result = OperationResultPanel.this.getModelObject();
				result.setShowMore(!result.isShowMore());
				result.setAlreadyShown(false);  // hack to be able to expand/collapse OpResult after rendered.
				target.add(OperationResultPanel.this);
			}
		};

		showMore.add(message);
		box.add(showMore);

		AjaxLink<String> backgroundTask = new AjaxLink<String>(ID_BACKGROUND_TASK) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				final OpResult opResult = OperationResultPanel.this.getModelObject();
				String oid = opResult.getBackgroundTaskOid();
				if (oid == null || !opResult.isBackgroundTaskVisible()) {
					return; // just for safety
				}
				ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(oid, ObjectTypes.TASK);
				WebComponentUtil.dispatchToObjectDetailsPage(ref, getPageBase(), false);
			}
		};
		backgroundTask.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return getModelObject().getBackgroundTaskOid() != null
						&& getModelObject().isBackgroundTaskVisible();
			}
		});
		box.add(backgroundTask);

		AjaxLink<String> showAll = new AjaxLink<String>(ID_SHOW_ALL) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				showHideAll(true, OperationResultPanel.this.getModelObject(), target);
			}
		};
		showAll.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return !OperationResultPanel.this.getModelObject().isShowMore();
			}
		});

		box.add(showAll);

		AjaxLink<String> hideAll = new AjaxLink<String>(ID_HIDE_ALL) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				showHideAll(false, OperationResultPanel.this.getModel().getObject(), target);
			}
		};
		hideAll.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return OperationResultPanel.this.getModelObject().isShowMore();
			}
		});

		box.add(hideAll);

		AjaxLink<String> close = new AjaxLink<String>("close") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				close(target);

			}
		};

		box.add(close);

		DownloadLink downloadXml = new DownloadLink("downloadXml", new AbstractReadOnlyModel<File>() {
			private static final long serialVersionUID = 1L;

			@Override
			public File getObject() {
				String home = getPageBase().getMidpointConfiguration().getMidpointHome();
				File f = new File(home, "result");
				DataOutputStream dos = null;
				try {
					dos = new DataOutputStream(new FileOutputStream(f));

					dos.writeBytes(OperationResultPanel.this.getModel().getObject().getXml());
				} catch (IOException e) {
					LOGGER.error("Could not download result: {}", e.getMessage(), e);
				} finally {
					IOUtils.closeQuietly(dos);
				}

				return f;
			}

		});
		downloadXml.setDeleteAfterDownload(true);
		box.add(downloadXml);
	}

	public void close(AjaxRequestTarget target) {
		this.setVisible(false);
		target.add(this);
	}

	private Label createMessage() {
		Label message = null;
		if (StringUtils.isNotBlank(getModelObject().getMessage())) {
			PropertyModel<String> messageModel = new PropertyModel<String>(getModel(), "message");
			message = new Label(ID_MESSAGE_LABEL, messageModel);
		} else {
			message = new Label(ID_MESSAGE_LABEL, new LoadableModel<Object>() {
				private static final long serialVersionUID = 1L;

				@Override
				protected Object load() {
					OpResult result = OperationResultPanel.this.getModelObject();
					String resourceKey = OPERATION_RESOURCE_KEY_PREFIX + result.getOperation();
					return getPage().getString(resourceKey, null, resourceKey);
				}
			});
		}

		//message.setRenderBodyOnly(true);
		message.setOutputMarkupId(true);
		return message;
	}

	private void initDetails(WebMarkupContainer box, Page parentPage) {

		final WebMarkupContainer details = new WebMarkupContainer("details", getModel());
		details.setOutputMarkupId(true);
		details.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return getModel().getObject().isShowMore();
			}
		});

		box.add(details);

		WebMarkupContainer operationPanel = new WebMarkupContainer("type");
		operationPanel.setOutputMarkupId(true);
		operationPanel.add(new AttributeAppender("class", new LoadableModel<String>() {
			private static final long serialVersionUID = 1L;

			@Override
			protected String load() {
				return getLabelCss(getModel());
			}
		}, " "));
		details.add(operationPanel);

		Label operationLabel = new Label("operationLabel",
				parentPage.getString("FeedbackAlertMessageDetails.operation"));
		operationLabel.setOutputMarkupId(true);
		operationPanel.add(operationLabel);

		Label operation = new Label("operation", new LoadableModel<Object>() {
			private static final long serialVersionUID = 1L;

			@Override
			protected Object load() {
				OpResult result = getModelObject();

				String resourceKey = OPERATION_RESOURCE_KEY_PREFIX + result.getOperation();
				return getPage().getString(resourceKey, null, resourceKey);
			}
		});
		operation.setOutputMarkupId(true);
		operationPanel.add(operation);

		Label count = new Label("countLabel", parentPage.getString("FeedbackAlertMessageDetails.count"));
		count.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				OpResult result = getModelObject();
				return result.getCount() > 1;
			}
		});
		operationPanel.add(count);
		operationPanel.add(initCountPanel(getModel()));

		Label message = new Label("resultMessage",
				new PropertyModel<String>(getModel(), "message").getObject());// PageBase.new
		// PropertyModel<String>(model,
		// "message"));
		message.setOutputMarkupId(true);

		message.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return StringUtils.isNotBlank(getModelObject().getMessage());
			}
		});

		operationPanel.add(message);

		Label messageLabel = new Label("messageLabel", parentPage.getString("FeedbackAlertMessageDetails.message"));
		messageLabel.setOutputMarkupId(true);

		messageLabel.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return StringUtils.isNotBlank(getModelObject().getMessage());
			}
		});

		operationPanel.add(messageLabel);

		initParams(operationPanel, getModel(), parentPage);
		initContexts(operationPanel, getModel(), parentPage);
		initError(operationPanel, getModel(), parentPage);
	}

	private void initParams(WebMarkupContainer operationContent, final IModel<OpResult> model, Page parentPage) {

		Label paramsLabel = new Label("paramsLabel", parentPage.getString("FeedbackAlertMessageDetails.params"));
		paramsLabel.setOutputMarkupId(true);
		paramsLabel.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return CollectionUtils.isNotEmpty(model.getObject().getParams());
			}
		});

		operationContent.add(paramsLabel);

		ListView<Param> params = new ListView<Param>(ID_PARAMS, createParamsModel(model)) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<Param> item) {
				item.add(new Label("paramName", new PropertyModel<Object>(item.getModel(), "name")));
				item.add(new Label("paramValue", new PropertyModel<Object>(item.getModel(), "value")));
			}
		};
		params.setOutputMarkupId(true);
		params.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return CollectionUtils.isNotEmpty(model.getObject().getParams());
			}
		});

		operationContent.add(params);

		ListView<OpResult> subresults = new ListView<OpResult>("subresults", createSubresultsModel(model)) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(final ListItem<OpResult> item) {
				Panel subresult = new OperationResultPanel("subresult", item.getModel(), getPage());
				subresult.setOutputMarkupId(true);
				item.add(subresult);
			}
		};
		subresults.setOutputMarkupId(true);
		subresults.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return CollectionUtils.isNotEmpty(model.getObject().getSubresults());
			}
		});

		operationContent.add(subresults);

	}

	private void initContexts(WebMarkupContainer operationContent, final IModel<OpResult> model, Page parentPage) {

		Label contextsLabel = new Label("contextsLabel", parentPage.getString("FeedbackAlertMessageDetails.contexts"));
		contextsLabel.setOutputMarkupId(true);
		contextsLabel.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return CollectionUtils.isNotEmpty(model.getObject().getContexts());
			}
		});

		operationContent.add(contextsLabel);

		ListView<Context> contexts = new ListView<Context>("contexts", createContextsModel(model)) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<Context> item) {
				item.add(new Label("contextName", new PropertyModel<Object>(item.getModel(), "name")));
				item.add(new Label("contextValue", new PropertyModel<Object>(item.getModel(), "value")));
			}
		};
		contexts.setOutputMarkupId(true);
		contexts.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return CollectionUtils.isNotEmpty(model.getObject().getContexts());
			}
		});
		operationContent.add(contexts);
	}

	private void initError(WebMarkupContainer operationPanel, final IModel<OpResult> model, Page parentPage) {
		Label errorLabel = new Label("errorLabel", parentPage.getString("FeedbackAlertMessageDetails.error"));
		errorLabel.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

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
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				// return true;
				return StringUtils.isNotBlank(model.getObject().getExceptionsStackTrace());

			}
		});
		errorMessage.setOutputMarkupId(true);
		operationPanel.add(errorMessage);

		final Label errorStackTrace = new Label(ID_ERROR_STACK_TRACE,
				new PropertyModel<String>(model, "exceptionsStackTrace"));
		errorStackTrace.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				// return true;
				return model.getObject().isShowError();

			}
		});
		errorStackTrace.setOutputMarkupId(true);
		operationPanel.add(errorStackTrace);

		AjaxLink errorStackTraceLink = new AjaxLink("errorStackTraceLink") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				OpResult result = OperationResultPanel.this.getModelObject();
				result.setShowError(!model.getObject().isShowError());
				result.setAlreadyShown(false);  // hack to be able to expand/collapse OpResult after rendered.
//				model.getObject().setShowError(!model.getObject().isShowError());
				target.add(OperationResultPanel.this);
			}

		};
		errorStackTraceLink.setOutputMarkupId(true);
		errorStackTraceLink.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

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
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				OpResult result = model.getObject();
				return result.getCount() > 1;
			}
		});
		return count;

	}

	private void showHideAll(final boolean show, OpResult opresult, AjaxRequestTarget target) {
		opresult.setShowMoreAll(show);
		opresult.setAlreadyShown(false);  // hack to be able to expand/collapse OpResult after rendered.
		target.add(OperationResultPanel.this);
	}

	private IModel<String> createHeaderCss() {

		return new AbstractReadOnlyModel<String>() {
			private static final long serialVersionUID = 1L;

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

						return " box-danger";
					case UNKNOWN:
					case PARTIAL_ERROR:
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
			private static final long serialVersionUID = 1L;

			@Override
			protected List<Param> load() {
				OpResult result = model.getObject();
				return result.getParams();
			}
		};
	}

	static IModel<List<Context>> createContextsModel(final IModel<OpResult> model) {
		return new LoadableModel<List<Context>>(false) {
			private static final long serialVersionUID = 1L;

			@Override
			protected List<Context> load() {
				OpResult result = model.getObject();
				return result.getContexts();
			}
		};
	}

	private IModel<List<OpResult>> createSubresultsModel(final IModel<OpResult> model) {
		return new LoadableModel<List<OpResult>>(false) {
			private static final long serialVersionUID = 1L;

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

				return "left-danger";
			case UNKNOWN:
			case PARTIAL_ERROR:
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

				return "fa-times-circle-o text-danger";
			case UNKNOWN:
			case PARTIAL_ERROR:
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

	@Override
	public int getWidth() {
		return 900;
	}

	@Override
	public int getHeight() {
		return 500;
	}

	@Override
	public StringResourceModel getTitle() {
		return new StringResourceModel("OperationResultPanel.result");
	}

	@Override
	public Component getComponent() {
		return this;
	}

}
