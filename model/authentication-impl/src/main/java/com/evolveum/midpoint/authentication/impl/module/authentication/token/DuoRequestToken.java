/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
