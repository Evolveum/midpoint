/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.ldap;

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
