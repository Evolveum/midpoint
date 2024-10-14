/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.configuration;

import com.evolveum.midpoint.authentication.impl.filter.oidc.OpaqueTokenUserDetailsIntrospector;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OidcAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OidcResourceServerAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OpaqueTokenOidcResourceServerType;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;

/**
 * @author skublik
 */

public class OpaqueTokenOidcResourceServerConfiguration extends RemoteModuleWebSecurityConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(OpaqueTokenOidcResourceServerConfiguration.class);

    private OpaqueTokenIntrospector introspector;

    private OpaqueTokenOidcResourceServerConfiguration() {
    }

    public OpaqueTokenIntrospector getIntrospector() {
        return introspector;
    }

    public static OpaqueTokenOidcResourceServerConfiguration build(OidcAuthenticationModuleType modelType, String prefixOfSequence) {
        OpaqueTokenOidcResourceServerConfiguration configuration = buildInternal(modelType, prefixOfSequence);
        configuration.validate();
        return configuration;
    }

    private static OpaqueTokenOidcResourceServerConfiguration buildInternal(OidcAuthenticationModuleType modelType, String prefixOfSequence) {
        OpaqueTokenOidcResourceServerConfiguration configuration = new OpaqueTokenOidcResourceServerConfiguration();
        build(configuration, modelType, prefixOfSequence);

        OidcResourceServerAuthenticationModuleType resourceServer = modelType.getResourceServer();
        OpaqueTokenOidcResourceServerType opaqueTokenConfig = resourceServer.getOpaqueToken();

        ClientRegistration.Builder builder = null;
        try {
            builder = ClientRegistrations.fromOidcIssuerLocation(opaqueTokenConfig.getIssuerUri());
        } catch (Exception e) {
            LOGGER.debug("Couldn't create oidc client builder by issuer uri.");
        }

        if (builder == null) {
            builder = ClientRegistration.withRegistrationId("unknownRegistrationId");
        } else {
            builder.registrationId("unknownRegistrationId");
        }

        //hack, we need ClientRegistration, but it can be empty we use only user info uri
        builder.authorizationGrantType(AuthorizationGrantType.JWT_BEARER);

        if (StringUtils.isNotEmpty(opaqueTokenConfig.getUserInfoUri())) {
            builder.userInfoUri(opaqueTokenConfig.getUserInfoUri());
        }

        if (StringUtils.isNotEmpty(opaqueTokenConfig.getNameOfUsernameClaim())) {
            builder.userNameAttributeName(opaqueTokenConfig.getNameOfUsernameClaim());
        }

        builder.scope("openid");

        ClientRegistration clientRegistration = builder.build();
        configuration.introspector = new OpaqueTokenUserDetailsIntrospector(clientRegistration);
        return configuration;
    }

    @Override
    protected void validate() {
        super.validate();
        if (getIntrospector() == null) {
            throw new IllegalArgumentException("Opaque token introspector is null, please define "
                    + "user info uri or issuer uri in configuration of OIDC authentication module");
        }
    }
}
