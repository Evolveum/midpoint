/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
