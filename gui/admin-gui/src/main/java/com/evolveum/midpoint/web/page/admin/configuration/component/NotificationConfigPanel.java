/*
 * Copyright (c) 2010-2015 Evolveum
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
package com.evolveum.midpoint.web.page.admin.configuration.component;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.input.ChoiceableChoiceRenderer;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.dto.MailServerConfigurationTypeDto;
import com.evolveum.midpoint.web.page.admin.configuration.dto.NotificationConfigurationDto;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailTransportSecurityType;

/**
 *
 * @author katkav
 *
 */
public class NotificationConfigPanel extends SimplePanel<NotificationConfigurationDto> {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private static final String ID_DEFAULT_FROM = "defaultFrom";
	private static final String ID_DEBUG = "debugCheckbox";

	private static final String ID_MAIL_SERVER = "mailServer";
	private static final String ID_MAIL_SERVER_CONFIG_CONTAINER = "mailServerConfigContainer";
	private static final String ID_BUTTON_ADD_NEW_MAIL_SERVER_CONFIG = "addNewConfigButton";
	private static final String ID_BUTTON_REMOVE_MAIL_SERVER_CONFIG = "removeConfigButton";
	private static final String ID_MAIL_SERVER_TOOLTIP = "serverConfigTooltip";
	private static final String ID_HOST = "host";
	private static final String ID_PORT = "port";
	private static final String ID_USERNAME = "username";
	private static final String ID_PASSWORD = "password";
	private static final String ID_TRANSPORT_SECURITY = "transportSecurity";
	private static final String ID_REDIRECT_TO_FILE = "redirectToFile";

	private static final String ID_LABEL_SIZE = "col-lg-4";
	private static final String ID_INPUT_SIZE = "col-lg-4";

