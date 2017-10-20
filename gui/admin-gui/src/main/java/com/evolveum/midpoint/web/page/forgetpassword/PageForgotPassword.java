/*
 * Copyright (c) 2012-2017 Biznet, Evolveum
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
package com.evolveum.midpoint.web.page.forgetpassword;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.validation.validator.RfcCompliantEmailAddressValidator;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.SearchResultList;
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
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.prism.DynamicFormPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.forgetpassword.ResetPolicyDto.ResetMethod;
import com.evolveum.midpoint.web.page.login.PageLogin;
import com.evolveum.midpoint.web.page.login.PageRegistrationBase;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NonceCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NonceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

@PageDescriptor(url = "/forgotpassword")
public class PageForgotPassword extends PageRegistrationBase {
	private static final long serialVersionUID = 1L;

	private static final String ID_PWDRESETFORM = "pwdresetform";
	private static final String ID_USERNAME_CONTAINER = "usernameContainer";
	private static final String ID_USERNAME = "username";
	private static final String ID_EMAIL_CONTAINER = "emailContainer";
	private static final String ID_EMAIL = "email";
	private static final String ID_SUBMIT = "submitButton";
	private static final String ID_BACK = "back";

	private static final String ID_STATIC_LAYOUT = "staticLayout";
	private static final String ID_DYNAMIC_LAYOUT = "dynamicLayout";

	private static final String ID_DYNAMIC_FORM = "dynamicForm";

	private static final String DOT_CLASS = PageForgotPassword.class.getName() + ".";
	protected static final String OPERATION_LOAD_RESET_PASSWORD_POLICY = DOT_CLASS
			+ "loadPasswordResetPolicy";
	private static final String ID_PASSWORD_RESET_SUBMITED = "resetPasswordInfo";
	private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";

	private static final Trace LOGGER = TraceManager.getTrace(PageForgotPassword.class);

	public PageForgotPassword() {
		super();
		initLayout();
	}

	private boolean submited;

	@Override
	protected void createBreadcrumb() {
		// don't create breadcrumb for this page
	}

	private void initLayout() {
		Form<?> form = new Form(ID_PWDRESETFORM);
		form.setOutputMarkupId(true);
		form.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return !submited;
			}

		});

		initStaticLayout(form);

		initDynamicLayout(form);

		initButtons(form);

	}

	private void initStaticLayout(Form<?> mainForm) {
		WebMarkupContainer staticLayout = new WebMarkupContainer(ID_STATIC_LAYOUT);
		staticLayout.setOutputMarkupId(true);
		mainForm.add(staticLayout);

		staticLayout.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return !isDynamicForm();
			}
		});

		WebMarkupContainer userNameContainer = new WebMarkupContainer(ID_USERNAME_CONTAINER);
		userNameContainer.setOutputMarkupId(true);
		staticLayout.add(userNameContainer);

		RequiredTextField<String> userName = new RequiredTextField<String>(ID_USERNAME, new Model<String>());
		userName.setOutputMarkupId(true);
		userNameContainer.add(userName);
		userNameContainer.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			public boolean isVisible() {
				return getResetPasswordPolicy().getResetMethod() == ResetMethod.SECURITY_QUESTIONS;
			};
		});

		WebMarkupContainer emailContainer = new WebMarkupContainer(ID_EMAIL_CONTAINER);
		emailContainer.setOutputMarkupId(true);
		staticLayout.add(emailContainer);
		RequiredTextField<String> email = new RequiredTextField<String>(ID_EMAIL, new Model<String>());
		email.add(RfcCompliantEmailAddressValidator.getInstance());
		email.setOutputMarkupId(true);
		emailContainer.add(email);
		emailContainer.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			public boolean isVisible() {
				ResetMethod resetMethod = getResetPasswordPolicy().getResetMethod();
				return resetMethod == ResetMethod.SECURITY_QUESTIONS || resetMethod == ResetMethod.MAIL;
			};
		});

	}

	private boolean isDynamicForm() {
		return getResetPasswordPolicy().getFormRef() != null;
	}

	private void initDynamicLayout(final Form<?> mainForm) {
		WebMarkupContainer dynamicLayout = new WebMarkupContainer(ID_DYNAMIC_LAYOUT);
		dynamicLayout.setOutputMarkupId(true);
		mainForm.add(dynamicLayout);

		dynamicLayout.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return isDynamicForm();
			}
		});

		DynamicFormPanel<UserType> searchAttributesForm = runPrivileged(
				() -> {
					ObjectReferenceType formRef = getResetPasswordPolicy().getFormRef();
					if (formRef == null) {
						return null;
					}
					Task task = createAnonymousTask(OPERATION_LOAD_DYNAMIC_FORM);
					return new DynamicFormPanel<UserType>(ID_DYNAMIC_FORM, UserType.COMPLEX_TYPE,
							formRef.getOid(), mainForm, task, PageForgotPassword.this, true);
				});

		if (searchAttributesForm != null) {
			dynamicLayout.add(searchAttributesForm);
		}

	}

	private void initButtons(Form<?> form) {
		AjaxSubmitButton submit = new AjaxSubmitButton(ID_SUBMIT) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				processResetPassword(target, form);
			}

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				target.add(getFeedbackPanel());
			}

		};
		submit.setOutputMarkupId(true);
		form.add(submit);

		AjaxButton backButton = new AjaxButton(ID_BACK) {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				setResponsePage(PageLogin.class);
			}
		};
		backButton.setOutputMarkupId(true);
		form.add(backButton);

		add(form);

		MultiLineLabel label = new MultiLineLabel(ID_PASSWORD_RESET_SUBMITED,
				createStringResource("PageForgotPassword.form.submited.message"));
		add(label);
		label.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return submited;
			}

			@Override
			public boolean isEnabled() {
				return submited;
			}

		});
	}

	private void processResetPassword(AjaxRequestTarget target, Form<?> form) {

		UserType user = searchUser(form);

		if (user == null) {
			getSession().error(getString("pageForgetPassword.message.user.not.found"));
			throw new RestartResponseException(PageForgotPassword.class);
		}
		LOGGER.trace("Reset Password user: {}", user);

		if (getResetPasswordPolicy() == null) {
			LOGGER.debug("No policies for reset password defined");
			getSession().error(getString("pageForgetPassword.message.policy.not.found"));
			throw new RestartResponseException(PageForgotPassword.class);
		}

		switch (getResetPasswordPolicy().getResetMethod()) {
			case MAIL:
				OperationResult result = saveUserNonce(user, getResetPasswordPolicy().getNoncePolicy());
				if (result.getStatus() == OperationResultStatus.SUCCESS) {
					submited = true;
					target.add(PageForgotPassword.this);
				} else {
					getSession().error(getString("PageForgotPassword.send.nonce.failed"));
					LOGGER.error("Failed to sent none to user: {} ", result.getMessage());
					throw new RestartResponseException(PageForgotPassword.this);
				}

				break;
			case SECURITY_QUESTIONS:
				LOGGER.trace("Forward to PageSecurityQuestions");
				PageParameters params = new PageParameters();
				params.add(PageSecurityQuestions.SESSION_ATTRIBUTE_POID, user.getOid());
				setResponsePage(PageSecurityQuestions.class, params);
				break;
			default:
				getSession().error(getString("pageForgetPassword.message.reset.method.not.supported"));
				LOGGER.error("Reset method {} not supported.", getResetPasswordPolicy().getResetMethod());
				throw new RestartResponseException(PageForgotPassword.this);
		}

	}

	private UserType searchUser(Form form) {
		ObjectQuery query = null;

		if (isDynamicForm()) {
			query = createDynamicFormQuery(form);
		} else {
			query = createStaticFormQuery(form);
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Searching for user with query:\n{}", query.debugDump(1));
		}

		return searchUserPrivileged(query);

	}

	private ObjectQuery createDynamicFormQuery(Form form) {
		DynamicFormPanel<UserType> userDynamicPanel = (DynamicFormPanel<UserType>) form
				.get(createComponentPath(ID_DYNAMIC_LAYOUT, ID_DYNAMIC_FORM));
		List<ItemPath> filledItems = userDynamicPanel.getChangedItems();
		PrismObject<UserType> user;
		try {
			user = userDynamicPanel.getObject();
		} catch (SchemaException e1) {
			getSession().error(getString("pageForgetPassword.message.usernotfound"));
			throw new RestartResponseException(PageForgotPassword.class);
		}

		List<EqualFilter> filters = new ArrayList<>();
		for (ItemPath path : filledItems) {
			PrismProperty property = user.findProperty(path);
			EqualFilter filter = EqualFilter.createEqual(path, property.getDefinition(), null);
			filter.setValue(property.getAnyValue().clone());
			filters.add(filter);
		}
		return ObjectQuery.createObjectQuery(AndFilter.createAnd((List) filters));
	}

	private ObjectQuery createStaticFormQuery(Form form) {
		RequiredTextField<String> usernameTextFiled = (RequiredTextField) form
				.get(createComponentPath(ID_STATIC_LAYOUT, ID_USERNAME_CONTAINER, ID_USERNAME));
		RequiredTextField<String> emailTextField = (RequiredTextField) form
				.get(createComponentPath(ID_STATIC_LAYOUT, ID_EMAIL_CONTAINER, ID_EMAIL));
		String username = usernameTextFiled != null ? usernameTextFiled.getModelObject() : null;
		String email = emailTextField != null ? emailTextField.getModelObject() : null;
		LOGGER.debug("Reset Password user info form submitted. username={}, email={}", username, email);

		ResetPolicyDto resetPasswordPolicy = getResetPasswordPolicy();
		if (resetPasswordPolicy == null) {
			passwordResetNotSupported();
		}
		ResetMethod method = resetPasswordPolicy.getResetMethod();
		if (method == null) {
			passwordResetNotSupported();
		}

		switch (method) {
			case MAIL:
				return QueryBuilder.queryFor(UserType.class, getPrismContext()).item(UserType.F_EMAIL_ADDRESS)
						.eq(email).matchingCaseIgnore().build();

			case SECURITY_QUESTIONS:
				return QueryBuilder.queryFor(UserType.class, getPrismContext()).item(UserType.F_NAME)
						.eqPoly(username).matchingNorm().and().item(UserType.F_EMAIL_ADDRESS).eq(email)
						.matchingCaseIgnore().build();

			default:
				passwordResetNotSupported();
				return null; // not reached
		}

	}

	private void passwordResetNotSupported() {
		getSession().error(getString("PageForgotPassword.unsupported.reset.type"));
		throw new RestartResponseException(PageForgotPassword.this);
	}

	private UserType searchUserPrivileged(ObjectQuery query) {
		UserType userType = runPrivileged(new Producer<UserType>() {

			@Override
			public UserType run() {

				Task task = createAnonymousTask("load user");
				OperationResult result = new OperationResult("search user");

				SearchResultList<PrismObject<UserType>> users;
				try {
					users = getModelService().searchObjects(UserType.class, query, null, task, result);
				} catch (SchemaException | ObjectNotFoundException | SecurityViolationException
						| CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
					LoggingUtils.logException(LOGGER, "failed to search user", e);
					return null;
				}

				if ((users == null) || (users.isEmpty())) {
					LOGGER.trace("Empty user list in ForgetPassword");
					return null;
				}

				if (users.size() > 1) {
					LOGGER.trace("Problem while seeking for user");
					return null;
				}

				UserType user = users.iterator().next().asObjectable();
				LOGGER.trace("User found for ForgetPassword: {}", user);

				return user;
			}

		});
		return userType;
	}

	private OperationResult saveUserNonce(final UserType user, final NonceCredentialsPolicyType noncePolicy) {
		return runPrivileged(new Producer<OperationResult>() {

			@Override
			public OperationResult run() {
				Task task = createAnonymousTask("generateUserNonce");
				task.setChannel(SchemaConstants.CHANNEL_GUI_RESET_PASSWORD_URI);
				task.setOwner(user.asPrismObject());
				OperationResult result = new OperationResult("generateUserNonce");
				ProtectedStringType nonceCredentials = new ProtectedStringType();
				try {
					nonceCredentials
							.setClearValue(generateNonce(noncePolicy, task, user.asPrismObject(), result));

					NonceType nonceType = new NonceType();
					nonceType.setValue(nonceCredentials);

					ObjectDelta<UserType> nonceDelta;

					nonceDelta = ObjectDelta.createModificationReplaceContainer(UserType.class, user.getOid(),
							SchemaConstants.PATH_NONCE, getPrismContext(), nonceType);

					WebModelServiceUtils.save(nonceDelta, result, task, PageForgotPassword.this);
				} catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException | ConfigurationException | SecurityViolationException e) {
					result.recordFatalError("Failed to generate nonce for user");
					LoggingUtils.logException(LOGGER, "Failed to generate nonce for user: " + e.getMessage(),
							e);
				}

				result.computeStatusIfUnknown();
				return result;
			}

		});
	}

	private <O extends ObjectType> String generateNonce(NonceCredentialsPolicyType noncePolicy, Task task,
			PrismObject<O> user, OperationResult result)
					throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		ValuePolicyType policy = null;

		if (noncePolicy != null && noncePolicy.getValuePolicyRef() != null) {
			PrismObject<ValuePolicyType> valuePolicy = WebModelServiceUtils.loadObject(ValuePolicyType.class,
					noncePolicy.getValuePolicyRef().getOid(), PageForgotPassword.this, task, result);
			policy = valuePolicy.asObjectable();
		}

		return getModelInteractionService().generateValue(policy, 24, false, user, "nonce generation", task, result);
	}

}
