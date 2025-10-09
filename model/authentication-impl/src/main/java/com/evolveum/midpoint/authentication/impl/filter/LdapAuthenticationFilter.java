/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.filter;

import com.evolveum.midpoint.authentication.impl.module.authentication.token.LdapAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * @author skublik
 */

public class LdapAuthenticationFilter extends MidpointUsernamePasswordAuthenticationFilter {

    protected UsernamePasswordAuthenticationToken createAuthenticationToken(String username, String password) {
        return new LdapAuthenticationToken(username, password);
    }
}
