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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.gui.api.component.captcha.CaptchaPanel;
import com.evolveum.midpoint.gui.api.component.password.PasswordPanel;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.DynamicFormPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NonceCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NonceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

public abstract class PageAbstractFlow extends PageRegistrationBase {

	private static final long serialVersionUID = 1L;
	
	private static final Trace LOGGER = TraceManager.getTrace(PageAbstractFlow.class);
	
	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_FIRST_NAME = "firstName";
	private static final String ID_LAST_NAME = "lastName";
	private static final String ID_EMAIL = "email";
	private static final String ID_PASSWORD = "password";
	private static final String ID_SUBMIT_REGISTRATION = "submitRegistration";
	private static final String ID_REGISTRATION_SUBMITED = "registrationInfo";
	private static final String ID_FEEDBACK = "feedback";
	private static final String ID_BACK = "back";
	
	private static final String ID_TOOLTIP = "tooltip";

	private static final String ID_DYNAMIC_FORM_PANEL = "registrationForm";
	private static final String ID_STATIC_FORM = "staticForm";
	private static final String ID_DYNAMIC_FORM = "dynamicForm";

	private static final String ID_CAPTCHA = "captcha";

	private static final String DOT_CLASS = PageAbstractFlow.class.getName() + ".";
	
	private static final String OPERATION_SAVE_USER = DOT_CLASS + "saveUser";
	private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";

	private static final String PARAM_USER_OID = "user";
	
	public abstract void initalizeModel();
	public abstract IModel<UserType> getUserModel();
	public abstract boolean isCustomFormDefined();
	protected abstract WebMarkupContainer initStaticLayout();
	protected abstract WebMarkupContainer initDynamicLayout();
	
	public PageAbstractFlow() {
		initalizeModel();
		initLayout();
	}

	private void initLayout() {

		
//		initAccessBehaviour(mainForm);
//		add(mainForm);

		Form<?> mainForm = new Form<>(ID_MAIN_FORM);
		add(mainForm);

		
		if (!isCustomFormDefined()) {
			Fragment staticForm = new Fragment("contentArea", "staticContent", this);
			staticForm.add(initStaticLayout());
			
			mainForm.add(staticForm);
		} else {
			Fragment dynamicForm = new Fragment("contentArea", "dynamicContent", this);
			dynamicForm.add(initDynamicLayout());
			mainForm.add(dynamicForm);
		}
		

		CaptchaPanel captcha = new CaptchaPanel(ID_CAPTCHA);
		captcha.setOutputMarkupId(true);
		mainForm.add(captcha);

		AjaxSubmitButton register = new AjaxSubmitButton(ID_SUBMIT_REGISTRATION, createStringResource("PageSelfRegistration.register")) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onError(AjaxRequestTarget target,
					org.apache.wicket.markup.html.form.Form<?> form) {
				showErrors(target);
			}

			protected void onSubmit(AjaxRequestTarget target,
					org.apache.wicket.markup.html.form.Form<?> form) {

				submitRegistration(target);

			}
		};

		mainForm.add(register);

		AjaxButton back = new AjaxButton(ID_BACK, createStringResource("PageSelfRegistration.back")) {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				setResponsePage(PageLogin.class);
			}
		};
		mainForm.add(back);
	}
	
	protected Form<?> getMainForm() {
		return (Form<?>) get(ID_MAIN_FORM);
	}
	
//	private WebMarkupContainer initStaticFormLayout() {
//		// feedback
//		final Form<?> mainForm = new Form<>(ID_MAIN_FORM);
//		FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK,
//				new ContainerFeedbackMessageFilter(PageAbstractFlow.this));
//		feedback.setOutputMarkupId(true);
//		mainForm.add(feedback);
//
//		WebMarkupContainer staticRegistrationForm = createMarkupContainer(ID_STATIC_FORM,
//				new VisibleEnableBehaviour() {
//
//					private static final long serialVersionUID = 1L;
//
//					@Override
//					public boolean isVisible() {
//						return !isCustomFormDefined();
////						return getSelfRegistrationConfiguration().getFormRef() == null;
//					}
//				}, mainForm);
//
//		TextPanel<String> firstName = new TextPanel<>(ID_FIRST_NAME,
//				new PropertyModel<String>(getUserModel(), UserType.F_GIVEN_NAME.getLocalPart() + ".orig") {
//
//					private static final long serialVersionUID = 1L;
//
//					@Override
//					public void setObject(String object) {
//						getUserModel().getObject().setGivenName(new PolyStringType(object));
//					}
//				});
//		initInputProperties(feedback, firstName);
//		staticRegistrationForm.add(firstName);
//
//		TextPanel<String> lastName = new TextPanel<>(ID_LAST_NAME,
//				new PropertyModel<String>(getUserModel(), UserType.F_FAMILY_NAME.getLocalPart() + ".orig") {
//
//					private static final long serialVersionUID = 1L;
//
//					@Override
//					public void setObject(String object) {
//						getUserModel().getObject().setFamilyName(new PolyStringType(object));
//					}
//
//				});
//		initInputProperties(feedback, lastName);
//		staticRegistrationForm.add(lastName);
//
//		TextPanel<String> email = new TextPanel<>(ID_EMAIL,
//				new PropertyModel<>(getUserModel(), UserType.F_EMAIL_ADDRESS.getLocalPart()));
//		initInputProperties(feedback, email);
//		staticRegistrationForm.add(email);
//
//		createPasswordPanel(staticRegistrationForm);
//		return staticRegistrationForm;
//	}
	
