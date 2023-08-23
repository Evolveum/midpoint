/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.filter.saml;

import java.io.IOException;
import jakarta.servlet.ServletException;
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

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        remoteUnsuccessfulAuthentication(
                request, response, failed, auditProvider, getRememberMeServices(), getFailureHandler(), "SAML");
    }
}
