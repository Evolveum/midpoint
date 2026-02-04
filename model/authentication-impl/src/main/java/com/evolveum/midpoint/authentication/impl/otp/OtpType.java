/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.otp;

public enum OtpType {

    TOTP("totp");

    public final String name;

    OtpType(String name) {
        this.name = name;
    }
}
