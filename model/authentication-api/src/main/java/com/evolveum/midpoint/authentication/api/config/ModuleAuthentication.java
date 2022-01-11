/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.api.config;

import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.springframework.security.core.Authentication;

import javax.xml.namespace.QName;

/**
 * Wrapper for authentication module, provide all information about actual state
 *
 * @author skublik
 */

public interface ModuleAuthentication {

    /**
     * @return name of authentication module, get from configuration
     */
    String getNameOfModule();

    /**
     * @return type of authentication module
     */
    String getNameOfModuleType();

    /**
     * @return state of module
     */
    AuthenticationModuleState getState();

    void setState(AuthenticationModuleState state);

    /**
     * @return authentication token for module
     */
    Authentication getAuthentication();

    @Experimental
    void setAuthentication(Authentication authentication);

    /**
     * @return prefix used in url
     */
    String getPrefix();

    /**
     * @return type of authenticated object, get from configuration
     */
    QName getFocusType();
}
