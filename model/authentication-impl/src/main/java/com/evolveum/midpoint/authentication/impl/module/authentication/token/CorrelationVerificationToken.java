/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.authentication.token;

import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Map;

public class CorrelationVerificationToken extends AbstractAuthenticationToken {

    private String correlatorName;
    private Map<ItemPath, String> attributes;

    public CorrelationVerificationToken(Map<ItemPath, String> attributes) {
        super(null);
        this.attributes = attributes;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Map<ItemPath, String> getPrincipal() {
        return null;
    }


    @Override
    public Map<ItemPath, String> getDetails() {
        return attributes;
    }

    public String getCorrelatorName() {
        return correlatorName;
    }

    public FocusType getPreFocus() {
        return new UserType();
    }
}
