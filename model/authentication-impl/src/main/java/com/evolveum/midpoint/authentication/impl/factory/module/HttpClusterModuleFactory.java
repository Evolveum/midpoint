/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.factory.module;

import java.util.Map;
import jakarta.servlet.ServletRequest;

import com.evolveum.midpoint.authentication.api.AuthModule;
import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.authentication.api.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.provider.ClusterProvider;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.util.AuthModuleImpl;
import com.evolveum.midpoint.authentication.impl.module.configurer.HttpClusterModuleWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.module.configuration.ModuleWebSecurityConfigurationImpl;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

/**
 * @author skublik
 */
@Component
public class HttpClusterModuleFactory extends AbstractModuleFactory<AbstractAuthenticationModuleType, ModuleAuthenticationImpl> {

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return false;
    }

    @Override
    public AuthModule<ModuleAuthenticationImpl> createModuleFilter(AbstractAuthenticationModuleType moduleType, String sequenceSuffix,
            ServletRequest request, Map<Class<?>, Object> sharedObjects,
            AuthenticationModulesType authenticationsPolicy, CredentialsPolicyType credentialPolicy,
            AuthenticationChannel authenticationChannel, AuthenticationSequenceModuleType sequenceModule) throws Exception {

        ModuleWebSecurityConfiguration configuration = createConfiguration(moduleType, sequenceSuffix);

        configuration.addAuthenticationProvider(createProvider());

        HttpClusterModuleWebSecurityConfigurer<ModuleWebSecurityConfiguration> module = createModule(configuration);
        HttpSecurity http = getNewHttpSecurity(module);
        setSharedObjects(http, sharedObjects);

        ModuleAuthenticationImpl moduleAuthentication = createEmptyModuleAuthentication(configuration, sequenceModule);
        SecurityFilterChain filter = http.build();
        return AuthModuleImpl.build(filter, configuration, moduleAuthentication);
    }

    private ModuleWebSecurityConfiguration createConfiguration(AbstractAuthenticationModuleType moduleType, String prefixOfSequence) {
        ModuleWebSecurityConfigurationImpl configuration = ModuleWebSecurityConfigurationImpl.build(moduleType,prefixOfSequence);
        configuration.setSequenceSuffix(prefixOfSequence);
        return configuration;
    }

    private HttpClusterModuleWebSecurityConfigurer<ModuleWebSecurityConfiguration> createModule(ModuleWebSecurityConfiguration configuration) {
        return getObjectObjectPostProcessor().postProcess(new HttpClusterModuleWebSecurityConfigurer<>(configuration));
    }

    private AuthenticationProvider createProvider() {
        return getObjectObjectPostProcessor().postProcess(new ClusterProvider());
    }

    private ModuleAuthenticationImpl createEmptyModuleAuthentication(ModuleWebSecurityConfiguration configuration, AuthenticationSequenceModuleType sequenceModule) {
        ModuleAuthenticationImpl moduleAuthentication = new ModuleAuthenticationImpl(AuthenticationModuleNameConstants.CLUSTER, sequenceModule);
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        return moduleAuthentication;
    }

}
