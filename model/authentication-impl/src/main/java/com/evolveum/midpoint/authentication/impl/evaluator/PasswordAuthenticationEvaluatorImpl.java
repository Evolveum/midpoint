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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.PasswordAuthenticationContext;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

@Component("passwordAuthenticationEvaluator")
public class PasswordAuthenticationEvaluatorImpl extends AuthenticationEvaluatorImpl<PasswordType, PasswordAuthenticationContext> {

    @Override
    protected void checkEnteredCredentials(ConnectionEnvironment connEnv, PasswordAuthenticationContext authCtx) {
        if (StringUtils.isBlank(authCtx.getUsername())) {
            recordAuthenticationFailure(authCtx.getUsername(), connEnv, "empty login provided");
//            recordAuthenticationBehavior(authCtx.getUsername(), null, connEnv, "empty login provided", authCtx.getPrincipalType(), false);
            throw new UsernameNotFoundException("web.security.provider.invalid.credentials");
        }
        if (StringUtils.isBlank(authCtx.getPassword())) {
            recordAuthenticationFailure(authCtx.getUsername(), connEnv, "empty password provided");
//            recordAuthenticationBehavior(authCtx.getUsername(), null, connEnv, "empty password provided", authCtx.getPrincipalType(), false);
            throw new BadCredentialsException("web.security.provider.invalid.credentials");
        }
    }

    @Override
    protected boolean supportsAuthzCheck() {
        return true;
    }

    @Override
    protected PasswordType getCredential(CredentialsType credentials) {
        return credentials.getPassword();
    }

    @Override
    protected void validateCredentialNotNull(ConnectionEnvironment connEnv,
            @NotNull MidPointPrincipal principal, PasswordType credential) {

        ProtectedStringType protectedString = credential.getValue();

        if (protectedString == null) {
            recordPasswordAuthenticationFailure(principal.getUsername(), principal, connEnv, null, "no stored password value");
//            recordAuthenticationBehavior(principal.getUsername(), principal, connEnv, "no stored password value", principal.getFocus().getClass(), false);
            throw new AuthenticationCredentialsNotFoundException("web.security.provider.password.bad");
        }

    }

    @Override
    protected boolean passwordMatches(
            ConnectionEnvironment connEnv, @NotNull MidPointPrincipal principal,
            PasswordType passwordType, PasswordAuthenticationContext authCtx) {
        return decryptAndMatch(connEnv, principal, passwordType.getValue(), authCtx.getPassword());
    }

    @Override
    protected CredentialPolicyType getEffectiveCredentialPolicy(SecurityPolicyType securityPolicy,
            PasswordAuthenticationContext authnCtx) {
        return SecurityUtil.getEffectivePasswordCredentialsPolicy(securityPolicy);
    }

    @Override
    protected boolean supportsActivation() {
        return true;
    }
}
