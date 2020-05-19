/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.web.security.module.authentication.Saml2ModuleAuthentication;
import com.evolveum.midpoint.web.security.util.IdentityProvider;

/**
 * @author skublik
 */
public class SamlAuthenticationEntryPoint extends WicketLoginUrlAuthenticationEntryPoint {

    public SamlAuthenticationEntryPoint(String loginFormUrl) {
        super(loginFormUrl);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        List<IdentityProvider> providers = new ArrayList<IdentityProvider>();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
            if (moduleAuthentication instanceof Saml2ModuleAuthentication) {
                providers = ((Saml2ModuleAuthentication) moduleAuthentication).getProviders();
                if (providers.size() == 1
                        && request.getSession().getAttribute("SPRING_SECURITY_LAST_EXCEPTION") == null) {
                    response.sendRedirect(providers.get(0).getRedirectLink());
                    return;
                }
            }
        }
        super.commence(request, response, authException);
    }
}
