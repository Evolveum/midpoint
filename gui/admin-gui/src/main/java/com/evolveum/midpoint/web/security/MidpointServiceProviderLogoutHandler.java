/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml.provider.provisioning.SamlProviderProvisioning;
import org.springframework.security.saml.provider.service.ServiceProviderService;
import org.springframework.security.saml.provider.service.authentication.ServiceProviderLogoutHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author skublik
 */
public class MidpointServiceProviderLogoutHandler extends ServiceProviderLogoutHandler {

    private SamlProviderProvisioning<ServiceProviderService> provisioning;

    public MidpointServiceProviderLogoutHandler(SamlProviderProvisioning<ServiceProviderService> provisioning) {
        super(provisioning);
        this.provisioning = provisioning;
    }

    public SamlProviderProvisioning<ServiceProviderService> getProvisioning() {
        return provisioning;
    }

    protected void spInitiatedLogout(HttpServletRequest request,
                                     HttpServletResponse response,
                                     Authentication authentication) throws IOException {

        if (authentication instanceof MidpointAuthentication) {
            ModuleAuthentication moduleAuthentication = ((MidpointAuthentication) authentication).getProcessingModuleAuthentication();
            super.spInitiatedLogout(request, response, moduleAuthentication.getAuthentication());
        } else {
            String message = "Unsupported type " + (authentication == null ? null : authentication.getClass().getName())
                    + " of authentication for MidpointLogoutRedirectFilter, supported is only MidpointAuthentication";
            throw new IllegalArgumentException(message);
        }

    }
}
