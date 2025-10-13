/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.filter.saml;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.module.authentication.Saml2ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.util.RequestState;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.provider.service.authentication.AbstractSaml2AuthenticationRequest;
import org.springframework.security.saml2.provider.service.web.Saml2WebSsoAuthenticationRequestFilter;
import org.springframework.security.saml2.provider.service.web.authentication.OpenSaml4AuthenticationRequestResolver;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.IOException;

public class MidpointSaml2WebSsoAuthenticationRequestFilter extends Saml2WebSsoAuthenticationRequestFilter {

    private final OpenSaml4AuthenticationRequestResolver authenticationRequestResolver;

    private final SecurityContextRepository securityContextRepository;

    public MidpointSaml2WebSsoAuthenticationRequestFilter(
            OpenSaml4AuthenticationRequestResolver authenticationRequestContextResolver,
            SecurityContextRepository securityContextRepository) {
        super(authenticationRequestContextResolver);
        this.authenticationRequestResolver = authenticationRequestContextResolver;
        this.securityContextRepository = securityContextRepository;
    }

    public OpenSaml4AuthenticationRequestResolver getAuthenticationRequestResolver() {
        return authenticationRequestResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        AbstractSaml2AuthenticationRequest authenticationRequest = getAuthenticationRequestResolver().resolve(request);
        if (authenticationRequest != null) {
            securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
        }

        super.doFilterInternal(request, response, filterChain);

        if (authenticationRequest == null) {
            return;
        }

        MidpointAuthentication authentication = AuthUtil.getMidpointAuthentication();
        Saml2ModuleAuthenticationImpl moduleAuthentication =
                (Saml2ModuleAuthenticationImpl) authentication.getProcessingModuleAuthentication();
        moduleAuthentication.setRequestState(RequestState.SENT);
    }
}
