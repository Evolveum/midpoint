/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.security.factory.module;

import java.util.Map;
import javax.servlet.ServletRequest;

import com.evolveum.midpoint.authentication.api.AuthModule;
import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.authentication.api.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.security.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.security.provider.ClusterProvider;
import com.evolveum.midpoint.authentication.impl.security.util.AuthModuleImpl;
import com.evolveum.midpoint.authentication.impl.security.module.configurer.HttpClusterModuleWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.security.module.configuration.ModuleWebSecurityConfigurationImpl;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationModulesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsPolicyType;

/**
 * @author skublik
 */
@Component
public class HttpClusterModuleFactory extends AbstractModuleFactory {

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType) {
        return false;
    }

    @Override
    public AuthModule createModuleFilter(AbstractAuthenticationModuleType moduleType, String prefixOfSequence,
                                         ServletRequest request, Map<Class<?>, Object> sharedObjects,
                                         AuthenticationModulesType authenticationsPolicy, CredentialsPolicyType credentialPolicy, AuthenticationChannel authenticationChannel) throws Exception {

        ModuleWebSecurityConfiguration configuration = createConfiguration(moduleType, prefixOfSequence);

        configuration.addAuthenticationProvider(createProvider());

        HttpClusterModuleWebSecurityConfigurer<ModuleWebSecurityConfiguration> module = createModule(configuration);
        module.setObjectPostProcessor(getObjectObjectPostProcessor());
        HttpSecurity http = module.getNewHttpSecurity();
        setSharedObjects(http, sharedObjects);

        ModuleAuthenticationImpl moduleAuthentication = createEmptyModuleAuthentication(configuration);
        SecurityFilterChain filter = http.build();
        return AuthModuleImpl.build(filter, configuration, moduleAuthentication);
    }

    private ModuleWebSecurityConfiguration createConfiguration(AbstractAuthenticationModuleType moduleType, String prefixOfSequence) {
        ModuleWebSecurityConfigurationImpl configuration = ModuleWebSecurityConfigurationImpl.build(moduleType,prefixOfSequence);
        configuration.setPrefixOfSequence(prefixOfSequence);
        return configuration;
    }

    private HttpClusterModuleWebSecurityConfigurer<ModuleWebSecurityConfiguration> createModule(ModuleWebSecurityConfiguration configuration) {
        return getObjectObjectPostProcessor().postProcess(new HttpClusterModuleWebSecurityConfigurer<>(configuration));
    }

    private AuthenticationProvider createProvider() {
        return getObjectObjectPostProcessor().postProcess(new ClusterProvider());
    }

    private ModuleAuthenticationImpl createEmptyModuleAuthentication(ModuleWebSecurityConfiguration configuration) {
        ModuleAuthenticationImpl moduleAuthentication = new ModuleAuthenticationImpl(AuthenticationModuleNameConstants.CLUSTER);
        moduleAuthentication.setPrefix(configuration.getPrefix());
        moduleAuthentication.setNameOfModule(configuration.getNameOfModule());
        return moduleAuthentication;
    }

}
