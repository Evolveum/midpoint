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
import org.springframework.security.saml.SamlAuthentication;
import org.springframework.security.saml.SamlException;
import org.springframework.security.saml.SamlTemplateEngine;
import org.springframework.security.saml.provider.SamlFilter;
import org.springframework.security.saml.provider.provisioning.SamlProviderProvisioning;
import org.springframework.security.saml.provider.service.ServiceProviderService;
import org.springframework.security.saml.provider.service.authentication.ServiceProviderLogoutHandler;
import org.springframework.security.saml.saml2.authentication.LogoutRequest;
import org.springframework.security.saml.saml2.authentication.NameIdPrincipal;
import org.springframework.security.saml.saml2.metadata.Binding;
import org.springframework.security.saml.saml2.metadata.IdentityProviderMetadata;
import org.springframework.security.saml.saml2.metadata.ServiceProviderMetadata;
import org.springframework.security.saml.spi.opensaml.OpenSamlVelocityEngine;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.MediaType.TEXT_HTML_VALUE;
import static org.springframework.util.StringUtils.hasText;

/**
 * @author skublik
 */
public class MidpointServiceProviderLogoutHandler extends ServiceProviderLogoutHandler {

    private static final String POST_TEMPLATE = "/templates/saml2-post-binding.vm";

    private SamlTemplateEngine samlTemplateEngine = new OpenSamlVelocityEngine();
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
            if (moduleAuthentication.getAuthentication() instanceof SamlAuthentication) {
                SamlAuthentication sa = (SamlAuthentication) moduleAuthentication.getAuthentication();
                ServiceProviderService provider = provisioning.getHostedProvider();
                IdentityProviderMetadata idp = provider.getRemoteProvider(sa.getAssertingEntityId());
                LogoutRequest lr = provider.logoutRequest(idp, (NameIdPrincipal) sa.getSamlPrincipal());
                if (lr.getDestination().getBinding().equals(Binding.REDIRECT)) {
                    super.spInitiatedLogout(request, response, sa);
                } else if (lr.getDestination().getBinding().equals(Binding.POST)) {
                    processPostLogout(request, response, lr, idp, provider);
                } else {
                    String message = "Unsupported binding for logout " + lr.getDestination().getBinding();
                    throw new IllegalArgumentException(message);
                }
            }
        } else {
            String message = "Unsupported type " + (authentication == null ? null : authentication.getClass().getName())
                    + " of authentication for MidpointLogoutRedirectFilter, supported is only MidpointAuthentication";
            throw new IllegalArgumentException(message);
        }
    }

    private void processPostLogout(HttpServletRequest request, HttpServletResponse response,
            LogoutRequest logoutRequest, IdentityProviderMetadata idp, ServiceProviderService provider) {
        String encoded = provider.toEncodedXml(logoutRequest, false);
        Map<String, Object> model = new HashMap<>();
        model.put("action", logoutRequest.getDestination().getLocation());
        model.put("SAMLRequest", encoded);
        String relayState = getLogoutRelayState(request, idp);
        if (hasText(relayState)) {
            model.put("RelayState", HtmlUtils.htmlEscape(relayState));
        }

        response.setContentType(TEXT_HTML_VALUE);
        response.setCharacterEncoding(UTF_8.name());
        StringWriter out = new StringWriter();
        samlTemplateEngine.process(
                request,
                POST_TEMPLATE,
                model,
                out
        );
        try {
            response.getWriter().write(out.toString());
        } catch (IOException e) {
            throw new SamlException(e);
        }
    }
}
