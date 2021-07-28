/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security.saml;

import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationRequestFactory;
import org.springframework.security.saml2.provider.service.servlet.filter.Saml2WebSsoAuthenticationRequestFilter;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationRequestContextResolver;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

//TODO we need save NameID and set it to LogoutRequest
public class MidpointSaml2WebSsoLogoutRequestFilter extends Saml2WebSsoAuthenticationRequestFilter {

    private final Saml2AuthenticationRequestContextResolver authenticationRequestContextResolver;

    public MidpointSaml2WebSsoLogoutRequestFilter(Saml2AuthenticationRequestContextResolver authenticationRequestContextResolver, Saml2AuthenticationRequestFactory authenticationRequestFactory) {
        super(authenticationRequestContextResolver, authenticationRequestFactory);
        this.authenticationRequestContextResolver = authenticationRequestContextResolver;
        setRedirectMatcher(new AntPathRequestMatcher("/**"));
    }
}
