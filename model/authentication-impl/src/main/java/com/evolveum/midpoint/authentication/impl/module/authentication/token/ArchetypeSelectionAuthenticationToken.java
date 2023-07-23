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

    private String archetypeOid;
    private boolean allowUndefinedArchetype;

    public ArchetypeSelectionAuthenticationToken(String archetypeOid, boolean allowUndefinedArchetype) {
        super(null);
        this.archetypeOid = archetypeOid;
        this.allowUndefinedArchetype = allowUndefinedArchetype;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return null;
    }

    @Override
    public String getDetails() {
        return archetypeOid;
    }

    public String getArchetypeOid() {
        return archetypeOid;
    }

    public boolean isAllowUndefinedArchetype() {
        return allowUndefinedArchetype;
    }
}
