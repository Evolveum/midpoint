/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.boot.testsaml;

import com.evolveum.midpoint.model.api.authentication.UserProfileService;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.web.security.AuditedLogoutHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml.SamlAuthentication;
import org.springframework.security.saml.SamlRequestMatcher;
import org.springframework.security.saml.provider.HostedProviderService;
import org.springframework.security.saml.provider.SamlProviderLogoutFilter;
import org.springframework.security.saml.provider.SamlServerConfiguration;
import org.springframework.security.saml.provider.provisioning.SamlProviderProvisioning;
import org.springframework.security.saml.provider.service.SelectIdentityProviderFilter;
import org.springframework.security.saml.provider.service.ServiceProviderService;
import org.springframework.security.saml.provider.service.authentication.GenericErrorAuthenticationFailureHandler;
import org.springframework.security.saml.provider.service.authentication.ServiceProviderLogoutHandler;
import org.springframework.security.saml.provider.service.config.SamlServiceProviderServerBeanConfiguration;
import org.springframework.security.saml.saml2.Saml2Object;
import org.springframework.security.saml.saml2.authentication.LogoutRequest;
import org.springframework.security.saml.saml2.authentication.NameIdPrincipal;
import org.springframework.security.saml.saml2.metadata.IdentityProviderMetadata;
import org.springframework.security.saml.saml2.metadata.ServiceProviderMetadata;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import static java.lang.String.format;
import static org.springframework.security.saml.util.StringUtils.*;
import static org.springframework.security.saml.util.StringUtils.addAliasPath;
import static org.springframework.util.StringUtils.hasText;

/**
 * @author skublik
 */

@Configuration
public class SamlProviderServerBeanConfiguration extends SamlServiceProviderServerBeanConfiguration {

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private AuditedLogoutHandler auditedLogoutHandler;

    private final SamlConfiguration config;

    public SamlProviderServerBeanConfiguration(SamlConfiguration config) {
        this.config = config;
    }

    @Override
    protected SamlServerConfiguration getDefaultHostSamlServerConfiguration() {
        return config;
    }



    @Override
    public Filter spAuthenticationResponseFilter() {
        MidpointSamlAuthenticationResponseFilter authenticationFilter =
                new MidpointSamlAuthenticationResponseFilter(getSamlProvisioning());
        authenticationFilter.setAuthenticationManager(new MidpointSimpleAuthenticationManager());
        authenticationFilter.setAuthenticationSuccessHandler(new SavedRequestAwareAuthenticationSuccessHandler());
        authenticationFilter.setAuthenticationFailureHandler(new GenericErrorAuthenticationFailureHandler());
        authenticationFilter.setUserProfileService(userProfileService);
        return authenticationFilter;
    }

    @Override
    public Filter spSelectIdentityProviderFilter() {
        return new SelectIdentityProviderFilter(getSamlProvisioning(), new SamlRequestMatcher(getSamlProvisioning(), "select"){
            @Override
            public boolean matches(HttpServletRequest request) {
                return false;
            }
        });
    }

    @Override
    public Filter spSamlLogoutFilter() {
        return new SamlProviderLogoutFilter(
                getSamlProvisioning(),
                new ServiceProviderLogoutHandler(getSamlProvisioning()){

                    protected void spInitiatedLogout(HttpServletRequest request,
                                                     HttpServletResponse response,
                                                     Authentication authentication) throws IOException {
                        if (authentication instanceof SamlAuthentication) {
                            SamlAuthentication sa = (SamlAuthentication) authentication;
//                            logger.debug(format("Initiating SP logout for SP:%s", sa.getHoldingEntityId()));
                            ServiceProviderService provider = getSamlProvisioning().getHostedProvider();
                            ServiceProviderMetadata sp = provider.getMetadata();
                            IdentityProviderMetadata idp = provider.getRemoteProvider(sa.getAssertingEntityId());
                            LogoutRequest lr = null;
                            if (sa.getAssertion() != null && sa.getAssertion().getSubject() != null && sa.getAssertion().getSubject().getPrincipal() != null) {
                                lr = provider.logoutRequest(idp, (NameIdPrincipal) sa.getAssertion().getSubject().getPrincipal());
                            }
                            if (lr.getDestination() != null) {
//                                logger.debug("Sending logout request through redirect.");
                                String redirect = getRedirectUrl(
                                        provider,
                                        lr,
                                        lr.getDestination().getLocation(),
                                        "SAMLRequest",
                                        getLogoutRelayState(
                                                request,
                                                idp
                                        )
                                );
                                response.sendRedirect(redirect);
                            }
                            else {
//                                logger.debug("Unable to send logout request. No destination set.");
                            }
                        }
                    }

                    private String getRedirectUrl(ServiceProviderService provider,
                                                  Saml2Object lr,
                                                  String location,
                                                  String paramName,
                                                  String relayState)
                            throws UnsupportedEncodingException {
                        String xml = provider.toXml(lr);
                        String value = provider.toEncodedXml(xml, true);
                        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(location);
                        if (hasText(relayState)) {
                            builder.queryParam("RelayState", UriUtils.encode(relayState, StandardCharsets.UTF_8.name()));
                        }
                        return builder.queryParam(paramName, UriUtils.encode(value, StandardCharsets.UTF_8.name()))
                                .queryParam("return", "google.com")
                                .build()
                                .toUriString();
                    }

                },
                new SimpleUrlLogoutSuccessHandler(),
                new SecurityContextLogoutHandler()
//                new CookieClearingLogoutHandler("JSESSIONID")
        );
    }

//    private class MidpointSamlRequestMatcher extends SamlRequestMatcher {
//
//        private String path;
//
//        public MidpointSamlRequestMatcher(SamlProviderProvisioning provisioning, String path) {
//            super(provisioning, path);
//            this.path = path;
//        }
//
//        @Override
//        public boolean matches(HttpServletRequest request) {
//            HostedProviderService provider = getSamlProvisioning().getHostedProvider();
//            String prefix = provider.getConfiguration().getPrefix();
//            String alias = provider.getConfiguration().getAlias();
//            String path = this.path;
//            String matcherUrl = getExpectedPath(request.getServletPath(), prefix, alias, path);
//            AntPathRequestMatcher matcher = new AntPathRequestMatcher(matcherUrl);
////            return matcher.matches(request);
//            return false;
//        }
//
//        private String getExpectedPath(String requestServletPath, String prefix, String alias, String path) {
//            String result = "/" + stripSlashes(prefix);
//            if (!stripEndingSlases(result).equals(requestServletPath)
//                    && !(stripEndingSlases(result)+"/").equals(requestServletPath)) {
//                result = stripEndingSlases(result) + "/" + stripSlashes(path);
//            }
//            if (isMatchAgainstAliasPath()) {
//                result = appendSlash(result);
//                result = addAliasPath(result, alias);
//            }
//            result = result + "/**";
//            return result;
//        }
//    }

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
