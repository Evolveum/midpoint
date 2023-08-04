/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.factory.module;

import jakarta.servlet.ServletRequest;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.module.authentication.HttpHeaderModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configuration.HttpHeaderModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configurer.HttpHeaderModuleWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.provider.PreAuthenticatedProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.HttpHeaderAuthenticationModuleType;

/**
 * @author skublik
 */
@Component
public class HttpHeaderModuleFactory extends AbstractModuleFactory<
        HttpHeaderModuleWebSecurityConfiguration,
        HttpHeaderModuleWebSecurityConfigurer,
        HttpHeaderAuthenticationModuleType,
        ModuleAuthenticationImpl> {

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof HttpHeaderAuthenticationModuleType;
    }

    @Override
    protected HttpHeaderModuleWebSecurityConfigurer createModuleConfigurer(HttpHeaderAuthenticationModuleType moduleType, String sequenceSuffix, AuthenticationChannel authenticationChannel, ObjectPostProcessor<Object> objectPostProcessor, ServletRequest request) {
        return new HttpHeaderModuleWebSecurityConfigurer(moduleType, sequenceSuffix, authenticationChannel,
                objectPostProcessor, request,
                new PreAuthenticatedProvider());
    }

    @Override
    protected ModuleAuthenticationImpl createEmptyModuleAuthentication(HttpHeaderAuthenticationModuleType moduleType, HttpHeaderModuleWebSecurityConfiguration configuration, AuthenticationSequenceModuleType sequenceModule, ServletRequest request) {
        HttpHeaderModuleAuthentication moduleAuthentication = new HttpHeaderModuleAuthentication(sequenceModule);
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        return moduleAuthentication;
    }

}
