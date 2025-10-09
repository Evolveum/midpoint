/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl;

import org.springframework.security.authentication.AuthenticationServiceException;

public class NotShowedAuthenticationServiceException extends AuthenticationServiceException {
    public NotShowedAuthenticationServiceException(String msg) {
        super(msg);
    }
}
