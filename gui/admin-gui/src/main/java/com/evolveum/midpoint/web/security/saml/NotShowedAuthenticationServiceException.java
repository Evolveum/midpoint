/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security.saml;

import org.springframework.security.authentication.AuthenticationServiceException;

public class NotShowedAuthenticationServiceException extends AuthenticationServiceException {
    public NotShowedAuthenticationServiceException(String msg) {
        super(msg);
    }
}
