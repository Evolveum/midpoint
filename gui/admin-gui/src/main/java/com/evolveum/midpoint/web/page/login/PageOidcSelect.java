/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.login;

import com.evolveum.midpoint.authentication.api.PageDescriptor;
import com.evolveum.midpoint.authentication.api.Url;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;

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
}
