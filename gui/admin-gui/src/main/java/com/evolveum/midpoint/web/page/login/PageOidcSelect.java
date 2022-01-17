/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.login;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;

import java.io.Serializable;

/**
 * @author skublik
 */
@PageDescriptor(urls = {
        @Url(mountUrl = "/oidc/select", matchUrlForSecurity = "/oidc/select")
}, permitAll = true, loginPage = true, authModule = AuthenticationModuleNameConstants.OIDC)
public class PageOidcSelect extends AbstractPageRemoteAuthenticationSelect implements Serializable {
    private static final long serialVersionUID = 1L;

    public PageOidcSelect() {
    }

    @Override
    protected Class<? extends Authentication> getSupportedAuthToken(){
        return OAuth2LoginAuthenticationToken.class;
    }

    @Override
    protected String getErrorKeyUnsupportedType() {
        return "PageOidcSelect.unsupported.authentication.type";
    }

    @Override
    protected String getErrorKeyEmptyProviders() {
        return "PageOidcSelect.empty.providers";
    }
}
