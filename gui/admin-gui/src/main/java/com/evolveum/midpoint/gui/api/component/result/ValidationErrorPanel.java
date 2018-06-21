package com.evolveum.midpoint.gui.api.component.result;

import java.io.Serializable;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class ValidationErrorPanel extends BasePanel<FeedbackMessage> {

	private static final String ID_MESSAGE = "message";

	static final String OPERATION_RESOURCE_KEY_PREFIX = "operation.";

	private static final Trace LOGGER = TraceManager.getTrace(OperationResultPanel.class);

	public ValidationErrorPanel(String id, IModel<FeedbackMessage> model) {
		super(id, model);

		initLayout();
	}

	public void initLayout() {

		WebMarkupContainer detailsBox = new WebMarkupContainer("detailsBox");
		detailsBox.setOutputMarkupId(true);
		detailsBox.add(AttributeModifier.append("class", createHeaderCss()));
		add(detailsBox);

		initHeader(detailsBox);

	}

	private IModel<String> createHeaderCss() {

		return new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				FeedbackMessage result = getModelObject();

				if (result == null) {
					return " box-warning";
				}

				switch (result.getLevel()) {
					case FeedbackMessage.INFO:
					case FeedbackMessage.DEBUG:
						return " box-info";
					case FeedbackMessage.SUCCESS:
						return " box-success";
					case FeedbackMessage.ERROR:
					case FeedbackMessage.FATAL:
						return " box-danger";
					case FeedbackMessage.UNDEFINED:
					case FeedbackMessage.WARNING: // TODO:
					default:
						return " box-warning";
					}
				}

		};
	}

	private void initHeader(WebMarkupContainer box) {
		WebMarkupContainer iconType = new WebMarkupContainer("iconType");
		iconType.setOutputMarkupId(true);
		iconType.add(new AttributeAppender("class", new AbstractReadOnlyModel() {
			@Override
			public Object getObject() {

				FeedbackMessage result = getModelObject();

				if (result == null) {
					return " fa-info";
				}

				switch (result.getLevel()) {
					case FeedbackMessage.INFO:
					case FeedbackMessage.DEBUG:
						return " fa-info";
					case FeedbackMessage.SUCCESS:
						return " fa-check";
					case FeedbackMessage.ERROR:
					case FeedbackMessage.FATAL:
						return " fa-ban";
					case FeedbackMessage.UNDEFINED:
					case FeedbackMessage.WARNING: // TODO:
					default:
						return " fa-warning";
					}
				}

		}));

		box.add(iconType);

		Label message =  new Label(ID_MESSAGE, new PropertyModel<Serializable>(getModel(), "message"));
		box.add(message);

		AjaxLink close = new AjaxLink("close") {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				close(target);

			}
		};

		box.add(close);
	}

	public void close(AjaxRequestTarget target){
		this.setVisible(false);
		target.add(this);
	}
}
