/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.factory.module;

import jakarta.servlet.ServletRequest;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.module.authentication.ArchetypeSelectionModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configuration.LoginFormModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configurer.ArchetypeSelectionModuleWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.provider.ArchetypeSelectionAuthenticationProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeSelectionModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;

@Component
public class ArchetypeSelectionModuleFactory extends AbstractModuleFactory<
        LoginFormModuleWebSecurityConfiguration,
        ArchetypeSelectionModuleWebSecurityConfigurer,
        ArchetypeSelectionModuleType,
        ArchetypeSelectionModuleAuthenticationImpl> {


    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof ArchetypeSelectionModuleType;
    }

    @Override
    protected ArchetypeSelectionModuleAuthenticationImpl createEmptyModuleAuthentication(ArchetypeSelectionModuleType moduleType,
            LoginFormModuleWebSecurityConfiguration configuration, AuthenticationSequenceModuleType sequenceModule, ServletRequest request) {
        ArchetypeSelectionModuleAuthenticationImpl moduleAuthentication = new ArchetypeSelectionModuleAuthenticationImpl(sequenceModule);
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        moduleAuthentication.setCredentialName(moduleType.getCredentialName());
        moduleAuthentication.setNameOfModule(moduleType.getIdentifier());
        moduleAuthentication.setAllowUndefined(BooleanUtils.isTrue(moduleType.isAllowUndefinedArchetype()));
        moduleAuthentication.setArchetypeSelection(moduleType.getArchetypeSelection());
        return moduleAuthentication;
    }

    @Override
    protected ArchetypeSelectionModuleWebSecurityConfigurer createModuleConfigurer(
            ArchetypeSelectionModuleType moduleType,
            String sequenceSuffix,
            AuthenticationChannel authenticationChannel,
            ObjectPostProcessor<Object> objectPostProcessor, ServletRequest request) {
        return new ArchetypeSelectionModuleWebSecurityConfigurer(moduleType, sequenceSuffix,
                authenticationChannel, objectPostProcessor, request,
                new ArchetypeSelectionAuthenticationProvider());
    }

}
