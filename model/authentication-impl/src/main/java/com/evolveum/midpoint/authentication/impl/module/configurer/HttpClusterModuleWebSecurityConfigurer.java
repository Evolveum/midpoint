/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.configurer;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.authorization.evaluator.MidpointAllowAllAuthorizationEvaluator;
import com.evolveum.midpoint.authentication.impl.entry.point.HttpAuthenticationEntryPoint;
import com.evolveum.midpoint.authentication.impl.MidpointAuthenticationTrustResolverImpl;
import com.evolveum.midpoint.authentication.impl.filter.HttpClusterAuthenticationFilter;
import com.evolveum.midpoint.authentication.impl.filter.configurers.MidpointExceptionHandlingConfigurer;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;

import com.evolveum.midpoint.authentication.impl.module.configuration.ModuleWebSecurityConfigurationImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;

import jakarta.servlet.ServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.TaskManager;

/**
 * @author skublik
 */

public class HttpClusterModuleWebSecurityConfigurer extends ModuleWebSecurityConfigurer<ModuleWebSecurityConfigurationImpl, AbstractAuthenticationModuleType> {

    @Autowired private SecurityEnforcer securityEnforcer;
    @Autowired private SecurityContextManager securityContextManager;
    @Autowired private TaskManager taskManager;
    @Autowired private ApplicationContext applicationContext;

    public HttpClusterModuleWebSecurityConfigurer(AbstractAuthenticationModuleType moduleType,
            String sequeneSuffix,
            AuthenticationChannel authenticationChannel,
            ObjectPostProcessor<Object> postProcessor,
            ServletRequest request,
            AuthenticationProvider provider) {
        super(moduleType, sequeneSuffix, authenticationChannel, postProcessor, request, provider);
    }

    @Override
    protected ModuleWebSecurityConfigurationImpl buildConfiguration(AbstractAuthenticationModuleType moduleType, String sequenceSuffix, AuthenticationChannel authenticationChannel, ServletRequest request) {
        ModuleWebSecurityConfigurationImpl configuration = ModuleWebSecurityConfigurationImpl.build(moduleType, sequenceSuffix);
        configuration.setSequenceSuffix(sequenceSuffix);
        return configuration;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        super.configure(http);
        HttpAuthenticationEntryPoint entryPoint = getObjectPostProcessor().postProcess(new HttpAuthenticationEntryPoint());
        http.securityMatcher(AuthUtil.stripEndingSlashes(getPrefix()) + "/**");

        HttpClusterAuthenticationFilter filter = getObjectPostProcessor().postProcess(new HttpClusterAuthenticationFilter(authenticationManager(), entryPoint));
        RememberMeServices rememberMeServices = http.getSharedObject(RememberMeServices.class);
        if (rememberMeServices != null) {
            filter.setRememberMeServices(rememberMeServices);
        }
        http.authorizeRequests().accessDecisionManager(new MidpointAllowAllAuthorizationEvaluator(
                securityEnforcer, securityContextManager, taskManager, applicationContext));
        http.addFilterAt(filter, BasicAuthenticationFilter.class);
        http.formLogin().disable()
                .csrf().disable();
        getOrApply(http, new MidpointExceptionHandlingConfigurer<>())
                .authenticationEntryPoint(entryPoint)
                .authenticationTrustResolver(new MidpointAuthenticationTrustResolverImpl());
    }

}
