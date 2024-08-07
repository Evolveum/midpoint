/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.configurer;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.authorization.evaluator.MidpointHttpAuthorizationEvaluator;
import com.evolveum.midpoint.authentication.impl.entry.point.HttpAuthenticationEntryPoint;
import com.evolveum.midpoint.authentication.impl.entry.point.HttpSecurityQuestionsAuthenticationEntryPoint;
import com.evolveum.midpoint.authentication.impl.MidpointAuthenticationTrustResolverImpl;
import com.evolveum.midpoint.authentication.impl.filter.HttpSecurityQuestionsAuthenticationFilter;
import com.evolveum.midpoint.authentication.impl.filter.SequenceAuditFilter;
import com.evolveum.midpoint.authentication.impl.filter.configurers.MidpointExceptionHandlingConfigurer;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;

import com.evolveum.midpoint.authentication.impl.module.configuration.ModuleWebSecurityConfigurationImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.HttpSecQAuthenticationModuleType;

import jakarta.servlet.ServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.TaskManager;

/**
 * @author skublik
 */

public class HttpSecurityQuestionsModuleWebSecurityConfigurer extends ModuleWebSecurityConfigurer<ModuleWebSecurityConfigurationImpl, HttpSecQAuthenticationModuleType> {

    @Autowired
    private ModelService model;

    @Autowired
    private SecurityEnforcer securityEnforcer;

    @Autowired
    private SecurityContextManager securityContextManager;

    @Autowired
    private TaskManager taskManager;

    @Autowired
    private ApplicationContext applicationContext;

    public HttpSecurityQuestionsModuleWebSecurityConfigurer(HttpSecQAuthenticationModuleType moduleType,
            String suffix,
            AuthenticationChannel authenticationChannel,
            ObjectPostProcessor<Object> postProcessor,
            ServletRequest request,
            AuthenticationProvider provider) {
        super(moduleType, suffix, authenticationChannel, postProcessor, request, provider);
    }

    @Override
    protected ModuleWebSecurityConfigurationImpl buildConfiguration(HttpSecQAuthenticationModuleType moduleType, String sequenceSuffix, AuthenticationChannel authenticationChannel, ServletRequest request) {
        ModuleWebSecurityConfigurationImpl configuration = ModuleWebSecurityConfigurationImpl.build(moduleType, sequenceSuffix);
        configuration.setSequenceSuffix(sequenceSuffix);
        return configuration;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        super.configure(http);
        HttpAuthenticationEntryPoint entryPoint = getObjectPostProcessor().postProcess(new HttpSecurityQuestionsAuthenticationEntryPoint());
        http.securityMatcher(AuthUtil.stripEndingSlashes(getPrefix()) + "/**");

        http.authorizeRequests().accessDecisionManager(new MidpointHttpAuthorizationEvaluator(
                securityEnforcer, securityContextManager, taskManager, model, applicationContext));

        HttpSecurityQuestionsAuthenticationFilter filter = getObjectPostProcessor().postProcess(new HttpSecurityQuestionsAuthenticationFilter(authenticationManager(), entryPoint));
        RememberMeServices rememberMeServices = http.getSharedObject(RememberMeServices.class);
        if (rememberMeServices != null) {
            filter.setRememberMeServices(rememberMeServices);
        }
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
