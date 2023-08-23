/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.login.module;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;

import org.apache.wicket.model.IModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author skublik
 */
@PageDescriptor(urls = {
        @Url(mountUrl = "/duo/select", matchUrlForSecurity = "/duo/select")
}, permitAll = true, loginPage = true, authModule = AuthenticationModuleNameConstants.DUO)
public class PageDuoSelect extends AbstractPageRemoteAuthenticationSelect implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    public PageDuoSelect() {
    }

    @Override
    protected boolean isBackButtonVisible() {
        return false;
    }

    @Override
    protected IModel<String> getLoginPanelTitleModel() {
        return createStringResource("PageDuoSelect.title");
    }

    @Override
    protected IModel<String> getLoginPanelDescriptionModel() {
        return createStringResource("PageDuoSelect.title.description");
    }

    @Override
    protected Class<? extends Authentication> getSupportedAuthToken() {
        return null;
    }

    @Override
    protected String getErrorKeyUnsupportedType() {
        return "PageDuoSelect.unsupported.authentication.type";
    }

    @Override
    protected String getErrorKeyEmptyProviders() {
        return "PageDuoSelect.empty.providers";
    }
}
