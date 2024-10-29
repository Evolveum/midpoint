/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.configuration;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.module.authentication.RemoteModuleAuthenticationImpl;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.nimbusds.jose.util.Base64URL;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.common.util.Base64Exception;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCSException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.*;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
import java.util.Objects;

import static com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil.getBasePath;

/**
 * @author skublik
 */

public class OidcClientModuleWebSecurityConfiguration extends RemoteModuleWebSecurityConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(OidcClientModuleWebSecurityConfiguration.class);

    private InMemoryClientRegistrationRepository clientRegistrationRepository;
    private final Map<String, OidcAdditionalConfiguration> additionalConfiguration = new HashMap<>();

    private OidcClientModuleWebSecurityConfiguration() {
    }

    public static OidcClientModuleWebSecurityConfiguration build(OidcAuthenticationModuleType modelType, String prefixOfSequence,
                                                           String publicHttpUrlPattern, ServletRequest request) {
        OidcClientModuleWebSecurityConfiguration configuration = buildInternal(modelType, prefixOfSequence, publicHttpUrlPattern, request);
        configuration.validate();
        return configuration;
    }

    private static OidcClientModuleWebSecurityConfiguration buildInternal(OidcAuthenticationModuleType oidcModule, String prefixOfSequence,
                                                                    String publicHttpUrlPattern, ServletRequest request) {
        OidcClientModuleWebSecurityConfiguration configuration = new OidcClientModuleWebSecurityConfiguration();
        build(configuration, oidcModule, prefixOfSequence);

        List<OidcClientAuthenticationModuleType> clients = oidcModule.getClient();
        List<ClientRegistration> registrations = new ArrayList<>();
        clients.forEach(client -> {

            OidcOpenIdProviderType openIdProvider = client.getOpenIdProvider();
            Assert.notNull(openIdProvider, "openIdProvider cannot be null");
            ClientRegistration.Builder builder = null;
            try {
                builder = ClientRegistrations.fromOidcIssuerLocation(openIdProvider.getIssuerUri());
            } catch (Exception e) {
                LOGGER.debug("Couldn't create oidc client builder by issuer uri: " + openIdProvider.getIssuerUri(), e);
            }

            Assert.hasText(client.getRegistrationId(), "registrationId cannot be empty");
            if (builder == null) {
                builder = ClientRegistration.withRegistrationId(client.getRegistrationId());
            } else {
                builder.registrationId(client.getRegistrationId());
            }
            builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
            builder.userInfoAuthenticationMethod(AuthenticationMethod.HEADER);
            UriComponentsBuilder redirectUri = UriComponentsBuilder.fromUriString(
                    StringUtils.isNotBlank(publicHttpUrlPattern) ? publicHttpUrlPattern : getBasePath((HttpServletRequest) request));
            redirectUri.pathSegment(
                    DEFAULT_PREFIX_OF_MODULE,
                    AuthUtil.stripSlashes(prefixOfSequence),
                    AuthUtil.stripSlashes(getAuthenticationModuleIdentifier(oidcModule)),
                    AuthUtil.stripSlashes(RemoteModuleAuthenticationImpl.AUTHENTICATION_REQUEST_PROCESSING_URL_SUFFIX),
                    client.getRegistrationId());
            builder.redirectUri(redirectUri.toUriString());

            Assert.hasText(client.getClientId(), "clientId cannot be empty");
            builder.clientId(client.getClientId());

            if (client.getNameOfUsernameAttribute() != null) {
                builder.userNameAttributeName(client.getNameOfUsernameAttribute());
            }

            if (!Objects.isNull(client.getClientSecret())) {
                try {
                    String clientSecret = protector.decryptString(client.getClientSecret());
                    builder.clientSecret(clientSecret);
                } catch (EncryptionException e) {
                    LOGGER.error("Couldn't obtain clear string for client secret", e);
                }
            }

            getOptionalIfNotEmpty(client.getClientName()).ifPresent(builder::clientName);
            getOptionalIfNotEmpty(openIdProvider.getAuthorizationUri()).ifPresent(builder::authorizationUri);
            getOptionalIfNotEmpty(openIdProvider.getTokenUri()).ifPresent(builder::tokenUri);
            getOptionalIfNotEmpty(openIdProvider.getUserInfoUri()).ifPresent(builder::userInfoUri);
            getOptionalIfNotEmpty(openIdProvider.getIssuerUri()).ifPresent(builder::issuerUri);
            getOptionalIfNotEmpty(openIdProvider.getJwkSetUri()).ifPresent(builder::jwkSetUri);
            ClientRegistration clientRegistration = builder.build();

            List<String> scopes = new ArrayList<>();
            if (clientRegistration.getScopes() != null) {
                scopes.addAll(clientRegistration.getScopes());
            }
            scopes.addAll(client.getScope());
            if (!scopes.contains("openid")) {
                scopes.add("openid");
            }
            builder.scope(scopes);

            if (StringUtils.isNotEmpty(openIdProvider.getEndSessionUri())) {
                Map<String, Object> configurationMetadata = new HashMap<>(clientRegistration.getProviderDetails().getConfigurationMetadata());
                configurationMetadata.remove("end_session_endpoint");
                configurationMetadata.put("end_session_endpoint", openIdProvider.getEndSessionUri());
                builder.providerConfigurationMetadata(configurationMetadata);
            }

            if (client.getClientAuthenticationMethod() != null) {
                builder.clientAuthenticationMethod(
                        new ClientAuthenticationMethod(client.getClientAuthenticationMethod().name().toLowerCase()));
            }

            clientRegistration = builder.build();

            registrations.add(clientRegistration);

            OidcAdditionalConfiguration.Builder additionalConfBuilder = OidcAdditionalConfiguration.builder()
                    .singingAlg(client.getClientSigningAlgorithm())
                    .usePKCE(client.getUsePkce());
            if (client.getSimpleProofKey() != null) {
                initializeProofKey(client.getSimpleProofKey(), additionalConfBuilder);
            } else if (client.getKeyStoreProofKey() != null) {
                initializeProofKey(client.getKeyStoreProofKey(), additionalConfBuilder);
            }

            configuration.additionalConfiguration.put(client.getRegistrationId(), additionalConfBuilder.build());
        });

        configuration.clientRegistrationRepository = new InMemoryClientRegistrationRepository(registrations);
        return configuration;
    }

    private static Optional<String> getOptionalIfNotEmpty(String value) {
        return Optional.ofNullable(value).filter((s) -> !s.isEmpty());
    }

    public InMemoryClientRegistrationRepository getClientRegistrationRepository() {
        return clientRegistrationRepository;
    }

    public Map<String, OidcAdditionalConfiguration> getAdditionalConfiguration() {
        return additionalConfiguration;
    }

    @Override
    protected void validate() {
        super.validate();
        if (getClientRegistrationRepository() == null) {
            throw new IllegalArgumentException("Oidc configuration is null");
        }
    }

    public String getPrefixOfSequence() {
        return DEFAULT_PREFIX_OF_MODULE_WITH_SLASH + "/" + AuthUtil.stripSlashes(getSequenceSuffix());
    }

    private static void initializeProofKey(AbstractSimpleKeyType key, OidcAdditionalConfiguration.Builder builder) {
        if (key == null) {
            return;
        }
        PrivateKey pkey;
        try {
            pkey = getPrivateKey(key, protector);
        } catch (IOException | OperatorCreationException | PKCSException | EncryptionException e) {
            throw new OAuth2AuthenticationException(new OAuth2Error("missing_key"), "Unable get key from " + key, e);
        }
        if (!(pkey instanceof RSAPrivateKey)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("missing_key"), "Unable get key from " + key);
        }

        PublicKey publicKey;
        try {
            Certificate certificate = getCertificate(key, protector);
            publicKey = certificate.getPublicKey();
            builder.thumbprint256(DigestUtils.sha256Hex(certificate.getEncoded()))
                    .thumbprint(DigestUtils.sha1Hex(certificate.getEncoded()));
        } catch (Base64Exception | EncryptionException | CertificateException e) {
            throw new OAuth2AuthenticationException(new OAuth2Error("missing_key"), "Unable get certificate from " + key, e);
        }

        builder.privateKey((RSAPrivateKey) pkey);
        builder.publicKey((RSAPublicKey) publicKey);
    }

    private static void initializeProofKey(AbstractKeyStoreKeyType key, OidcAdditionalConfiguration.Builder builder) {
        if (key == null) {
            return;
        }
        PrivateKey pkey;
        try {
            pkey = getPrivateKey(key, protector);
        } catch (KeyStoreException | IOException | EncryptionException | CertificateException |
                NoSuchAlgorithmException | UnrecoverableKeyException e) {
            LOGGER.error("Unable get key from " + key, e);
            throw new OAuth2AuthenticationException(new OAuth2Error("missing_key"), "Unable get key from " + key, e);
        }
        if (!(pkey instanceof RSAPrivateKey)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("missing_key"), "Alias " + key.getKeyAlias() + " don't return key of RSAPrivateKey type.");
        }

        PublicKey publicKey;
        try {
            Certificate certificate = getCertificate(key, protector);
            publicKey = certificate.getPublicKey();
            builder.thumbprint256(DigestUtils.sha256Hex(certificate.getEncoded()))
                    .thumbprint(DigestUtils.sha1Hex(certificate.getEncoded()));
        } catch (EncryptionException | CertificateException  | KeyStoreException | IOException | NoSuchAlgorithmException e) {
            throw new OAuth2AuthenticationException(new OAuth2Error("missing_key"), "Unable get certificate from " + key, e);
        }
        if (!(publicKey instanceof RSAPublicKey)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("missing_key"), "Alias " + key.getKeyAlias() + " don't return public key of RSAPublicKey type.");
        }
        builder.privateKey((RSAPrivateKey) pkey);
        builder.publicKey((RSAPublicKey) publicKey);
    }
}
