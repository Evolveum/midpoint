/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.filter.ldap;

import org.springframework.security.core.AuthenticationException;

public class AuditedAuthenticationException extends AuthenticationException {
    public AuditedAuthenticationException(AuthenticationException cause) {
        super(cause.getMessage(), cause);
    }

    @Override
    public synchronized AuthenticationException getCause() {
        return (AuthenticationException) super.getCause();
    }
}
