/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.module.authentication.token;

import java.util.Map;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import com.evolveum.midpoint.prism.path.ItemPath;

public class ArchetypeSelectionAuthenticationToken extends AbstractAuthenticationToken {

    private Map<ItemPath, String> attributes;

    public ArchetypeSelectionAuthenticationToken(Map<ItemPath, String> attributes) {
        super(null);
        this.attributes = attributes;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return null;
    }
}
