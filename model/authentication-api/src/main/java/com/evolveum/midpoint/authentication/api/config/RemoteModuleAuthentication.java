/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.api.config;

import com.evolveum.midpoint.authentication.api.IdentityProvider;

import java.util.List;

/**
 * Interface for authentication module for remote authentication module
 *
 * @author skublik
 */

public interface RemoteModuleAuthentication extends ModuleAuthentication {

    /**
     * @return identity provider created by configuration
     */
    List<IdentityProvider> getProviders();
}
