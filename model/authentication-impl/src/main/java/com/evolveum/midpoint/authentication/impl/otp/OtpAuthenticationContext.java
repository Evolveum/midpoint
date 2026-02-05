/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.otp;

import java.util.List;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.evaluator.context.AbstractAuthenticationContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtpCredentialsPolicyType;

public class OtpAuthenticationContext extends AbstractAuthenticationContext {

    private final Integer code;

    private OtpCredentialsPolicyType policy;

    public OtpAuthenticationContext(
            String username,
            Class<? extends FocusType> principalType,
            Integer code,
            List<ObjectReferenceType> requireAssignment,
            AuthenticationChannel channel) {

        super(username, principalType, requireAssignment, channel);

        this.code = code;
    }

    @Override
    public Integer getEnteredCredential() {
        return getCode();
    }

    public Integer getCode() {
        return code;
    }

    public OtpCredentialsPolicyType getPolicy() {
        return policy;
    }

    public void setPolicy(OtpCredentialsPolicyType policy) {
        this.policy = policy;
    }
}
