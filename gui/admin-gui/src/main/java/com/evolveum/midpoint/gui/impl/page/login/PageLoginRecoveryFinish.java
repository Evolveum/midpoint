/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.login;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.security.api.AuthorizationConstants;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.Serial;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/loginRecovery", matchUrlForSecurity = "/loginRecovery")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_LOGIN_RECOVERY_FINISH_URL,
                        label = "PageLoginRecoveryFinish.auth.label",
                        description = "PageLoginRecoveryFinish.auth.description") })
public class PageLoginRecoveryFinish extends AbstractPageLogin {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_USER_LOGIN_PANEL = "userLoginName";

    @Override
    protected boolean isBackButtonVisible() {
        return true;
    }

    @Override
    protected void initCustomLayout() {
        Label loginNamePanel = new Label(ID_USER_LOGIN_PANEL, Model.of(getAuthorizedUserName()));
        loginNamePanel.setOutputMarkupId(true);
        loginNamePanel.add(new VisibleBehaviour(this::isUserAuthorized));
        add(loginNamePanel);
    }

    @Override
    protected IModel<String> getLoginPanelTitleModel() {
        return createStringResource(getTitleKey());
    }

    private String getTitleKey() {
        return isUserAuthorized() ? "PageLoginRecoveryFinish.title.success" : "PageLoginRecoveryFinish.title.fail";
    }

    @Override
    protected IModel<String> getLoginPanelDescriptionModel() {
        return createStringResource(getTitleDescriptionKey());
    }

    private String getTitleDescriptionKey() {
        return isUserAuthorized() ? "PageLoginRecoveryFinish.title.success" : "PageLoginRecoveryFinish.title.fail";
    }

    private boolean isUserAuthorized() {
        return getAuthorizedUserName() != null;
    }

    private String getAuthorizedUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof MidpointAuthentication) {
            return ((MidpointAuthentication) authentication).getUsername();
        }
        return null;
    }

}
