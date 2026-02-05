/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.otp;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class OtpAuthenticationToken extends UsernamePasswordAuthenticationToken {

    public OtpAuthenticationToken(Object principal, Object credentials) {
        super(principal, credentials);
    }

    // todo implement
}
