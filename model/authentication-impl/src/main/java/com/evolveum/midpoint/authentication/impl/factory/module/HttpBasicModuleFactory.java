/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.factory.module;

import jakarta.servlet.ServletRequest;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.authentication.impl.module.authentication.HttpModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configuration.ModuleWebSecurityConfigurationImpl;
import com.evolveum.midpoint.authentication.impl.module.configurer.HttpBasicModuleWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.provider.PasswordProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */
@Component
public class HttpBasicModuleFactory extends AbstractCredentialModuleFactory<
        ModuleWebSecurityConfigurationImpl,
        HttpBasicModuleWebSecurityConfigurer,
        HttpBasicAuthenticationModuleType,
        ModuleAuthenticationImpl> {

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof HttpBasicAuthenticationModuleType;
    }

    @Override
    protected HttpBasicModuleWebSecurityConfigurer createModuleConfigurer(HttpBasicAuthenticationModuleType moduleType, String sequenceSuffix, AuthenticationChannel authenticationChannel, ObjectPostProcessor<Object> objectPostProcessor, ServletRequest request) {
        return new HttpBasicModuleWebSecurityConfigurer(moduleType, sequenceSuffix, authenticationChannel,
                objectPostProcessor, request,
                new PasswordProvider());
    }

    @Override
    protected Class<? extends CredentialPolicyType> supportedClass() {
        return PasswordCredentialsPolicyType.class;
    }

    @Override
    protected ModuleAuthenticationImpl createEmptyModuleAuthentication(HttpBasicAuthenticationModuleType moduleType,
            ModuleWebSecurityConfigurationImpl configuration, AuthenticationSequenceModuleType sequenceModule, ServletRequest request) {
        HttpModuleAuthentication moduleAuthentication = new HttpModuleAuthentication(AuthenticationModuleNameConstants.HTTP_BASIC, sequenceModule);
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        moduleAuthentication.setCredentialName(moduleType.getCredentialName());
        moduleAuthentication.setCredentialType(supportedClass());
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        moduleAuthentication.setRealm(moduleType.getRealm());
        return moduleAuthentication;
    }

}
