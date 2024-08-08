/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.api;

import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/**
 * Define authentication module created by module configuration, with all filters and configuration
 *
 * @author skublik
 */

public interface AuthModule<MA extends ModuleAuthentication> {

    /**
     * @return module authentication (result after authentication process)
     */
    MA getBaseModuleAuthentication();

    String getModuleIdentifier();

    /**
     * @return order of authentication module
     */
    Integer getOrder();

    List<AuthenticationProvider> getAuthenticationProviders();

    SecurityFilterChain getSecurityFilterChain();

}
