/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.login.module;

import java.io.Serial;
import java.io.Serializable;

import org.apache.wicket.model.IModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;

/**
 * @author skublik
 */
@PageDescriptor(urls = {
        @Url(mountUrl = "/saml2/select", matchUrlForSecurity = "/saml2/select")
}, permitAll = true, loginPage = true, authModule = AuthenticationModuleNameConstants.SAML_2)
public class PageSamlSelect extends AbstractPageRemoteAuthenticationSelect implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    public PageSamlSelect() {
    }

    @Override
    protected boolean isBackButtonVisible() {
        return false;
    }

    @Override
    protected IModel<String> getLoginPanelTitleModel() {
        return createStringResource("PageSamlSelect.title");
    }

    @Override
    protected IModel<String> getLoginPanelDescriptionModel() {
        return createStringResource("PageSamlSelect.title.description");
    }

    @Override
    protected Class<? extends Authentication> getSupportedAuthToken() {
        return Saml2AuthenticationToken.class;
    }

    @Override
    protected String getErrorKeyUnsupportedType() {
        return "PageSamlSelect.unsupported.authentication.type";
    }

    @Override
    protected String getErrorKeyEmptyProviders() {
        return "PageSamlSelect.empty.providers";
    }
}
