/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.authentication.token;

import com.evolveum.midpoint.prism.path.ItemPath;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Map;

public class FocusVerificationToken extends AbstractAuthenticationToken {

    private Map<ItemPath, String> attributes;

    public FocusVerificationToken(Map<ItemPath, String> attributes) {
        super(null);
        this.attributes = attributes;
    }

    @Override
    public Map<ItemPath, String> getCredentials() {
        return attributes;
    }

    @Override
    public String getPrincipal() {
        if (attributes != null && attributes.size() == 1) {
            return attributes.values().stream().findFirst().orElse(null);
        }
        return null;
    }


    @Override
    public Map<ItemPath, String> getDetails() {
        return attributes;
    }
}
