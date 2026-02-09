/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.otp;

import jakarta.servlet.ServletRequest;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.factory.module.AbstractCredentialModuleFactory;
import com.evolveum.midpoint.authentication.impl.module.configuration.LoginFormModuleWebSecurityConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class OtpModuleFactory extends AbstractCredentialModuleFactory<
        LoginFormModuleWebSecurityConfiguration,
        OtpModuleWebSecurityConfigurer,
        OtpAuthenticationModuleType,
        OtpModuleAuthenticationImpl> {

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof OtpAuthenticationModuleType;
    }

    @Override
    protected OtpModuleWebSecurityConfigurer createModuleConfigurer(
            OtpAuthenticationModuleType moduleType,
            String sequenceSuffix,
            AuthenticationChannel authenticationChannel,
            ObjectPostProcessor<Object> objectPostProcessor,
            ServletRequest request) {

        return new OtpModuleWebSecurityConfigurer(
                moduleType,
                sequenceSuffix,
                authenticationChannel,
                objectPostProcessor,
                request,
                new OtpAuthenticationProvider(moduleType));
    }

    @Override
    protected Class<? extends CredentialPolicyType> supportedClass() {
        return OtpCredentialsPolicyType.class;
    }

    @Override
    protected OtpModuleAuthenticationImpl createEmptyModuleAuthentication(
            OtpAuthenticationModuleType moduleType,
            LoginFormModuleWebSecurityConfiguration configuration,
            AuthenticationSequenceModuleType sequenceModule,
            ServletRequest request) {

        OtpModuleAuthenticationImpl auth = new OtpModuleAuthenticationImpl(sequenceModule);
        auth.setPrefix(configuration.getPrefixOfModule());
        auth.setCredentialName(moduleType.getCredentialName());
        auth.setCredentialType(supportedClass());
        auth.setNameOfModule(configuration.getModuleIdentifier());

        auth.setModule(moduleType);

        return auth;
    }
}
