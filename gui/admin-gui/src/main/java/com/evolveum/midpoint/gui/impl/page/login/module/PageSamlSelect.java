/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.login.module;

import java.io.Serial;
import java.io.Serializable;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
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
    protected IModel<String> getDefaultLoginPanelTitleModel() {
        return createStringResource("PageSamlSelect.select.identity.provider");
    }

    @Override
    protected IModel<String> getDefaultLoginPanelDescriptionModel() {
        return Model.of();
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
