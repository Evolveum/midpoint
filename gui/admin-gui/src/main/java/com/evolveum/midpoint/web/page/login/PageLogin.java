/*
 * Copyright (c) 2010-2016 Evolveum
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

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.forgetpassword.PageForgotPassword;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RegistrationsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.springframework.security.web.WebAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * @author mserbak
 */
@PageDescriptor(url = "/login")
public class PageLogin extends PageBase {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(PageLogin.class);

    private static final String ID_FORGET_PASSWORD = "forgetpassword";
    private static final String ID_SELF_REGISTRATION = "selfRegistration";

    private static final String DOT_CLASS = PageLogin.class.getName() + ".";
    protected static final String OPERATION_LOAD_RESET_PASSWORD_POLICY = DOT_CLASS + "loadPasswordResetPolicy";
    private static final String OPERATION_LOAD_REGISTRATION_POLICY = DOT_CLASS + "loadRegistrationPolicy";
    
    public PageLogin() {
        BookmarkablePageLink<String> link = new BookmarkablePageLink<>(ID_FORGET_PASSWORD, PageForgotPassword.class);
        link.add(new VisibleEnableBehaviour() {
        	private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                OperationResult parentResult = new OperationResult(OPERATION_LOAD_RESET_PASSWORD_POLICY);

                SecurityPolicyType securityPolicy = null;
                try {
                    securityPolicy = getModelInteractionService().getSecurityPolicy(null, null, parentResult);
                } catch (ObjectNotFoundException | SchemaException e) {
                    LOGGER.warn("Cannot read credentials policy: " + e.getMessage(), e);
                }

                boolean linkIsVisible = false;
                
                if (securityPolicy == null) {
                	return linkIsVisible;
                }
                
                CredentialsPolicyType creds = securityPolicy.getCredentials();
                
                if (creds != null
                        && ((creds.getSecurityQuestions() != null
                        && creds.getSecurityQuestions().getQuestionNumber() != null) || (securityPolicy.getCredentialsReset() != null))) {
                    linkIsVisible = true;
                }

                return linkIsVisible;
            }
        });
        add(link);
        
        AjaxLink<String> registration = new AjaxLink<String>(ID_SELF_REGISTRATION) {
        	
        	@Override
        	public void onClick(AjaxRequestTarget target) {
        		setResponsePage(PageSelfRegistration.class);
        	}
        };
        registration.add(new VisibleEnableBehaviour() {
        	private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                OperationResult parentResult = new OperationResult(OPERATION_LOAD_REGISTRATION_POLICY);

                RegistrationsPolicyType registrationPolicies = null;
                try {
                	Task task = createAnonymousTask(OPERATION_LOAD_REGISTRATION_POLICY);
                	registrationPolicies = getModelInteractionService().getRegistrationPolicy(null, task, parentResult);
                } catch (ObjectNotFoundException | SchemaException e) {
                    LOGGER.warn("Cannot read credentials policy: " + e.getMessage(), e);
                }

                boolean linkIsVisible = false;
                if (registrationPolicies != null
                        && registrationPolicies.getSelfRegistration() != null) {
                    linkIsVisible = true;
                }

                return linkIsVisible;
            }
        });
        add(registration);
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();

        ServletWebRequest req = (ServletWebRequest) RequestCycle.get().getRequest();
        HttpServletRequest httpReq = req.getContainerRequest();
        HttpSession httpSession = httpReq.getSession();

        Exception ex = (Exception) httpSession.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        if (ex == null) {
            return;
        }

        String key = ex.getMessage() != null ? ex.getMessage() : "web.security.provider.unavailable";
        error(getString(key));

        httpSession.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);

        clearBreadcrumbs();
    }

    @Override
    protected void createBreadcrumb() {
        //don't create breadcrumb for login page
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        if (SecurityUtils.getPrincipalUser() != null) {
            MidPointApplication app = getMidpointApplication();
            throw new RestartResponseException(app.getHomePage());
        }
    }
}
