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

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.commons.codec.binary.Base32;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.OtpService;
import com.evolveum.midpoint.common.Clock;

public abstract class OtpServiceImpl implements OtpService {

    public static final String DEFAULT_ISSUER = "MidPoint";

    public static final OtpAlgorithm DEFAULT_ALGORITHM = OtpAlgorithm.SHA1;

    public static final int DEFAULT_DIGITS = 6;

    public static final int DEFAULT_SKEW = 1;

    private static final String AUTH_URL_TEMPLATE = "otpauth://%s/%s:%s?%s";

    private static final SecureRandom RANDOM = new SecureRandom();

    protected final OtpType type;

    protected final Clock clock;

    protected final String issuer;

    protected final OtpAlgorithm algorithm;

    protected final int digits;

    protected final int secretLength;

    protected final int window;

    public OtpServiceImpl(
            @NotNull OtpType type,
            @NotNull Clock clock,
            String issuer,
            OtpAlgorithm algorithm,
            Integer secretLength,
            Integer digits,
            Integer window) {

        this.type = type;
        this.clock = clock;
        this.issuer = issuer != null ? issuer : DEFAULT_ISSUER;
        this.algorithm = algorithm != null ? algorithm : DEFAULT_ALGORITHM;
        this.secretLength = secretLength != null && secretLength > 0 ? secretLength : this.algorithm.secretLength;
        this.digits = digits != null && digits > 0 ? digits : DEFAULT_DIGITS;
        this.window = window != null && window >= 1 ? window : DEFAULT_SKEW;
    }

    public @NotNull OtpType getType() {
        return type;
    }

    @Override
    public String generateAuthUrl(String account, String secret) {
        var acct = urlEncode(account);
        var iss = urlEncode(issuer);

        Map<String, String> params = new HashMap<>();
        params.put("issuer", urlEncode(issuer));
        params.put("algorithm", algorithm.value);
        params.put("digits", Integer.toString(digits));
        params.put("secret", secret);

        updateAuthUrlParameters(params);

        var paramsEncoded = params.entrySet().stream()
                .filter(e -> e.getKey() != null)
                .map(e -> e.getKey() + "=" + Objects.toString(e.getValue(), ""))
                .collect(Collectors.joining("&"));

        return String.format(AUTH_URL_TEMPLATE, type.authUrlName, iss, acct, paramsEncoded);
    }

    protected void updateAuthUrlParameters(Map<String, String> params) {
        // intentionally empty, subclasses may override to add more parameters
    }

    private String urlEncode(String value) {
        return value != null ? URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20") : null;
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
