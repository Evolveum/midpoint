/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.transport.impl;

import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OAuth2CredentialsType;

/**
 * Utility class for OAuth2 token retrieval using Spring Security's OAuth2 client infrastructure.
 */
public class OAuth2TokenService {

    public static final String REGISTRATION_ID = "mail-oauth2";

    /**
     * Retrieves an OAuth2 access token using client credentials flow.
     */
    public static String getAccessToken(OAuth2CredentialsType oauth2Credentials, String clientSecret) throws OAuth2TokenRetrievalException {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId(REGISTRATION_ID)
                .clientId(oauth2Credentials.getClientId())
                .clientSecret(clientSecret)
                .tokenUri(oauth2Credentials.getTokenEndpoint())
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope(oauth2Credentials.getScope())
                .build();

        InMemoryClientRegistrationRepository repo = new InMemoryClientRegistrationRepository(clientRegistration);
        OAuth2AuthorizedClientService service = new InMemoryOAuth2AuthorizedClientService(repo);
        OAuth2AuthorizedClientManager manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(repo, service);

        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(REGISTRATION_ID)
                .principal(oauth2Credentials.getUsername())
                .build();

        OAuth2AuthorizedClient authorizedClient = manager.authorize(authorizeRequest);
        if (authorizedClient == null) {
            throw new OAuth2TokenRetrievalException("Failed to authorize client");
        }
        if (authorizedClient.getAccessToken() == null) {
            throw new OAuth2TokenRetrievalException("Failed to retrieve access token");
        }

        return authorizedClient.getAccessToken().getTokenValue();
    }
}
