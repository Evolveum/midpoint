/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.entry.point;

import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.evolveum.midpoint.authentication.api.IdentityProvider;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;

import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;

import com.evolveum.midpoint.authentication.impl.module.authentication.Saml2ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configurer.SamlModuleWebSecurityConfigurer;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author skublik
 */
public class SamlAuthenticationEntryPoint extends WicketLoginUrlAuthenticationEntryPoint {

    public SamlAuthenticationEntryPoint(String loginFormUrl) {
        super(loginFormUrl);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
            if (moduleAuthentication instanceof Saml2ModuleAuthenticationImpl) {
                List<IdentityProvider> providers = ((Saml2ModuleAuthenticationImpl) moduleAuthentication).getProviders();
                if (request.getSession().getAttribute("SPRING_SECURITY_LAST_EXCEPTION") == null) {
                    if (providers.size() == 1) {
                        response.sendRedirect(providers.get(0).getRedirectLink());
                        return;
                    }
                } else if (SamlModuleWebSecurityConfigurer.SAML_LOGIN_PATH.equals(request.getServletPath())
                        && AuthenticationModuleState.LOGIN_PROCESSING.equals(moduleAuthentication.getState())) {
                    return;
                }
            }
        }
        super.commence(request, response, authException);
    }
}
