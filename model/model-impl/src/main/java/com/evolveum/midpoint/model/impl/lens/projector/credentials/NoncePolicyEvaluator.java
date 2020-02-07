/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.credentials;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 *
 */
public class NoncePolicyEvaluator<F extends FocusType> extends CredentialPolicyEvaluator<NonceType, NonceCredentialsPolicyType, F> {

    private static final ItemPath NONCE_CONTAINER_PATH = UserType.F_CREDENTIALS.append(CredentialsType.F_NONCE);

    private NoncePolicyEvaluator(Builder<F> builder) {
        super(builder);
    }

    @Override
    public ItemPath getCredentialsContainerPath() {
        return NONCE_CONTAINER_PATH;
    }

    @Override
    protected String getCredentialHumanReadableName() {
        return "nonce";
    }

    @Override
    protected String getCredentialHumanReadableKey() {
        return "nonce";
    }

    @Override
    protected boolean supportsHistory() {
        return false;
    }

    @Override
    protected NonceCredentialsPolicyType determineEffectiveCredentialPolicy() throws SchemaException {
        return SecurityUtil.getEffectiveNonceCredentialsPolicy(getSecurityPolicy());
    }

    public static class Builder<F extends FocusType> extends CredentialPolicyEvaluator.Builder<F> {
        public NoncePolicyEvaluator<F> build() {
            return new NoncePolicyEvaluator<>(this);
        }
    }
}
