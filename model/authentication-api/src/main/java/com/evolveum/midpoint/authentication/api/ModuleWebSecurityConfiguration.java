/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.api;

import java.util.List;

import org.springframework.security.authentication.AuthenticationProvider;

/**
 * Define configuration for authentication module, contains all variables which we need for creating authentication filters
 * for module and all component which authentication modules uses.
 *
 * @author skublik
 */

public interface ModuleWebSecurityConfiguration {

    String DEFAULT_PREFIX_OF_MODULE = "auth";
    String DEFAULT_PREFIX_OF_MODULE_WITH_SLASH = "/" + DEFAULT_PREFIX_OF_MODULE;
    String DEFAULT_PREFIX_FOR_DEFAULT_MODULE = "/default/";

    void setDefaultSuccessLogoutURL(String defaultSuccessLogoutURL);

    String getDefaultSuccessLogoutURL();

    void setAuthenticationProviders(List<AuthenticationProvider> authenticationProviders);

    void addAuthenticationProvider(AuthenticationProvider authenticationProvider);

    List<AuthenticationProvider> getAuthenticationProviders();

    String getSequenceSuffix();

    void setSequenceSuffix(String sequenceSuffix);

    String getModuleIdentifier();

    void setModuleIdentifier(String moduleIdentifier);

    String getPrefixOfModule();

    String getSpecificLoginUrl();

}
