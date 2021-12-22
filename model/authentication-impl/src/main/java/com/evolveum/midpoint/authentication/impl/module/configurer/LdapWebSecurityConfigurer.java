/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.module.configurer;

import com.evolveum.midpoint.authentication.impl.filter.LdapAuthenticationFilter;
import com.evolveum.midpoint.authentication.impl.filter.configurers.MidpointFormLoginConfigurer;
import com.evolveum.midpoint.authentication.impl.module.configuration.LdapModuleWebSecurityConfiguration;

/**
 * @author lskublik
 */

public class LdapWebSecurityConfigurer<C extends LdapModuleWebSecurityConfiguration> extends LoginFormModuleWebSecurityConfigurer<C> {

    public LdapWebSecurityConfigurer(C configuration) {
        super(configuration);
    }

    protected MidpointFormLoginConfigurer getMidpointFormLoginConfigurer() {
        return new MidpointFormLoginConfigurer(new LdapAuthenticationFilter());
    }
}
