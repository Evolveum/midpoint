/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.login;

import java.io.Serial;

import org.apache.wicket.model.IModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.CredentialModuleAuthentication;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.web.component.form.MidpointForm;

/**
 * @author mserbak
 * @author lskublik
 */
@PageDescriptor(urls = {
        @Url(mountUrl = "/login", matchUrlForSecurity = "/login")
}, permitAll = true, loginPage = true)
public class PageLogin extends PageAbstractAuthenticationModule<CredentialModuleAuthentication>  {
    @Serial private static final long serialVersionUID = 1L;

    public PageLogin() {
        super(null);
    }

    @Override
    protected void initModuleLayout(MidpointForm form) {

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
