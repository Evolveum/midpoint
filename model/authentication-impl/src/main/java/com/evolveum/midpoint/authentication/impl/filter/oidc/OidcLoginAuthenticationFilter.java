/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.filter.oidc;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;

import com.evolveum.midpoint.authentication.impl.filter.RemoteAuthenticationFilter;
import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;

import com.evolveum.midpoint.model.api.ModelAuditRecorder;

import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.*;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@Order
public class OidcLoginAuthenticationFilter extends OAuth2LoginAuthenticationFilter implements RemoteAuthenticationFilter {

    private static final String AUTHORIZATION_REQUEST_NOT_FOUND_ERROR_CODE = "authorization_request_not_found";
    private static final String CLIENT_REGISTRATION_NOT_FOUND_ERROR_CODE = "client_registration_not_found";
    private static final String INVALID_REQUEST_ERROR_CODE = "invalid_request";

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository;
    private final ModelAuditRecorder auditProvider;

    public OidcLoginAuthenticationFilter(ClientRegistrationRepository clientRegistrationRepository, String filterProcessesUrl,
            ModelAuditRecorder auditProvider) {
        super(clientRegistrationRepository, new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(
                new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository)), filterProcessesUrl);
        this.authorizationRequestRepository = new HttpSessionOAuth2AuthorizationRequestRepository();
        Assert.notNull(clientRegistrationRepository, "clientRegistrationRepository cannot be null");
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.auditProvider = auditProvider;
    }

    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        MultiValueMap<String, String> params = toMultiMap(request.getParameterMap());
        if (!isAuthorizationResponse(params)) {
            OAuth2Error oauth2Error = new OAuth2Error(INVALID_REQUEST_ERROR_CODE);
            throw new OAuth2AuthenticationException(oauth2Error, "web.security.provider.invalid");
        } else {
            OAuth2AuthorizationRequest authorizationRequest = this.authorizationRequestRepository.removeAuthorizationRequest(request, response);
            if (authorizationRequest == null) {
                OAuth2Error oauth2Error = new OAuth2Error(AUTHORIZATION_REQUEST_NOT_FOUND_ERROR_CODE);
                throw new OAuth2AuthenticationException(oauth2Error, "web.security.provider.invalid");
            } else {
                String registrationId = authorizationRequest.getAttribute("registration_id");
                ClientRegistration clientRegistration = this.clientRegistrationRepository.findByRegistrationId(registrationId);
                if (clientRegistration == null) {
                    OAuth2Error oauth2Error = new OAuth2Error(CLIENT_REGISTRATION_NOT_FOUND_ERROR_CODE,
                            "Client Registration not found with Id: " + registrationId, null);
                    throw new OAuth2AuthenticationException(oauth2Error, "web.security.provider.invalid");
                } else {
                    String redirectUri = UriComponentsBuilder.fromHttpUrl(UrlUtils.buildFullRequestUrl(request))
                            .replaceQuery(null).build().toUriString();
                    OAuth2AuthorizationResponse authorizationResponse = convert(params, redirectUri);
                    OAuth2LoginAuthenticationToken authenticationRequest = new OAuth2LoginAuthenticationToken(clientRegistration,
                            new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse));
                    MidpointAuthentication authenticationResult =
                            (MidpointAuthentication) this.getAuthenticationManager().authenticate(authenticationRequest);
                    Assert.notNull(authenticationResult, "authentication result cannot be null");
                    return authenticationResult;
                }
            }
        }
    }

    private boolean isAuthorizationResponse(MultiValueMap<String, String> request) {
        return StringUtils.hasText(request.getFirst("code")) && StringUtils.hasText(request.getFirst("state"))
                || StringUtils.hasText(request.getFirst("error")) && StringUtils.hasText(request.getFirst("state"));
    }

    private OAuth2AuthorizationResponse convert(MultiValueMap<String, String> request, String redirectUri) {
        String code = request.getFirst("code");
        String errorCode = request.getFirst("error");
        String state = request.getFirst("state");
        if (StringUtils.hasText(code)) {
            return OAuth2AuthorizationResponse.success(code).redirectUri(redirectUri).state(state).build();
        } else {
            String errorDescription = request.getFirst("error_description");
            String errorUri = request.getFirst("error_uri");
            return OAuth2AuthorizationResponse.error(errorCode).redirectUri(redirectUri).errorDescription(errorDescription).errorUri(errorUri).state(state).build();
        }
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        remoteUnsuccessfulAuthentication(
                request, response, failed, auditProvider, getRememberMeServices(), getFailureHandler(), "OIDC");
    }
}
