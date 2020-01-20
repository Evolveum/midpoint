/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.authentication;

import org.springframework.security.authentication.AuthenticationProvider;

import java.util.List;

/**
 * @author skublik
 */

public interface ModuleWebSecurityConfiguration {

    public static final String DEFAULT_PREFIX_OF_MODULE = "auth";
    public static final String DEFAULT_PREFIX_OF_MODULE_WITH_SLASH = "/" + DEFAULT_PREFIX_OF_MODULE;
    public static final String DEFAULT_PREFIX_FOR_DEFAULT_MODULE = "/default/";

    public void setDefaultSuccessLogoutURL(String defaultSuccessLogoutURL);

    public String getDefaultSuccessLogoutURL();

    public void setAuthenticationProviders(List<AuthenticationProvider> authenticationProviders);

    public void addAuthenticationProvider(AuthenticationProvider authenticationProvider);

    public List<AuthenticationProvider> getAuthenticationProviders();

    public String getPrefixOfSequence();

    public void setPrefixOfSequence(String prefixOfSequence);

    public String getNameOfModule();

    public void setNameOfModule(String nameOfModule);

    public String getPrefix();

    public String getSpecificLoginUrl();

}
