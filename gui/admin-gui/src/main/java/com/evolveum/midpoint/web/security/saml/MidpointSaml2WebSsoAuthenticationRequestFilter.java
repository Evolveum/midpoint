/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security.saml;

import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.web.security.module.authentication.Saml2ModuleAuthentication;
import com.evolveum.midpoint.web.security.util.RequestState;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationRequestContext;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationRequestFactory;
import org.springframework.security.saml2.provider.service.servlet.filter.Saml2WebSsoAuthenticationRequestFilter;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationRequestContextResolver;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
            Saml2ModuleAuthentication moduleAuthentication = (Saml2ModuleAuthentication) mpAuthentication.getProcessingModuleAuthentication();
            moduleAuthentication.setRequestState(RequestState.SENDED);
        }
    }
}
