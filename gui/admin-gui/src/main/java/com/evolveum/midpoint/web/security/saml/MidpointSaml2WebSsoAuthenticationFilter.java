/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security.saml;

import com.evolveum.midpoint.model.api.ModelAuditRecorder;
import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.web.security.module.authentication.Saml2ModuleAuthentication;
import com.evolveum.midpoint.web.security.util.RequestState;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.saml2.provider.service.servlet.filter.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.web.authentication.AuthenticationConverter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class MidpointSaml2WebSsoAuthenticationFilter extends Saml2WebSsoAuthenticationFilter {

    private ModelAuditRecorder auditProvider;

    public MidpointSaml2WebSsoAuthenticationFilter(AuthenticationConverter authenticationConverter, String filterProcessingUrl,
            ModelAuditRecorder auditProvider) {
        super(authenticationConverter, filterProcessingUrl);
        this.auditProvider = auditProvider;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean sendedRequest = false;
        Saml2ModuleAuthentication moduleAuthentication = null;
        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            moduleAuthentication = (Saml2ModuleAuthentication) mpAuthentication.getProcessingModuleAuthentication();
            if (moduleAuthentication != null && RequestState.SENDED.equals(moduleAuthentication.getRequestState())) {
                sendedRequest = true;
            }
            boolean requiresAuthentication = requiresAuthentication((HttpServletRequest) req, (HttpServletResponse) res);

            if (!requiresAuthentication && sendedRequest) {
                AuthenticationServiceException exception = new AuthenticationServiceException("web.security.flexAuth.saml.not.response");
                unsuccessfulAuthentication((HttpServletRequest) req, (HttpServletResponse) res, exception);
                return;
            } else {
                if (moduleAuthentication != null && requiresAuthentication && sendedRequest) {
                    moduleAuthentication.setRequestState(RequestState.RECEIVED);
                }
                super.doFilter(req, res, chain);
            }
        } else {
            throw new AuthenticationServiceException("Unsupported type of Authentication");
        }
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        String channel;
        Authentication actualAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (actualAuthentication instanceof MidpointAuthentication && ((MidpointAuthentication) actualAuthentication).getAuthenticationChannel() != null) {
            channel = ((MidpointAuthentication) actualAuthentication).getAuthenticationChannel().getChannelId();
        } else {
            channel = SchemaConstants.CHANNEL_USER_URI;
        }

        auditProvider.auditLoginFailure("unknown user", null, ConnectionEnvironment.create(channel), "SAML authentication module: " + failed.getMessage());

        getRememberMeServices().loginFail(request, response);

        getFailureHandler().onAuthenticationFailure(request, response, failed);
    }
}
