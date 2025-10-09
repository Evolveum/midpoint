/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.api.evaluator.context;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NonceCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import java.util.List;

public class NonceAuthenticationContext extends AbstractAuthenticationContext {

    private final String nonce;
    private NonceCredentialsPolicyType policy;


    public NonceAuthenticationContext(
            String username,
            Class<? extends FocusType> principalType,
            String nonce,
            List<ObjectReferenceType> requireAssignment,
            AuthenticationChannel channel) {
        super(username, principalType, requireAssignment, channel);
        this.nonce = nonce;
    }

    public String getNonce() {
        return nonce;
    }

    public NonceCredentialsPolicyType getPolicy() {
        return policy;
    }

    public void setPolicy(NonceCredentialsPolicyType policy) {
        this.policy = policy;
    }

    @Override
    public Object getEnteredCredential() {
        return getNonce();
    }
}
