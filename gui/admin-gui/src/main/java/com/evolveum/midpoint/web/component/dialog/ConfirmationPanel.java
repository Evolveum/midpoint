/*
 * Copyright (c) 2010-2017 Evolveum
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.evolveum.midpoint.web.component.dialog;

import com.evolveum.midpoint.web.component.AjaxButton;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

/**
 * Created by Honchar.
 *
 * class is created based on the ConfirmationDialog. ConfirmationPanel panel is
 * to be added to main popup (from PageBase class) as a content
 *
 */
public class ConfirmationPanel extends Panel implements Popupable {

	private static final long serialVersionUID = 1L;
	private static final String ID_PANEL = "panel";
	private static final String ID_CONFIRM_TEXT = "confirmText";
	private static final String ID_YES = "yes";
	private static final String ID_NO = "no";

	private int confirmType;

	public ConfirmationPanel(String id) {
		this(id, null);
	}

	public ConfirmationPanel(String id, IModel<String> message) {
		super(id);

		if (message == null) {
			message = new Model<>();
		}
		initLayout(message);
	}

	public boolean getLabelEscapeModelStrings() {
		return true;
	}

	public void setMessage(IModel<String> message) {
		Label label = (Label) get(ID_PANEL).get(ID_CONFIRM_TEXT);
		label.setDefaultModel(message);
	}

	private void initLayout(IModel<String> message) {
		WebMarkupContainer panel = new WebMarkupContainer(ID_PANEL);

		Label label = new Label(ID_CONFIRM_TEXT, message);
		label.setEscapeModelStrings(getLabelEscapeModelStrings());
		panel.add(label);

		AjaxButton yesButton = new AjaxButton(ID_YES,
				new StringResourceModel("confirmationDialog.yes", this, null)) {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				yesPerformed(target);
			}
		};
		panel.add(yesButton);

		AjaxButton noButton = new AjaxButton(ID_NO,
				new StringResourceModel("confirmationDialog.no", this, null)) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				noPerformed(target);
			}
		};
		panel.add(noButton);
		add(panel);
	}

	public void yesPerformed(AjaxRequestTarget target) {

	}

	public void noPerformed(AjaxRequestTarget target) {
		ModalWindow modalWindow = findParent(ModalWindow.class);
		if (modalWindow != null) {
			modalWindow.close(target);
		}
	}

	/**
	 * @return confirmation type identifier
	 */
	public int getConfirmType() {
		return confirmType;
	}

	/**
	 * This method provides solution for reusing one confirmation dialog for
	 * more messages/actions by using confirmType identifier. See for example
	 * {@link com.evolveum.midpoint.web.page.admin.users.component.TreeTablePanel}
	 *
	 * @param confirmType
	 */
	public void setConfirmType(int confirmType) {
		this.confirmType = confirmType;
	}

	@Override
	public int getWidth() {
		return 350;
	}

	@Override
	public int getHeight() {
		return 150;
	}

	@Override
	public StringResourceModel getTitle() {
		return new StringResourceModel("AssignmentTablePanel.modal.title.confirmDeletion");
	}

	@Override
	public Component getComponent() {
		return this;
	}

}
