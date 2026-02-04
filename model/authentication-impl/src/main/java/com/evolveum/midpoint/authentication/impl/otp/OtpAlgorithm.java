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

    OtpAlgorithm(String algorithm, String value, int secretLength) {
        this.algorithm = algorithm;
        this.value = value;
        this.secretLength = secretLength;
    }
}
