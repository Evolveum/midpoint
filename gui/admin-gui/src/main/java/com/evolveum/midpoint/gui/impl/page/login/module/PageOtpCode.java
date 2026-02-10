/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.login.module;

import java.io.Serial;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/otp_verify", matchUrlForSecurity = "/otp_verify")
        },
        permitAll = true,
        loginPage = true,
        authModule = AuthenticationModuleNameConstants.OTP)
@SuppressWarnings("unused")
public class PageOtpCode extends PageAbstractAuthenticationModule<ModuleAuthentication> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String DEFAULT_FORM_URL = "./spring_security_login";
    private static final String OTP_FORM_URL = "/otp_verify";

    private static final String ID_CODE = "code";

    @Override
    protected void initModuleLayout(MidpointForm form) {
        TextField<String> code = new TextField<>(ID_CODE);
        code.setOutputMarkupId(true);
        code.add(new EnableBehaviour(() -> searchUser() != null));
        form.add(code);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        String codeId = get(createComponentPath("form", ID_CODE)).getMarkupId();
        response.render(OnDomReadyHeaderItem.forScript(String.format("$('#%s').focus();", codeId)));
    }

    protected String getUrlProcessingLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof MidpointAuthentication mpa)) {
            return DEFAULT_FORM_URL;
        }

        ModuleAuthentication moduleAuthentication = mpa.getProcessingModuleAuthentication();
        if (isModuleApplicable(moduleAuthentication)) {
            String prefix = moduleAuthentication.getPrefix();
            return AuthUtil.stripSlashes(prefix) + OTP_FORM_URL;
        }

        return DEFAULT_FORM_URL;
    }

    @Override
    protected boolean isModuleApplicable(ModuleAuthentication moduleAuthentication) {
        return AuthenticationModuleNameConstants.OTP.equals(moduleAuthentication.getModuleTypeName());
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
        if (!(authentication instanceof MidpointAuthentication ma)) {
            return false;
        }

        int loginFormModulesCount = (int) ma.getAuthModules().stream()
                .filter(module -> module != null && isModuleApplicable(module.getBaseModuleAuthentication()))
                .count();
        return loginFormModulesCount > 1;
    }

    private String getProcessingModuleName() {
        return getAuthenticationModuleConfiguration().getModuleIdentifier();
    }
}
