/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.boot.auth.module.factory;

import com.evolveum.midpoint.web.boot.auth.module.AuthModule;
import com.evolveum.midpoint.web.boot.auth.module.ModuleWebSecurityConfig;
import com.evolveum.midpoint.web.boot.auth.module.authentication.LoginFormModuleAuthentication;
import com.evolveum.midpoint.web.boot.auth.module.authentication.ModuleAuthentication;
import com.evolveum.midpoint.web.boot.auth.module.configuration.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.web.boot.auth.module.LoginFormModuleWebSecurityConfig;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationModuleLoginFormType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

import javax.servlet.ServletRequest;
import java.util.Map;

/**
 * @author skublik
 */
@Component
public class LoginFormModuleFactory extends ModuleFactory {

    @Autowired
    private AuthenticationProvider midPointAuthenticationProvider;

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType) {
        if (moduleType instanceof AuthenticationModuleLoginFormType) {
            return true;
        }
        return false;
    }

    @Override
    public AuthModule createModuleFilter(AbstractAuthenticationModuleType moduleType, String prefixOfSequence,
                                         ServletRequest request, Map<Class<? extends Object>, Object> sharedObjects) throws Exception {
        ModuleWebSecurityConfiguration configuration = ModuleWebSecurityConfiguration.build(moduleType,prefixOfSequence);
        configuration.setPrefixOfSequence(prefixOfSequence);
        configuration.addAuthenticationProvider(midPointAuthenticationProvider);

        ModuleWebSecurityConfig module = getObjectObjectPostProcessor().postProcess(new LoginFormModuleWebSecurityConfig(configuration));
        module.setObjectPostProcessor(getObjectObjectPostProcessor());
        HttpSecurity http = module.getNewHttpSecurity();
        setSharedObjects(http, sharedObjects);

        ModuleAuthentication moduleAuthentication = createEmptyModuleAuthentication(configuration);
        SecurityFilterChain filter = http.build();
        return AuthModule.build(filter, configuration, moduleAuthentication);
    }

    private ModuleAuthentication createEmptyModuleAuthentication(ModuleWebSecurityConfiguration configuration) {
        LoginFormModuleAuthentication moduleAuthentication = new LoginFormModuleAuthentication();
        moduleAuthentication.setPrefix(configuration.getPrefix());
        moduleAuthentication.setNameOfModule(configuration.getNameOfModule());
        moduleAuthentication.setPrefix(configuration.getPrefix());
        return moduleAuthentication;
    }
}
