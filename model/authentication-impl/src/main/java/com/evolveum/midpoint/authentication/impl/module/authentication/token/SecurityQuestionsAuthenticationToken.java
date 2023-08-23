/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.authentication.token;

import java.util.Collection;
import java.util.Map;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * @author skublik
 */

public class SecurityQuestionsAuthenticationToken extends UsernamePasswordAuthenticationToken {

    public SecurityQuestionsAuthenticationToken(Object principal, Object credentials) {
        super(principal, credentials);
    }

    @Override
    public Map<String, String> getCredentials() {
        return (Map<String, String>) super.getCredentials();
    }
}
