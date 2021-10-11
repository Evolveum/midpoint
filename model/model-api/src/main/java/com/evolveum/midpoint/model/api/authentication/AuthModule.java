/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.authentication;

import org.springframework.security.web.SecurityFilterChain;

/**
 * @author skublik
 */

public interface AuthModule {

    public SecurityFilterChain getSecurityFilterChain();

    public ModuleWebSecurityConfiguration getConfiguration();

    public ModuleAuthentication getBaseModuleAuthentication();

}
