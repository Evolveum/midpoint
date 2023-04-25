/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.oidc;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.util.Assert;

import java.util.Map;

public class OidcUserTokenService extends DefaultOAuth2UserService {

    private static final Trace LOGGER = TraceManager.getTrace(OidcUserTokenService.class);

    private JwtDecoderFactory<ClientRegistration> jwtDecoderFactory = new OidcIdTokenDecoderFactory();

    private static final String MISSING_USER_NAME_ATTRIBUTE_ERROR_CODE = "missing_user_name_attribute";

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        Assert.notNull(userRequest, "userRequest cannot be null");

        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint()
                .getUserNameAttributeName();
        if (StringUtils.isEmpty(userNameAttributeName)) {
            OAuth2Error oauth2Error = new OAuth2Error(MISSING_USER_NAME_ATTRIBUTE_ERROR_CODE,
                    "Missing required \"user name\" attribute name in UserInfoEndpoint for Client Registration: "
                            + userRequest.getClientRegistration().getRegistrationId(),
                    null);
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
        }

        if (userRequest instanceof OidcUserRequest) {
            OidcUserRequest oidcRequest = (OidcUserRequest) userRequest;
            Map<String, Object> claims = oidcRequest.getIdToken().getClaims();
            if (claims != null && claims.containsKey(userNameAttributeName)) {
                return new DefaultOAuth2User(null, claims, userNameAttributeName);
            }
        }

        if (userRequest.getAccessToken() != null
                && StringUtils.isNotEmpty(userRequest.getAccessToken().getTokenValue())) {
            JwtDecoder jwtDecoder = this.jwtDecoderFactory.createDecoder(userRequest.getClientRegistration());
            try {
                Jwt jwt = jwtDecoder.decode(userRequest.getAccessToken().getTokenValue());
                Map<String, Object> claims = jwt.getClaims();
                if (claims != null && claims.containsKey(userNameAttributeName)) {
                    return new DefaultOAuth2User(null, claims, userNameAttributeName);
                }
            }
            catch (JwtException e) {
                LOGGER.debug("Couldn't decode JWT from access token", e);
            }
        }

        return super.loadUser(userRequest);
    }

    public void setJwtDecoderFactory(JwtDecoderFactory<ClientRegistration> jwtDecoderFactory) {
        this.jwtDecoderFactory = jwtDecoderFactory;
    }
}
