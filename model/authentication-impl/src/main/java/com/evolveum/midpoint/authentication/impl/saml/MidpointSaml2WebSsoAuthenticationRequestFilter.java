/*
 * Copyright (C) 2010-2023 Evolveum and contributors
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
import org.springframework.security.saml2.provider.service.web.Saml2WebSsoAuthenticationRequestFilter;
import org.springframework.security.saml2.provider.service.web.authentication.OpenSaml4AuthenticationRequestResolver;
import org.springframework.security.saml2.provider.service.web.authentication.Saml2AuthenticationRequestResolver;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class MidpointSaml2WebSsoAuthenticationRequestFilter extends Saml2WebSsoAuthenticationRequestFilter {

    private final OpenSaml4AuthenticationRequestResolver authenticationRequestResolver;

    public MidpointSaml2WebSsoAuthenticationRequestFilter(OpenSaml4AuthenticationRequestResolver authenticationRequestResolver) {
        super(authenticationRequestResolver);
        this.authenticationRequestResolver = authenticationRequestResolver;
    }

    public OpenSaml4AuthenticationRequestResolver getAuthenticationRequestResolver() {
        return authenticationRequestResolver;
    }
}
