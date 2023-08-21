/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.factory.module;

import jakarta.servlet.ServletRequest;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.module.authentication.AttributeVerificationModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configuration.LoginFormModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configurer.AttributeVerificationModuleWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.provider.AttributeVerificationProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeVerificationAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;

@Component
public class AttributeVerificationModuleFactory extends AbstractModuleFactory<
        LoginFormModuleWebSecurityConfiguration,
        AttributeVerificationModuleWebSecurityConfigurer,
        AttributeVerificationAuthenticationModuleType,
        AttributeVerificationModuleAuthenticationImpl> {

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof AttributeVerificationAuthenticationModuleType;
    }

    @Override
    protected AttributeVerificationModuleWebSecurityConfigurer createModuleConfigurer(
            AttributeVerificationAuthenticationModuleType moduleType,
            String sequenceSuffix,
            AuthenticationChannel authenticationChannel,
            ObjectPostProcessor<Object> objectPostProcessor, ServletRequest request) {
        return new AttributeVerificationModuleWebSecurityConfigurer(moduleType, sequenceSuffix,
                authenticationChannel, objectPostProcessor, request,
                new AttributeVerificationProvider());
    }

    @Override
    protected AttributeVerificationModuleAuthenticationImpl createEmptyModuleAuthentication(AttributeVerificationAuthenticationModuleType moduleType,
                                                                                            LoginFormModuleWebSecurityConfiguration configuration, AuthenticationSequenceModuleType sequenceModule, ServletRequest request) {
        AttributeVerificationModuleAuthenticationImpl moduleAuthentication = new AttributeVerificationModuleAuthenticationImpl(sequenceModule);
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        moduleAuthentication.setCredentialName(moduleType.getCredentialName());
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        moduleAuthentication.setPathsToVerify(moduleType.getPath());
        return moduleAuthentication;
    }

}
