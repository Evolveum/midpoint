/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.authentication.token;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class AttributeVerificationToken extends UsernamePasswordAuthenticationToken {

    public AttributeVerificationToken(Object principal, Object credentials) {
        super(principal, credentials);
    }
}
