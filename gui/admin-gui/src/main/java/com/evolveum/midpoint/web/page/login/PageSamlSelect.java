/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.login;

import java.io.Serializable;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;

/**
 * @author skublik
 */
@PageDescriptor(urls = {
        @Url(mountUrl = "/saml2/select", matchUrlForSecurity = "/saml2/select")
}, permitAll = true, loginPage = true, authModule = AuthenticationModuleNameConstants.SAML_2)
public class PageSamlSelect extends AbstractPageRemoteAuthenticationSelect implements Serializable {
    private static final long serialVersionUID = 1L;

    public PageSamlSelect() {
    }

    protected boolean existRemoteAuthentication(ModuleAuthentication actualModule) {
        return AuthenticationModuleNameConstants.SAML_2.equals(actualModule.getNameOfModuleType())
                && (actualModule.getAuthentication() instanceof Saml2AuthenticationToken
                || (actualModule.getAuthentication() instanceof AnonymousAuthenticationToken
                && actualModule.getAuthentication().getDetails() instanceof Saml2AuthenticationToken));
    }
}
