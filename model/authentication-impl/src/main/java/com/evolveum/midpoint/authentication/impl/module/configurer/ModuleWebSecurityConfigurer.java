/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.configurer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.MidpointAuthenticationTrustResolverImpl;
import com.evolveum.midpoint.authentication.impl.MidpointProviderManager;
import com.evolveum.midpoint.authentication.impl.authorization.evaluator.MidPointGuiAuthorizationEvaluator;
import com.evolveum.midpoint.authentication.impl.factory.channel.AuthChannelRegistryImpl;
import com.evolveum.midpoint.authentication.impl.factory.module.AuthModuleRegistryImpl;
import com.evolveum.midpoint.authentication.impl.handler.AuditedAccessDeniedHandler;
import com.evolveum.midpoint.authentication.impl.handler.AuditedLogoutHandler;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.filter.MidpointAnonymousAuthenticationFilter;
import com.evolveum.midpoint.authentication.impl.filter.configurers.MidpointExceptionHandlingConfigurer;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.api.ModuleWebSecurityConfiguration;

import com.evolveum.midpoint.authentication.impl.filter.RedirectForLoginPagesWithAuthenticationFilter;

import com.evolveum.midpoint.authentication.impl.module.configuration.LoginFormModuleWebSecurityConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.evolveum.midpoint.prism.PrismContext;

/**
 * @author skublik
 */

public class ModuleWebSecurityConfigurer<C extends ModuleWebSecurityConfiguration, MT extends AbstractAuthenticationModuleType> {//extends WebSecurityConfigurerAdapter {

    @Autowired private AuditedAccessDeniedHandler accessDeniedHandler;
    @Autowired private MidPointGuiAuthorizationEvaluator accessDecisionManager;
    @Autowired private MidpointProviderManager authenticationManager;
    @Autowired private AuthModuleRegistryImpl authRegistry;
    @Autowired private AuthChannelRegistryImpl authChannelRegistry;
    @Autowired private PrismContext prismContext;
    @Autowired private ApplicationContext context;
//    @Autowired private FocusAuthenticationResultRecorder authenticationRecorder;

    @Value("${security.enable-csrf:true}")
    private boolean csrfEnabled;


    private ObjectPostProcessor<Object> objectPostProcessor;
    private final C configuration;

    public ModuleWebSecurityConfigurer(C configuration){
//        super(true);
        this.configuration = configuration;
    }

    public ModuleWebSecurityConfigurer(MT moduleType,
            String sequenceSuffix,
            AuthenticationChannel authenticationChannel,
            ObjectPostProcessor<Object> objectPostProcessor) {
        this.configuration = buildConfiguration(moduleType, sequenceSuffix, authenticationChannel);
        this.objectPostProcessor = objectPostProcessor;

    }

    protected C buildConfiguration(MT moduleType, String sequenceSuffix, AuthenticationChannel authenticationChannel) {
        LoginFormModuleWebSecurityConfiguration config = new LoginFormModuleWebSecurityConfiguration();
        config.setSequenceSuffix(sequenceSuffix);
        config.setModuleIdentifier(moduleType.getIdentifier() != null ? moduleType.getIdentifier() : moduleType.getName());
        return (C) config;
    }

    public C getConfiguration() {
        return configuration;
    }

    public String getPrefix() {
        return configuration.getPrefixOfModule();
    }

//    @Override
    public void setObjectPostProcessor(ObjectPostProcessor<Object> objectPostProcessor) {
        this.objectPostProcessor = objectPostProcessor;
//        super.setObjectPostProcessor(objectPostProcessor);
    }

    public ObjectPostProcessor<Object> getObjectPostProcessor() {
        return objectPostProcessor;
    }

    public HttpSecurity getNewHttpSecurity() throws Exception {
        AuthenticationManagerBuilder authenticationBuilder = new AuthenticationManagerBuilder(this.objectPostProcessor);
        authenticationBuilder.parentAuthenticationManager(authenticationManager());
        configure(authenticationBuilder);
        HttpSecurity http = new HttpSecurity(this.objectPostProcessor, authenticationBuilder, createSharedObjects());
        configure(http);
        return http;
    }

