/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.otp;

public interface OtpService {

    String generateSecret();

    String generateAuthUrl(String account, String secret);

    boolean verifyCode(String secret, int code);
}
