/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.configuration;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;

/**
 * @author skublik
 */

public class JwtOidcResourceServerConfiguration extends RemoteModuleWebSecurityConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(JwtOidcResourceServerConfiguration.class);

    private JwtDecoder decoder;

    private JwtOidcResourceServerConfiguration() {
    }

    public JwtDecoder getDecoder() {
        return decoder;
    }

    public static JwtOidcResourceServerConfiguration build(OidcAuthenticationModuleType modelType, String prefixOfSequence) {
        JwtOidcResourceServerConfiguration configuration = buildInternal(modelType, prefixOfSequence);
        configuration.validate();
        return configuration;
    }

    private static JwtOidcResourceServerConfiguration buildInternal(OidcAuthenticationModuleType modelType, String prefixOfSequence) {
        JwtOidcResourceServerConfiguration configuration = new JwtOidcResourceServerConfiguration();
        build(configuration, modelType, prefixOfSequence);

        OidcResourceServerAuthenticationModuleType resourceServer = modelType.getResourceServer();
        String trustedAlgorithm = getAttribute(resourceServer, JwtOidcResourceServerType.F_TRUSTED_ALGORITHM);

        AbstractKeyStoreKeyType keyStoreTrustingAsymmetricKey = getAttribute(
                resourceServer, JwtOidcResourceServerType.F_KEY_STORE_TRUSTING_ASYMMETRIC_KEY);
        ProtectedStringType trustingAsymmetricCertificate = getAttribute(
                resourceServer, JwtOidcResourceServerType.F_TRUSTING_ASYMMETRIC_CERTIFICATE);

        ProtectedStringType singleSymmetricKey = getAttribute(
                resourceServer, JwtOidcResourceServerType.F_SINGLE_SYMMETRIC_KEY);

        String jwkSetUri = getAttribute(resourceServer, JwtOidcResourceServerType.F_JWK_SET_URI);

        String issuerUri = getAttribute(resourceServer, JwtOidcResourceServerType.F_ISSUER_URI);

        if (trustingAsymmetricCertificate != null || keyStoreTrustingAsymmetricKey != null) {
            NimbusJwtDecoder.PublicKeyJwtDecoderBuilder builder;
            if (keyStoreTrustingAsymmetricKey != null) {
                builder = initializePublicKeyDecoderFromKeyStore(keyStoreTrustingAsymmetricKey);
            } else {
                builder = initializePublicKeyDecoderFromCertificate(trustingAsymmetricCertificate);
            }

            if (trustedAlgorithm != null) {
                builder.signatureAlgorithm(SignatureAlgorithm.from(trustedAlgorithm));
            }
            configuration.decoder = builder.build();
        } else if (singleSymmetricKey != null) {
            try {
                byte[] key;
                String clearValue = protector.decryptString(singleSymmetricKey);
                if (Base64.isBase64(clearValue)) {
                    boolean isBase64Url = clearValue.contains("-") || clearValue.contains("_");
                    key = Base64Utility.decode(clearValue, isBase64Url);
                } else {
                    key = protector.decryptString(singleSymmetricKey).getBytes();
                }
                String algorithm = MacAlgorithm.HS256.getName();
                if (trustedAlgorithm != null) {
                    algorithm = trustedAlgorithm;
                }
                NimbusJwtDecoder.SecretKeyJwtDecoderBuilder builder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(key, algorithm));
                builder.macAlgorithm(MacAlgorithm.from(algorithm));
                configuration.decoder = builder.build();
            } catch (EncryptionException e) {
                throw new OAuth2AuthenticationException(new OAuth2Error("missing_key"), "Unable get single symmetric key", e);
            } catch (Base64Exception e) {
                e.printStackTrace();
            }
        } else if (jwkSetUri != null){
            if (trustedAlgorithm != null) {
                configuration.decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                        .jwsAlgorithm(SignatureAlgorithm.from(trustedAlgorithm))
                        .build();
            } else {
                try {
                    JWSKeySelector<SecurityContext> jwsKeySelector =
                            JWSAlgorithmFamilyJWSKeySelector.fromJWKSetURL(new URL(jwkSetUri));
                    DefaultJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
                    jwtProcessor.setJWSKeySelector(jwsKeySelector);

                    configuration.decoder = new NimbusJwtDecoder(jwtProcessor);
                } catch (KeySourceException | MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        } else if (issuerUri != null) {
            configuration.decoder = JwtDecoders.fromIssuerLocation(issuerUri);
        }
        return configuration;
    }

    private static <T extends Object> T getAttribute(
            OidcResourceServerAuthenticationModuleType resourceServer, ItemName itemName) {
        Containerable parent = resourceServer;
        if (resourceServer.getJwt() != null) {
            parent = resourceServer.getJwt();
        }
        String methodName = "get" + StringUtils.capitalize(itemName.getLocalPart());
        try {
            Method method = parent.getClass().getMethod(methodName);
            return (T) method.invoke(parent);
        } catch (NoSuchMethodException e) {
            LOGGER.debug("Couldn't find method " + methodName + " in class " + parent.getClass());
        } catch (InvocationTargetException | IllegalAccessException e) {
            LOGGER.debug("Couldn't invoke method " + methodName + " on object " + parent);
        }
        return null;
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
