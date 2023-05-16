/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.configuration;

import com.nimbusds.jose.util.Base64URL;

import java.io.Serializable;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * @author skublik
 */

public class OidcAdditionalConfiguration implements Serializable {

    private final String singingAlg;
    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;
    private final Base64URL thumbprint;
    private final Base64URL thumbprint256;

    private OidcAdditionalConfiguration(
            String singingAlg, RSAPublicKey publicKey, RSAPrivateKey privateKey, String thumbprint, String thumbprint256) {
        this.singingAlg = singingAlg;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.thumbprint = thumbprint != null ? Base64URL.encode(thumbprint) : null;
        this.thumbprint256 = thumbprint256 != null ? Base64URL.encode(thumbprint256) : null;
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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String singingAlg;
        private RSAPublicKey publicKey;
        private RSAPrivateKey privateKey;

        private String thumbprint;
        private String thumbprint256;

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

        public OidcAdditionalConfiguration build(){
            return new OidcAdditionalConfiguration(
                    this.singingAlg, this.publicKey, this.privateKey, this.thumbprint, this.thumbprint256);
        }
    }
}
