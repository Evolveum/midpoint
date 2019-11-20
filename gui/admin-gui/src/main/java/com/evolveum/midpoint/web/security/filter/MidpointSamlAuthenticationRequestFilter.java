/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.filter;

import com.evolveum.midpoint.web.security.module.authentication.MidpointAuthentication;
import com.evolveum.midpoint.web.security.module.authentication.Saml2ModuleAuthentication;
import com.evolveum.midpoint.web.security.util.RequestState;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml.provider.provisioning.SamlProviderProvisioning;
import org.springframework.security.saml.provider.service.SamlAuthenticationRequestFilter;
import org.springframework.security.saml.provider.service.ServiceProviderService;
import org.springframework.security.saml.saml2.authentication.AuthenticationRequest;
import org.springframework.security.saml.saml2.metadata.Endpoint;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author skublik
 */

public class MidpointSamlAuthenticationRequestFilter extends SamlAuthenticationRequestFilter {
    public MidpointSamlAuthenticationRequestFilter(SamlProviderProvisioning<ServiceProviderService> provisioning) {
        super(provisioning);
    }

    @Override
    protected void sendAuthenticationRequest(ServiceProviderService provider, HttpServletRequest request, HttpServletResponse response, AuthenticationRequest authenticationRequest, Endpoint location) throws IOException {
        super.sendAuthenticationRequest(provider, request, response, authenticationRequest, location);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            Saml2ModuleAuthentication moduleAuthentication = (Saml2ModuleAuthentication) mpAuthentication.getProcessingModuleAuthentication();
            moduleAuthentication.setRequestState(RequestState.SENDED);
        }
    }
}
