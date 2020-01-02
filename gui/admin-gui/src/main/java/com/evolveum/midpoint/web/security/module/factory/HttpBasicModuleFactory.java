/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.module.factory;

import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.model.api.authentication.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.model.api.authentication.NameOfModuleType;
import com.evolveum.midpoint.web.security.module.HttpBasicModuleWebSecurityConfig;
import com.evolveum.midpoint.web.security.module.ModuleWebSecurityConfig;
import com.evolveum.midpoint.web.security.module.authentication.PasswordModuleAuthentication;
import com.evolveum.midpoint.web.security.module.configuration.ModuleWebSecurityConfigurationImpl;
import com.evolveum.midpoint.web.security.provider.InternalPasswordProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.stereotype.Component;

/**
 * @author skublik
 */
@Component
public class HttpBasicModuleFactory extends AbstractPasswordModuleFactory {

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType) {
        if (moduleType instanceof AuthenticationModuleHttpBasicType) {
            return true;
        }
        return false;
    }

    @Override
    protected ModuleWebSecurityConfiguration createConfiguration(AbstractAuthenticationModuleType moduleType, String prefixOfSequence) {
        ModuleWebSecurityConfigurationImpl configuration = ModuleWebSecurityConfigurationImpl.build(moduleType,prefixOfSequence);
        configuration.setPrefixOfSequence(prefixOfSequence);
        return configuration;
    }

    @Override
    protected ModuleWebSecurityConfig createModule(ModuleWebSecurityConfiguration configuration) {
        return  getObjectObjectPostProcessor().postProcess(new HttpBasicModuleWebSecurityConfig(configuration));
    }

    @Override
    protected AuthenticationProvider createProvider(CredentialPolicyType usedPolicy) {
        return new InternalPasswordProvider();
    }

    @Override
    protected Class<? extends CredentialPolicyType> supportedClass() {
        return PasswordCredentialsPolicyType.class;
    }

    @Override
    protected ModuleAuthentication createEmptyModuleAuthentication(AbstractAuthenticationModuleType moduleType, ModuleWebSecurityConfiguration configuration) {
        PasswordModuleAuthentication moduleAuthentication = new PasswordModuleAuthentication(NameOfModuleType.HTTP_BASIC);
        moduleAuthentication.setPrefix(configuration.getPrefix());
        moduleAuthentication.setCredentialName(((AbstractPasswordAuthenticationModuleType)moduleType).getCredentialName());
        moduleAuthentication.setNameOfModule(configuration.getNameOfModule());
        return moduleAuthentication;
    }

}
