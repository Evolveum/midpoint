/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.configuration;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
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

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.context.DeferredSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.authentication.impl.factory.channel.AuthChannelRegistryImpl;

import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;

import java.util.HashMap;
import java.util.Map;

/**
 * Class with basic components for authentication filters.
 * Also this class contains configuration for path which ignore authentication.
 *
 * @author skublik
 */
@Order(SecurityProperties.BASIC_AUTH_ORDER - 1)
@Configuration
@EnableWebSecurity
@DependsOn("initialSecurityConfiguration")
public class SecurityConfigurer {

    @Autowired
    private AuthChannelRegistryImpl authChannelRegistry;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ApplicationContext applicationContext;

    private ObjectPostProcessor<Object> objectObjectPostProcessor;
    private ContentNegotiationStrategy contentNegotiationStrategy = new HeaderContentNegotiationStrategy();

    @PostConstruct
    void initSecurityContext() {
        SecurityContextHolder.setContextHolderStrategy(new MidpointSecurityContextHolderStrategy());
    }

    @Autowired(required = false)
    void setContentNegotiationStrategy(ContentNegotiationStrategy contentNegotiationStrategy) {
        this.contentNegotiationStrategy = contentNegotiationStrategy;
    }

    @Autowired
    public void setObjectPostProcessor(ObjectPostProcessor<Object> objectPostProcessor) {
        this.objectObjectPostProcessor = objectPostProcessor;
    }

    @Bean
    public MidPointGuiAuthorizationEvaluator accessDecisionManager(SecurityEnforcer securityEnforcer,
            SecurityContextManager securityContextManager,
            TaskManager taskManager) {
        return new MidPointGuiAuthorizationEvaluator(securityEnforcer, securityContextManager, taskManager, applicationContext);
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
    protected MidpointProviderManager authenticationManager() throws Exception {
        return new MidpointProviderManager();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> {
            // Web (SOAP) services
            web.ignoring().requestMatchers(new AntPathRequestMatcher("/model/**"));

            // Special intra-cluster service to download and delete report outputs
            web.ignoring().requestMatchers(new AntPathRequestMatcher("/report"));

            web.ignoring().requestMatchers(new AntPathRequestMatcher("/js/**"));
            web.ignoring().requestMatchers(new AntPathRequestMatcher("/css/**"));
            web.ignoring().requestMatchers(new AntPathRequestMatcher("/img/**"));
            web.ignoring().requestMatchers(new AntPathRequestMatcher("/fonts/**"));

            web.ignoring().requestMatchers(new AntPathRequestMatcher("/static/**"));
            web.ignoring().requestMatchers(new AntPathRequestMatcher("/static-web/**"));
            web.ignoring().requestMatchers(new AntPathRequestMatcher("/less/**"));

            web.ignoring().requestMatchers(new AntPathRequestMatcher("/wicket/resource/**"));

            web.ignoring().requestMatchers(new AntPathRequestMatcher("/favicon.ico"));
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.setSharedObject(AuthenticationTrustResolverImpl.class, new MidpointAuthenticationTrustResolverImpl());
        http.addFilter(new WebAsyncManagerIntegrationFilter())
                .sessionManagement(configurer -> {})
                .securityContext(configurer -> {
                    configurer.securityContextRepository(createSessionContextRepository(http));
                });
        http.with(new AuthFilterConfigurer(), authFilterConfigurer -> {});

        http.setSharedObject(SecurityContextRepository.class, createSessionContextRepository(http));

        http.sessionManagement(configurer -> {
            configurer
                    .maximumSessions(-1)
                    .sessionRegistry(sessionRegistry)
                    .maxSessionsPreventsLogin(true);
                });
        return http.build();
    }

    @Bean
    @Primary
    public HttpSecurity httpSecurity() throws Exception {
        AuthenticationManagerBuilder authenticationBuilder = new AuthenticationManagerBuilder(this.objectObjectPostProcessor);
        authenticationBuilder.parentAuthenticationManager(authenticationManager());
        HttpSecurity http = new HttpSecurity(this.objectObjectPostProcessor, authenticationBuilder, createSharedObjects());
        return http;
    }

    private Map<Class<?>, Object> createSharedObjects() {
        Map<Class<?>, Object> sharedObjects = new HashMap<>();
        sharedObjects.put(ApplicationContext.class, this.context);
        sharedObjects.put(ContentNegotiationStrategy.class, this.contentNegotiationStrategy);
        return sharedObjects;
    }

    private HttpSessionSecurityContextRepository createSessionContextRepository(HttpSecurity http) {
        HttpSessionSecurityContextRepository httpSecurityRepository = new HttpSessionSecurityContextRepository() {
            @Override
            public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
                if(!AuthSequenceUtil.isRecordSessionLessAccessChannel(request)) {
                    super.saveContext(context, request, response);
                }
            }

            @Override
            public DeferredSecurityContext loadDeferredContext(HttpServletRequest request) {
                if(AuthSequenceUtil.isRecordSessionLessAccessChannel(request)) {
                    return super.loadDeferredContext(new HttpServletRequestWrapper(request) {
                        @Override
                        public HttpSession getSession(boolean create) {
                            return null;
                        }
                    });
                }
                return super.loadDeferredContext(request);
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
        return httpSecurityRepository;
    }

    @Bean
    public ServletListenerRegistrationBean httpSessionEventPublisher() {
        return new ServletListenerRegistrationBean(new HttpSessionEventPublisher());
    }
}
