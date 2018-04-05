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

package com.evolveum.midpoint.web.page.self;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.context.PasswordAuthenticationContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.login.PageLogin;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

@PageDescriptor(urls = {@Url(mountUrl = SchemaConstants.ACCOUNT_ACTIVATION_PREFIX)}, permitAll = true)
public class PageAccountActivation extends PageBase {

	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(PageAccountActivation.class);

	private IModel<UserType> userModel;

	private static final String DOT_CLASS = PageUser.class.getName() + ".";
	private static final String LOAD_USER = DOT_CLASS + "loadUser";
	private static final String OPERATION_ACTIVATE_SHADOWS = DOT_CLASS + "activateShadows";

	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_NAME = "username";
	private static final String ID_PASSWORD = "password";
	private static final String ID_CONFIRM = "confirm";
	private static final String ID_ACTIVATION_CONTAINER = "activationContainer";
	private static final String ID_CONFIRMATION_CONTAINER = "confirmationContainer";
	private static final String ID_ACTIVATED_SHADOWS = "activatedShadows";
	private static final String ID_LINK_TO_LOGIN = "linkToLogin";

	private boolean activated = false;

	@SpringBean(name = "passwordAuthenticationEvaluator")
	private AuthenticationEvaluator<PasswordAuthenticationContext> authenticationEvaluator;

	public PageAccountActivation(PageParameters params) {

		UserType user = loadUser(params);
		
		if (user == null) {
			getSession().error(getString("PageAccountActivation.account.activation.failed"));
			throw new RestartResponseException(PageLogin.class);
		}

		userModel = new LoadableModel<UserType>(false) {

			private static final long serialVersionUID = 1L;

			@Override
			protected UserType load() {
				return user;
			}
		};

		initLayout();

	}

	private UserType loadUser(PageParameters params){
		String userOid = getOidFromParameter(params);
		if (userOid == null) {
			getSession().error(getString("PageAccountActivation.user.not found"));
			throw new RestartResponseException(PageLogin.class);
		}

		Task task = createAnonymousTask(LOAD_USER);
		OperationResult result = new OperationResult(LOAD_USER);

		return runPrivileged(new Producer<UserType>() {
			private static final long serialVersionUID = 1L;

			@Override
			public UserType run() {
				Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(UserType.F_LINK_REF, GetOperationOptions.createResolve());
				PrismObject<UserType> user = WebModelServiceUtils.loadObject(UserType.class, userOid, options, PageAccountActivation.this, task, result);
				if (user == null) {
					return null;
				}
				return user.asObjectable();
			}
		});


	}

