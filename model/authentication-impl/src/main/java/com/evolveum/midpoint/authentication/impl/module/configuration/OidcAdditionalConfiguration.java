/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.configuration;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.nimbusds.jose.util.Base64URL;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * @author skublik
 */

public class OidcAdditionalConfiguration implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(OidcAdditionalConfiguration.class);

    private final String singingAlg;
    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;
    private final Base64URL thumbprint;
    private final Base64URL thumbprint256;
    private final boolean usePKCE;

    private OidcAdditionalConfiguration(
            String singingAlg, RSAPublicKey publicKey, RSAPrivateKey privateKey, String thumbprint, String thumbprint256, boolean usePKCE) {
        this.singingAlg = singingAlg;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.thumbprint = thumbprint != null ? createBase64(thumbprint) : null;
        this.thumbprint256 = thumbprint256 != null ? createBase64(thumbprint256) : null;
        this.usePKCE = usePKCE;
    }

    public String getSingingAlg() {
        return singingAlg;
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public Base64URL getThumbprint() {
        return thumbprint;
    }

    public Base64URL getThumbprint256() {
        return thumbprint256;
    }

    public boolean isUsePKCE() {
        return usePKCE;
    }

    private Base64URL createBase64(@NotNull String thumbprint) {
        try {
            return Base64URL.encode(Hex.decodeHex(thumbprint.toUpperCase()));
        } catch (DecoderException e) {
            LOGGER.error("Couldn't decode thumbprint " + thumbprint, e);
        }
        return null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String singingAlg;
        private RSAPublicKey publicKey;
        private RSAPrivateKey privateKey;

        private String thumbprint;
        private String thumbprint256;

        private boolean usePKCE = false;

        private Builder() {
        }

        public Builder singingAlg(String singingAlg) {
            this.singingAlg = singingAlg;
            return this;
        }

        public Builder publicKey(RSAPublicKey publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        public Builder privateKey(RSAPrivateKey privateKey) {
            this.privateKey = privateKey;
            return this;
        }

        public Builder thumbprint(String thumbprint) {
            this.thumbprint = thumbprint;
            return this;
        }

        public Builder thumbprint256(String thumbprint256) {
            this.thumbprint256 = thumbprint256;
            return this;
        }

        public Builder usePKCE(Boolean usePKCE) {
            if (usePKCE != null) {
                this.usePKCE = usePKCE;
            }
            return this;
        }

        public OidcAdditionalConfiguration build(){
            return new OidcAdditionalConfiguration(
                    this.singingAlg, this.publicKey, this.privateKey, this.thumbprint, this.thumbprint256, this.usePKCE);
        }
    }
}