    private Map<Class<?>, Object> createSharedObjects() {
        Map<Class<?>, Object> sharedObjects = new HashMap<>();
        sharedObjects.put(ApplicationContext.class, this.context);
        return sharedObjects;
    }

//    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.setSharedObject(AuthenticationTrustResolver.class, new MidpointAuthenticationTrustResolverImpl());
        http.authorizeRequests()
                .accessDecisionManager(accessDecisionManager)
                .anyRequest().fullyAuthenticated();
        getOrApply(http, new MidpointExceptionHandlingConfigurer<>())
                .accessDeniedHandler(accessDeniedHandler)
                .authenticationTrustResolver(new MidpointAuthenticationTrustResolverImpl());
        http.headers().and()
                .requestCache().and()
                .anonymous().authenticationFilter(createAnonymousFilter()).and()
                .servletApi();

        http.addFilterAfter(new RedirectForLoginPagesWithAuthenticationFilter(), CsrfFilter.class);
//        http.addFilterBefore(new SequenceAuditFilter(authenticationRecorder), RequestCacheAwareFilter.class);

        http.csrf();
        if (!csrfEnabled) {
            http.csrf().disable();
        }

        http.headers().disable();
        http.headers().frameOptions().sameOrigin();
    }

    protected AnonymousAuthenticationFilter createAnonymousFilter() {
        return new MidpointAnonymousAuthenticationFilter(authRegistry, authChannelRegistry, prismContext,
                UUID.randomUUID().toString(), "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
    }

//    @Override
    protected AuthenticationManager authenticationManager() throws Exception {
        if (configuration != null && !configuration.getAuthenticationProviders().isEmpty()) {
            for (AuthenticationProvider authenticationProvider : configuration.getAuthenticationProviders()) {
                if (!(authenticationManager.getProviders().contains(authenticationProvider))) {
                    authenticationManager.getProviders().add(authenticationProvider);
                }
            }
        }
        return authenticationManager;
    }

//    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        if (configuration != null && !configuration.getAuthenticationProviders().isEmpty()) {
            for (AuthenticationProvider authenticationProvider : configuration.getAuthenticationProviders()) {
                auth.authenticationProvider(authenticationProvider);
            }
        }
    }

    protected RequestMatcher getLogoutMatcher(HttpSecurity http, String logoutUrl) {
        return httpServletRequest -> {
            ModuleAuthenticationImpl module = (ModuleAuthenticationImpl) AuthUtil.getProcessingModuleIfExist();
            if (module != null && module.isInternalLogout()) {
                module.setInternalLogout(false);
                return true;
            }
            RequestMatcher logoutRequestMatcher;
            if (http.getConfigurer(CsrfConfigurer.class) != null) {
                logoutRequestMatcher = new AntPathRequestMatcher(logoutUrl, "POST");
            } else {
                logoutRequestMatcher = new OrRequestMatcher(
                        new AntPathRequestMatcher(logoutUrl, "GET"),
                        new AntPathRequestMatcher(logoutUrl, "POST"),
                        new AntPathRequestMatcher(logoutUrl, "PUT"),
                        new AntPathRequestMatcher(logoutUrl, "DELETE"));
            }
            return logoutRequestMatcher.matches(httpServletRequest);
        };
    }

    protected LogoutSuccessHandler createLogoutHandler() {
        return createLogoutHandler(null);
    }

    protected LogoutSuccessHandler createLogoutHandler(String defaultSuccessLogoutURL) {
        AuditedLogoutHandler handler = objectPostProcessor.postProcess(new AuditedLogoutHandler());
        if (StringUtils.isNotBlank(defaultSuccessLogoutURL)
        && (defaultSuccessLogoutURL.startsWith("/") || defaultSuccessLogoutURL.startsWith("http")
        || defaultSuccessLogoutURL.startsWith("https"))) {
            handler.setDefaultTargetUrl(defaultSuccessLogoutURL);
        }
        return handler;
    }

    protected  <CA extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity>> CA getOrApply(HttpSecurity http, CA configurer) throws Exception {
        CA existingConfigurer = (CA) http.getConfigurer(configurer.getClass());
        return existingConfigurer != null ? existingConfigurer : http.apply(configurer);
    }

}
