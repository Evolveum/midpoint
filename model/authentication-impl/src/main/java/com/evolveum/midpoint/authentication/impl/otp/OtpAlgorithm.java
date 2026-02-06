/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.otp;

public enum OtpAlgorithm {

    SHA1("sha1", "HmacSHA1", 20),

    SHA256("sha256", "HmacSHA256", 32),

    SHA512("sha512", "HmacSHA512", 64);

    public final String value;

    public final String algorithm;

    public final int secretLength;

    OtpAlgorithm(String value, String algorithm, int secretLength) {
        this.value = value;
        this.algorithm = algorithm;
        this.secretLength = secretLength;
    }

    public static OtpAlgorithm fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (OtpAlgorithm alg : values()) {
            if (alg.value.equalsIgnoreCase(value)) {
                return alg;
            }
        }
        throw new IllegalArgumentException("Unsupported OTP algorithm: " + value);
    }
}
