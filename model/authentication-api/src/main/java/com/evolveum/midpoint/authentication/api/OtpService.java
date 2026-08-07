/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.api;

import org.jetbrains.annotations.NotNull;

/**
 * Service for TOTP-based one-time password operations: secret generation, authenticator URL creation, and code verification.
 *
 * @author Viliam Repan
 */
public interface OtpService {

    /** Generates a new random TOTP shared secret. */
    String generateSecret();

    /**
     * Builds an {@code otpauth://} URL for registering the secret in an authenticator app.
     *
     * @param account the user account identifier shown in the authenticator app
     * @param secret  the shared secret returned by {@link #generateSecret()}
     */
    String generateAuthUrl(String account, @NotNull String secret);

    /**
     * Verifies a TOTP code against the shared secret.
     *
     * @param secret the shared secret
     * @param code   the code entered by the user
     * @return {@code true} if the code is valid for the current time window
     */
    boolean verifyCode(@NotNull String secret, int code);
}
