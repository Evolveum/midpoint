/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.security.module.configurer;

import java.util.Arrays;

import com.evolveum.midpoint.authentication.impl.security.handler.AuditedLogoutHandler;
import com.evolveum.midpoint.authentication.impl.security.handler.MidPointAuthenticationSuccessHandler;
import com.evolveum.midpoint.authentication.impl.security.handler.MidpointAuthenticationFailureHandler;
import com.evolveum.midpoint.authentication.impl.security.entry.point.WicketLoginUrlAuthenticationEntryPoint;
import com.evolveum.midpoint.authentication.impl.security.filter.configurers.MidpointExceptionHandlingConfigurer;
import com.evolveum.midpoint.authentication.impl.security.filter.configurers.MidpointFormLoginConfigurer;
import com.evolveum.midpoint.authentication.impl.security.filter.MidpointUsernamePasswordAuthenticationFilter;
import com.evolveum.midpoint.authentication.impl.security.module.configuration.LoginFormModuleWebSecurityConfiguration;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.preauth.RequestAttributeAuthenticationFilter;

/**
 * @author skublik
 */

public class LoginFormModuleWebSecurityConfigurer<C extends LoginFormModuleWebSecurityConfiguration> extends ModuleWebSecurityConfigurer<C> {

    @Autowired
    private AuditedLogoutHandler auditedLogoutHandler;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    private Environment environment;

    @Autowired(required = false)
    private RequestAttributeAuthenticationFilter requestAttributeAuthenticationFilter;

    @Autowired(required = false)
    private CasAuthenticationFilter casFilter;

    @Autowired(required = false)
    private LogoutFilter requestSingleLogoutFilter;

    private final C configuration;

    public LoginFormModuleWebSecurityConfigurer(C configuration) {
        super(configuration);
        this.configuration = configuration;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        http.antMatcher(AuthUtil.stripEndingSlashes(getPrefix()) + "/**");
        getOrApply(http, getMidpointFormLoginConfigurer())
                .loginPage("/login")
                .loginProcessingUrl(AuthUtil.stripEndingSlashes(getPrefix()) + "/spring_security_login")
                .failureHandler(new MidpointAuthenticationFailureHandler())
                .successHandler(getObjectPostProcessor().postProcess(
                        new MidPointAuthenticationSuccessHandler())).permitAll();
        getOrApply(http, new MidpointExceptionHandlingConfigurer<>())
                .authenticationEntryPoint(new WicketLoginUrlAuthenticationEntryPoint("/login"));

        http.logout().clearAuthentication(true)
                .logoutRequestMatcher(getLogoutMatcher(http, getPrefix() + "/logout"))
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler(createLogoutHandler());

        if (Arrays.stream(environment.getActiveProfiles()).anyMatch(p -> p.equalsIgnoreCase("cas"))) {
            http.addFilterAt(casFilter, CasAuthenticationFilter.class);
            http.addFilterBefore(requestSingleLogoutFilter, LogoutFilter.class);
        }

        if (Arrays.stream(environment.getActiveProfiles()).anyMatch(p -> p.equalsIgnoreCase("ssoenv"))) {
            http.addFilterBefore(requestAttributeAuthenticationFilter, LogoutFilter.class);
        }
    }

    protected MidpointFormLoginConfigurer getMidpointFormLoginConfigurer() {
        return new MidpointFormLoginConfigurer<>(new MidpointUsernamePasswordAuthenticationFilter());
    }

    @Override
    public C getConfiguration() {
        return configuration;
    }

    protected SessionRegistry getSessionRegistry() {
        return sessionRegistry;
    }
}
