/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.module.configurer;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.filter.LdapAuthenticationFilter;
import com.evolveum.midpoint.authentication.impl.filter.configurers.MidpointFormLoginConfigurer;
import com.evolveum.midpoint.authentication.impl.module.configuration.LdapModuleWebSecurityConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LdapAuthenticationModuleType;

import jakarta.servlet.ServletRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;

/**
 * @author lskublik
 */

public class LdapWebSecurityConfigurer extends LoginFormModuleWebSecurityConfigurer<LdapModuleWebSecurityConfiguration, LdapAuthenticationModuleType> {

    public LdapWebSecurityConfigurer(LdapAuthenticationModuleType module,
            String sequenceSuffix,
            AuthenticationChannel authenticationChannel,
            ObjectPostProcessor<Object> postProcessor,
            ServletRequest request,
            AuthenticationProvider provider) {
        super(module, sequenceSuffix, authenticationChannel, postProcessor, request, provider);
    }

    @Override
    protected LdapModuleWebSecurityConfiguration buildConfiguration(LdapAuthenticationModuleType moduleType, String sequenceSuffix, AuthenticationChannel authenticationChannel, ServletRequest request) {
        LdapModuleWebSecurityConfiguration configuration = LdapModuleWebSecurityConfiguration.build(moduleType, sequenceSuffix);
        configuration.setSequenceSuffix(sequenceSuffix);
        return configuration;
    }

    protected MidpointFormLoginConfigurer getMidpointFormLoginConfigurer() {
        return new MidpointFormLoginConfigurer(new LdapAuthenticationFilter());
    }
}
