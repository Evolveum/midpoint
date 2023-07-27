/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.factory.module;

import com.evolveum.midpoint.authentication.impl.provider.SecurityQuestionProvider;
import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.module.configurer.SecurityQuestionsFormModuleWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.module.authentication.SecurityQuestionFormModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.configuration.LoginFormModuleWebSecurityConfiguration;

import jakarta.servlet.ServletRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */
@Component
public class SecurityQuestionFormModuleFactory extends AbstractCredentialModuleFactory<
        LoginFormModuleWebSecurityConfiguration,
        SecurityQuestionsFormModuleWebSecurityConfigurer<LoginFormModuleWebSecurityConfiguration>,
        SecurityQuestionsFormAuthenticationModuleType,
        SecurityQuestionFormModuleAuthentication> {

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof SecurityQuestionsFormAuthenticationModuleType;
    }

    @Override
    protected SecurityQuestionsFormModuleWebSecurityConfigurer<LoginFormModuleWebSecurityConfiguration> createModuleConfigurer(SecurityQuestionsFormAuthenticationModuleType moduleType, String sequenceSuffix, AuthenticationChannel authenticationChannel, ObjectPostProcessor<Object> objectPostProcessor, ServletRequest request) {
        return new SecurityQuestionsFormModuleWebSecurityConfigurer<>(moduleType, sequenceSuffix, authenticationChannel, objectPostProcessor, request);
    }

    @Override
    protected AuthenticationProvider createProvider(CredentialPolicyType usedPolicy) {
        return new SecurityQuestionProvider();
    }

    @Override
    protected Class<? extends CredentialPolicyType> supportedClass() {
        return SecurityQuestionsCredentialsPolicyType.class;
    }

    @Override
    protected SecurityQuestionFormModuleAuthentication createEmptyModuleAuthentication(SecurityQuestionsFormAuthenticationModuleType moduleType,
            LoginFormModuleWebSecurityConfiguration configuration, AuthenticationSequenceModuleType sequenceModule, ServletRequest request) {
        SecurityQuestionFormModuleAuthentication moduleAuthentication = new SecurityQuestionFormModuleAuthentication(sequenceModule);
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        moduleAuthentication.setCredentialName(moduleType.getCredentialName());
        moduleAuthentication.setCredentialType(supportedClass());
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        return moduleAuthentication;
    }

}
