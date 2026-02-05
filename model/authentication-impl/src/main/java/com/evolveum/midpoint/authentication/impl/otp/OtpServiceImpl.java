/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.otp;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OtpAuthenticationModuleType;

import org.apache.commons.codec.binary.Base32;
import org.jetbrains.annotations.NotNull;

public abstract class OtpServiceImpl<T extends OtpAuthenticationModuleType> implements OtpService<T> {

    private static final String AUTH_URL_TEMPLATE = "otpauth://%s/%s:%s?%s";

    public static final OtpAlgorithm DEFAULT_ALGORITHM = OtpAlgorithm.SHA1;

    public static final int DEFAULT_DIGITS = 6;

    private static final SecureRandom RANDOM = new SecureRandom();

    protected final String issuer;

    protected final OtpAlgorithm algorithm;

    protected final int digits;

    protected final int secretLength;

    public OtpServiceImpl(String issuer) {
        this(issuer, DEFAULT_ALGORITHM, DEFAULT_ALGORITHM.secretLength, DEFAULT_DIGITS);
    }

    public OtpServiceImpl(String issuer, OtpAlgorithm algorithm, int secretLength, int digits) {
        this.issuer = issuer;
        this.algorithm = algorithm != null ? algorithm : DEFAULT_ALGORITHM;
        this.secretLength = secretLength > 0 ? secretLength : this.algorithm.secretLength;
        this.digits = digits > 0 ? digits : DEFAULT_DIGITS;
    }

    protected abstract @NotNull OtpType getServiceType();

    @Override
    public String generateAuthUrl(String account) {
        var acct = urlEncode(account);
        var iss = urlEncode(issuer);

        var params = createAuthUrlParameters();
        var paramsEncoded = params.entrySet().stream()
                .filter(e -> e.getKey() != null)
                .map(e -> e.getKey() + "=" + Objects.toString(e.getValue(), ""))
                .collect(Collectors.joining("&"));

        return String.format(AUTH_URL_TEMPLATE, getServiceType().authUrlName, iss, acct, paramsEncoded);
    }

    protected Map<String, String> createAuthUrlParameters() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("issuer", urlEncode(issuer));
        parameters.put("algorithm", algorithm.value);
        parameters.put("digits", Integer.toString(digits));
        parameters.put("secret", generateSecret());

        return parameters;
    }

    private String urlEncode(String value) {
        return value != null ? URLEncoder.encode(value, StandardCharsets.UTF_8) : null;
    }

    @Override
    public String generateSecret() {
        byte[] bytes = new byte[secretLength];
        RANDOM.nextBytes(bytes);
        return new Base32().encodeToString(bytes);
    }

    @Override
    public boolean verifyCode(String secret, int code) {
        return code == generateCode(secret);
    }

    protected abstract int generateCode(String secret);
}
