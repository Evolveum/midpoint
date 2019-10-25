/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.boot.testsaml;

import com.evolveum.midpoint.model.api.authentication.UserProfileService;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml.SamlRequestMatcher;
import org.springframework.security.saml.provider.HostedProviderService;
import org.springframework.security.saml.provider.SamlServerConfiguration;
import org.springframework.security.saml.provider.provisioning.SamlProviderProvisioning;
import org.springframework.security.saml.provider.service.SelectIdentityProviderFilter;
import org.springframework.security.saml.provider.service.authentication.GenericErrorAuthenticationFailureHandler;
import org.springframework.security.saml.provider.service.config.SamlServiceProviderServerBeanConfiguration;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;

import static org.springframework.security.saml.util.StringUtils.*;
import static org.springframework.security.saml.util.StringUtils.addAliasPath;

/**
 * @author skublik
 */

@Configuration
public class SamlProviderServerBeanConfiguration extends SamlServiceProviderServerBeanConfiguration {

    @Autowired
    private UserProfileService userProfileService;

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
        return new SelectIdentityProviderFilter(getSamlProvisioning(), new MidpointSamlRequestMatcher(getSamlProvisioning(), "select"));
    }

    private class MidpointSamlRequestMatcher extends SamlRequestMatcher {

        private String path;

        public MidpointSamlRequestMatcher(SamlProviderProvisioning provisioning, String path) {
            super(provisioning, path);
            this.path = path;
        }

        @Override
        public boolean matches(HttpServletRequest request) {
            HostedProviderService provider = getSamlProvisioning().getHostedProvider();
            String prefix = provider.getConfiguration().getPrefix();
            String alias = provider.getConfiguration().getAlias();
            String path = this.path;
            String matcherUrl = getExpectedPath(request.getServletPath(), prefix, alias, path);
            AntPathRequestMatcher matcher = new AntPathRequestMatcher(matcherUrl);
            return matcher.matches(request);
        }

        private String getExpectedPath(String requestServletPath, String prefix, String alias, String path) {
            String result = "/" + stripSlashes(prefix);
            if (!stripEndingSlases(result).equals(requestServletPath)
                    && !(stripEndingSlases(result)+"/").equals(requestServletPath)) {
                result = stripEndingSlases(result) + "/" + stripSlashes(path);
            }
            if (isMatchAgainstAliasPath()) {
                result = appendSlash(result);
                result = addAliasPath(result, alias);
            }
            result = result + "/**";
            return result;
        }
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
