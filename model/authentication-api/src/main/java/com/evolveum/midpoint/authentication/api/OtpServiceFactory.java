/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.api;

import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OtpAuthenticationModuleType;

public interface OtpServiceFactory {

    OtpService create(@NotNull OtpAuthenticationModuleType config);

    OtpService create(@NotNull ModuleAuthentication moduleAuthentication);
}
