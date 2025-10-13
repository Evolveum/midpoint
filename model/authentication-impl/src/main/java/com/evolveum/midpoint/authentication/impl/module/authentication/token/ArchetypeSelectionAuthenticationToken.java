/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
