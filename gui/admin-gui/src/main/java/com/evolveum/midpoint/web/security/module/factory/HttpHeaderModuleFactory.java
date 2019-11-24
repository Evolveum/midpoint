/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.module.factory;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.module.AuthModule;
import com.evolveum.midpoint.web.security.module.HttpHeaderModuleWebConfig;
import com.evolveum.midpoint.web.security.module.LoginFormModuleWebSecurityConfig;
import com.evolveum.midpoint.web.security.module.ModuleWebSecurityConfig;
import com.evolveum.midpoint.web.security.module.authentication.HttpHeaderModuleAuthentication;
import com.evolveum.midpoint.web.security.module.authentication.LoginFormModuleAuthentication;
import com.evolveum.midpoint.web.security.module.authentication.ModuleAuthentication;
import com.evolveum.midpoint.web.security.module.configuration.HttpHeaderModuleWebSecurityConfiguration;
import com.evolveum.midpoint.web.security.module.configuration.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationModuleHttpHeaderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationModuleLoginFormType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationModuleSaml2Type;
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
public class HttpHeaderModuleFactory extends ModuleFactory {

    private static final transient Trace LOGGER = TraceManager.getTrace(HttpHeaderModuleFactory.class);

    @Autowired
    private AuthenticationProvider midPointAuthenticationProvider;

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType) {
        if (moduleType instanceof AuthenticationModuleHttpHeaderType) {
            return true;
        }
        return false;
    }

    @Override
    public AuthModule createModuleFilter(AbstractAuthenticationModuleType moduleType, String prefixOfSequence,
                                         ServletRequest request, Map<Class<? extends Object>, Object> sharedObjects) throws Exception {
        if (!(moduleType instanceof AuthenticationModuleHttpHeaderType)) {
            LOGGER.error("This factory support only AuthenticationModuleHttpHeaderType, but modelType is " + moduleType);
            return null;
        }

        HttpHeaderModuleWebSecurityConfiguration configuration = HttpHeaderModuleWebSecurityConfiguration.build((AuthenticationModuleHttpHeaderType)moduleType, prefixOfSequence);
        configuration.addAuthenticationProvider(midPointAuthenticationProvider);
        ModuleWebSecurityConfig module = getObjectObjectPostProcessor().postProcess(new HttpHeaderModuleWebConfig(configuration));
        module.setObjectPostProcessor(getObjectObjectPostProcessor());
        HttpSecurity http = module.getNewHttpSecurity();
        setSharedObjects(http, sharedObjects);

        ModuleAuthentication moduleAuthentication = createEmptyModuleAuthentication(configuration);
        SecurityFilterChain filter = http.build();
        return AuthModule.build(filter, configuration, moduleAuthentication);
    }

    private ModuleAuthentication createEmptyModuleAuthentication(ModuleWebSecurityConfiguration configuration) {
        HttpHeaderModuleAuthentication moduleAuthentication = new HttpHeaderModuleAuthentication();
        moduleAuthentication.setPrefix(configuration.getPrefix());
        moduleAuthentication.setNameOfModule(configuration.getNameOfModule());
        return moduleAuthentication;
    }
}
