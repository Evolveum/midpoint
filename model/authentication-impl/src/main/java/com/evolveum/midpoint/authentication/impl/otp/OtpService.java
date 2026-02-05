/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.otp;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OtpAuthenticationModuleType;

public interface OtpService<T extends OtpAuthenticationModuleType> {

    String generateSecret();

    String generateAuthUrl(String account);

    boolean verifyCode(String secret, int code);
}
