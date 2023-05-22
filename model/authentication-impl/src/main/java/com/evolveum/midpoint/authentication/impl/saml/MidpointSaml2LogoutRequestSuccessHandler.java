/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.saml;

import java.io.IOException;
import jakarta.servlet.http.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.evolveum.midpoint.authentication.impl.handler.AuditedLogoutHandler;

import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.web.authentication.logout.Saml2RelyingPartyInitiatedLogoutSuccessHandler;


public class MidpointSaml2LogoutRequestSuccessHandler extends AuditedLogoutHandler {

    private final Saml2RelyingPartyInitiatedLogoutSuccessHandler samlHandler;

    public MidpointSaml2LogoutRequestSuccessHandler(Saml2RelyingPartyInitiatedLogoutSuccessHandler samlHandler) {
        this.samlHandler = samlHandler;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException, ServletException {
        samlHandler.onLogoutSuccess(httpServletRequest, httpServletResponse, authentication);
        if (httpServletResponse.getStatus() == 401) {
            super.onLogoutSuccess(httpServletRequest, httpServletResponse, authentication);
        } else {
          auditEvent(httpServletRequest, authentication);
        }
    }
}
