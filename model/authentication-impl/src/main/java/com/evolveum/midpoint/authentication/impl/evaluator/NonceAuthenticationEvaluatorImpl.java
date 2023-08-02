/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.evaluator;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.authentication.api.NonceAuthenticationContext;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NonceCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NonceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;

@Component("nonceAuthenticationEvaluator")
public class NonceAuthenticationEvaluatorImpl extends AuthenticationEvaluatorImpl<NonceType, NonceAuthenticationContext> {


    @Override
    protected void checkEnteredCredentials(ConnectionEnvironment connEnv,
            NonceAuthenticationContext authCtx) {
        if (StringUtils.isBlank(authCtx.getNonce())) {
            recordAuthenticationFailure(authCtx.getUsername(), connEnv, "empty nonce provided");
            throw new BadCredentialsException("web.security.provider.nonce.bad");
        }
    }

    @Override
    protected boolean supportsAuthzCheck() {
        return false;
    }

    @Override
    protected NonceType getCredential(CredentialsType credentials) {
        return credentials.getNonce();
    }

    @Override
    protected void validateCredentialNotNull(ConnectionEnvironment connEnv,
            @NotNull MidPointPrincipal principal, NonceType credential) {
        if (credential.getValue() == null) {
            recordModuleAuthenticationFailure(principal.getUsername(), principal, connEnv, null, "no stored nonce value");
            throw new AuthenticationCredentialsNotFoundException("web.security.provider.nonce.bad");
        }
    }

    @Override
    protected boolean passwordMatches(
            ConnectionEnvironment connEnv, @NotNull MidPointPrincipal principal,
            NonceType passwordType, NonceAuthenticationContext authCtx) {
        return decryptAndMatch(connEnv, principal, passwordType.getValue(), authCtx.getNonce());
    }

    @Override
    protected CredentialPolicyType getEffectiveCredentialPolicy(SecurityPolicyType securityPolicy,
            NonceAuthenticationContext authnCtx) throws SchemaException {
        NonceCredentialsPolicyType policy = authnCtx.getPolicy();
        if (policy == null) {
            policy = SecurityUtil.getEffectiveNonceCredentialsPolicy(securityPolicy);
        }
        return policy;
    }



    @Override
    protected boolean supportsActivation() {
        return false;
    }
}
