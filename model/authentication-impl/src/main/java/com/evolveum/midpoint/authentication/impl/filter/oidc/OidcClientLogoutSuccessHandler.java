/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.filter.oidc;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.handler.AuditedLogoutHandler;

import com.evolveum.midpoint.authentication.impl.module.authentication.OidcClientModuleAuthenticationImpl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class OidcClientLogoutSuccessHandler extends AuditedLogoutHandler {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private String postLogoutRedirectUri;
    private String publicUrlPrefix;

    public OidcClientLogoutSuccessHandler(ClientRegistrationRepository clientRegistrationRepository) {
        Assert.notNull(clientRegistrationRepository, "clientRegistrationRepository cannot be null");
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication)
            throws IOException, ServletException {
        super.handle(httpServletRequest, httpServletResponse, authentication);
        auditEvent(httpServletRequest, authentication);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String targetUrl = null;
        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mPAuthentication = (MidpointAuthentication) authentication;
            ModuleAuthentication moduleAuthentication = mPAuthentication.getProcessingModuleAuthentication();
            if (moduleAuthentication instanceof OidcClientModuleAuthenticationImpl) {
                Authentication internalAuthentication = moduleAuthentication.getAuthentication();
                if (internalAuthentication instanceof PreAuthenticatedAuthenticationToken
                        || internalAuthentication instanceof AnonymousAuthenticationToken) {
                    Object details = internalAuthentication.getDetails();
                    if (details instanceof OAuth2LoginAuthenticationToken) {
                        OidcUser oidcUser = this.getOidcUser((OAuth2LoginAuthenticationToken) details);
                        if (oidcUser != null) {
                            OAuth2LoginAuthenticationToken oidcAuthentication = (OAuth2LoginAuthenticationToken) details;
                            String registrationId = oidcAuthentication.getClientRegistration().getRegistrationId();
                            ClientRegistration clientRegistration = this.clientRegistrationRepository.findByRegistrationId(registrationId);
                            URI endSessionEndpoint = this.endSessionEndpoint(clientRegistration);
                            if (endSessionEndpoint != null) {
                                String idToken = this.idToken(oidcUser);
                                String postLogoutRedirectUri = this.postLogoutRedirectUri(request);
                                targetUrl = this.endpointUri(endSessionEndpoint, idToken, postLogoutRedirectUri);
                            }
                        }
                    }
                }
            }
        }

        return targetUrl != null ? targetUrl : super.determineTargetUrl(request, response);
    }

    private OidcUser getOidcUser(OAuth2LoginAuthenticationToken authentication) {
        if (authentication.getPrincipal() instanceof OidcUser) {
            return (OidcUser) authentication.getPrincipal();
        }

        if (authentication.getDetails() instanceof OidcUser) {
            return (OidcUser) authentication.getDetails();
        }
        return null;
    }

    private URI endSessionEndpoint(ClientRegistration clientRegistration) {
        if (clientRegistration != null) {
            ClientRegistration.ProviderDetails providerDetails = clientRegistration.getProviderDetails();
            Object endSessionEndpoint = providerDetails.getConfigurationMetadata().get("end_session_endpoint");
            if (endSessionEndpoint != null) {
                return URI.create(endSessionEndpoint.toString());
            }
        }

        return null;
    }

    private String idToken(OidcUser oidcUser) {
        return oidcUser.getIdToken().getTokenValue();
    }

    private String postLogoutRedirectUri(HttpServletRequest request) {
        if (this.postLogoutRedirectUri == null) {
            return null;
        }
        if (StringUtils.isEmpty(publicUrlPrefix)) {
            return UriComponentsBuilder.fromHttpUrl(UrlUtils.buildFullRequestUrl(request))
                    .replacePath(request.getContextPath())
                    .pathSegment(AuthUtil.stripStartingSlashes(this.postLogoutRedirectUri))
                    .build()
                    .toUriString();
        }
        return  UriComponentsBuilder.fromUriString(publicUrlPrefix)
                .pathSegment(AuthUtil.stripStartingSlashes(this.postLogoutRedirectUri))
                .build()
                .toUriString();
    }

    private String endpointUri(URI endSessionEndpoint, String idToken, String postLogoutRedirectUri) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUri(endSessionEndpoint);
        builder.queryParam("id_token_hint", idToken);
        if (postLogoutRedirectUri != null) {
            builder.queryParam("post_logout_redirect_uri", postLogoutRedirectUri);
        }

        return builder.encode(StandardCharsets.UTF_8).build().toUriString();
    }

    public void setPostLogoutRedirectUri(String postLogoutRedirectUri) {
        Assert.notNull(postLogoutRedirectUri, "postLogoutRedirectUri cannot be null");
        this.postLogoutRedirectUri = postLogoutRedirectUri;
    }

    public void setPublicUrlPrefix(String publicUrlPrefix) {
        this.publicUrlPrefix = publicUrlPrefix;
    }
}
