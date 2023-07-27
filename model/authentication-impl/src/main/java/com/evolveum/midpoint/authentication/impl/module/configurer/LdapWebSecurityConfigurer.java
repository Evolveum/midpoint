/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.module.configurer;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.filter.LdapAuthenticationFilter;
import com.evolveum.midpoint.authentication.impl.filter.configurers.MidpointFormLoginConfigurer;
import com.evolveum.midpoint.authentication.impl.module.configuration.LdapModuleWebSecurityConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LdapAuthenticationModuleType;

import jakarta.servlet.ServletRequest;
import org.springframework.security.config.annotation.ObjectPostProcessor;

/**
 * @author lskublik
 */

public class LdapWebSecurityConfigurer extends LoginFormModuleWebSecurityConfigurer<LdapModuleWebSecurityConfiguration, LdapAuthenticationModuleType> {

    public LdapWebSecurityConfigurer(LdapModuleWebSecurityConfiguration configuration) {
        super(configuration);
    }

    public LdapWebSecurityConfigurer(LdapAuthenticationModuleType module,
            String sequenceSuffix,
            AuthenticationChannel authenticationChannel,
            ObjectPostProcessor<Object> postProcessor,
            ServletRequest request) {
        super(module, sequenceSuffix, authenticationChannel, postProcessor, request);
    }

    protected MidpointFormLoginConfigurer getMidpointFormLoginConfigurer() {
        return new MidpointFormLoginConfigurer(new LdapAuthenticationFilter());
    }
}
