/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