	public NotificationConfigPanel(String id, IModel<NotificationConfigurationDto> model) {
		super(id, model);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void initLayout() {
		TextField<String> defaultFromField = WebComponentUtil.createAjaxTextField(ID_DEFAULT_FROM,
				new PropertyModel<String>(getModel(), "defaultFrom"));

		CheckBox debugCheck = WebComponentUtil.createAjaxCheckBox(ID_DEBUG, new PropertyModel<Boolean>(getModel(), "debug"));

		DropDownChoice mailServerConfigChooser = new DropDownChoice<>(ID_MAIL_SERVER,
				new PropertyModel<MailServerConfigurationTypeDto>(getModel(),
						NotificationConfigurationDto.F_SELECTED_SERVER),
				new AbstractReadOnlyModel<List<MailServerConfigurationTypeDto>>() {

					@Override
					public List<MailServerConfigurationTypeDto> getObject() {
						return getModel().getObject().getServers();
					}
				}, new ChoiceableChoiceRenderer<MailServerConfigurationTypeDto>());
		mailServerConfigChooser.setNullValid(true);
		mailServerConfigChooser.add(new AjaxFormSubmitBehavior("click") {

			@Override
			protected void onEvent(AjaxRequestTarget target) {
				getForm().onFormSubmitted();
			}
		});
		mailServerConfigChooser.add(new OnChangeAjaxBehavior() {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				preparePasswordFieldPlaceholder();
				target.add(NotificationConfigPanel.this);
			}
		});
		add(mailServerConfigChooser);

		Label serverConfigTooltip = new Label(ID_MAIL_SERVER_TOOLTIP);
		serverConfigTooltip.add(new InfoTooltipBehavior());
		add(serverConfigTooltip);

		WebMarkupContainer serverConfigContainer = new WebMarkupContainer(ID_MAIL_SERVER_CONFIG_CONTAINER);
		serverConfigContainer.setOutputMarkupId(true);
		serverConfigContainer.setOutputMarkupPlaceholderTag(true);
		serverConfigContainer.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				if (getModelObject() != null) {
					return getModelObject().getSelectedServer() != null;
				}

				return false;
			}
		});
		add(serverConfigContainer);

		TextField<String> hostField = WebComponentUtil.createAjaxTextField(ID_HOST,
				new PropertyModel<String>(getModel(), "selectedServer.host"));
		TextField<Integer> portField = WebComponentUtil.createAjaxTextField(ID_PORT,
				new PropertyModel<Integer>(getModel(), "selectedServer.port"));
		TextField<String> userNameField = WebComponentUtil.createAjaxTextField(ID_USERNAME,
				new PropertyModel<String>(getModel(), "selectedServer.username"));
		PasswordTextField passwordField = new PasswordTextField(ID_PASSWORD,
				new PropertyModel<String>(getModel(), "selectedServer.password"));
		passwordField.setRequired(false);
		passwordField.add(new EmptyOnChangeAjaxFormUpdatingBehavior());

		TextField<String> redirectToFileField = WebComponentUtil.createAjaxTextField(ID_REDIRECT_TO_FILE,
				new PropertyModel<String>(getModel(), "redirectToFile"));

		DropDownFormGroup transportSecurity = new DropDownFormGroup<>(ID_TRANSPORT_SECURITY,
				new PropertyModel<MailTransportSecurityType>(getModel(), "selectedServer.mailTransportSecurityType"),
				WebComponentUtil.createReadonlyModelFromEnum(MailTransportSecurityType.class),
				new EnumChoiceRenderer<MailTransportSecurityType>(this),
				createStringResource("SystemConfigPanel.mail.transportSecurity"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
		// transportSecurity.add(new EmptyOnChangeAjaxFormUpdatingBehavior());

		serverConfigContainer.add(hostField);
		serverConfigContainer.add(portField);
		serverConfigContainer.add(userNameField);
		serverConfigContainer.add(passwordField);
		serverConfigContainer.add(transportSecurity);

		add(defaultFromField);
		add(debugCheck);
		add(redirectToFileField);

		AjaxSubmitLink buttonAddNewMailServerConfig = new AjaxSubmitLink(ID_BUTTON_ADD_NEW_MAIL_SERVER_CONFIG) {

			@Override
			protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
				MailServerConfigurationTypeDto newConfig = new MailServerConfigurationTypeDto();
				newConfig.setHost(getString("SystemConfigPanel.mail.config.placeholder"));

				if (getModelObject() != null) {
					getModelObject().getServers().add(newConfig);
					getModelObject().setSelectedServer(newConfig);
				}

				preparePasswordFieldPlaceholder();
				target.add(NotificationConfigPanel.this, getPageBase().getFeedbackPanel());
			}

			@Override
			protected void onError(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
				target.add(getPageBase().getFeedbackPanel());
			}
		};
		add(buttonAddNewMailServerConfig);

		AjaxSubmitLink removeMailServerConfig = new AjaxSubmitLink(ID_BUTTON_REMOVE_MAIL_SERVER_CONFIG) {

			@Override
			protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
				if (getModelObject() != null) {
					NotificationConfigurationDto notificationConfig = getModelObject();

					MailServerConfigurationTypeDto selected = notificationConfig.getSelectedServer();

					if (notificationConfig.getServers().contains(selected)) {
						notificationConfig.getServers().remove(selected);
						notificationConfig.setSelectedServer(null);
					} else {
						warn(getString("SystemConfigPanel.mail.server.remove.warn"));
					}

					target.add(NotificationConfigPanel.this, getPageBase().getFeedbackPanel());
				}
			}

			@Override
			protected void onError(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
				target.add(getPageBase().getFeedbackPanel());
			}
		};
		removeMailServerConfig.add(new AttributeAppender("class", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				if (getModelObject() != null && getModelObject().getSelectedServer() != null) {
					return null;
				} else {
					return " disabled";
				}
			}
		}));
		add(removeMailServerConfig);

	}

	private void preparePasswordFieldPlaceholder() {
		PasswordTextField passwordField = (PasswordTextField) get(ID_MAIL_SERVER_CONFIG_CONTAINER + ":" + ID_PASSWORD);

		if (getModelObject() != null) {
			if (getModelObject().getSelectedServer() != null
					&& getModelObject().getSelectedServer().getPassword() != null) {

				passwordField.add(new AttributeModifier("placeholder",
						createStringResource("SystemConfigPanel.mail.password.placeholder.set")));
			} else {
				passwordField.add(new AttributeModifier("placeholder",
						createStringResource("SystemConfigPanel.mail.password.placeholder.empty")));
			}
		}
	}

}
