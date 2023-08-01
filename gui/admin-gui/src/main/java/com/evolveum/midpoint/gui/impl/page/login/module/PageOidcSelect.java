/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.login.module;

import java.io.Serial;

import org.apache.wicket.model.IModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;

/**
 * @author skublik
 */
@PageDescriptor(urls = {
        @Url(mountUrl = "/oidc/select", matchUrlForSecurity = "/oidc/select")
}, permitAll = true, loginPage = true, authModule = AuthenticationModuleNameConstants.OIDC)
public class PageOidcSelect extends AbstractPageRemoteAuthenticationSelect {
    @Serial private static final long serialVersionUID = 1L;

    public PageOidcSelect() {
    }

    @Override
    protected boolean isBackButtonVisible() {
        return false;
    }

    @Override
    protected IModel<String> getLoginPanelTitleModel() {
        return createStringResource("PageOidcSelect.title");
    }

    @Override
    protected IModel<String> getLoginPanelDescriptionModel() {
        return createStringResource("PageOidcSelect.title.description");
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
