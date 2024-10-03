/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.module.configurer;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.MidpointAuthenticationTrustResolverImpl;
import com.evolveum.midpoint.authentication.impl.authorization.evaluator.MidpointHttpAuthorizationEvaluator;
import com.evolveum.midpoint.authentication.impl.entry.point.HttpAuthenticationEntryPoint;
import com.evolveum.midpoint.authentication.impl.filter.SequenceAuditFilter;
import com.evolveum.midpoint.authentication.impl.filter.configurers.MidpointExceptionHandlingConfigurer;
import com.evolveum.midpoint.authentication.impl.module.configuration.JwtOidcResourceServerConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configuration.OpaqueTokenOidcResourceServerConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configuration.RemoteModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.filter.oidc.OidcBearerTokenAuthenticationFilter;
import com.evolveum.midpoint.authentication.impl.provider.OidcResourceServerProvider;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.TaskManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OidcAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OidcResourceServerAuthenticationModuleType;

import jakarta.servlet.ServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * @author skublik
 */

public class OidcResourceServerModuleWebSecurityConfigurer<C extends RemoteModuleWebSecurityConfiguration>
        extends ModuleWebSecurityConfigurer<C, OidcAuthenticationModuleType> {

    @Autowired private ModelService model;
    @Autowired private SecurityEnforcer securityEnforcer;
    @Autowired private SecurityContextManager securityContextManager;
    @Autowired TaskManager taskManager;
    @Autowired private ApplicationContext applicationContext;

    public OidcResourceServerModuleWebSecurityConfigurer(OidcAuthenticationModuleType moduleType,
            String sequenceSuffix,
            AuthenticationChannel authenticationChannel,
            ObjectPostProcessor<Object> objectPostProcessor,
            ServletRequest request,
            AuthenticationProvider provider) {
        super(moduleType, sequenceSuffix, authenticationChannel, objectPostProcessor, request, provider);
    }

    @Override
    protected C buildConfiguration(OidcAuthenticationModuleType moduleType, String sequenceSuffix, AuthenticationChannel authenticationChannel, ServletRequest request) {
        OidcResourceServerAuthenticationModuleType resourceServer = moduleType.getResourceServer();
        if (resourceServer.getJwt() != null) {
            return createJwtResourceServerConfiguration(moduleType, resourceServer, sequenceSuffix);
        }
        if (resourceServer.getOpaqueToken() != null) {
            return createOpaqueTokenResourceServerConfiguration(moduleType, resourceServer, sequenceSuffix);
        }

        return createJwtResourceServerConfiguration(moduleType, resourceServer, sequenceSuffix);
    }

    private C createJwtResourceServerConfiguration(
            AbstractAuthenticationModuleType moduleType,
            OidcResourceServerAuthenticationModuleType resourceServer,
            String sequenceSuffix) {

        JwtOidcResourceServerConfiguration configuration =
                JwtOidcResourceServerConfiguration.build(
                        (OidcAuthenticationModuleType)moduleType,
                        sequenceSuffix);
        configuration.setSequenceSuffix(sequenceSuffix);

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        if (resourceServer.getJwt() != null && resourceServer.getJwt().getNameOfUsernameClaim() != null) {
            jwtAuthenticationConverter.setPrincipalClaimName(resourceServer.getJwt().getNameOfUsernameClaim());
        } else if (resourceServer.getNameOfUsernameClaim() != null) {
            jwtAuthenticationConverter.setPrincipalClaimName(resourceServer.getNameOfUsernameClaim());
        }
        configuration.addAuthenticationProvider(getObjectPostProcessor().postProcess(
                new OidcResourceServerProvider(configuration.getDecoder(), jwtAuthenticationConverter)));
        return (C) configuration;
    }

    private C createOpaqueTokenResourceServerConfiguration(
            AbstractAuthenticationModuleType moduleType,
            OidcResourceServerAuthenticationModuleType resourceServer,
            String sequenceSuffix) {
        OpaqueTokenOidcResourceServerConfiguration configuration =
                OpaqueTokenOidcResourceServerConfiguration.build(
                        (OidcAuthenticationModuleType)moduleType,
                        sequenceSuffix);
        configuration.setSequenceSuffix(sequenceSuffix);

        configuration.addAuthenticationProvider(getObjectPostProcessor().postProcess(
                new OidcResourceServerProvider(configuration.getIntrospector())));
        return (C) configuration;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);

        HttpAuthenticationEntryPoint entryPoint = getObjectPostProcessor().postProcess(new HttpAuthenticationEntryPoint());
        http.securityMatcher(AuthUtil.stripEndingSlashes(getPrefix()) + "/**");

        OidcBearerTokenAuthenticationFilter filter = getObjectPostProcessor().postProcess(new OidcBearerTokenAuthenticationFilter(authenticationManager(), entryPoint));
        RememberMeServices rememberMeServices = http.getSharedObject(RememberMeServices.class);
        if (rememberMeServices != null) {
            filter.setRememberMeServices(rememberMeServices);
        }
        http.authorizeRequests().accessDecisionManager(new MidpointHttpAuthorizationEvaluator(
                securityEnforcer, securityContextManager, taskManager, model, applicationContext));
        http.addFilterAt(filter, BasicAuthenticationFilter.class);

        http.formLogin().disable()
                .csrf().disable();
        getOrApply(http, new MidpointExceptionHandlingConfigurer<>())
                .authenticationEntryPoint(entryPoint)
                .authenticationTrustResolver(new MidpointAuthenticationTrustResolverImpl());

        SequenceAuditFilter sequenceAuditFilter = new SequenceAuditFilter();
        sequenceAuditFilter.setRecordOnEndOfChain(false);
        http.addFilterAfter(getObjectPostProcessor().postProcess(sequenceAuditFilter), FilterSecurityInterceptor.class);
    }
}
