/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.factory.module;

import com.evolveum.midpoint.authentication.impl.provider.PasswordProvider;
import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.api.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configurer.HttpBasicModuleWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.module.authentication.HttpModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.configuration.ModuleWebSecurityConfigurationImpl;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.stereotype.Component;

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
    protected HttpBasicModuleWebSecurityConfigurer createModule(ModuleWebSecurityConfigurationImpl configuration) {
        return  getObjectObjectPostProcessor().postProcess(new HttpBasicModuleWebSecurityConfigurer(configuration));
    }

    @Override
    protected HttpBasicModuleWebSecurityConfigurer createModuleConfigurer(HttpBasicAuthenticationModuleType moduleType, String sequenceSuffix, AuthenticationChannel authenticationChannel, ObjectPostProcessor<Object> objectPostProcessor) {
        return new HttpBasicModuleWebSecurityConfigurer(moduleType, sequenceSuffix, authenticationChannel, objectPostProcessor);
    }

    @Override
    protected AuthenticationProvider createProvider(CredentialPolicyType usedPolicy) {
        return new PasswordProvider();
    }

    @Override
    protected Class<? extends CredentialPolicyType> supportedClass() {
        return PasswordCredentialsPolicyType.class;
    }

    @Override
    protected ModuleAuthenticationImpl createEmptyModuleAuthentication(HttpBasicAuthenticationModuleType moduleType,
            ModuleWebSecurityConfigurationImpl configuration, AuthenticationSequenceModuleType sequenceModule) {
        HttpModuleAuthentication moduleAuthentication = new HttpModuleAuthentication(AuthenticationModuleNameConstants.HTTP_BASIC, sequenceModule);
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        moduleAuthentication.setCredentialName(moduleType.getCredentialName());
        moduleAuthentication.setCredentialType(supportedClass());
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        moduleAuthentication.setRealm(moduleType.getRealm());
        return moduleAuthentication;
    }

}
