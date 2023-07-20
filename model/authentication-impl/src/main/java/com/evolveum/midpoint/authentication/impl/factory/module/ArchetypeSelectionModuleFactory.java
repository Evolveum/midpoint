/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.factory.module;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.module.authentication.ArchetypeSelectionModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configuration.LoginFormModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configurer.ArchetypeSelectionModuleWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.provider.ArchetypeSelectionAuthenticationProvider;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.stereotype.Component;

@Component
public class ArchetypeSelectionModuleFactory extends AbstractCredentialModuleFactory<LoginFormModuleWebSecurityConfiguration, ArchetypeSelectionModuleWebSecurityConfigurer<LoginFormModuleWebSecurityConfiguration>> {

    private static final Trace LOGGER = TraceManager.getTrace(ArchetypeSelectionModuleFactory.class);

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof ArchetypeSelectionModuleType;
    }

    @Override
    protected ModuleAuthenticationImpl createEmptyModuleAuthentication(AbstractAuthenticationModuleType moduleType, LoginFormModuleWebSecurityConfiguration configuration, AuthenticationSequenceModuleType sequenceModule) {
        ArchetypeSelectionModuleAuthentication moduleAuthentication = new ArchetypeSelectionModuleAuthentication(sequenceModule);
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        moduleAuthentication.setCredentialName(((AbstractCredentialAuthenticationModuleType)moduleType).getCredentialName());
        moduleAuthentication.setCredentialType(supportedClass());
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        return moduleAuthentication;
    }

    @Override
    protected LoginFormModuleWebSecurityConfiguration createConfiguration(AbstractAuthenticationModuleType moduleType, String prefixOfSequence, AuthenticationChannel authenticationChannel) {
        LoginFormModuleWebSecurityConfiguration configuration = LoginFormModuleWebSecurityConfiguration.build(moduleType,prefixOfSequence);
        configuration.setSequenceSuffix(prefixOfSequence);
        return configuration;
    }

    @Override
    protected ArchetypeSelectionModuleWebSecurityConfigurer<LoginFormModuleWebSecurityConfiguration> createModule(LoginFormModuleWebSecurityConfiguration configuration) {
        return  getObjectObjectPostProcessor().postProcess(new ArchetypeSelectionModuleWebSecurityConfigurer<>(configuration));
    }

    @Override
    protected AuthenticationProvider createProvider(CredentialPolicyType usedPolicy) {
        return new ArchetypeSelectionAuthenticationProvider();
    }

    @Override
    protected Class<? extends CredentialPolicyType> supportedClass() {
        return null;
    }

}
