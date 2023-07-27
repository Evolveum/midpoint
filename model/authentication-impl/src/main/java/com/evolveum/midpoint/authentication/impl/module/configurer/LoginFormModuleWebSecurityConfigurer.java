/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.configurer;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.handler.AuditedLogoutHandler;
import com.evolveum.midpoint.authentication.impl.handler.MidPointAuthenticationSuccessHandler;
import com.evolveum.midpoint.authentication.impl.handler.MidpointAuthenticationFailureHandler;
import com.evolveum.midpoint.authentication.impl.module.configuration.LoginFormModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.entry.point.WicketLoginUrlAuthenticationEntryPoint;
import com.evolveum.midpoint.authentication.impl.filter.configurers.MidpointExceptionHandlingConfigurer;
import com.evolveum.midpoint.authentication.impl.filter.configurers.MidpointFormLoginConfigurer;
import com.evolveum.midpoint.authentication.impl.filter.MidpointUsernamePasswordAuthenticationFilter;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;

import jakarta.servlet.ServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.preauth.RequestAttributeAuthenticationFilter;

/**
 * @author skublik
 */

public class LoginFormModuleWebSecurityConfigurer<C extends LoginFormModuleWebSecurityConfiguration, MT extends AbstractAuthenticationModuleType> extends ModuleWebSecurityConfigurer<C, MT> {

    @Autowired
    private AuditedLogoutHandler auditedLogoutHandler;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    private Environment environment;

    @Autowired(required = false)
    private RequestAttributeAuthenticationFilter requestAttributeAuthenticationFilter;

    @Autowired(required = false)
    private LogoutFilter requestSingleLogoutFilter;

//    private final C configuration;

    public LoginFormModuleWebSecurityConfigurer(C configuration) {
        super(configuration);
//        this.configuration = configuration;
    }

    public LoginFormModuleWebSecurityConfigurer(MT moduleType,
            String prefixOfSequence,
            AuthenticationChannel authenticationChannel,
            ObjectPostProcessor<Object> postProcessor,
            ServletRequest request,
            AuthenticationProvider provider) {
        super(moduleType, prefixOfSequence, authenticationChannel, postProcessor, request, provider);
//        this.configuration = getConfiguration();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        http.securityMatcher(AuthUtil.stripEndingSlashes(getPrefix()) + "/**");
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
    }

    protected MidpointFormLoginConfigurer getMidpointFormLoginConfigurer() {
        return new MidpointFormLoginConfigurer<>(new MidpointUsernamePasswordAuthenticationFilter());
    }

//    @Override
//    public C getConfiguration() {
//        return configuration;
//    }

    protected SessionRegistry getSessionRegistry() {
        return sessionRegistry;
    }
}
