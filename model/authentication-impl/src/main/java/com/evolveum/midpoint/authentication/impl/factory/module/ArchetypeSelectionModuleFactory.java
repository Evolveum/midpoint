/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.factory.module;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.module.authentication.ArchetypeSelectionModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.configuration.LoginFormModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configurer.ArchetypeSelectionModuleWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.provider.ArchetypeSelectionAuthenticationProvider;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.servlet.ServletRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class ArchetypeSelectionModuleFactory extends AbstractModuleFactory<
        LoginFormModuleWebSecurityConfiguration,
        ArchetypeSelectionModuleWebSecurityConfigurer<LoginFormModuleWebSecurityConfiguration>,
        ArchetypeSelectionModuleType,
        ArchetypeSelectionModuleAuthentication> {

    private static final Trace LOGGER = TraceManager.getTrace(ArchetypeSelectionModuleFactory.class);

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof ArchetypeSelectionModuleType;
    }

    @Override
    protected ArchetypeSelectionModuleAuthentication createEmptyModuleAuthentication(ArchetypeSelectionModuleType moduleType,
            LoginFormModuleWebSecurityConfiguration configuration, AuthenticationSequenceModuleType sequenceModule, ServletRequest request) {
        ArchetypeSelectionModuleAuthentication moduleAuthentication = new ArchetypeSelectionModuleAuthentication(sequenceModule);
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        moduleAuthentication.setCredentialName(moduleType.getCredentialName());
        moduleAuthentication.setNameOfModule(moduleType.getIdentifier());
        return moduleAuthentication;
    }

    @Override
    protected ArchetypeSelectionModuleWebSecurityConfigurer<LoginFormModuleWebSecurityConfiguration> createModuleConfigurer(
            ArchetypeSelectionModuleType moduleType,
            String sequenceSuffix,
            AuthenticationChannel authenticationChannel,
            ObjectPostProcessor<Object> objectPostProcessor, ServletRequest request) {
        return new ArchetypeSelectionModuleWebSecurityConfigurer<>(moduleType, sequenceSuffix,
                authenticationChannel, objectPostProcessor, request,
                new ArchetypeSelectionAuthenticationProvider());
//        return null;
    }

}
