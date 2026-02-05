/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.login.module;

import java.io.Serial;

import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;

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
import com.evolveum.midpoint.web.component.util.EnableBehaviour;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/otp_verify", matchUrlForSecurity = "/otp_verify")
        },
        permitAll = true,
        loginPage = true,
authModule = AuthenticationModuleNameConstants.OTP)
//TODO ModuleAuthentication because it might be either credentials or ldap module authentication
public class PageOtpCode extends PageAbstractAuthenticationModule<ModuleAuthentication> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CODE = "code";

    public PageOtpCode() {
        super(null);
    }

    @Override
    protected void initModuleLayout(MidpointForm form) {
        TextField<String> code = new TextField<>(ID_CODE);
        code.add(new EnableBehaviour(() -> searchUser() != null));
        form.add(code);
    }

    protected String getUrlProcessingLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof MidpointAuthentication mpa)) {
            return "./spring_security_login";
        }

        ModuleAuthentication moduleAuthentication = mpa.getProcessingModuleAuthentication();
        if (isModuleApplicable(moduleAuthentication)) {
            String prefix = moduleAuthentication.getPrefix();
            return AuthUtil.stripSlashes(prefix) + "/spring_security_login";
        }

        return "./spring_security_login";
    }

    @Override
    protected IModel<String> getDefaultLoginPanelTitleModel() {
        return createStringResource("PageOtpCode.loginToYourAccount");
    }

    @Override
    protected IModel<String> getDefaultLoginPanelDescriptionModel() {
        return severalLoginFormModulesExist() ?
                createStringResource("PageOtpCode.panelDescriptionWithModuleName", getProcessingModuleName())
                : createStringResource("PageOtpCode.enterAccountDetails");
    }

    private boolean severalLoginFormModulesExist() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof MidpointAuthentication mpAuthentication) {
            int loginFormModulesCount = (int) mpAuthentication.getAuthModules()
                    .stream()
                    .filter(module -> module != null && isModuleApplicable(module.getBaseModuleAuthentication()))
                    .count();
            return loginFormModulesCount > 1;
        }
        return false;
    }

    private String getProcessingModuleName() {
        return getAuthenticationModuleConfiguration().getModuleIdentifier();
    }
}
