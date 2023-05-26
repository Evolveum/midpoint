/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.saml;

import jakarta.servlet.http.HttpServletRequest;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;

import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;

import com.evolveum.midpoint.authentication.impl.module.authentication.Saml2ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.provider.Saml2Provider;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.opensaml.saml.saml2.core.LogoutRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.Saml2Exception;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.security.saml2.provider.service.authentication.logout.Saml2LogoutRequest;
import org.springframework.security.saml2.provider.service.web.authentication.logout.OpenSaml4LogoutRequestResolver;
import org.springframework.security.saml2.provider.service.web.authentication.logout.Saml2LogoutRequestResolver;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

public class MidpointSaml2LogoutRequestResolver implements Saml2LogoutRequestResolver {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointSaml2LogoutRequestResolver.class);

    private final OpenSaml4LogoutRequestResolver resolver;

    public MidpointSaml2LogoutRequestResolver(OpenSaml4LogoutRequestResolver resolver) {
        this.resolver = resolver;
        this.resolver.setParametersConsumer(this::resolveParameters);
    }

    private void resolveParameters(OpenSaml4LogoutRequestResolver.LogoutRequestParameters logoutRequestParameters) {
        if (logoutRequestParameters.getLogoutRequest() == null) {
            return;
        }
        if (logoutRequestParameters.getAuthentication() != null
                && logoutRequestParameters.getAuthentication().getPrincipal() instanceof Saml2Provider.MidpointSaml2AuthenticatedPrincipal) {
            LogoutRequest logoutRequest = logoutRequestParameters.getLogoutRequest();
            Saml2Provider.MidpointSaml2AuthenticatedPrincipal principal =
                    (Saml2Provider.MidpointSaml2AuthenticatedPrincipal) logoutRequestParameters.getAuthentication().getPrincipal();
            logoutRequest.getNameID().setSPNameQualifier(principal.getSpNameQualifier());
            logoutRequest.getNameID().setFormat(principal.getNameIdFormat());
        }
    }

    @Override
    public Saml2LogoutRequest resolve(HttpServletRequest httpServletRequest, Authentication authentication) {
        try {
            Saml2AuthenticationToken token = null;
            if (authentication instanceof MidpointAuthentication) {
                ModuleAuthentication authModule = ((MidpointAuthentication) authentication).getProcessingModuleAuthentication();
                if (authModule instanceof Saml2ModuleAuthenticationImpl) {
                    if (authModule.getAuthentication() instanceof Saml2AuthenticationToken) {
                        token = (Saml2AuthenticationToken) authModule.getAuthentication();
                    } else if ((authModule.getAuthentication() instanceof PreAuthenticatedAuthenticationToken
                            || authModule.getAuthentication() instanceof AnonymousAuthenticationToken)
                            && authModule.getAuthentication().getDetails() instanceof Saml2AuthenticationToken) {
                        token = (Saml2AuthenticationToken) authModule.getAuthentication().getDetails();
                    }
                }
            } else if (authentication instanceof AnonymousAuthenticationToken
                    && authentication.getDetails() instanceof Saml2AuthenticationToken) {
                token = (Saml2AuthenticationToken) authentication.getDetails();
            }
            if (token != null) {
                AuthenticatedPrincipal principal = token.getDetails() instanceof AuthenticatedPrincipal ?
                        (AuthenticatedPrincipal) token.getDetails() : null;
                if (!(principal instanceof Saml2AuthenticatedPrincipal)) {
                    String name = token.getRelyingPartyRegistration().getEntityId();
                    String relyingPartyRegistrationId = token.getRelyingPartyRegistration().getRegistrationId();
                    principal = new Saml2AuthenticatedPrincipal() {
                        @Override
                        public String getName() {
                            return name;
                        }

                        @Override
                        public String getRelyingPartyRegistrationId() {
                            return relyingPartyRegistrationId;
                        }
                    };
                }
                return resolver.resolve(httpServletRequest, new Saml2Authentication(principal, token.getSaml2Response(), null));
            }
            return resolver.resolve(httpServletRequest, authentication);
        } catch (Saml2Exception e) {
            LOGGER.debug("Couldn't create logout request.", e);
        }
        return null;
    }
}
