/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.self;

import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.web.page.admin.home.dto.MyCredentialsDto;
import com.evolveum.midpoint.web.page.self.component.SecurityQuestionsPanel;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;

import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PageDescriptor(url = {"/self/credentials"}, action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_CREDENTIALS_URL,
                label = "PageSelfCredentials.auth.credentials.label",
                description = "PageSelfCredentials.auth.credentials.description")})
public class PageSelfCredentials extends PageAbstractSelfCredentials{

    private static final long serialVersionUID = 1L;

    @Override
    protected boolean isCheckOldPassword() {
        return (getPasswordDto().getPasswordChangeSecurity() == null) || (getPasswordDto().getPasswordChangeSecurity() != null &&
                (getPasswordDto().getPasswordChangeSecurity().equals(PasswordChangeSecurityType.OLD_PASSWORD)
                        || (getPasswordDto().getPasswordChangeSecurity().equals(PasswordChangeSecurityType.OLD_PASSWORD_IF_EXISTS)
                        && getPasswordDto().getFocus().findProperty(ItemPath.create(FocusType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE)) != null)));
    }

    @Override
    protected void finishChangePassword(OperationResult result, AjaxRequestTarget target, boolean showFeedback) {
        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            setNullEncryptedPasswordData();
            if (showFeedback) {
                showResult(result);
            }
            target.add(getFeedbackPanel());
        } else {

            target.add(getFeedbackPanel());
        }
    }

    @Override
    protected Collection<? extends ITab> createSpecificTabs() {
        List<ITab> tabs = new ArrayList<>();
        if (showQuestions()) {
            tabs.add(new AbstractTab(createStringResource("PageSelfCredentials.tabs.securityQuestion")) {
                private static final long serialVersionUID = 1L;

                @Override
                public WebMarkupContainer getPanel(String panelId) {
                    return new SecurityQuestionsPanel(panelId, new PropertyModel<>(getModel(), MyCredentialsDto.F_PASSWORD_QUESTIONS_DTO));
                }
            });
        }
        return tabs;
    }

    private boolean showQuestions() {
        GuiProfiledPrincipal principal = SecurityUtils.getPrincipalUser();
        if (principal == null) {
            return false;
        }

        CredentialsPolicyType credentialsPolicyType = principal.getApplicableSecurityPolicy().getCredentials();
        if (credentialsPolicyType == null) {
            return false;
        }
        SecurityQuestionsCredentialsPolicyType securityQuestionsPolicy = credentialsPolicyType.getSecurityQuestions();
        if (securityQuestionsPolicy == null) {
            return false;
        }

        List<SecurityQuestionDefinitionType> secQuestAnsList = securityQuestionsPolicy.getQuestion();
        return secQuestAnsList != null && !secQuestAnsList.isEmpty();
    }
}
