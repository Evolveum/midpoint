/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.module;

import com.evolveum.midpoint.web.security.MidpointAuthenticationFauileHandler;
import com.evolveum.midpoint.web.security.filter.configurers.MidpointExceptionHandlingConfigurer;
import com.evolveum.midpoint.web.security.filter.configurers.MidpointFormLoginConfigurer;
import com.evolveum.midpoint.web.security.module.configuration.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.web.security.MidPointAuthenticationSuccessHandler;
import com.evolveum.midpoint.web.security.AuditedLogoutHandler;
import com.evolveum.midpoint.web.security.MidPointAccessDeniedHandler;
import com.evolveum.midpoint.web.security.WicketLoginUrlAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.preauth.RequestAttributeAuthenticationFilter;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

import java.util.Arrays;

import static org.springframework.security.saml.util.StringUtils.stripEndingSlases;

/**
 * @author skublik
 */

public class LoginFormModuleWebSecurityConfig extends ModuleWebSecurityConfig {

    @Autowired
    private MidPointAccessDeniedHandler accessDeniedHandler;

    @Autowired
    private AuditedLogoutHandler auditedLogoutHandler;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    private Environment environment;

    @Autowired(required = false)
    private RequestHeaderAuthenticationFilter requestHeaderAuthenticationFilter;

    @Autowired(required = false)
    private RequestAttributeAuthenticationFilter requestAttributeAuthenticationFilter;

    @Autowired(required = false)
    private CasAuthenticationFilter casFilter;

    @Autowired(required = false)
    private LogoutFilter requestSingleLogoutFilter;

    private ModuleWebSecurityConfiguration configuration;

    public LoginFormModuleWebSecurityConfig(ModuleWebSecurityConfiguration configuration) {
        super(configuration);
        this.configuration = configuration;
    }

    @Override
    public HttpSecurity getNewHttpSecurity() throws Exception {
        return getHttp();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        http.antMatcher(stripEndingSlases(getPrefix()) + "/**");
        http.apply(new MidpointFormLoginConfigurer())
                .loginPage("/login")
                .failureUrl("/")
                .loginProcessingUrl(stripEndingSlases(getPrefix()) + "/spring_security_login")
                .failureHandler(new MidpointAuthenticationFauileHandler())
                .successHandler(getObjectPostProcessor().postProcess(
                        new MidPointAuthenticationSuccessHandler().setPrefix(configuration.getPrefix()))).permitAll();
        http.apply(new MidpointExceptionHandlingConfigurer())
                .authenticationEntryPoint(new WicketLoginUrlAuthenticationEntryPoint("/login"));
//                .accessDeniedHandler(accessDeniedHandler)
//                .authenticationTrustResolver(new MidpointAuthenticationTrustResolverImpl());

        http.logout().clearAuthentication(true)
                .logoutUrl(stripEndingSlases(getPrefix()) +"/logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler(auditedLogoutHandler);
//        http.sessionManagement()
//                .sessionCreationPolicy(SessionCreationPolicy.NEVER)
//                .maximumSessions(-1)
//                .sessionRegistry(sessionRegistry)
//                .maxSessionsPreventsLogin(true);

        if (Arrays.stream(environment.getActiveProfiles()).anyMatch(p -> p.equalsIgnoreCase("cas"))) {
            http.addFilterAt(casFilter, CasAuthenticationFilter.class);
            http.addFilterBefore(requestSingleLogoutFilter, LogoutFilter.class);
        }

        if (Arrays.stream(environment.getActiveProfiles()).anyMatch(p -> p.equalsIgnoreCase("sso"))) {
            http.addFilterBefore(requestHeaderAuthenticationFilter, LogoutFilter.class);
        }

        if (Arrays.stream(environment.getActiveProfiles()).anyMatch(p -> p.equalsIgnoreCase("ssoenv"))) {
            http.addFilterBefore(requestAttributeAuthenticationFilter, LogoutFilter.class);
        }
    }
}
