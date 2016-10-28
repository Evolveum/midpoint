/*
 * Copyright (c) 2012-2016 Biznet, Evolveum
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

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.PolyStringOrigMatchingRule;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@PageDescriptor(url = "/forgetpassword")
public class PageForgetPassword extends PageBase {
	private static final long serialVersionUID = 1L;
	
	private static final String ID_PWDRESETFORM = "pwdresetform";
	private static final String ID_USERNAME = "username";
	private static final String ID_EMAIL ="email";
	
	private static final String DOT_CLASS = PageForgetPassword.class.getName() + ".";
	protected static final String OPERATION_LOAD_RESET_PASSWORD_POLICY = DOT_CLASS + "loadPasswordResetPolicy";
	private static final String OPERATION_RESET_PASSWORD = DOT_CLASS + "resetPassword";
	private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";
	
	private static final Trace LOGGER = TraceManager.getTrace(PageForgetPassword.class);

	public PageForgetPassword() {
		super();
		initLayout();
	}

	@Override
	protected void createBreadcrumb() {
		//don't create breadcrumb for this page
	}
	
	private void initLayout() {
		Form form = new Form(ID_PWDRESETFORM) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit() {
				try {
					RequiredTextField<String> username = (RequiredTextField) get(ID_USERNAME);
					RequiredTextField<String> email = (RequiredTextField) get(ID_EMAIL);
					LOGGER.debug("Reset Password user info form submitted. username={}, email={}", username.getModelObject(), email.getModelObject());
					
					UserType user = checkUser(email.getModelObject(), username.getModelObject());
					LOGGER.trace("Reset Password user: {}", user);
					
					if (user != null) {
	
						getSession().setAttribute("pOid", user.getOid());
						LOGGER.trace("Forward to PageSecurityQuestions");
				        setResponsePage(PageSecurityQuestions.class);
				        
					} else {
						
						LOGGER.debug("User for username={}, email={} not found", username.getModelObject(), email.getModelObject());
						getSession().error(getString("pageForgetPassword.message.usernotfound"));
						throw new RestartResponseException(PageForgetPassword.class);
	
	
					}
					
				} catch (RestartResponseException e) {
					throw e;
				} catch (Throwable e) {
					LOGGER.error("Error during processing of security questions: {}", e.getMessage(), e);
					// Just log the error, but do not display it. We are still in unprivileged part of the web
					// we do not want to provide any information to the attacker.
					throw new RestartResponseException(PageError.class);
				} 

			}
		};

		form.add(new RequiredTextField<String>(ID_USERNAME, new Model<String>()));
		form.add(new RequiredTextField<String>(ID_EMAIL, new Model<String>()));

		add(form);


	}

	// Check if the user exists with the given email and username in the idm
	public UserType checkUser(final String email, final String username) {

		UserType user = runPrivileged(new Producer<UserType>() {
			
			@Override
			public UserType run() {
				return getUser(email, username);
			}
			
			@Override
			public String toString() {
				return DOT_CLASS + "getUser";
			}
			
		});
		
		LOGGER.trace("got user {}", user);
		if (user == null) {
			return null;
		}

 		if (user.getEmailAddress().equalsIgnoreCase(email)) {
			return user;
		} else {
			LOGGER.debug("The supplied e-mail address '{}' and the e-mail address of user {} '{}' do not match",
					email, user, user.getEmailAddress());
			return null;
		}

	}
	
	private UserType getUser(String email, String username) {
		try {

			Task task = createAnonymousTask(OPERATION_LOAD_USER);
			OperationResult result = task.getResult();

			ObjectQuery query = QueryBuilder.queryFor(UserType.class, getPrismContext())
					.item(UserType.F_NAME).eqPoly(username).matchingNorm()
					.and().item(UserType.F_EMAIL_ADDRESS).eq(email).matchingCaseIgnore()
					.build();

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Searching for user with query:\n{}", query.debugDump(1));
			}

			Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
			// Do NOT use WebModelServiceUtils.searchObjects() here. We do NOT want the standard error handling.
			List<PrismObject<UserType>> userList = ((PageBase) getPage()).getModelService().searchObjects(UserType.class, query, options, task, result);

			if ((userList==null) || (userList.isEmpty())) {
				LOGGER.trace("Empty user list in ForgetPassword");
				return null;
			}

			UserType user=  userList.get(0).asObjectable();
			LOGGER.trace("User found for ForgetPassword: {}", user);
			
			return user;

		} catch (Exception e) {
			LOGGER.error("Error getting user: {}", e.getMessage(), e);
			// Just log the error, but do not display it. We are still in unprivileged part of the web
			// we do not want to provide any information to the attacker.
			return null;
		} 

	}

}
