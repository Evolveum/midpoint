/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.factory.module;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.module.authentication.AttributeVerificationModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.configuration.LoginFormModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configurer.AttributeVerificationModuleWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.provider.AttributeVerificationProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.servlet.ServletRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class AttributeVerificationModuleFactory extends AbstractCredentialModuleFactory<
        LoginFormModuleWebSecurityConfiguration,
        AttributeVerificationModuleWebSecurityConfigurer<LoginFormModuleWebSecurityConfiguration>,
        AttributeVerificationAuthenticationModuleType,
        AttributeVerificationModuleAuthentication> {

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof AttributeVerificationAuthenticationModuleType;
    }

    @Override
    protected AttributeVerificationModuleWebSecurityConfigurer<LoginFormModuleWebSecurityConfiguration> createModuleConfigurer(
            AttributeVerificationAuthenticationModuleType moduleType,
            String sequenceSuffix,
            AuthenticationChannel authenticationChannel,
            ObjectPostProcessor<Object> objectPostProcessor, ServletRequest request) {
        return new AttributeVerificationModuleWebSecurityConfigurer<>(moduleType, sequenceSuffix, authenticationChannel, objectPostProcessor, request);
//        return null;
    }

    @Override
    protected AuthenticationProvider createProvider(CredentialPolicyType usedPolicy) {
        return new AttributeVerificationProvider();
    }

    @Override
    protected Class<? extends CredentialPolicyType> supportedClass() {
        return null;    //todo for now we don't have credentials policy for attribute verification
    }

    @Override
    protected AttributeVerificationModuleAuthentication createEmptyModuleAuthentication(AttributeVerificationAuthenticationModuleType moduleType,
            LoginFormModuleWebSecurityConfiguration configuration, AuthenticationSequenceModuleType sequenceModule, ServletRequest request) {
        AttributeVerificationModuleAuthentication moduleAuthentication = new AttributeVerificationModuleAuthentication(sequenceModule);
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        moduleAuthentication.setCredentialName(moduleType.getCredentialName());
        moduleAuthentication.setCredentialType(supportedClass());
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        return moduleAuthentication;
    }

}
