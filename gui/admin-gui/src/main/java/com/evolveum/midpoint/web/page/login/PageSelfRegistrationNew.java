/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.web.page.login;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.gui.api.component.password.PasswordPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.DynamicFormPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;


@PageDescriptor(urls = {@Url(mountUrl = "/registration")}, permitAll = true)
public class PageSelfRegistrationNew extends PageAbstractFlow {

	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(PageSelfRegistration.class);

	private static final String DOT_CLASS = PageSelfRegistration.class.getName() + ".";
	
	private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";
	
	private static final String ID_WELCOME = "welcome";
	private static final String ID_ADDITIONAL_TEXT = "additionalText";
	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_FIRST_NAME = "firstName";
	private static final String ID_LAST_NAME = "lastName";
	private static final String ID_EMAIL = "email";
	private static final String ID_PASSWORD = "password";
	private static final String ID_TOOLTIP = "tooltip";
	private static final String ID_FEEDBACK = "feedback";
	
	private static final String ID_STATIC_FORM = "staticForm";
	private static final String ID_DYNAMIC_FORM = "dynamicForm";
	
	private static final String PARAM_USER_OID = "user";

	
	private IModel<UserType> userModel;
	
	private PageParameters pageParameters;
	
	public PageSelfRegistrationNew(PageParameters pageParameters) {
		this.pageParameters = pageParameters;
		initalizeModel();
	}
	
	private String getOidFromParams(PageParameters pageParameters) {
		if (pageParameters == null) {
			return null;
		}

		StringValue oidValue = pageParameters.get(PARAM_USER_OID);
		if (oidValue != null) {
			return oidValue.toString();
		}

		return null;
	}
	
	@Override
	public void initalizeModel() {
		final String userOid = getOidFromParams(pageParameters);

		userModel = new LoadableModel<UserType>(false) {
			private static final long serialVersionUID = 1L;

			@Override
			protected UserType load() {
				return createUserModel(userOid);
			}
		};
	}
	
	private UserType createUserModel(String userOid) {
		if (userOid == null) {
			LOGGER.trace("Registration process for new user started");
			return instantiateUser();
		}

		PrismObject<UserType> result = runPrivileged(new Producer<PrismObject<UserType>>() {

			private static final long serialVersionUID = 1L;

			@Override
			public PrismObject<UserType> run() {
				LOGGER.trace("Loading preregistered user with oid {}.", userOid);
				Task task = createAnonymousTask(OPERATION_LOAD_USER);
				OperationResult result = new OperationResult(OPERATION_LOAD_USER);
				PrismObject<UserType> user = WebModelServiceUtils.loadObject(UserType.class, userOid,
						PageSelfRegistrationNew.this, task, result);
				result.computeStatus();
				return user;
			}

		});

		if (result == null) {
			LOGGER.error("Failed to load preregistered user");
			getSession().error(
					createStringResource("PageSelfRegistration.invalid.registration.link").getString());
			throw new RestartResponseException(PageLogin.class);
		}

		return result.asObjectable();
	}
	
	private UserType instantiateUser() {
		PrismObjectDefinition<UserType> userDef = getUserDefinition();
		PrismObject<UserType> user;
		try {
			user = userDef.instantiate();
		} catch (SchemaException e) {
			UserType userType = new UserType();
			user = userType.asPrismObject();

		}
		return user.asObjectable();
	}

	private PrismObjectDefinition<UserType> getUserDefinition() {
		return getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
	}

	
	@Override
	protected WebMarkupContainer initStaticLayout() {
		// feedback
//		final Form<?> mainForm = new Form<>(ID_MAIN_FORM);
		
		WebMarkupContainer staticRegistrationForm = createMarkupContainer(ID_STATIC_FORM, null);
		
		addMultilineLable(ID_WELCOME, "PageSelfRegistration.welcome.message", staticRegistrationForm);
		addMultilineLable(ID_ADDITIONAL_TEXT, "PageSelfRegistration.additional.message", staticRegistrationForm);
		
		FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK,
				new ContainerFeedbackMessageFilter(PageSelfRegistrationNew.this));
		feedback.setOutputMarkupId(true);
		add(feedback);

		

