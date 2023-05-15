/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.saml;

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
}
