/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.security.factory.module;

import com.evolveum.midpoint.authentication.api.AuthModule;
import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.IdentityProvider;
import com.evolveum.midpoint.authentication.impl.security.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.security.module.authentication.Saml2ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.security.module.configuration.SamlMidpointAdditionalConfiguration;
import com.evolveum.midpoint.authentication.impl.security.module.configuration.SamlModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.security.module.configurer.SamlModuleWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.security.provider.Saml2Provider;
import com.evolveum.midpoint.authentication.impl.security.util.AuthModuleImpl;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SystemConfigurationTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.saml2.provider.service.servlet.filter.Saml2WebSsoAuthenticationRequestFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.ServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author skublik
 */
public abstract class RemoteModuleFactory extends AbstractModuleFactory {

    private static final Trace LOGGER = TraceManager.getTrace(RemoteModuleFactory.class);

    @Autowired
    private Protector protector;

    @Autowired
    private SystemObjectCache systemObjectCache;

    protected String getPublicUrlPrefix(ServletRequest request) {
        try {
            PrismObject<SystemConfigurationType> systemConfig = systemObjectCache.getSystemConfiguration(new OperationResult("load system configuration"));
            return SystemConfigurationTypeUtil.getPublicHttpUrlPattern(systemConfig.asObjectable(), request.getServerName());
        } catch (SchemaException e) {
            LOGGER.error("Couldn't load system configuration", e);
            return null;
        }
    }

    protected Protector getProtector() {
        return protector;
    }
}
