/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.util;

import com.evolveum.midpoint.authentication.api.AuthModule;

import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.ModuleWebSecurityConfiguration;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/**
 * @author skublik
 */

public class AuthModuleImpl<MA extends ModuleAuthentication> implements AuthModule<MA> {

    @VisibleForTesting
    public AuthModuleImpl(){

    }

    private SecurityFilterChain securityFilterChain;

    private ModuleWebSecurityConfiguration configuration;

    private MA baseModuleAuthentication;

    public SecurityFilterChain getSecurityFilterChain() {
        return securityFilterChain;
    }

    private void setSecurityFilterChain(SecurityFilterChain securityFilterChain) {
        this.securityFilterChain = securityFilterChain;
    }

    private void setConfiguration(ModuleWebSecurityConfiguration configuration) {
        this.configuration = configuration;
    }

    public MA getBaseModuleAuthentication() {
        return (MA) baseModuleAuthentication.clone();
    }

    @Override
    public Integer getOrder() {
        return baseModuleAuthentication.getOrder();
    }

    @Override
    public List<AuthenticationProvider> getAuthenticationProviders() {
        return configuration != null ? configuration.getAuthenticationProviders() : null;
    }

    @Override
    public String getModuleIdentifier() {
        return configuration.getModuleIdentifier();
    }

    private void setBaseModuleAuthentication(MA baseModuleAuthentication) {
        this.baseModuleAuthentication = baseModuleAuthentication;
    }

    public static AuthModule build(SecurityFilterChain securityFilterChain, ModuleWebSecurityConfiguration configuration,
                                       ModuleAuthentication baseModuleAuthentication) {
        Validate.notNull(securityFilterChain, "Couldn't build AuthModuleImpl, because filter is null");
        Validate.notNull(configuration, "Couldn't build AuthModuleImpl, because configuration is null");
        Validate.notNull(baseModuleAuthentication, "Couldn't build AuthModuleImpl, because base authentication module is null");
        AuthModuleImpl module = new AuthModuleImpl();
        module.setSecurityFilterChain(securityFilterChain);
        module.setConfiguration(configuration);
        module.setBaseModuleAuthentication(baseModuleAuthentication);
        return module;
    }
}
