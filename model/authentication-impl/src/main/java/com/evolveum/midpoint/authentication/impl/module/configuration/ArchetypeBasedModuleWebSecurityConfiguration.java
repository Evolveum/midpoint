/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.module.configuration;

import com.evolveum.midpoint.authentication.api.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;

public class ArchetypeBasedModuleWebSecurityConfiguration extends ModuleWebSecurityConfigurationImpl {

    public static <T extends ModuleWebSecurityConfiguration> T build(AbstractAuthenticationModuleType module, String prefixOfSequence){
        ArchetypeBasedModuleWebSecurityConfiguration configuration = build(new ArchetypeBasedModuleWebSecurityConfiguration(),
                module, prefixOfSequence);
        configuration.validate();
        return (T) configuration;
    }
}
