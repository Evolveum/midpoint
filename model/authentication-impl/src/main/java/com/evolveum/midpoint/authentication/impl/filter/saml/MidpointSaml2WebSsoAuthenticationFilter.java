/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.filter.saml;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.evolveum.midpoint.authentication.impl.filter.RemoteAuthenticationFilter;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml2.provider.service.web.authentication.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.web.authentication.AuthenticationConverter;

import com.evolveum.midpoint.model.api.ModelAuditRecorder;

public class MidpointSaml2WebSsoAuthenticationFilter extends Saml2WebSsoAuthenticationFilter implements RemoteAuthenticationFilter {

    private final ModelAuditRecorder auditProvider;

    public MidpointSaml2WebSsoAuthenticationFilter(AuthenticationConverter authenticationConverter, String filterProcessingUrl,
            ModelAuditRecorder auditProvider) {
        super(authenticationConverter, filterProcessingUrl);
        this.auditProvider = auditProvider;
    }

    public boolean requiresAuth(HttpServletRequest request, HttpServletResponse response) {
        return super.requiresAuthentication(request, response);
    }

    public void unsuccessfulAuth(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws ServletException, IOException {
        remoteUnsuccessfulAuthentication(request, response, failed, getRememberMeServices(), getFailureHandler());
    }

    @Override
    public String getErrorMessageKeyNotResponse() {
        return "web.security.flexAuth.saml.not.response";
    }

    @Override
    public void doAuth(ServletRequest req, ServletResponse res, FilterChain chain) throws ServletException, IOException {
        super.doFilter(req, res, chain);
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        doRemoteFilter(req, res, chain);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        remoteUnsuccessfulAuthentication(
                request, response, failed, auditProvider, getRememberMeServices(), getFailureHandler(), "SAML");
    }
}
