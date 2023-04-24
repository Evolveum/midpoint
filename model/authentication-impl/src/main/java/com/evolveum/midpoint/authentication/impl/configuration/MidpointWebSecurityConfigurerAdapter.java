/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.evolveum.midpoint.authentication.impl.MidpointAuthenticationTrustResolverImpl;
import com.evolveum.midpoint.authentication.impl.MidpointProviderManager;
import com.evolveum.midpoint.authentication.impl.MidpointSecurityContext;
import com.evolveum.midpoint.authentication.impl.authorization.evaluator.MidPointGuiAuthorizationEvaluator;
import com.evolveum.midpoint.authentication.impl.entry.point.WicketLoginUrlAuthenticationEntryPoint;
import com.evolveum.midpoint.authentication.impl.filter.configurers.AuthFilterConfigurer;
import com.evolveum.midpoint.authentication.impl.handler.AuditedAccessDeniedHandler;
import com.evolveum.midpoint.authentication.impl.handler.AuditedLogoutHandler;
import com.evolveum.midpoint.authentication.impl.handler.MidPointAuthenticationSuccessHandler;
import com.evolveum.midpoint.authentication.impl.session.SessionAndRequestScope;
import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.authentication.impl.factory.channel.AuthChannelRegistryImpl;

/**
 * @author skublik
 */
@Order(SecurityProperties.BASIC_AUTH_ORDER - 1)
@Configuration
@EnableWebSecurity
@DependsOn("initialSecurityConfiguration")
public class MidpointWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

    @Autowired
    private AuthChannelRegistryImpl authChannelRegistry;

    @Autowired
    private SessionRegistry sessionRegistry;

    private ObjectPostProcessor<Object> objectObjectPostProcessor;

    public MidpointWebSecurityConfigurerAdapter() {
        super(true);
    }

    @Autowired
    @Override
    public void setObjectPostProcessor(ObjectPostProcessor<Object> objectPostProcessor) {
        this.objectObjectPostProcessor = objectPostProcessor;
        super.setObjectPostProcessor(objectPostProcessor);
    }

    @Bean
    public MidPointGuiAuthorizationEvaluator accessDecisionManager(SecurityEnforcer securityEnforcer,
            SecurityContextManager securityContextManager,
            TaskManager taskManager) {
        return new MidPointGuiAuthorizationEvaluator(securityEnforcer, securityContextManager, taskManager);
    }

    @Bean
    public MidPointAuthenticationSuccessHandler authenticationSuccessHandler() {
        MidPointAuthenticationSuccessHandler handler = new MidPointAuthenticationSuccessHandler();
        handler.setUseReferer(true);
        handler.setDefaultTargetUrl("/login");

        return handler;
    }

    @Bean
    public AuditedLogoutHandler logoutHandler() {
        AuditedLogoutHandler handler = new AuditedLogoutHandler();
        handler.setDefaultTargetUrl("/");

        return handler;
    }

    @Bean
    public AuditedAccessDeniedHandler accessDeniedHandler() {
        return objectObjectPostProcessor.postProcess(new AuditedAccessDeniedHandler());
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return new WicketLoginUrlAuthenticationEntryPoint("/login");
    }

    @Bean
    @SessionAndRequestScope
    @Override
    protected MidpointProviderManager authenticationManager() throws Exception {
        return new MidpointProviderManager();
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        super.configure(web);
        // Web (SOAP) services
        web.ignoring().antMatchers("/model/**");

        // Special intra-cluster service to download and delete report outputs
        web.ignoring().antMatchers("/report");

        web.ignoring().antMatchers("/js/**");
        web.ignoring().antMatchers("/css/**");
        web.ignoring().antMatchers("/img/**");
        web.ignoring().antMatchers("/fonts/**");

        web.ignoring().antMatchers("/static/**");
        web.ignoring().antMatchers("/static-web/**");
        web.ignoring().antMatchers("/less/**");

        web.ignoring().antMatchers("/wicket/resource/**");

        web.ignoring().antMatchers("/favicon.ico");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.setSharedObject(AuthenticationTrustResolverImpl.class, new MidpointAuthenticationTrustResolverImpl());
        http.addFilter(new WebAsyncManagerIntegrationFilter())
                .sessionManagement().and()
                .securityContext();
        http.apply(new AuthFilterConfigurer());

        createSessionContextRepository(http);

        http.sessionManagement()
                .maximumSessions(-1)
                .sessionRegistry(sessionRegistry)
                .maxSessionsPreventsLogin(true);
    }

    private void createSessionContextRepository(HttpSecurity http) {
        HttpSessionSecurityContextRepository httpSecurityRepository = new HttpSessionSecurityContextRepository() {
            @Override
            public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
                if(!AuthSequenceUtil.isRecordSessionLessAccessChannel(request)) {
                    super.saveContext(context, request, response);
                }
            }

            @Override
            protected SecurityContext generateNewContext() {
                return new MidpointSecurityContext(super.generateNewContext());
            }
        };
        httpSecurityRepository.setDisableUrlRewriting(true);
        AuthenticationTrustResolver trustResolver = http.getSharedObject(AuthenticationTrustResolver.class);
        if (trustResolver != null) {
            httpSecurityRepository.setTrustResolver(trustResolver);
        }
        http.setSharedObject(SecurityContextRepository.class, httpSecurityRepository);
    }

    @Bean
    public ServletListenerRegistrationBean httpSessionEventPublisher() {
        return new ServletListenerRegistrationBean(new HttpSessionEventPublisher());
    }
}
