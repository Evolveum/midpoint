/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.boot.auth.module;

import com.evolveum.midpoint.web.boot.auth.MidpointAuthenticationTrustResolverImpl;
import com.evolveum.midpoint.web.boot.auth.MidpointProviderManager;
import com.evolveum.midpoint.web.boot.auth.filter.MidpointAnonymousAuthenticationFilter;
import com.evolveum.midpoint.web.boot.auth.filter.configurers.MidpointExceptionHandlingConfigurer;
import com.evolveum.midpoint.web.boot.auth.module.configuration.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.web.security.MidPointAccessDeniedHandler;
import com.evolveum.midpoint.web.security.MidPointGuiAuthorizationEvaluator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import java.util.UUID;

/**
 * @author skublik
 */

public class ModuleWebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    private MidPointGuiAuthorizationEvaluator accessDecisionManager;

    @Autowired
    private MidPointAccessDeniedHandler accessDeniedHandler;

    @Autowired
    private MidpointProviderManager authenticationManager;

    @Autowired
    private AuthenticationProvider midPointAuthenticationProvider;

    @Value("${security.enable-csrf:true}")
    private boolean csrfEnabled;

    private ObjectPostProcessor<Object> objectPostProcessor;
    private ModuleWebSecurityConfiguration configuration;

    public ModuleWebSecurityConfig(ModuleWebSecurityConfiguration configuration){
        super(true);
        this.configuration = configuration;
    }

    public String getPrefix() {
        return configuration.getPrefix();
    }

    @Override
    public void setObjectPostProcessor(ObjectPostProcessor<Object> objectPostProcessor) {
        this.objectPostProcessor = objectPostProcessor;
        super.setObjectPostProcessor(objectPostProcessor);
    }

    public ObjectPostProcessor<Object> getObjectPostProcessor() {
        return objectPostProcessor;
    }

    public HttpSecurity getNewHttpSecurity() throws Exception {
        return getHttp();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        AnonymousAuthenticationFilter anonymousFilter = new MidpointAnonymousAuthenticationFilter(UUID.randomUUID().toString(), "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        http.setSharedObject(AuthenticationTrustResolver.class, new MidpointAuthenticationTrustResolverImpl());
        http.authorizeRequests()
                .accessDecisionManager(accessDecisionManager)
                .anyRequest().fullyAuthenticated();
        http.apply(new MidpointExceptionHandlingConfigurer())
                .accessDeniedHandler(accessDeniedHandler)
                .authenticationTrustResolver(new MidpointAuthenticationTrustResolverImpl());
        http
//                .csrf().and()
//                .addFilter(new WebAsyncManagerIntegrationFilter())
//                .apply(new ExceptionHandlingConfigurer<>() {}).and()
                .headers().and()
//                .sessionManagement().and()
//                .securityContext().and()
                .requestCache().and()
                .anonymous().authenticationFilter(anonymousFilter).and()
                .servletApi();//.and()
//                .apply(new DefaultLoginPageConfigurer<>()).and()
//                .logout();

        http.csrf();
        if (!csrfEnabled) {
            http.csrf().disable();
        }

        http.headers().disable();
        http.headers().frameOptions().sameOrigin();

        http.sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.NEVER)
                .maximumSessions(-1)
                .sessionRegistry(sessionRegistry)
                .maxSessionsPreventsLogin(true);
    }

    @Override
    protected AuthenticationManager authenticationManager() throws Exception {
//        authenticationManager.getProviders().clear();
//        authenticationManager.getProviders().add(midPointAuthenticationProvider);
        if (configuration != null && !configuration.getAuthenticationProviders().isEmpty()) {
            for (AuthenticationProvider authenticationProvider : configuration.getAuthenticationProviders()) {
                if (!(authenticationManager.getProviders().contains(authenticationProvider))) {
                    authenticationManager.getProviders().add(authenticationProvider);
                }
            }
        }
        return authenticationManager;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        if (configuration != null && !configuration.getAuthenticationProviders().isEmpty()) {
            for (AuthenticationProvider authenticationProvider : configuration.getAuthenticationProviders()) {
                auth.authenticationProvider(authenticationProvider);
            }
        } else {
            super.configure(auth);
        }
    }

}
