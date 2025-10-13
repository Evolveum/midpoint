/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.module.authentication.token;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.security.api.MidPointPrincipal;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Map;

public class AttributeVerificationToken extends AbstractAuthenticationToken {

    private MidPointPrincipal principal;
    private Map<ItemPath, String> attributeValues;


    public AttributeVerificationToken(MidPointPrincipal principal, Map<ItemPath, String> attributeValues) {
        super(null);
        this.principal = principal;
        this.attributeValues = attributeValues;
    }

    @Override
    public Map<ItemPath, String> getCredentials() {
        return attributeValues;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

}
