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

package com.evolveum.midpoint.web.component.message;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessagesModel;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.component.result.OperationResultPanel;
import com.evolveum.midpoint.gui.api.component.result.ValidationErrorPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * @author lazyman
 * @author katkav
 */
public class FeedbackListView extends ListView<FeedbackMessage> {

	private static final long serialVersionUID = 1L;

	public FeedbackListView(String id, Component component) {
		super(id);
		setDefaultModel(new FeedbackMessagesModel(component));
	}

	@Override
	protected void populateItem(final ListItem<FeedbackMessage> item) {

		final FeedbackMessage message = item.getModelObject();

		if (message.getMessage() instanceof OpResult) {
			OperationResultPanel panel = new OperationResultPanel("message",
					new PropertyModel<OpResult>(item.getModel(), "message"), getPage()) {

				private static final long serialVersionUID = 1L;

				@Override
				public void close(AjaxRequestTarget target) {
					super.close(target);
					message.markRendered();
				}

				protected void onAfterRender() {
					((OpResult) message.getMessage()).setAlreadyShown(true);
					super.onAfterRender();
				};
			};
			panel.add(new VisibleEnableBehaviour() {

				private static final long serialVersionUID = 1L;

				public boolean isVisible() {
					return !((OpResult) item.getModelObject().getMessage()).isAlreadyShown();
				};
			});

			panel.setOutputMarkupId(true);
			item.add(panel);
		} else if (!(message.getMessage() instanceof OpResult)) {

			message.markRendered();
			ValidationErrorPanel validationPanel = new ValidationErrorPanel("message", item.getModel()) {

				private static final long serialVersionUID = 1L;

				@Override
				public void close(AjaxRequestTarget target) {
					super.close(target);
					message.markRendered();
				}

			};
			validationPanel.setOutputMarkupId(true);
			item.add(validationPanel);

		}
	}
}
