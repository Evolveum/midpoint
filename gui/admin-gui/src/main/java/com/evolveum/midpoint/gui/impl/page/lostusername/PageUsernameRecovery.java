/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.lostusername;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.gui.impl.page.login.AbstractPageLogin;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.web.page.self.PageSelf;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/loginRecovery1", matchUrlForSecurity = "/loginRecovery1")
        },
        action = {
                @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                        label = PageSelf.AUTH_SELF_ALL_LABEL,
                        description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USERNAME_RECOVERY_URL) })
public class PageUsernameRecovery extends AbstractPageLogin {

    private static final String ID_FOUND_USERS = "foundUsers";


    public PageUsernameRecovery() {
        super();
    }

    @Override
    protected boolean isBackButtonVisible() {
        return true;
    }

    @Override
    protected void initCustomLayout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof MidpointAuthentication mpAuthentication)) {
            getSession().error(getString("No midPoint authentication is found"));
            throw new RestartResponseException(PageError.class);
        }
        Object principal = mpAuthentication.getPrincipal();
        if (principal instanceof MidPointPrincipal mpPrincipal) {
            Label label = new Label(ID_FOUND_USERS, mpPrincipal.getUsername());
            add(label);
        } else {
            Label label = new Label(ID_FOUND_USERS, getString("PageUsernameRecovery.noUserFound"));
            add(label);
        }
    }

    @Override
    protected IModel<String> getLoginPanelTitleModel() {
        return createStringResource("Username recovery");
    }

    @Override
    protected IModel<String> getLoginPanelDescriptionModel() {
        return createStringResource("Username recovery description");
    }

}
