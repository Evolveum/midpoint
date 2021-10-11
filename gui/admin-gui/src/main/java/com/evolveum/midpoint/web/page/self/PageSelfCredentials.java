/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.self;

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordChangeSecurityType;

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
        return (getModelObject().getPasswordChangeSecurity() == null) || (getModelObject().getPasswordChangeSecurity() != null &&
                getModelObject().getPasswordChangeSecurity().equals(PasswordChangeSecurityType.OLD_PASSWORD));
    }

    @Override
    protected void finishChangePassword(OperationResult result, AjaxRequestTarget target) {
        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            setNullEncryptedPasswordData();
            showResult(result);
            target.add(getFeedbackPanel());
        } else {
            showResult(result);
            setResponsePage(getMidpointApplication().getHomePage());
        }
    }
}
