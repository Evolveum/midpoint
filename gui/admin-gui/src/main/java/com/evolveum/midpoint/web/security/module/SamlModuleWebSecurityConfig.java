/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security.module;

import java.util.Collections;
import java.util.UUID;

import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.web.security.saml.MidpointMetadataRelyingPartyRegistrationResolver;

import com.evolveum.midpoint.web.security.saml.MidpointSaml2LoginConfigurer;

import com.evolveum.midpoint.web.security.saml.MidpointSaml2LogoutRequestResolver;
import com.evolveum.midpoint.web.security.saml.MidpointSaml2LogoutRequestSuccessHandler;
import com.evolveum.midpoint.web.security.util.SecurityUtils;

import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;

import com.evolveum.midpoint.web.security.filter.MidpointAnonymousAuthenticationFilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.security.saml2.provider.service.metadata.OpenSamlMetadataResolver;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.servlet.filter.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.Saml2MetadataFilter;

import com.evolveum.midpoint.model.api.ModelAuditRecorder;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.*;
import com.evolveum.midpoint.web.security.filter.configurers.MidpointExceptionHandlingConfigurer;
import com.evolveum.midpoint.web.security.module.configuration.SamlModuleWebSecurityConfiguration;

import org.springframework.security.saml2.provider.service.web.authentication.logout.OpenSaml4LogoutRequestResolver;
import org.springframework.security.saml2.provider.service.web.authentication.logout.Saml2LogoutRequestResolver;
import org.springframework.security.saml2.provider.service.web.authentication.logout.Saml2RelyingPartyInitiatedLogoutSuccessHandler;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

/**
 * @author skublik
 */

public class SamlModuleWebSecurityConfig<C extends SamlModuleWebSecurityConfiguration> extends ModuleWebSecurityConfig<C> {

    private static final Trace LOGGER = TraceManager.getTrace(SamlModuleWebSecurityConfig.class);
    public static final String SAML_LOGIN_PATH = "/saml2/select";

    @Autowired
    private ModelAuditRecorder auditProvider;

    public SamlModuleWebSecurityConfig(C configuration) {
        super(configuration);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);

        http.antMatcher(SecurityUtils.stripEndingSlashes(getPrefix()) + "/**");
        http.csrf().disable();

        MidpointExceptionHandlingConfigurer exceptionConfigurer = new MidpointExceptionHandlingConfigurer() {
            @Override
            protected Authentication createNewAuthentication(AnonymousAuthenticationToken anonymousAuthenticationToken) {
                if (anonymousAuthenticationToken.getDetails() instanceof Saml2AuthenticationToken) {
                    return (Saml2AuthenticationToken) anonymousAuthenticationToken.getDetails();
                }
                return null;
            }
        };
        getOrApply(http, exceptionConfigurer)
                .authenticationEntryPoint(new SamlAuthenticationEntryPoint(SAML_LOGIN_PATH));

        MidpointSaml2LoginConfigurer configurer = new MidpointSaml2LoginConfigurer(auditProvider);
        configurer.relyingPartyRegistrationRepository(relyingPartyRegistrations())
                .loginProcessingUrl(getConfiguration().getPrefix() + SamlModuleWebSecurityConfiguration.SSO_LOCATION_URL_SUFFIX)
                .successHandler(getObjectPostProcessor().postProcess(
                        new MidPointAuthenticationSuccessHandler().setPrefix(getConfiguration().getPrefix())))
                .failureHandler(new MidpointAuthenticationFailureHandler());
        try {
            configurer.authenticationManager(new ProviderManager(Collections.emptyList(), authenticationManager()));
        } catch (Exception e) {
            LOGGER.error("Couldn't initialize authentication manager for saml2 module");
        }
        getOrApply(http, configurer);

        RelyingPartyRegistrationResolver registrationResolver = new DefaultRelyingPartyRegistrationResolver(relyingPartyRegistrations());
        LogoutSuccessHandler logoutRequestSuccessHandler = logoutRequestSuccessHandler(registrationResolver);

        http.logout().clearAuthentication(true)
                .logoutRequestMatcher(new AntPathRequestMatcher(getPrefix() + "/logout"))
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler(logoutRequestSuccessHandler);

        Saml2MetadataFilter filter = new Saml2MetadataFilter(new MidpointMetadataRelyingPartyRegistrationResolver(relyingPartyRegistrations()),
                new OpenSamlMetadataResolver());
        filter.setRequestMatcher(new AntPathRequestMatcher( getConfiguration().getPrefix() + "/metadata/*"));
        http.addFilterAfter(filter, Saml2WebSsoAuthenticationFilter.class);
    }

    @Override
    protected AnonymousAuthenticationFilter createAnonymousFilter() {
        AnonymousAuthenticationFilter filter = new MidpointAnonymousAuthenticationFilter(authRegistry, authChannelRegistry, prismContext,
                UUID.randomUUID().toString(), "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")){
            @Override
            protected void processAuthentication(ServletRequest req) {
                if (SecurityContextHolder.getContext().getAuthentication() instanceof MidpointAuthentication) {
                    MidpointAuthentication mpAuthentication = (MidpointAuthentication) SecurityContextHolder.getContext().getAuthentication();
                    ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
                    if (moduleAuthentication != null
                            && (moduleAuthentication.getAuthentication() == null
                            || moduleAuthentication.getAuthentication() instanceof Saml2AuthenticationToken)) {
                        Authentication authentication = createBasicAuthentication((HttpServletRequest) req);
                        moduleAuthentication.setAuthentication(authentication);
                        mpAuthentication.setPrincipal(authentication.getPrincipal());
                    }
                }
            }
        };

        filter.setAuthenticationDetailsSource(new SamlAuthenticationDetailsSource());
        return filter;
    }

    private InMemoryRelyingPartyRegistrationRepository relyingPartyRegistrations() {
        return getConfiguration().getRelyingPartyRegistrationRepository();
    }

    private static class SamlAuthenticationDetailsSource implements AuthenticationDetailsSource<HttpServletRequest, Object> {

        private final WebAuthenticationDetailsSource detailsSource = new WebAuthenticationDetailsSource();

        @Override
        public Object buildDetails(HttpServletRequest context) {
            if (SecurityContextHolder.getContext().getAuthentication() instanceof MidpointAuthentication) {
                MidpointAuthentication mpAuthentication = (MidpointAuthentication) SecurityContextHolder.getContext().getAuthentication();
                ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
                if (moduleAuthentication != null && moduleAuthentication.getAuthentication() instanceof Saml2AuthenticationToken) {
                    return moduleAuthentication.getAuthentication();
                }
            }
            return detailsSource.buildDetails(context);
        }
    }

    private LogoutSuccessHandler logoutRequestSuccessHandler(RelyingPartyRegistrationResolver registrationResolver) {
        Saml2LogoutRequestResolver logoutRequestResolver = new MidpointSaml2LogoutRequestResolver(
                new OpenSaml4LogoutRequestResolver(registrationResolver));
        Saml2RelyingPartyInitiatedLogoutSuccessHandler handler = new Saml2RelyingPartyInitiatedLogoutSuccessHandler(logoutRequestResolver);
        return getObjectPostProcessor().postProcess(new MidpointSaml2LogoutRequestSuccessHandler(
                handler));
    }
}
