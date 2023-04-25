/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.oidc;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.impl.module.authentication.OidcClientModuleAuthenticationImpl;

import com.evolveum.midpoint.model.api.ModelAuditRecorder;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.ThrowableAnalyzer;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class OidcAuthorizationRequestRedirectFilter extends OncePerRequestFilter {

    private final OAuth2AuthorizationRequestResolver authorizationRequestResolver;

    private final ModelAuditRecorder auditProvider;

    private AuthenticationFailureHandler failureHandler;

    public OidcAuthorizationRequestRedirectFilter(ClientRegistrationRepository clientRegistrationRepository,
            String authorizationRequestBaseUri, ModelAuditRecorder auditProvider) {
        this.authorizationRequestResolver = new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository,
                authorizationRequestBaseUri);
        this.auditProvider = auditProvider;
    }

    public void setAuthenticationFailureHandler(AuthenticationFailureHandler failureHandler) {
        Assert.notNull(failureHandler, "failureHandler cannot be null");
        this.failureHandler = failureHandler;
    }

    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        String channel;
        Authentication actualAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (actualAuthentication instanceof MidpointAuthentication && ((MidpointAuthentication) actualAuthentication).getAuthenticationChannel() != null) {
            channel = ((MidpointAuthentication) actualAuthentication).getAuthenticationChannel().getChannelId();
        } else {
            channel = SchemaConstants.CHANNEL_USER_URI;
        }

        auditProvider.auditLoginFailure("unknown user", null, ConnectionEnvironment.create(channel), "OIDC authentication module: " + failed.getMessage());

        this.failureHandler.onAuthenticationFailure(request, response, failed);
    }

    private final ThrowableAnalyzer throwableAnalyzer = new OidcAuthorizationRequestRedirectFilter.DefaultThrowableAnalyzer();

    private final RedirectStrategy authorizationRedirectStrategy = new DefaultRedirectStrategy();

    private final AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository = new HttpSessionOAuth2AuthorizationRequestRepository();

    private RequestCache requestCache = new HttpSessionRequestCache();

    public final void setRequestCache(RequestCache requestCache) {
        Assert.notNull(requestCache, "requestCache cannot be null");
        this.requestCache = requestCache;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof MidpointAuthentication) {
            try {
                OAuth2AuthorizationRequest authorizationRequest = this.authorizationRequestResolver.resolve(request);
                if (authorizationRequest != null) {
                    this.sendRedirectForAuthorization(request, response, authorizationRequest);
                    return;
                }
            } catch (Exception ex) {
                unsuccessfulAuthentication(request, response,
                        new InternalAuthenticationServiceException("web.security.provider.invalid", ex));
                return;
            }
            try {
                filterChain.doFilter(request, response);
            } catch (IOException ex) {
                throw ex;
            } catch (Exception ex) {
                // Check to see if we need to handle ClientAuthorizationRequiredException
                Throwable[] causeChain = this.throwableAnalyzer.determineCauseChain(ex);
                ClientAuthorizationRequiredException authzEx = (ClientAuthorizationRequiredException) this.throwableAnalyzer
                        .getFirstThrowableOfType(ClientAuthorizationRequiredException.class, causeChain);
                if (authzEx != null) {
                    try {
                        OAuth2AuthorizationRequest authorizationRequest = this.authorizationRequestResolver.resolve(request,
                                authzEx.getClientRegistrationId());
                        if (authorizationRequest == null) {
                            throw authzEx;
                        }
                        this.sendRedirectForAuthorization(request, response, authorizationRequest);
                        this.requestCache.saveRequest(request, response);
                    } catch (Exception failed) {
                        unsuccessfulAuthentication(request, response,
                                new InternalAuthenticationServiceException("web.security.provider.invalid", failed));
                    }
                    return;
                }
                if (ex instanceof ServletException) {
                    throw (ServletException) ex;
                }
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                }
                throw new RuntimeException(ex);
            }
        } else {
            throw new AuthenticationServiceException("Unsupported type of Authentication");
        }
    }

    private void sendRedirectForAuthorization(HttpServletRequest request, HttpServletResponse response,
            OAuth2AuthorizationRequest authorizationRequest) throws IOException {
        this.authorizationRequestRepository.saveAuthorizationRequest(authorizationRequest, request, response);
        this.authorizationRedirectStrategy.sendRedirect(request, response,
                authorizationRequest.getAuthorizationRequestUri());
    }

    private static final class DefaultThrowableAnalyzer extends ThrowableAnalyzer {

        @Override
        protected void initExtractorMap() {
            super.initExtractorMap();
            registerExtractor(ServletException.class, (throwable) -> {
                ThrowableAnalyzer.verifyThrowableHierarchy(throwable, ServletException.class);
                return ((ServletException) throwable).getRootCause();
            });
        }

    }
}
