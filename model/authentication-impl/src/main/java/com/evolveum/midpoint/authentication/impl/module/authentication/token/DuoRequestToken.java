/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.module.authentication.token;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Collections;

public class DuoRequestToken extends AbstractAuthenticationToken {

    private final String duoCode;
    private final String username;

    public DuoRequestToken(String duoCode, String username) {
        super(Collections.emptyList());
        this.duoCode = duoCode;
        this.username = username;
    }

    @Override
    public String getName() {
        return username;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return null;
    }

    public String getDuoCode() {
        return duoCode;
    }
}
