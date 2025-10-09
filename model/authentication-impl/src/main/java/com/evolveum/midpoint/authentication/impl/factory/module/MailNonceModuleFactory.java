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
import com.evolveum.midpoint.authentication.impl.module.authentication.MailNonceModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configuration.ModuleWebSecurityConfigurationImpl;
import com.evolveum.midpoint.authentication.impl.module.configurer.MailNonceFormModuleWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.provider.MailNonceProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */
@Component
public class MailNonceModuleFactory extends AbstractCredentialModuleFactory<
        ModuleWebSecurityConfigurationImpl,
        MailNonceFormModuleWebSecurityConfigurer,
        MailNonceAuthenticationModuleType,
        MailNonceModuleAuthenticationImpl> {

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof MailNonceAuthenticationModuleType;
    }

    @Override
    protected MailNonceFormModuleWebSecurityConfigurer createModuleConfigurer(MailNonceAuthenticationModuleType moduleType, String sequenceSuffix, AuthenticationChannel authenticationChannel, ObjectPostProcessor<Object> objectPostProcessor, ServletRequest request) {
        return new MailNonceFormModuleWebSecurityConfigurer(moduleType, sequenceSuffix, authenticationChannel,
                objectPostProcessor, request,
                new MailNonceProvider());
    }

    @Override
    protected Class<? extends CredentialPolicyType> supportedClass() {
        return NonceCredentialsPolicyType.class;
    }

    @Override
    protected MailNonceModuleAuthenticationImpl createEmptyModuleAuthentication(MailNonceAuthenticationModuleType moduleType,
            ModuleWebSecurityConfigurationImpl configuration, AuthenticationSequenceModuleType sequenceModule, ServletRequest request) {
        MailNonceModuleAuthenticationImpl moduleAuthentication = new MailNonceModuleAuthenticationImpl(sequenceModule);
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        moduleAuthentication.setCredentialName(moduleType.getCredentialName());
        moduleAuthentication.setCredentialType(supportedClass());
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        return moduleAuthentication;
    }

    @Override
    protected void isSupportedChannel(AuthenticationChannel authenticationChannel) {
        //supported for all modules
    }

}
