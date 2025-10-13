/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.factory.module;

import com.evolveum.midpoint.authentication.api.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.configurer.ModuleWebSecurityConfigurer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialPolicyType;

/**
 * @author skublik
 */
public abstract class AbstractCredentialModuleFactory<
        C extends ModuleWebSecurityConfiguration,
        CA extends ModuleWebSecurityConfigurer<C, MT>,
        MT extends AbstractAuthenticationModuleType,
        MA extends ModuleAuthentication>
        extends AbstractModuleFactory<C, CA, MT, MA> {


    protected abstract Class<? extends CredentialPolicyType> supportedClass();
}
