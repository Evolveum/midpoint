/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.login.module;

import java.io.Serial;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
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
    protected IModel<String> getDefaultLoginPanelTitleModel() {
        return createStringResource("PageOidcSelect.select.identity.provider");
    }

    @Override
    protected IModel<String> getDefaultLoginPanelDescriptionModel() {
        return Model.of();
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
