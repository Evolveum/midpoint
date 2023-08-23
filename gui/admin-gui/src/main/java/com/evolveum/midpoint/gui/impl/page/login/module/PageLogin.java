/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.login.module;

import java.io.Serial;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.web.component.form.MidpointForm;

@PageDescriptor(urls = {
        @Url(mountUrl = "/login", matchUrlForSecurity = "/login")
}, permitAll = true, loginPage = true)
//TODO ModuleAuthentication because it might be either credentials or ldap module authentication
public class PageLogin extends PageAbstractAuthenticationModule<ModuleAuthentication> {
    @Serial private static final long serialVersionUID = 1L;
    private static final String ID_USERNAME = "username";

    public PageLogin() {
        super(null);
    }

    @Override
    protected void initModuleLayout(MidpointForm form) {
        TextField<String> username = new TextField<>(ID_USERNAME);
        username.add(AttributeAppender.append("value", WebComponentUtil.getName(searchUser())));
        username.add(new EnableBehaviour(() -> searchUser() == null));
        form.add(username);

    }

    protected String getUrlProcessingLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof MidpointAuthentication mpAuthentication)) {
            return "./spring_security_login";
        }

        ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
        if (isModuleApplicable(moduleAuthentication)){
            String prefix = moduleAuthentication.getPrefix();
            return AuthUtil.stripSlashes(prefix) + "/spring_security_login";
        }

        return "./spring_security_login";
    }


    @Override
    protected IModel<String> getLoginPanelTitleModel() {
        return createStringResource("PageLogin.loginToYourAccount");
    }

    @Override
    protected IModel<String> getLoginPanelDescriptionModel() {
        return severalLoginFormModulesExist() ?
                createStringResource("PageLogin.panelDescriptionWithModuleName", getProcessingModuleName())
                : createStringResource("PageLogin.enterAccountDetails");
    }

    private boolean severalLoginFormModulesExist() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof MidpointAuthentication mpAuthentication) {
            int loginFormModulesCount = (int) mpAuthentication.getAuthModules()
                    .stream()
                    .filter(module -> isModuleApplicable(module.getBaseModuleAuthentication()))
                    .count();
            return loginFormModulesCount > 1;
        }
        return false;
    }

    private String getProcessingModuleName() {
        return getAuthenticationModuleConfiguration().getModuleIdentifier();
    }
}
