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

package com.evolveum.midpoint.web.page.login;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.forgetpassword.PageForgetPassword;
import com.evolveum.midpoint.web.page.self.PageSelfDashboard;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsPolicyType;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;

/**
 * @author mserbak
 */
@PageDescriptor(url = "/login")
public class PageLogin extends PageBase {

	private static final Trace LOGGER = TraceManager.getTrace(PageLogin.class);

    private static final String ID_LOGIN_FORM = "loginForm";

    private static final String ID_USERNAME = "username";
    private static final String ID_PASSWORD = "password";
    private static final String ID_FORGET_PASSWORD = "forgetpassword";

    protected static final String OPERATION_LOAD_RESET_PASSWORD_POLICY = "LOAD PASSWORD RESET POLICY";


    public PageLogin() {
        if (SecurityUtils.getPrincipalUser() != null) {
            MidPointApplication app = getMidpointApplication();
            setResponsePage(app.getHomePage());
        }

        Form form = new Form(ID_LOGIN_FORM) {

            @Override
            protected void onSubmit() {
                MidPointAuthWebSession session = MidPointAuthWebSession.getSession();

                RequiredTextField<String> username = (RequiredTextField) get(ID_USERNAME);
                PasswordTextField password = (PasswordTextField) get(ID_PASSWORD);
                if (session.authenticate(username.getModelObject(), password.getModelObject())) {
                    if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_DASHBOARD_URL,
                            AuthorizationConstants.AUTZ_UI_HOME_ALL_URL)) {
                        setResponsePage(PageDashboard.class);
                    } else {
                        setResponsePage(PageSelfDashboard.class);
                    }
                }
            }
        };

        BookmarkablePageLink<String> link = new BookmarkablePageLink<>(ID_FORGET_PASSWORD, PageForgetPassword.class);
        link.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                OperationResult parentResult = new OperationResult(OPERATION_LOAD_RESET_PASSWORD_POLICY);

                CredentialsPolicyType creds = null;
                try {
                    creds = getModelInteractionService().getCredentialsPolicy(null, null, parentResult);
                } catch (ObjectNotFoundException | SchemaException e) {
                    LOGGER.warn("Cannot read credentials policy: "+e.getMessage(), e);
                }

                boolean linkIsVisible = false;
                if (creds != null
                        && creds.getSecurityQuestions() != null
                        && creds.getSecurityQuestions().getQuestionNumber() != null) {
                    linkIsVisible = true;
                }

                return linkIsVisible;
            }
        });
        form.add(link);

        form.add(new RequiredTextField(ID_USERNAME, new Model<String>()));
        form.add(new PasswordTextField(ID_PASSWORD, new Model<String>()));

        add(form);
    }

    @Override
    protected void createBreadcrumb() {
        //don't create breadcrumb for login page
    }
}
