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
import com.evolveum.midpoint.authentication.impl.module.configuration.ModuleWebSecurityConfigurationImpl;
import com.evolveum.midpoint.authentication.impl.module.configurer.HttpSecurityQuestionsModuleWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.provider.SecurityQuestionProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */
@Component
public class HttpSecurityQuestionModuleFactory extends AbstractCredentialModuleFactory<
        ModuleWebSecurityConfigurationImpl,
        HttpSecurityQuestionsModuleWebSecurityConfigurer,
        HttpSecQAuthenticationModuleType,
        HttpModuleAuthentication> {

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof HttpSecQAuthenticationModuleType;
    }

    @Override
    protected HttpSecurityQuestionsModuleWebSecurityConfigurer createModuleConfigurer(HttpSecQAuthenticationModuleType moduleType, String sequenceSuffix, AuthenticationChannel authenticationChannel, ObjectPostProcessor<Object> objectPostProcessor, ServletRequest request) {
        return new HttpSecurityQuestionsModuleWebSecurityConfigurer(moduleType, sequenceSuffix, authenticationChannel,
                objectPostProcessor, request,
                new SecurityQuestionProvider());
    }

    @Override
    protected Class<? extends CredentialPolicyType> supportedClass() {
        return SecurityQuestionsCredentialsPolicyType.class;
    }

    @Override
    protected HttpModuleAuthentication createEmptyModuleAuthentication(HttpSecQAuthenticationModuleType moduleType,
            ModuleWebSecurityConfigurationImpl configuration, AuthenticationSequenceModuleType sequenceModule, ServletRequest request) {
        HttpModuleAuthentication moduleAuthentication = new HttpModuleAuthentication(AuthenticationModuleNameConstants.SECURITY_QUESTIONS, sequenceModule);
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        moduleAuthentication.setCredentialName(moduleType.getCredentialName());
        moduleAuthentication.setCredentialType(supportedClass());
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        moduleAuthentication.setRealm(moduleType.getRealm());
        return moduleAuthentication;
    }

}
