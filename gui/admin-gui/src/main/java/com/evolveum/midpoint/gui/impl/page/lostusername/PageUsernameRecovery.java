/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.lostusername;

import com.evolveum.midpoint.security.api.MidPointPrincipal;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.gui.impl.page.login.AbstractPageLogin;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.web.page.self.PageSelf;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/loginRecovery", matchUrlForSecurity = "/loginRecovery")
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
        Label label = new Label(ID_FOUND_USERS, getAuthorizedUserLoginNameModel());
        label.add(new VisibleBehaviour(this::isUserFound));
        add(label);
    }

    private IModel<String> getAuthorizedUserLoginNameModel() {
        var principal = getMidpointPrincipal();
        var loginName = principal == null ? "" : principal.getUsername();
        return Model.of(loginName);
    }

    @Override
    protected IModel<String> getLoginPanelTitleModel() {
        return createStringResource(getTitleKey());
    }

    private String getTitleKey() {
        return isUserFound() ? "PageLoginRecoveryFinish.title.success" : "PageLoginRecoveryFinish.title.fail";
    }

    @Override
    protected IModel<String> getLoginPanelDescriptionModel() {
        return createStringResource(getTitleDescriptionKey());
    }

    private String getTitleDescriptionKey() {
        return isUserFound() ?
                "PageLoginRecoveryFinish.title.success.description" : "PageLoginRecoveryFinish.title.fail.description";
    }

    private boolean isUserFound() {
        return getMidpointPrincipal() != null;
    }

    private MidPointPrincipal getMidpointPrincipal() {
        var mpAuthentication = getMidpointAuthentication();
        var principal = mpAuthentication.getPrincipal();
        if (principal instanceof MidPointPrincipal) {
            return (MidPointPrincipal) principal;
        }
        return null;
    }

    private MidpointAuthentication getMidpointAuthentication() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof MidpointAuthentication mpAuthentication)) {
            getSession().error(getString("No midPoint authentication is found"));
            throw new RestartResponseException(PageError.class);
        }
        return (MidpointAuthentication) authentication;
    }
}
