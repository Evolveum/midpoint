/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.credentials;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 */
public class PasswordPolicyEvaluator<F extends FocusType> extends CredentialPolicyEvaluator<PasswordType, PasswordCredentialsPolicyType, F> {

    private static final ItemPath PASSWORD_CONTAINER_PATH = ItemPath.create(FocusType.F_CREDENTIALS, CredentialsType.F_PASSWORD);

    private PasswordPolicyEvaluator(Builder<F> builder) {
        super(builder);
    }

    @Override
    public ItemPath getCredentialsContainerPath() {
        return PASSWORD_CONTAINER_PATH;
    }

    @Override
    protected String getCredentialHumanReadableName() {
        return "password";
    }

    @Override
    protected String getCredentialHumanReadableKey() {
        return "password";
    }

    @Override
    protected boolean supportsHistory() {
        return true;
    }

    @Override
    protected PasswordCredentialsPolicyType determineEffectiveCredentialPolicy() {
        return SecurityUtil.getEffectivePasswordCredentialsPolicy(getSecurityPolicy());
    }

    public static class Builder<F extends FocusType> extends CredentialPolicyEvaluator.Builder<F> {
        public PasswordPolicyEvaluator<F> build() {
            return new PasswordPolicyEvaluator<>(this);
        }
    }
}
