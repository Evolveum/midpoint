/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NonceCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import java.util.List;

public class NonceAuthenticationContext extends AbstractAuthenticationContext {

    private final String nonce;
    private final NonceCredentialsPolicyType policy;

    public NonceAuthenticationContext(
            String username, Class<? extends FocusType> principalType, String nonce, NonceCredentialsPolicyType policy) {
        this(username, principalType, nonce, policy, null);
    }

    public NonceAuthenticationContext(
            String username,
            Class<? extends FocusType> principalType,
            String nonce,
            NonceCredentialsPolicyType policy,
            List<ObjectReferenceType> requireAssignment) {
        super(username, principalType, requireAssignment);
        this.nonce = nonce;
        this.policy = policy;
    }

    public String getNonce() {
        return nonce;
    }

    public NonceCredentialsPolicyType getPolicy() {
        return policy;
    }

    @Override
    public Object getEnteredCredential() {
        return getNonce();
    }
}
