/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.authentication;

import com.evolveum.midpoint.web.security.module.configuration.ModuleWebSecurityConfigurationImpl;
import org.apache.commons.lang3.Validate;
import org.springframework.security.web.SecurityFilterChain;

/**
 * @author skublik
 */

public class AuthModuleImpl implements AuthModule {

    private AuthModuleImpl(){

    }

    private SecurityFilterChain securityFilterChain;

    private ModuleWebSecurityConfiguration configuration;

    private ModuleAuthentication baseModuleAuthentication;

    public SecurityFilterChain getSecurityFilterChain() {
        return securityFilterChain;
    }

    private void setSecurityFilterChain(SecurityFilterChain securityFilterChain) {
        this.securityFilterChain = securityFilterChain;
    }

    public ModuleWebSecurityConfiguration getConfiguration() {
        return configuration;
    }

    private void setConfiguration(ModuleWebSecurityConfiguration configuration) {
        this.configuration = configuration;
    }

    public ModuleAuthentication getBaseModuleAuthentication() {
        return baseModuleAuthentication.clone();
    }

    private void setBaseModuleAuthentication(ModuleAuthentication baseModuleAuthentication) {
        this.baseModuleAuthentication = baseModuleAuthentication;
    }

    public static AuthModule build(SecurityFilterChain securityFilterChain, ModuleWebSecurityConfiguration configuration,
                                       ModuleAuthentication baseModuleAuthentication) {
        Validate.notNull(securityFilterChain);
        Validate.notNull(configuration);
        Validate.notNull(baseModuleAuthentication);
        AuthModuleImpl module = new AuthModuleImpl();
        module.setSecurityFilterChain(securityFilterChain);
        module.setConfiguration(configuration);
        module.setBaseModuleAuthentication(baseModuleAuthentication);
        return module;
    }
}
