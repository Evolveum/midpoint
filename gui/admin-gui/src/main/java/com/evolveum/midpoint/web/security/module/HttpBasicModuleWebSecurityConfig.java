/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.module;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.authentication.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.web.security.MidpointAuthenticationTrustResolverImpl;
import com.evolveum.midpoint.web.security.HttpAuthenticationEntryPoint;
import com.evolveum.midpoint.web.security.MidpointHttpAuthorizationEvaluator;
import com.evolveum.midpoint.web.security.filter.HttpBasicAuthenticationFilter;
import com.evolveum.midpoint.web.security.filter.configurers.MidpointExceptionHandlingConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import static org.springframework.security.saml.util.StringUtils.stripEndingSlases;

/**
 * @author skublik
 */

public class HttpBasicModuleWebSecurityConfig<C extends ModuleWebSecurityConfiguration> extends ModuleWebSecurityConfig<C> {

    @Autowired
    private ModelService model;

    @Autowired
    private SecurityEnforcer securityEnforcer;

    @Autowired
    private SecurityContextManager securityContextManager;

    @Autowired
    private TaskManager taskManager;

    public HttpBasicModuleWebSecurityConfig(C configuration) {
        super(configuration);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        super.configure(http);
        HttpAuthenticationEntryPoint entryPoint = getObjectPostProcessor().postProcess(new HttpAuthenticationEntryPoint());
        http.antMatcher(stripEndingSlases(getPrefix()) + "/**");

        HttpBasicAuthenticationFilter filter = getObjectPostProcessor().postProcess(new HttpBasicAuthenticationFilter(authenticationManager(), entryPoint));
        RememberMeServices rememberMeServices = http.getSharedObject(RememberMeServices.class);
        if (rememberMeServices != null) {
            filter.setRememberMeServices(rememberMeServices);
        }
        http.authorizeRequests().accessDecisionManager(new MidpointHttpAuthorizationEvaluator(securityEnforcer, securityContextManager, taskManager, model));
        http.addFilterAt(filter, BasicAuthenticationFilter.class);
        http.formLogin().disable()
                .csrf().disable();
        getOrApply(http, new MidpointExceptionHandlingConfigurer())
                .authenticationEntryPoint(entryPoint)
                .authenticationTrustResolver(new MidpointAuthenticationTrustResolverImpl());
                //.and()
                //.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.NEVER);
    }

}
