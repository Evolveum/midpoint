/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.api;

import org.springframework.security.core.AuthenticationException;

public class AutheticationFailedData {
    private String failureMessage;
    private String username;

    private AuthenticationException authenticationException;

    public AutheticationFailedData(String failureMessage, String username) {
        this.failureMessage = failureMessage;
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setAuthenticationException(AuthenticationException authenticationException) {
        this.authenticationException = authenticationException;
    }

    public AuthenticationException getAuthenticationException() {
        return authenticationException;
    }
}
