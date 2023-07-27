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
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

/**
 * @author skublik
 */
@Component
public class HttpClusterModuleFactory extends AbstractModuleFactory<
        ModuleWebSecurityConfigurationImpl,
        HttpClusterModuleWebSecurityConfigurer,
        AbstractAuthenticationModuleType,
        ModuleAuthenticationImpl> {

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return false;
    }

    @Override
    protected HttpClusterModuleWebSecurityConfigurer createModuleConfigurer(AbstractAuthenticationModuleType moduleType, String sequenceSuffix, AuthenticationChannel authenticationChannel, ObjectPostProcessor<Object> objectPostProcessor, ServletRequest request) {
        return new HttpClusterModuleWebSecurityConfigurer(moduleType, sequenceSuffix, authenticationChannel,
                objectPostProcessor, request,
                new ClusterProvider());
    }

    @Override
    protected ModuleAuthenticationImpl createEmptyModuleAuthentication(AbstractAuthenticationModuleType moduleType, ModuleWebSecurityConfigurationImpl configuration, AuthenticationSequenceModuleType sequenceModule, ServletRequest request) {
        ModuleAuthenticationImpl moduleAuthentication = new ModuleAuthenticationImpl(AuthenticationModuleNameConstants.CLUSTER, sequenceModule);
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        return moduleAuthentication;
    }

    public AuthModule<ModuleAuthenticationImpl> createModuleFilter(AbstractAuthenticationModuleType moduleType, String sequenceSuffix,
            ServletRequest request, Map<Class<?>, Object> sharedObjects,
            AuthenticationModulesType authenticationsPolicy, CredentialsPolicyType credentialPolicy,
            AuthenticationChannel authenticationChannel, AuthenticationSequenceModuleType sequenceModule) throws Exception {

        ModuleWebSecurityConfigurationImpl configuration = createConfiguration(moduleType, sequenceSuffix);

        configuration.addAuthenticationProvider(createProvider());

        HttpClusterModuleWebSecurityConfigurer module = createModule(configuration);
        HttpSecurity http = module.getNewHttpSecurity(); //getNewHttpSecurity(module);
        setSharedObjects(http, sharedObjects);

        ModuleAuthenticationImpl moduleAuthentication = createEmptyModuleAuthentication(configuration, sequenceModule);
        SecurityFilterChain filter = http.build();
        return AuthModuleImpl.build(filter, configuration, moduleAuthentication);
    }

    private ModuleWebSecurityConfigurationImpl createConfiguration(AbstractAuthenticationModuleType moduleType, String prefixOfSequence) {
        ModuleWebSecurityConfigurationImpl configuration = ModuleWebSecurityConfigurationImpl.build(moduleType,prefixOfSequence);
        configuration.setSequenceSuffix(prefixOfSequence);
        return configuration;
    }

    private HttpClusterModuleWebSecurityConfigurer createModule(ModuleWebSecurityConfigurationImpl configuration) {
        return getObjectObjectPostProcessor().postProcess(new HttpClusterModuleWebSecurityConfigurer(configuration));
    }

    private AuthenticationProvider createProvider() {
        return getObjectObjectPostProcessor().postProcess(new ClusterProvider());
    }

    private ModuleAuthenticationImpl createEmptyModuleAuthentication(ModuleWebSecurityConfigurationImpl configuration, AuthenticationSequenceModuleType sequenceModule) {
        ModuleAuthenticationImpl moduleAuthentication = new ModuleAuthenticationImpl(AuthenticationModuleNameConstants.CLUSTER, sequenceModule);
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        return moduleAuthentication;
    }

}
