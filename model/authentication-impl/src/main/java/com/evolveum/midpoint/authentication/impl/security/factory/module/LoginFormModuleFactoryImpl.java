/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.security.factory.module;

import com.evolveum.midpoint.authentication.impl.security.provider.PasswordProvider;
import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.security.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.api.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.security.module.LoginFormModuleWebSecurityConfig;
import com.evolveum.midpoint.authentication.impl.security.module.ModuleWebSecurityConfig;
import com.evolveum.midpoint.authentication.impl.security.module.authentication.LoginFormModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.security.module.configuration.LoginFormModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.security.module.configuration.ModuleWebSecurityConfigurationImpl;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */
@Component
public class LoginFormModuleFactoryImpl extends AbstractCredentialModuleFactory {

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType) {
        if (moduleType instanceof LoginFormAuthenticationModuleType) {
            return true;
        }
        return false;
    }

    @Override
    protected ModuleWebSecurityConfiguration createConfiguration(AbstractAuthenticationModuleType moduleType, String prefixOfSequence, AuthenticationChannel authenticationChannel) {
        ModuleWebSecurityConfigurationImpl configuration = LoginFormModuleWebSecurityConfiguration.build(moduleType,prefixOfSequence);
        configuration.setPrefixOfSequence(prefixOfSequence);
        return configuration;
    }

    @Override
    protected ModuleWebSecurityConfig createModule(ModuleWebSecurityConfiguration configuration) {
        return  getObjectObjectPostProcessor().postProcess(new LoginFormModuleWebSecurityConfig((LoginFormModuleWebSecurityConfiguration) configuration));
    }

    @Override
    protected AuthenticationProvider createProvider(CredentialPolicyType usedPolicy) {
        return new PasswordProvider();
    }

    @Override
    protected Class<? extends CredentialPolicyType> supportedClass() {
        return PasswordCredentialsPolicyType.class;
    }

    @Override
    protected ModuleAuthenticationImpl createEmptyModuleAuthentication(AbstractAuthenticationModuleType moduleType,
                                                                   ModuleWebSecurityConfiguration configuration) {
        LoginFormModuleAuthenticationImpl moduleAuthentication = new LoginFormModuleAuthenticationImpl();
        moduleAuthentication.setPrefix(configuration.getPrefix());
        moduleAuthentication.setCredentialName(((AbstractCredentialAuthenticationModuleType)moduleType).getCredentialName());
        moduleAuthentication.setCredentialType(supportedClass());
        moduleAuthentication.setNameOfModule(configuration.getNameOfModule());
        return moduleAuthentication;
    }

}
