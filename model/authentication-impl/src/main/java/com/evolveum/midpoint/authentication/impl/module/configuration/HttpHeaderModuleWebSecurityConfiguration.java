/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.module.configuration;

import com.evolveum.midpoint.xml.ns._public.common.common_3.HttpHeaderAuthenticationModuleType;

/**
 * @author skublik
 */

public class HttpHeaderModuleWebSecurityConfiguration extends LoginFormModuleWebSecurityConfiguration {

    private String principalRequestHeader;

    private static final String DEFAULT_HEADER = "SM_USER";

    private HttpHeaderModuleWebSecurityConfiguration() {

    }

    public String getPrincipalRequestHeader() {
        return principalRequestHeader;
    }

    public void setPrincipalRequestHeader(String principalRequestHeader) {
        this.principalRequestHeader = principalRequestHeader;
    }

    public static HttpHeaderModuleWebSecurityConfiguration build(HttpHeaderAuthenticationModuleType module, String prefixOfSequence){
        HttpHeaderModuleWebSecurityConfiguration configuration = new HttpHeaderModuleWebSecurityConfiguration();
        configuration.setDefaultSuccessLogoutURL(module.getLogoutUrl());
        build(configuration, module, prefixOfSequence);
        if (module.getUsernameHeader() != null) {
            configuration.setPrincipalRequestHeader(module.getUsernameHeader());
        } else {
            configuration.setPrincipalRequestHeader(DEFAULT_HEADER);
        }
        configuration.validate();
        return configuration;
    }
}
