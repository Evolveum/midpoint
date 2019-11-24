/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.module.configuration;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationModuleHttpHeaderType;

/**
 * @author skublik
 */

public class HttpHeaderModuleWebSecurityConfiguration extends ModuleWebSecurityConfiguration {

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

    public static HttpHeaderModuleWebSecurityConfiguration build(AuthenticationModuleHttpHeaderType module, String prefixOfSequence){
        HttpHeaderModuleWebSecurityConfiguration configuration = new HttpHeaderModuleWebSecurityConfiguration();
        build(configuration, module, prefixOfSequence);
        if (module.getUsernameHeader() != null) {
            configuration.setPrincipalRequestHeader(module.getUsernameHeader());
        } else {
            configuration.setPrincipalRequestHeader(DEFAULT_HEADER);
        }
        return configuration;
    }
}
