/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.api.evaluator.context;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import java.util.List;

/**
 * @author skublik
 */

public class PreAuthenticationContext extends AbstractAuthenticationContext {

    public PreAuthenticationContext(
            String username, Class<? extends FocusType> principalType) {
        this(username, principalType, null, null);
    }

    public PreAuthenticationContext(
            String username, Class<? extends FocusType> principalType,
            List<ObjectReferenceType> requireAssignment,
            AuthenticationChannel channel) {
        super(username, principalType, requireAssignment, channel);
    }

    @Override
    public Object getEnteredCredential() {
        throw new UnsupportedOperationException();
    }
}
