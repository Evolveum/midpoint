/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.security.module;

import com.evolveum.midpoint.authentication.impl.security.filter.LdapAuthenticationFilter;
import com.evolveum.midpoint.authentication.impl.security.filter.configurers.MidpointFormLoginConfigurer;
import com.evolveum.midpoint.authentication.impl.security.module.configuration.LdapModuleWebSecurityConfiguration;

/**
 * @author lskublik
 */

public class LdapWebSecurityConfig<C extends LdapModuleWebSecurityConfiguration> extends LoginFormModuleWebSecurityConfig<C>{

    public LdapWebSecurityConfig(C configuration) {
        super(configuration);
    }

    protected MidpointFormLoginConfigurer getMidpointFormLoginConfiguration() {
        return new MidpointFormLoginConfigurer(new LdapAuthenticationFilter());
    }
}