//	private WebMarkupContainer initDynamicFormLayout() {
//		final Form<?> mainForm = new Form<>(ID_MAIN_FORM);
//		WebMarkupContainer dynamicRegistrationForm = createMarkupContainer(ID_DYNAMIC_FORM,
//				new VisibleEnableBehaviour() {
//
//					private static final long serialVersionUID = 1L;
//
//					@Override
//					public boolean isVisible() {
//						return isCustomFormDefined();
////						return getSelfRegistrationConfiguration().getFormRef() != null;
//					}
//				}, mainForm);
//
//		DynamicFormPanel<UserType> dynamicForm = initCustomForm(mainForm);
//		
////		DynamicFormPanel<UserType> dynamicForm = runPrivileged(
////				() -> {
////					return createDynamicPanel(mainForm);
////				});
////
//		if (dynamicForm != null) {
//			dynamicRegistrationForm.add(dynamicForm);
//		}
//		return dynamicRegistrationForm;
//	}
	
	protected DynamicFormPanel<UserType> createDynamicPanel(Form<?> mainForm, Task task) {
		final ObjectReferenceType ort = getSelfRegistrationConfiguration().getFormRef();

		if (ort == null) {
			return null;
		}
		
		return new DynamicFormPanel<>(ID_DYNAMIC_FORM_PANEL,
				getUserModel(), ort.getOid(), mainForm, task, PageAbstractFlow.this, true);
	}
	
	
	private WebMarkupContainer createMarkupContainer(String id, VisibleEnableBehaviour visibleEnableBehaviour,
			Form<?> mainForm) {
		WebMarkupContainer formContainer = new WebMarkupContainer(id);
		formContainer.setOutputMarkupId(true);

		formContainer.add(visibleEnableBehaviour);

		mainForm.add(formContainer);
		return formContainer;
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
	
	private void submitRegistration(AjaxRequestTarget target) {

		if (!validateCaptcha(target)) {
			return;
		}

		OperationResult result = runPrivileged(new Producer<OperationResult>() {

			private static final long serialVersionUID = 1L;

			@Override
			public OperationResult run() {

				Task task = createAnonymousTask(OPERATION_SAVE_USER);
				task.setChannel(SchemaConstants.CHANNEL_GUI_SELF_REGISTRATION_URI);
				OperationResult result = new OperationResult(OPERATION_SAVE_USER);
				saveUser(task, result);
				result.computeStatus();
				return result;
			}

		});

		if (result.getStatus() == OperationResultStatus.SUCCESS) {
			getSession()
					.success(createStringResource("PageSelfRegistration.registration.success").getString());

			switch (getSelfRegistrationConfiguration().getAuthenticationMethod()) {
				case MAIL:
					target.add(PageAbstractFlow.this);
					break;
				case SMS:
					throw new UnsupportedOperationException();
				case NONE:
					setResponsePage(PageLogin.class);
			}
			LOGGER.trace("Registration for user {} was successfull.", getUserModel().getObject());

		} else {
			getSession().error(
					createStringResource("PageSelfRegistration.registration.error", result.getMessage())
							.getString());
			// removePassword(target);
			updateCaptcha(target);
			target.add(getFeedbackPanel());
			LOGGER.error("Failed to register user {}. Reason {}", getUserModel().getObject(), result.getMessage());
			return;

		}

		target.add(getFeedbackPanel());
		MultiLineLabel label = new MultiLineLabel(ID_REGISTRATION_SUBMITED,
				createStringResource("PageSelfRegistration.registration.confirm.message"));
		Fragment messageContent = new Fragment("contentArea", "messageContent", this);
		messageContent.add(label);
		replace(messageContent);
		target.add(this);

	}
	
	private void showErrors(AjaxRequestTarget target) {
		target.add(get(createComponentPath(ID_MAIN_FORM, ID_FEEDBACK)));
		target.add(getFeedbackPanel());
	}

	private boolean validateCaptcha(AjaxRequestTarget target) {
		CaptchaPanel captcha = getCaptcha();

		if (captcha.getRandomText() == null) {
			String message = createStringResource("PageSelfRegistration.captcha.validation.failed")
					.getString();
			LOGGER.error(message);
			getSession().error(message);
			target.add(getFeedbackPanel());
			updateCaptcha(target);
			return false;
		}

		if (captcha.getCaptchaText() != null && captcha.getRandomText() != null) {
			if (!captcha.getCaptchaText().equals(captcha.getRandomText())) {
				String message = createStringResource("PageSelfRegistration.captcha.validation.failed")
						.getString();
				LOGGER.error(message);
				getSession().error(message);
				updateCaptcha(target);
				target.add(getFeedbackPanel());
				return false;
			}
		}
		LOGGER.trace("CAPTCHA Validation OK");
		return true;
	}
	
	private CaptchaPanel getCaptcha() {
		return (CaptchaPanel) get(createComponentPath(ID_MAIN_FORM, ID_CAPTCHA));
	}
	
	private void updateCaptcha(AjaxRequestTarget target) {

		CaptchaPanel captcha = new CaptchaPanel(ID_CAPTCHA);
		captcha.setOutputMarkupId(true);

		Form<?> form = (Form<?>) get(ID_MAIN_FORM);
		form.addOrReplace(captcha);
		target.add(form);
	}

	private void saveUser(Task task, OperationResult result) {

		ObjectDelta<UserType> userDelta;
		try {
			userDelta = prepareUserDelta(task, result);
		} catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException | ConfigurationException | SecurityViolationException e) {
			result.recordFatalError("Failed to create delta for user: " + e.getMessage(), e);
			return;
		}
		userDelta.setPrismContext(getPrismContext());

		WebModelServiceUtils.save(userDelta, ModelExecuteOptions.createOverwrite(), result, task,
				PageAbstractFlow.this);
		result.computeStatus();

	}

	private ObjectDelta<UserType> prepareUserDelta(Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		if (getOidFromParams(getPageParameters()) == null) {
			LOGGER.trace("Preparing user ADD delta (new user registration)");
			UserType userType = prepareUserToSave(task, result);
			ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(userType.asPrismObject());
			LOGGER.trace("Going to register user {}", userDelta);
			return userDelta;
		} else {
			LOGGER.trace("Preparing user MODIFY delta (preregistered user registration)");
			ObjectDelta<UserType> delta = null;
			if (getSelfRegistrationConfiguration().getFormRef() == null) {
				delta = ObjectDelta.createEmptyModifyDelta(UserType.class,
						getOidFromParams(getPageParameters()), getPrismContext());
				if (getSelfRegistrationConfiguration().getInitialLifecycleState() != null) {
					delta.addModificationReplaceProperty(UserType.F_LIFECYCLE_STATE,
							getSelfRegistrationConfiguration().getInitialLifecycleState());
				}
				 delta.addModificationReplaceProperty(SchemaConstants.PATH_PASSWORD_VALUE,
				 createPassword().getValue());
			} else {
				delta = getDynamicFormPanel().getObjectDelta();
			}

			delta.addModificationReplaceContainer(SchemaConstants.PATH_NONCE,
					createNonce(getSelfRegistrationConfiguration().getNoncePolicy(), task, result)
							.asPrismContainerValue());
			LOGGER.trace("Going to register user with modifications {}", delta);
			return delta;

		}
	}

	private UserType prepareUserToSave(Task task, OperationResult result) throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

		SelfRegistrationDto selfRegistrationConfiguration = getSelfRegistrationConfiguration();
		UserType userType = getUserModel().getObject();
		UserType userToSave = userType.clone();

		if (selfRegistrationConfiguration.getFormRef() == null) {
			userType.clone();
			if (selfRegistrationConfiguration.getRequiredLifecycleState() != null) {
				String userLifecycle = userToSave.getLifecycleState();
				if (!selfRegistrationConfiguration.getRequiredLifecycleState().equals(userLifecycle)) {
					LOGGER.error(
							"Registration not allowed for a user {} -> Unsatisfied Configuration for required lifecycle, expected {} but was {}",
							new Object[] {
									userToSave.getEmailAddress() != null ? userToSave.getEmailAddress()
											: userToSave,
									selfRegistrationConfiguration.getRequiredLifecycleState(),
									userLifecycle });
					getSession().error(createStringResource(
							"PageSelfRegistration.registration.failed.unsatisfied.registration.configuration")
									.getString());
					throw new RestartResponseException(this);
				}

			}
		} else {

			try {
				userToSave = getDynamicFormPanel().getObject().asObjectable().clone();
			} catch (SchemaException e) {
				LoggingUtils.logException(LOGGER, "Failed to construct delta " + e.getMessage(), e);
				new RestartResponseException(this);
			}
		}

		// CredentialsType credentials =
		createCredentials(userToSave, selfRegistrationConfiguration.getNoncePolicy(), task, result);
		// userToSave.setCredentials(credentials);
		if (selfRegistrationConfiguration.getInitialLifecycleState() != null) {
			LOGGER.trace("Setting initial lifecycle state of registered user to {}",
					selfRegistrationConfiguration.getInitialLifecycleState());
			userToSave.setLifecycleState(selfRegistrationConfiguration.getInitialLifecycleState());
		}

		try {
			getPrismContext().adopt(userToSave);
		} catch (SchemaException e) {
			// nothing to do, try without it
		}

		return userToSave;

	}

	private DynamicFormPanel<UserType> getDynamicFormPanel() {
		return (DynamicFormPanel<UserType>) get(
				createComponentPath(ID_MAIN_FORM, ID_DYNAMIC_FORM, ID_DYNAMIC_FORM_PANEL));
	}

	private void createCredentials(UserType user, NonceCredentialsPolicyType noncePolicy, Task task,
			OperationResult result) throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		NonceType nonceType = createNonce(noncePolicy, task, result);

		// PasswordType password = createPassword();

		CredentialsType credentials = user.getCredentials();
		if (user.getCredentials() == null) {
			credentials = new CredentialsType();
			user.setCredentials(credentials);
		}

		credentials.setNonce(nonceType);
		// credentials.setPassword(password);
		// return credentials;

	}

	private NonceType createNonce(NonceCredentialsPolicyType noncePolicy, Task task, OperationResult result) throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		ProtectedStringType nonceCredentials = new ProtectedStringType();
		nonceCredentials.setClearValue(generateNonce(noncePolicy, null, task, result));

		NonceType nonceType = new NonceType();
		nonceType.setValue(nonceCredentials);
		return nonceType;
	}

	private PasswordType createPassword() {
		PasswordType password = new PasswordType();
		ProtectedStringType protectedString = new ProtectedStringType();
		protectedString.setClearValue(getPassword());
		password.setValue(protectedString);
		return password;
	}

	private <O extends ObjectType> String generateNonce(NonceCredentialsPolicyType noncePolicy,
			PrismObject<O> user, Task task, OperationResult result) throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		ValuePolicyType policy = null;

		if (noncePolicy != null && noncePolicy.getValuePolicyRef() != null) {
			PrismObject<ValuePolicyType> valuePolicy = WebModelServiceUtils.loadObject(ValuePolicyType.class,
					noncePolicy.getValuePolicyRef().getOid(), PageAbstractFlow.this, task, result);
			if (valuePolicy == null) {
				LOGGER.error("Nonce cannot be generated, as value policy {} cannot be fetched", noncePolicy.getValuePolicyRef().getOid());
				throw new ObjectNotFoundException("Nonce cannot be generated");         // no more information (security); TODO implement more correctly
			}
			policy = valuePolicy.asObjectable();
		}
		
		return getModelInteractionService().generateValue(policy,
				24, false, user, "nonce generation (registration)", task, result);
	}

	 private String getPassword() {
		 PasswordPanel password = (PasswordPanel)
				 get(createComponentPath(ID_MAIN_FORM, ID_STATIC_FORM, ID_PASSWORD));
		 return (String) password.getBaseFormComponent().getModel().getObject();
	 }

	// private void removePassword(AjaxRequestTarget target) {
	// PasswordPanel password = (PasswordPanel)
	// get(createComponentPath(ID_MAIN_FORM, ID_PASSWORD));
	// for (FormComponent comp : password.getFormComponents()) {
	// comp.getModel().setObject(null);
	// }
	// target.add(password);
	// }

	@Override
	protected void createBreadcrumb() {
		// don't create breadcrumb for registration page
	}
	
}
