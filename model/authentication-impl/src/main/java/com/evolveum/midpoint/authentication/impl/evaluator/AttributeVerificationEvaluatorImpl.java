/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.evaluator;

import com.evolveum.midpoint.model.api.context.AttributeVerificationAuthenticationContext;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component("attributeVerificationEvaluator")
public class AttributeVerificationEvaluatorImpl extends AuthenticationEvaluatorImpl<AbstractCredentialType, AttributeVerificationAuthenticationContext> {

    @Override
    protected void checkEnteredCredentials(ConnectionEnvironment connEnv,
            AttributeVerificationAuthenticationContext authCtx) {

    }

    @Override
    protected boolean supportsAuthzCheck() {
        return true;
    }

    @Override
    protected SecurityQuestionsCredentialsType getCredential(CredentialsType credentials) {
        return credentials.getSecurityQuestions();
    }

    @Override
    protected void validateCredentialNotNull(ConnectionEnvironment connEnv,
            @NotNull MidPointPrincipal principal, AbstractCredentialType credential) {


    }

    @Override
    protected boolean passwordMatches(
            ConnectionEnvironment connEnv, @NotNull MidPointPrincipal principal,
            AbstractCredentialType passwordType, AttributeVerificationAuthenticationContext authCtx) {
        return true;
    }

    @Override
    protected CredentialPolicyType getEffectiveCredentialPolicy(
            SecurityPolicyType securityPolicy, AttributeVerificationAuthenticationContext authnCtx) {

        return null;
    }

    @Override
    protected boolean supportsActivation() {
        return true;
    }

}
