/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.module.authentication.token;

import java.util.Collection;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * @author skublik
 */

public class MailNonceAuthenticationToken extends UsernamePasswordAuthenticationToken {

    public MailNonceAuthenticationToken(Object principal, Object credentials) {
        super(principal, credentials);
    }

    public MailNonceAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
    }
}
