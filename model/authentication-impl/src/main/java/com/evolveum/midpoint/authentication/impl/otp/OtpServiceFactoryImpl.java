/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.otp;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.authentication.api.OtpService;
import com.evolveum.midpoint.authentication.api.OtpServiceFactory;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtpAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TOtpAuthenticationModuleType;

@Service
public class OtpServiceFactoryImpl implements OtpServiceFactory {

    private final Clock clock;

    public OtpServiceFactoryImpl(Clock clock) {
        this.clock = clock;
    }

    public OtpService create(@NotNull ModuleAuthentication moduleAuthentication) {
        if (!(moduleAuthentication instanceof OtpModuleAuthentication otpModule)) {
            throw new IllegalArgumentException("Authentication module not supported");
        }

        return create(otpModule.getModule());
    }

    @Override
    public OtpService create(@NotNull OtpAuthenticationModuleType config) {
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
