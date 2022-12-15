/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.configuration;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.proc.JWSAlgorithmFamilyJWSKeySelector;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.apache.commons.codec.binary.Base64;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;

/**
 * @author skublik
 */

public class OidcResourceServerModuleWebSecurityConfiguration extends RemoteModuleWebSecurityConfiguration {

    private JwtDecoder decoder;

    private OidcResourceServerModuleWebSecurityConfiguration() {
    }

    public JwtDecoder getDecoder() {
        return decoder;
    }

    public static OidcResourceServerModuleWebSecurityConfiguration build(OidcAuthenticationModuleType modelType, String prefixOfSequence) {
        OidcResourceServerModuleWebSecurityConfiguration configuration = buildInternal(modelType, prefixOfSequence);
        configuration.validate();
        return configuration;
    }

    private static OidcResourceServerModuleWebSecurityConfiguration buildInternal(OidcAuthenticationModuleType modelType, String prefixOfSequence) {
        OidcResourceServerModuleWebSecurityConfiguration configuration = new OidcResourceServerModuleWebSecurityConfiguration();
        build(configuration, modelType, prefixOfSequence);

        OidcResourceServerAuthenticationModuleType resourceServer = modelType.getResourceServer();
        if (resourceServer.getTrustingAsymmetricCertificate() != null || resourceServer.getKeyStoreTrustingAsymmetricKey() != null) {
            NimbusJwtDecoder.PublicKeyJwtDecoderBuilder builder;
            if (resourceServer.getKeyStoreTrustingAsymmetricKey() != null) {
                builder = initializePublicKeyDecoderFromKeyStore(resourceServer.getKeyStoreTrustingAsymmetricKey());
            } else {
                builder = initializePublicKeyDecoderFromCertificate(resourceServer.getTrustingAsymmetricCertificate());
            }
            if (resourceServer.getTrustedAlgorithm() != null) {
                builder.signatureAlgorithm(SignatureAlgorithm.from(resourceServer.getTrustedAlgorithm()));
            }
            configuration.decoder = builder.build();
        } else if (resourceServer.getSingleSymmetricKey() != null) {
            try {
                byte[] key;
                String clearValue = protector.decryptString(resourceServer.getSingleSymmetricKey());
                if (Base64.isBase64(clearValue)) {
                    boolean isBase64Url = clearValue.contains("-") || clearValue.contains("_");
                    key = Base64Utility.decode(clearValue, isBase64Url);
                } else {
                    key = protector.decryptString(resourceServer.getSingleSymmetricKey()).getBytes();
                }
                String algorithm = MacAlgorithm.HS256.getName();
                if (resourceServer.getTrustedAlgorithm() != null) {
                    algorithm = resourceServer.getTrustedAlgorithm();
                }
                NimbusJwtDecoder.SecretKeyJwtDecoderBuilder builder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(key, algorithm));
                builder.macAlgorithm(MacAlgorithm.from(algorithm));
                configuration.decoder = builder.build();
            } catch (EncryptionException e) {
                throw new OAuth2AuthenticationException(new OAuth2Error("missing_key"), "Unable get single symmetric key", e);
            } catch (Base64Exception e) {
                e.printStackTrace();
            }
        } else if (resourceServer.getJwkSetUri() != null){
            if (resourceServer.getTrustedAlgorithm() != null) {
                configuration.decoder = NimbusJwtDecoder.withJwkSetUri(resourceServer.getJwkSetUri())
                        .jwsAlgorithm(SignatureAlgorithm.from(resourceServer.getTrustedAlgorithm()))
                        .build();
            } else {
                try {
                    JWSKeySelector<SecurityContext> jwsKeySelector =
                            JWSAlgorithmFamilyJWSKeySelector.fromJWKSetURL(new URL(resourceServer.getJwkSetUri()));
                    DefaultJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
                    jwtProcessor.setJWSKeySelector(jwsKeySelector);

                    configuration.decoder = new NimbusJwtDecoder(jwtProcessor);
                } catch (KeySourceException | MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        } else if (resourceServer.getIssuerUri() != null) {
            configuration.decoder = JwtDecoders.fromIssuerLocation(resourceServer.getIssuerUri());
        }
        return configuration;
    }

    @Override
    protected void validate() {
        super.validate();
        if (getDecoder() == null) {
            throw new IllegalArgumentException("Jwt decoder is null, please define public key, "
                    + "client secret, JWS uri or issuer uri in configuration of OIDC authentication module");
        }
    }

    private static NimbusJwtDecoder.PublicKeyJwtDecoderBuilder initializePublicKeyDecoderFromCertificate(ProtectedStringType certificateType) {
        if (certificateType == null) {
            return null;
        }
        PublicKey publicKey;
        try {
            Certificate certificate = getCertificate(certificateType, protector);
            publicKey = certificate.getPublicKey();
        } catch (Base64Exception | EncryptionException | CertificateException e) {
            throw new OAuth2AuthenticationException(new OAuth2Error("missing_key"), "Unable get certificate", e);
        }

        return NimbusJwtDecoder.withPublicKey((RSAPublicKey) publicKey);
    }

    private static NimbusJwtDecoder.PublicKeyJwtDecoderBuilder initializePublicKeyDecoderFromKeyStore(AbstractKeyStoreKeyType key) {
        if (key == null) {
            return null;
        }
        PublicKey publicKey;
        try {
            Certificate certificate = getCertificate(key, protector);
            publicKey = certificate.getPublicKey();
        } catch (EncryptionException | CertificateException  | KeyStoreException | IOException | NoSuchAlgorithmException e) {
            throw new OAuth2AuthenticationException(new OAuth2Error("missing_key"), "Unable get certificate from " + key, e);
        }
        if (!(publicKey instanceof RSAPublicKey)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("missing_key"), "Alias " + key.getKeyAlias() + " don't return public key of RSAPublicKey type.");
        }
        return NimbusJwtDecoder.withPublicKey((RSAPublicKey) publicKey);
    }
}
