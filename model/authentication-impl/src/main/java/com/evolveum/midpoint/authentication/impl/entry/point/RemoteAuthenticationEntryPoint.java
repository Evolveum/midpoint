/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.entry.point;

import java.io.IOException;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.evolveum.midpoint.authentication.api.IdentityProvider;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;

import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;

import com.evolveum.midpoint.authentication.api.config.RemoteModuleAuthentication;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author skublik
 */
public class RemoteAuthenticationEntryPoint extends WicketLoginUrlAuthenticationEntryPoint {

    public RemoteAuthenticationEntryPoint(String loginFormUrl) {
        super(loginFormUrl);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
            if (moduleAuthentication instanceof RemoteModuleAuthentication) {
                List<IdentityProvider> providers = ((RemoteModuleAuthentication) moduleAuthentication).getProviders();
                if (request.getSession().getAttribute("SPRING_SECURITY_LAST_EXCEPTION") == null) {
                    if (providers.size() == 1) {
                        response.sendRedirect(providers.get(0).getRedirectLink());
                        return;
                    }
                } else if (getLoginFormUrl().equals(request.getServletPath())
                        && AuthenticationModuleState.LOGIN_PROCESSING.equals(moduleAuthentication.getState())) {
                    return;
                }
            }
        }
        super.commence(request, response, authException);
    }
}
