/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.saml;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;

import com.evolveum.midpoint.authentication.impl.module.authentication.Saml2ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.util.RequestState;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationRequestContext;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationRequestFactory;
import org.springframework.security.saml2.provider.service.servlet.filter.Saml2WebSsoAuthenticationRequestFilter;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationRequestContextResolver;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class MidpointSaml2WebSsoAuthenticationRequestFilter extends Saml2WebSsoAuthenticationRequestFilter {

    private RequestMatcher redirectMatcher = new AntPathRequestMatcher("/saml2/authenticate/{registrationId}");
    private final Saml2AuthenticationRequestContextResolver authenticationRequestContextResolver;

    public MidpointSaml2WebSsoAuthenticationRequestFilter(Saml2AuthenticationRequestContextResolver authenticationRequestContextResolver, Saml2AuthenticationRequestFactory authenticationRequestFactory) {
        super(authenticationRequestContextResolver, authenticationRequestFactory);
        this.authenticationRequestContextResolver = authenticationRequestContextResolver;
    }

    @Override
    public void setRedirectMatcher(RequestMatcher redirectMatcher) {
        super.setRedirectMatcher(redirectMatcher);
        this.redirectMatcher = redirectMatcher;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        super.doFilterInternal(request, response, filterChain);
        RequestMatcher.MatchResult matcher = this.redirectMatcher.matcher(request);
        if (!matcher.isMatch()) {
            return;
        }

        Saml2AuthenticationRequestContext context = this.authenticationRequestContextResolver.resolve(request);
        if (context == null) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            Saml2ModuleAuthenticationImpl moduleAuthentication = (Saml2ModuleAuthenticationImpl) mpAuthentication.getProcessingModuleAuthentication();
            moduleAuthentication.setRequestState(RequestState.SENDED);
        }
    }
}
