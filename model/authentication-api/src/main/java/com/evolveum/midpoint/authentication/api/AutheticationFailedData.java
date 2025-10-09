/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
