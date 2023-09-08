/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.filter;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.impl.NotShowedAuthenticationServiceException;
import com.evolveum.midpoint.authentication.impl.module.authentication.RemoteModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.util.RequestState;
import com.evolveum.midpoint.model.api.ModelAuditRecorder;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.RememberMeServices;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface RemoteAuthenticationFilter extends Filter {

    Trace LOGGER = TraceManager.getTrace(RemoteAuthenticationFilter.class);

    boolean requiresAuth(HttpServletRequest request, HttpServletResponse response);

    void unsuccessfulAuth(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed)
            throws IOException, ServletException;

    String getErrorMessageKeyNotResponse();

    void doAuth(ServletRequest req, ServletResponse res, FilterChain chain) throws ServletException, IOException;

    default void doRemoteFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean sentRequest = false;
        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            RemoteModuleAuthenticationImpl moduleAuthentication = (RemoteModuleAuthenticationImpl) mpAuthentication.getProcessingModuleAuthentication();
            if (moduleAuthentication != null && RequestState.SENDED.equals(moduleAuthentication.getRequestState())) {
                sentRequest = true;
            }
            boolean requiresAuthentication = requiresAuth((HttpServletRequest) req, (HttpServletResponse) res);

            if (!requiresAuthentication && sentRequest) {
                NotShowedAuthenticationServiceException exception =
                        new NotShowedAuthenticationServiceException(getErrorMessageKeyNotResponse());
                unsuccessfulAuth((HttpServletRequest) req, (HttpServletResponse) res, exception);
            } else {
                if (moduleAuthentication != null && requiresAuthentication && sentRequest) {
                    moduleAuthentication.setRequestState(RequestState.RECEIVED);
                }
                doAuth(req, res, chain);
            }
        } else {
            throw new AuthenticationServiceException("Unsupported type of Authentication");
        }
    }

    default void remoteUnsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException failed, ModelAuditRecorder auditProvider, RememberMeServices rememberMeService,
            AuthenticationFailureHandler failureHandler, String moduleName) throws IOException, ServletException {
        String channel;
        Authentication actualAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (actualAuthentication instanceof MidpointAuthentication && ((MidpointAuthentication) actualAuthentication).getAuthenticationChannel() != null) {
            channel = ((MidpointAuthentication) actualAuthentication).getAuthenticationChannel().getChannelId();
        } else {
            channel = SchemaConstants.CHANNEL_USER_URI;
        }

        auditProvider.auditLoginFailure("unknown user", null, ConnectionEnvironment.create(channel),
                moduleName + " authentication module: " + failed.getMessage());

        rememberMeService.loginFail(request, response);

        failureHandler.onAuthenticationFailure(request, response, failed);
    }

    default void remoteUnsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException failed, RememberMeServices rememberMeService,
            AuthenticationFailureHandler failureHandler) throws ServletException, IOException {
        LOGGER.trace("Failed to process authentication request", failed);
        LOGGER.trace("Cleared SecurityContextHolder");
        LOGGER.trace("Handling authentication failure");
        rememberMeService.loginFail(request, response);
        failureHandler.onAuthenticationFailure(request, response, failed);
    }
}
