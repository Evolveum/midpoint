/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security.module;

import com.evolveum.midpoint.model.api.authentication.UserProfileService;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.MidpointAuthenticationFauileHandler;
import com.evolveum.midpoint.web.security.MidpointServiceProviderLogoutHandler;
import com.evolveum.midpoint.web.security.filter.MidpointSamlAuthenticationRequestFilter;
import com.evolveum.midpoint.web.security.filter.MidpointSamlAuthenticationResponseFilter;
import com.evolveum.midpoint.web.security.filter.configurers.MidpointExceptionHandlingConfigurer;
import com.evolveum.midpoint.web.security.module.configuration.SamlModuleWebSecurityConfiguration;
import com.evolveum.midpoint.web.security.MidPointAuthenticationSuccessHandler;
import com.evolveum.midpoint.web.security.MidPointGuiAuthorizationEvaluator;
import com.evolveum.midpoint.web.security.WicketLoginUrlAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.saml.provider.SamlProviderLogoutFilter;
import org.springframework.security.saml.provider.SamlServerConfiguration;
import org.springframework.security.saml.provider.service.config.SamlServiceProviderServerBeanConfiguration;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.Filter;

import java.util.Collections;

import static org.springframework.security.saml.util.StringUtils.stripEndingSlases;


/**
 * @author skublik
 */

public class SamlModuleWebSecurityConfig<C extends SamlModuleWebSecurityConfiguration> extends ModuleWebSecurityConfig<C> {

    private static final Trace LOGGER = TraceManager.getTrace(SamlModuleWebSecurityConfig.class);

    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    private MidPointGuiAuthorizationEvaluator accessDecisionManager;

    private SamlServerConfiguration saml2Configuration;

    private MidpointSamlProviderServerBeanConfiguration beanConfiguration;

    public SamlModuleWebSecurityConfig(C configuration) {
        super(configuration);
        this.saml2Configuration = configuration.getSamlConfiguration();
        this.beanConfiguration = new MidpointSamlProviderServerBeanConfiguration(configuration);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        getObjectPostProcessor().postProcess(getBeanConfiguration());
        super.configure(http);
        String prefix = getPrefix();

        http.antMatcher(stripEndingSlases(getPrefix()) + "/**");
        http.csrf().disable();

        http.apply(new MidpointExceptionHandlingConfigurer())
                .authenticationEntryPoint(new WicketLoginUrlAuthenticationEntryPoint("/saml2/select"));
//                .accessDeniedHandler(accessDeniedHandler)
//                .authenticationTrustResolver(new MidpointAuthenticationTrustResolverImpl());

        http.addFilterAfter(
                        getBeanConfiguration().samlConfigurationFilter(),
                        BasicAuthenticationFilter.class
                )
                .addFilterAfter(
                        getBeanConfiguration().spMetadataFilter(),
                        getBeanConfiguration().samlConfigurationFilter().getClass()
                )
                .addFilterAfter(
                        getBeanConfiguration().spAuthenticationRequestFilter(),
                        getBeanConfiguration().spMetadataFilter().getClass()
                )
                .addFilterAfter(
                        getBeanConfiguration().spAuthenticationResponseFilter(),
                        getBeanConfiguration().spAuthenticationRequestFilter().getClass()
                )
                .addFilterAfter(
                        getBeanConfiguration().spSamlLogoutFilter(),
                        getBeanConfiguration().spAuthenticationResponseFilter().getClass()
                );
    }

    public SamlServiceProviderServerBeanConfiguration getBeanConfiguration() {
        return beanConfiguration;
    }

    private class MidpointSamlProviderServerBeanConfiguration extends SamlServiceProviderServerBeanConfiguration {

        @Autowired
        private UserProfileService userProfileService;

//        @Autowired
//        private AuditedLogoutHandler auditedLogoutHandler;

        private final SamlModuleWebSecurityConfiguration configuration;

        private final SamlServerConfiguration saml2Config;

        public MidpointSamlProviderServerBeanConfiguration(SamlModuleWebSecurityConfiguration configuration) {
            this.configuration = configuration;
            this.saml2Config = configuration.getSamlConfiguration();
        }

        @Override
        protected SamlServerConfiguration getDefaultHostSamlServerConfiguration() {
            return saml2Config;
        }

        @Override
        public Filter spAuthenticationRequestFilter() {
            return new MidpointSamlAuthenticationRequestFilter(getSamlProvisioning());
        }

        @Override
        public Filter spAuthenticationResponseFilter() {
            MidpointSamlAuthenticationResponseFilter authenticationFilter =
                    new MidpointSamlAuthenticationResponseFilter(getSamlProvisioning());
            try {
                authenticationFilter.setAuthenticationManager(new ProviderManager(Collections.emptyList(), authenticationManager()));
            } catch (Exception e) {
                LOGGER.error("Couldn't initialize authentication manager for saml2 module");
            }
            authenticationFilter.setAuthenticationSuccessHandler(getObjectPostProcessor().postProcess(
                    new MidPointAuthenticationSuccessHandler().setPrefix(configuration.getPrefix())));
            authenticationFilter.setAuthenticationFailureHandler(new MidpointAuthenticationFauileHandler());
            return authenticationFilter;
        }

        @Override
        public Filter spSamlLogoutFilter() {
            return new SamlProviderLogoutFilter(
                    getSamlProvisioning(),
                    new MidpointServiceProviderLogoutHandler(getSamlProvisioning()),
                    createLogoutHandler("/"),
                    new SecurityContextLogoutHandler(),
                    new CookieClearingLogoutHandler("JSESSIONID")
            );
        }

        private class MidpointSimpleAuthenticationManager implements AuthenticationManager {
            @Override
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {

                if (authentication.isAuthenticated() && authentication.getPrincipal() instanceof MidPointPrincipal) {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
                return authentication;
            }
        }
    }
}
