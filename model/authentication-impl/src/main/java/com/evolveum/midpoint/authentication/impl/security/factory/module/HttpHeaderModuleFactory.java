/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.security.factory.module;

import java.util.Map;
import javax.servlet.ServletRequest;

import com.evolveum.midpoint.authentication.impl.security.provider.PasswordProvider;
import com.evolveum.midpoint.authentication.impl.security.util.AuthModuleImpl;
import com.evolveum.midpoint.authentication.api.AuthModule;
import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.security.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.security.module.HttpHeaderModuleWebSecurityConfig;
import com.evolveum.midpoint.authentication.impl.security.module.ModuleWebSecurityConfig;
import com.evolveum.midpoint.authentication.impl.security.module.authentication.HttpHeaderModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.security.module.configuration.HttpHeaderModuleWebSecurityConfiguration;

import com.evolveum.midpoint.authentication.impl.security.module.configuration.ModuleWebSecurityConfigurationImpl;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationModulesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.HttpHeaderAuthenticationModuleType;

/**
 * @author skublik
 */
@Component
public class HttpHeaderModuleFactory extends AbstractModuleFactory {

    private static final Trace LOGGER = TraceManager.getTrace(HttpHeaderModuleFactory.class);

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType) {
        if (moduleType instanceof HttpHeaderAuthenticationModuleType) {
            return true;
        }
        return false;
    }

    @Override
    public AuthModule createModuleFilter(AbstractAuthenticationModuleType moduleType, String prefixOfSequence, ServletRequest request,
                                         Map<Class<? extends Object>, Object> sharedObjects, AuthenticationModulesType authenticationsPolicy, CredentialsPolicyType credentialPolicy, AuthenticationChannel authenticationChannel) throws Exception {
        if (!(moduleType instanceof HttpHeaderAuthenticationModuleType)) {
            LOGGER.error("This factory support only HttpHeaderAuthenticationModuleType, but modelType is " + moduleType);
            return null;
        }

        isSupportedChannel(authenticationChannel);
        HttpHeaderAuthenticationModuleType httpModuleType = (HttpHeaderAuthenticationModuleType) moduleType;
        HttpHeaderModuleWebSecurityConfiguration configuration = HttpHeaderModuleWebSecurityConfiguration.build(httpModuleType, prefixOfSequence);
        configuration.addAuthenticationProvider(getObjectObjectPostProcessor().postProcess(new PasswordProvider()));
        ModuleWebSecurityConfig module = getObjectObjectPostProcessor().postProcess(new HttpHeaderModuleWebSecurityConfig(configuration));
        module.setObjectPostProcessor(getObjectObjectPostProcessor());
        HttpSecurity http = module.getNewHttpSecurity();
        setSharedObjects(http, sharedObjects);

        ModuleAuthenticationImpl moduleAuthentication = createEmptyModuleAuthentication(configuration);
        moduleAuthentication.setFocusType(httpModuleType.getFocusType());
        SecurityFilterChain filter = http.build();
        return AuthModuleImpl.build(filter, configuration, moduleAuthentication);
    }

    private ModuleAuthenticationImpl createEmptyModuleAuthentication(ModuleWebSecurityConfigurationImpl configuration) {
        HttpHeaderModuleAuthentication moduleAuthentication = new HttpHeaderModuleAuthentication();
        moduleAuthentication.setPrefix(configuration.getPrefix());
        moduleAuthentication.setNameOfModule(configuration.getNameOfModule());
        return moduleAuthentication;
    }
}
