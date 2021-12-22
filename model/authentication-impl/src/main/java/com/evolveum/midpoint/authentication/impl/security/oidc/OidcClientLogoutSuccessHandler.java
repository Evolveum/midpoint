/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.security.oidc;

import com.evolveum.midpoint.authentication.impl.security.handler.AuditedLogoutHandler;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class OidcClientLogoutSuccessHandler extends AuditedLogoutHandler {

    private final OidcClientInitiatedLogoutSuccessHandler oidcHandler;

    public OidcClientLogoutSuccessHandler(OidcClientInitiatedLogoutSuccessHandler oidcHandler) {
        this.oidcHandler = oidcHandler;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication)
            throws IOException, ServletException {
        oidcHandler.setPostLogoutRedirectUri(getTargetUrl(authentication));
        oidcHandler.onLogoutSuccess(httpServletRequest, httpServletResponse, authentication);
        auditEvent(httpServletRequest, authentication);
    }
}
