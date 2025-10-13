/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.module.configuration;

import com.evolveum.midpoint.authentication.api.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;

/**
 * @author skublik
 */

public class LdapModuleWebSecurityConfiguration extends LoginFormModuleWebSecurityConfiguration {

    public static <T extends ModuleWebSecurityConfiguration> T build(AbstractAuthenticationModuleType module, String prefixOfSequence){
        LdapModuleWebSecurityConfiguration configuration = build(new LdapModuleWebSecurityConfiguration(), module, prefixOfSequence);
        configuration.validate();
        return (T) configuration;
    }
}
