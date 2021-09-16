/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security.saml;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.evolveum.midpoint.web.security.AuditedLogoutHandler;

import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.web.authentication.logout.Saml2LogoutRequestSuccessHandler;

public class MidpointSaml2LogoutRequestSuccessHandler extends AuditedLogoutHandler {

    private final Saml2LogoutRequestSuccessHandler samlHandler;

    public MidpointSaml2LogoutRequestSuccessHandler(Saml2LogoutRequestSuccessHandler samlHandler) {
        this.samlHandler = samlHandler;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException {
        samlHandler.onLogoutSuccess(httpServletRequest, httpServletResponse, authentication);
        auditEvent(httpServletRequest, authentication);
    }
}
