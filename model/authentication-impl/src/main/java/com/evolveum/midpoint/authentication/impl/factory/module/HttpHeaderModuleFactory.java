/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.factory.module;

import java.util.Map;
import jakarta.servlet.ServletRequest;

import com.evolveum.midpoint.authentication.impl.provider.PasswordProvider;
import com.evolveum.midpoint.authentication.impl.util.AuthModuleImpl;
import com.evolveum.midpoint.authentication.api.AuthModule;
import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configurer.HttpHeaderModuleWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.module.authentication.HttpHeaderModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.configuration.HttpHeaderModuleWebSecurityConfiguration;

import com.evolveum.midpoint.authentication.impl.module.configuration.ModuleWebSecurityConfigurationImpl;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author skublik
 */
@Component
public class HttpHeaderModuleFactory extends AbstractModuleFactory<
        HttpHeaderModuleWebSecurityConfiguration,
        HttpHeaderModuleWebSecurityConfigurer,
        HttpHeaderAuthenticationModuleType,
        ModuleAuthenticationImpl> {

    private static final Trace LOGGER = TraceManager.getTrace(HttpHeaderModuleFactory.class);

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof HttpHeaderAuthenticationModuleType;
    }

    @Override
    protected HttpHeaderModuleWebSecurityConfigurer createModuleConfigurer(HttpHeaderAuthenticationModuleType moduleType, String sequenceSuffix, AuthenticationChannel authenticationChannel, ObjectPostProcessor<Object> objectPostProcessor, ServletRequest request) {
        return new HttpHeaderModuleWebSecurityConfigurer(moduleType, sequenceSuffix, authenticationChannel, objectPostProcessor, request);
    }

    @Override
    protected ModuleAuthenticationImpl createEmptyModuleAuthentication(HttpHeaderAuthenticationModuleType moduleType, HttpHeaderModuleWebSecurityConfiguration configuration, AuthenticationSequenceModuleType sequenceModule, ServletRequest request) {
        HttpHeaderModuleAuthentication moduleAuthentication = new HttpHeaderModuleAuthentication(sequenceModule);
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        return moduleAuthentication;
    }

    @Override
    public AuthModule<ModuleAuthenticationImpl> createModuleFilter(HttpHeaderAuthenticationModuleType httpModuleType, String sequenceSuffix, ServletRequest request,
                                         Map<Class<?>, Object> sharedObjects, AuthenticationModulesType authenticationsPolicy,
            CredentialsPolicyType credentialPolicy, AuthenticationChannel authenticationChannel, AuthenticationSequenceModuleType sequenceModule) throws Exception {
        if (!(httpModuleType instanceof HttpHeaderAuthenticationModuleType)) {
            LOGGER.error("This factory support only HttpHeaderAuthenticationModuleType, but modelType is " + httpModuleType);
            return null;
        }

        HttpHeaderModuleWebSecurityConfiguration configuration = HttpHeaderModuleWebSecurityConfiguration.build(httpModuleType, sequenceSuffix);
        configuration.addAuthenticationProvider(getObjectObjectPostProcessor().postProcess(new PasswordProvider()));
        HttpHeaderModuleWebSecurityConfigurer module =
                getObjectObjectPostProcessor().postProcess(new HttpHeaderModuleWebSecurityConfigurer(configuration));
        HttpSecurity http = module.getNewHttpSecurity();//getNewHttpSecurity(module);
        setSharedObjects(http, sharedObjects);

        ModuleAuthenticationImpl moduleAuthentication = createEmptyModuleAuthentication(configuration, sequenceModule);
        moduleAuthentication.setFocusType(httpModuleType.getFocusType());
        SecurityFilterChain filter = http.build();
        return AuthModuleImpl.build(filter, configuration, moduleAuthentication);
    }

    private ModuleAuthenticationImpl createEmptyModuleAuthentication(
            ModuleWebSecurityConfigurationImpl configuration, AuthenticationSequenceModuleType sequenceModule) {
        HttpHeaderModuleAuthentication moduleAuthentication = new HttpHeaderModuleAuthentication(sequenceModule);
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        return moduleAuthentication;
    }
}
