/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.otp;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtpAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TOtpAuthenticationModuleType;

public class OtpServiceFactory {

    public static OtpServiceImpl create(Clock clock, OtpAuthenticationModuleType config) {
        String issuer = config.getIssuer();
        OtpAlgorithm algorithm = OtpAlgorithm.fromValue(config.getAlgorithm());
        Integer secretLength = config.getSecretLength();
        Integer digits = config.getDigits();
        Integer window = config.getWindow();

        if (config instanceof TOtpAuthenticationModuleType totp) {
            return new TOtpServiceImpl(
                    clock,
                    issuer,
                    algorithm,
                    secretLength,
                    digits,
                    window,
                    totp.getPeriod()
            );
        } else {
            throw new IllegalArgumentException("Unsupported OTP type: " + config.getClass());
        }
    }
}
