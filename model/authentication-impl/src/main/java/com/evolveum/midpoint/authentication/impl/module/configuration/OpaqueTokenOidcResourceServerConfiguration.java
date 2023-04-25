/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.configuration;

import com.evolveum.midpoint.authentication.api.RemoveUnusedSecurityFilterPublisher;
import com.evolveum.midpoint.authentication.impl.oidc.OpaqueTokenUserDetailsIntrospector;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractKeyStoreKeyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OidcAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OidcResourceServerAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OpaqueTokenOidcResourceServerType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.proc.JWSAlgorithmFamilyJWSKeySelector;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;

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

        builder.clientId("unknownClientId");

        if (StringUtils.isNotEmpty(opaqueTokenConfig.getUserInfoUri())) {
            builder.userInfoUri(opaqueTokenConfig.getUserInfoUri());
        }

        if (StringUtils.isNotEmpty(opaqueTokenConfig.getNameOfUsernameClaim())) {
            builder.userNameAttributeName(opaqueTokenConfig.getNameOfUsernameClaim());
        }

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