	private void initLayout(){
		WebMarkupContainer activationContainer= new WebMarkupContainer(ID_ACTIVATION_CONTAINER);
		activationContainer.setOutputMarkupId(true);
		add(activationContainer);
		activationContainer.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;
			@Override
			public boolean isVisible() {
				return !activated;
			}

		});

		Form<?> form = new com.evolveum.midpoint.web.component.form.Form<>(ID_MAIN_FORM);
		activationContainer.add(form);

		Label usernamePanel = new Label(ID_NAME, createStringResource("PageAccountActivation.activate.accounts.label", new PropertyModel<>(userModel, "name.orig")));
		usernamePanel.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isEnabled() {
				return false;
			}
		});
		form.add(usernamePanel);

		PasswordTextField passwordPanel = new PasswordTextField(ID_PASSWORD, Model.of(new String()));
		form.add(passwordPanel);

		AjaxSubmitButton confirmPasswrod = new AjaxSubmitButton(ID_CONFIRM) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target,
					Form<?> form) {
				propagatePassword(target, form);
			}

		@Override
		protected void onError(AjaxRequestTarget target,
				Form<?> form) {
			getSession().error(getString("PageAccountActivation.account.activation.failed"));
			target.add(getFeedbackPanel());
		}
		};

		form.add(confirmPasswrod);

		WebMarkupContainer confirmationContainer = new WebMarkupContainer(ID_CONFIRMATION_CONTAINER);
		confirmationContainer.setOutputMarkupId(true);
		confirmationContainer.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return activated;
			}
		});

		add(confirmationContainer);

		AjaxLink<Void> linkToLogin = new AjaxLink<Void>(ID_LINK_TO_LOGIN) {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				setResponsePage(PageLogin.class);
			}
		};
		confirmationContainer.add(linkToLogin);

		RepeatingView activatedShadows = new RepeatingView(ID_ACTIVATED_SHADOWS);
		confirmationContainer.add(activatedShadows);
		List<ShadowType> shadowsToActivate = getShadowsToActivate();

		if (shadowsToActivate.isEmpty()) {
			LOGGER.error("No accounts to validate for user {}", userModel.getObject());
			getSession().warn(getString("PageAccountActivation.nothing.to.activate"));
			throw new RestartResponseException(PageLogin.class);
		}
		for (ShadowType shadow : shadowsToActivate) {
			Label shadowDesc = new Label(activatedShadows.newChildId(), WebComponentUtil.getName(shadow) + " on resource " + WebComponentUtil.getName(shadow.getResourceRef()));
			activatedShadows.add(shadowDesc);

		}


	}

	private String getOidFromParameter(PageParameters params){

		if (params == null || params.isEmpty()) {
			LOGGER.error("No page paraeters found for account activation. No user to activate his/her accounts");
			return null;
		}

		StringValue userValue = params.get(SchemaConstants.USER_ID);
		if (userValue == null || userValue.isEmpty()) {
			LOGGER.error("No user defined in the page parameter. Expected user=? attribute filled but didmn't find one.");
			return null;
		}

		return userValue.toString();

	}

	private void propagatePassword(AjaxRequestTarget target,
			Form<?> form) {

		List<ShadowType> shadowsToActivate = getShadowsToActivate();

		PasswordTextField passwordPanel = (PasswordTextField) form.get(createComponentPath(ID_PASSWORD));
		String value = passwordPanel.getModelObject();

		ConnectionEnvironment connEnv = ConnectionEnvironment.create(SchemaConstants.CHANNEL_GUI_USER_URI);
		UsernamePasswordAuthenticationToken token;
		try {
			token = authenticationEvaluator.authenticate(connEnv, new PasswordAuthenticationContext(userModel.getObject().getName().getOrig(), value));
		} catch (Exception ex) {
			LOGGER.error("Failed to authenticate user, reason {}", ex.getMessage());
			getSession().error(getString("PageAccountActivation.authentication.failed"));
			throw new RestartResponseException(PageAccountActivation.class, getPageParameters());
		}
		if (token == null) {
			LOGGER.error("Failed to authenticate user");
			getSession().error(getString("PageAccountActivation.authentication.failed"));
			throw new RestartResponseException(PageAccountActivation.class, getPageParameters());
		}
		ProtectedStringType passwordValue = new ProtectedStringType();
		passwordValue.setClearValue(value);

		Collection<ObjectDelta<ShadowType>> passwordDeltas = new ArrayList<>(shadowsToActivate.size());
		for (ShadowType shadow : shadowsToActivate) {
			ObjectDelta<ShadowType> shadowDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class, shadow.getOid(), SchemaConstants.PATH_PASSWORD_VALUE, getPrismContext(), passwordValue);
			shadowDelta.addModificationReplaceProperty(ShadowType.F_LIFECYCLE_STATE, SchemaConstants.LIFECYCLE_PROPOSED);
			passwordDeltas.add(shadowDelta);
		}

		OperationResult result = runPrivileged(new Producer<OperationResult>() {
			private static final long serialVersionUID = 1L;

			@Override
			public OperationResult run() {
				OperationResult result = new OperationResult(OPERATION_ACTIVATE_SHADOWS);
				Task task = createAnonymousTask(OPERATION_ACTIVATE_SHADOWS);
				WebModelServiceUtils.save((Collection) passwordDeltas, null, result, task, PageAccountActivation.this);
				return result;
			}
		});

		result.recomputeStatus();

		if (!result.isSuccess()) {
			getSession().error(getString("PageAccountActivation.account.activation.failed"));
			LOGGER.error("Failed to acitvate accounts, reason: {} ", result.getMessage());
			target.add(getFeedbackPanel());
		} else {
			getSession().success(getString("PageAccountActivation.account.activation.successful"));
			target.add(getFeedbackPanel());
			activated = true;
		}

		target.add(PageAccountActivation.this);


	}

	private List<ShadowType> getShadowsToActivate(){
		UserType userType = userModel.getObject();
		List<ShadowType> shadowsToActivate = userType.getLink();
		if (shadowsToActivate == null || shadowsToActivate.isEmpty()) {
			return new ArrayList<>();
		}
		return shadowsToActivate.parallelStream().filter(shadow -> SchemaConstants.LIFECYCLE_PROPOSED.equals(shadow.getLifecycleState())).collect(Collectors.toList());
	}

}
