/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.api;

import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OtpAuthenticationModuleType;

/**
 * Factory for creating {@link OtpService} instances from OTP authentication module configuration.
 */
public interface OtpServiceFactory {

    /** Creates an {@link OtpService} from a raw module configuration. */
    OtpService create(@NotNull OtpAuthenticationModuleType config);

    /** Creates an {@link OtpService} from an active module authentication context. */
    OtpService create(@NotNull ModuleAuthentication moduleAuthentication);
}
