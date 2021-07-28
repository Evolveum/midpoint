/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security.saml;

import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;

import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;

import com.evolveum.midpoint.web.security.module.authentication.Saml2ModuleAuthentication;

import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.Saml2Exception;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationRequestFactory;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.security.saml2.provider.service.web.DefaultSaml2AuthenticationRequestContextResolver;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationRequestContextResolver;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author skublik
 */
public class MidpointSamlLogoutHandler implements LogoutHandler {

    private final MidpointSaml2WebSsoLogoutRequestFilter logoutRequestFilter;
    private final MidpointLogoutRelyingPartyRegistrationResolver registrationResolver = new MidpointLogoutRelyingPartyRegistrationResolver();

    public MidpointSamlLogoutHandler() {
        Saml2AuthenticationRequestFactory authenticationRequestResolver = new MidpointLogoutRequestFactory();

        Saml2AuthenticationRequestContextResolver contextResolver = new DefaultSaml2AuthenticationRequestContextResolver(registrationResolver);
        this.logoutRequestFilter = new MidpointSaml2WebSsoLogoutRequestFilter(contextResolver, authenticationRequestResolver);
    }

    @Override
    public void logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) {
        try {
            if (authentication instanceof MidpointAuthentication) {
                ModuleAuthentication moduleAuth = ((MidpointAuthentication) authentication).getProcessingModuleAuthentication();
                if (moduleAuth instanceof Saml2ModuleAuthentication) {
                    registrationResolver.setModuleAuthentication((Saml2ModuleAuthentication) moduleAuth);
                }
            }
            logoutRequestFilter.doFilter(httpServletRequest, httpServletResponse, new FilterChain() {
                @Override
                public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
                }
            });
        } catch (ServletException | IOException e) {
            throw new Saml2Exception(e);
        }
    }
}
