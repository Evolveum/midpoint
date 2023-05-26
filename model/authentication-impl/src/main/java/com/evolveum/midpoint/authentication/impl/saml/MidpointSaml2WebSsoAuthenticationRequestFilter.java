/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.saml;

import org.springframework.security.saml2.provider.service.web.Saml2WebSsoAuthenticationRequestFilter;
import org.springframework.security.saml2.provider.service.web.authentication.OpenSaml4AuthenticationRequestResolver;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class MidpointSaml2WebSsoAuthenticationRequestFilter extends Saml2WebSsoAuthenticationRequestFilter {

    private RequestMatcher redirectMatcher = new AntPathRequestMatcher("/saml2/authenticate/{registrationId}");
    private final OpenSaml4AuthenticationRequestResolver authenticationRequestResolver;

    public MidpointSaml2WebSsoAuthenticationRequestFilter(OpenSaml4AuthenticationRequestResolver authenticationRequestContextResolver) {
        super(authenticationRequestContextResolver);
        this.authenticationRequestResolver = authenticationRequestContextResolver;
    }


    public OpenSaml4AuthenticationRequestResolver getAuthenticationRequestResolver() {
        return authenticationRequestResolver;
    }
}
