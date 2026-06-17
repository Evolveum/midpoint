/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.otp;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base32;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.util.exception.SystemException;

public class TOtpServiceImpl extends OtpServiceImpl {

    public static final int DEFAULT_PERIOD = 30;

    private final int period;

    public TOtpServiceImpl(
            @NotNull Clock clock,
            String issuer,
            OtpAlgorithm algorithm,
            Integer secretLength,
            Integer digits,
            Integer window,
            Integer period) {

        super(OtpType.TOTP, clock, issuer, algorithm, secretLength, digits, window);

        this.period = period != null && period > 0 ? period : DEFAULT_PERIOD;
    }

    @Override
    protected void updateAuthUrlParameters(Map<String, String> params) {
        params.put("period", String.valueOf(period));
    }

    protected int generateCode(String secret) {
        Instant instant = Instant.ofEpochMilli(clock.currentTimeMillis());

        long timeSlice = (instant.getEpochSecond() / period);
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(timeSlice);
        byte[] timeBytes = buffer.array();

        byte[] hmacResult = hmacSha(new Base32().decode(secret), timeBytes);

        int offset = hmacResult[hmacResult.length - 1] & 0x0F;
        byte[] codeBytes = Arrays.copyOfRange(hmacResult, offset, offset + 4);
        codeBytes[0] &= 0x7F;

        int code = ByteBuffer.wrap(codeBytes).getInt();
        return code % (int) Math.pow(10, digits);
    }

    private byte[] hmacSha(byte[] key, byte[] timeBytes) {
        try {
            Mac hmac = Mac.getInstance(algorithm.algorithm);
            SecretKeySpec signKey = new SecretKeySpec(key, algorithm.algorithm);
            hmac.init(signKey);
            return hmac.doFinal(timeBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new SystemException("Error generating " + algorithm.algorithm + ": " + e.getMessage(), e);
        }
    }
}