		TextPanel<String> firstName = new TextPanel<>(ID_FIRST_NAME,
				new PropertyModel<String>(getUserModel(), UserType.F_GIVEN_NAME.getLocalPart() + ".orig") {

					private static final long serialVersionUID = 1L;

					@Override
					public void setObject(String object) {
						getUserModel().getObject().setGivenName(new PolyStringType(object));
					}
				});
		initInputProperties(feedback, firstName);
		staticRegistrationForm.add(firstName);

		TextPanel<String> lastName = new TextPanel<>(ID_LAST_NAME,
				new PropertyModel<String>(getUserModel(), UserType.F_FAMILY_NAME.getLocalPart() + ".orig") {

					private static final long serialVersionUID = 1L;

					@Override
					public void setObject(String object) {
						getUserModel().getObject().setFamilyName(new PolyStringType(object));
					}

				});
		initInputProperties(feedback, lastName);
		staticRegistrationForm.add(lastName);

		TextPanel<String> email = new TextPanel<>(ID_EMAIL,
				new PropertyModel<>(getUserModel(), UserType.F_EMAIL_ADDRESS.getLocalPart()));
		initInputProperties(feedback, email);
		staticRegistrationForm.add(email);

		createPasswordPanel(staticRegistrationForm);
		return staticRegistrationForm;
	}
	
	private void addMultilineLable(String id, String messageKey, WebMarkupContainer mainForm) {
		MultiLineLabel welcome = new MultiLineLabel(id, createStringResource(messageKey));
		welcome.setOutputMarkupId(true);
//		welcome.add(new VisibleEnableBehaviour() {
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public boolean isVisible() {
//				return !submited;
//			}
//		});
		mainForm.add(welcome);

	}
	
	private void initInputProperties(FeedbackPanel feedback, TextPanel<String> input) {
		input.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		input.getBaseFormComponent().setRequired(true);
		feedback.setFilter(new ContainerFeedbackMessageFilter(input.getBaseFormComponent()));

		input.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isEnabled() {
				return getOidFromParams(getPageParameters()) == null;
			}

		});

	}
	
	private void createPasswordPanel(WebMarkupContainer staticRegistrationForm) {
		// ProtectedStringType initialPassword = null;
		PasswordPanel password = new PasswordPanel(ID_PASSWORD,
            new PropertyModel<>(getUserModel(), "credentials.password.value"), false, true);
		password.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		password.getBaseFormComponent().setRequired(true);
		staticRegistrationForm.add(password);

		Label help = new Label(ID_TOOLTIP);
		final StringResourceModel tooltipText = createStringResource("PageSelfRegistration.password.policy");
		help.add(AttributeModifier.replace("title", tooltipText));
		help.add(new InfoTooltipBehavior());
		help.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {

				return StringUtils.isNotEmpty(tooltipText.getObject());
			}
		});
		staticRegistrationForm.add(help);
	}
	
	
	@Override
	protected WebMarkupContainer initDynamicLayout() {
//		final Form<?> mainForm = new Form<>(ID_MAIN_FORM);
		WebMarkupContainer dynamicRegistrationForm = createMarkupContainer(ID_DYNAMIC_FORM, getMainForm());
//				new VisibleEnableBehaviour() {
//
//					private static final long serialVersionUID = 1L;
//
//					@Override
//					public boolean isVisible() {
//						return isCustomFormDefined();
//					}
//				}, mainForm);
//
		
		DynamicFormPanel<UserType> dynamicForm = runPrivileged(
				() -> {
					Task task = createAnonymousTask(OPERATION_LOAD_DYNAMIC_FORM);
					return createDynamicPanel(getMainForm(), task);
				});

		if (dynamicForm != null) {
			dynamicRegistrationForm.add(dynamicForm);
		}
		
		return dynamicRegistrationForm;
	}

	private WebMarkupContainer createMarkupContainer(String id, Form<?> mainForm) {
		WebMarkupContainer formContainer = new WebMarkupContainer(id);
		formContainer.setOutputMarkupId(true);

		add(formContainer);
		return formContainer;
	}
	
	@Override
	public IModel<UserType> getUserModel() {
		return userModel;
	}

	@Override
	public boolean isCustomFormDefined() {
		return getSelfRegistrationConfiguration().getFormRef() != null;
	}
}
