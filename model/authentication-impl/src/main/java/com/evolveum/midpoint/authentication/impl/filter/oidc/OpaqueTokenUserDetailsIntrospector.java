/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.filter.oidc;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;

public class OpaqueTokenUserDetailsIntrospector implements OpaqueTokenIntrospector {

    private final DefaultOAuth2UserService userService = new DefaultOAuth2UserService();
    private final ClientRegistration clientRegistration;

    public OpaqueTokenUserDetailsIntrospector(ClientRegistration clientRegistration) {
        this.clientRegistration = clientRegistration;
    }

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String tokenValue) {
        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, tokenValue, null, null);

        OAuth2UserRequest userRequest = new OAuth2UserRequest(this.clientRegistration, token);

        return this.userService.loadUser(userRequest);
    }
}
