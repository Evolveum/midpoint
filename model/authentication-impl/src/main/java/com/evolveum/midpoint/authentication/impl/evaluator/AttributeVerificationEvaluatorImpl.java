/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.evaluator;

import com.evolveum.midpoint.authentication.api.evaluator.context.AttributeVerificationAuthenticationContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component("attributeVerificationEvaluator")
public class AttributeVerificationEvaluatorImpl extends CredentialsAuthenticationEvaluatorImpl<AttributeVerificationCredentialsType, AttributeVerificationAuthenticationContext> {

    @Override
    protected void checkEnteredCredentials(ConnectionEnvironment connEnv,
            AttributeVerificationAuthenticationContext authCtx) {

    }

    @Override
    protected boolean supportsAuthzCheck() {
        return true;
    }

    @Override
    protected AttributeVerificationCredentialsType getCredential(CredentialsType credentials) {
        if (credentials.getAttributeVerification() == null) {
            credentials.setAttributeVerification(new AttributeVerificationCredentialsType());
        }
        return credentials.getAttributeVerification();
    }

    @Override
    protected void validateCredentialNotNull(ConnectionEnvironment connEnv,
            @NotNull MidPointPrincipal principal, AttributeVerificationCredentialsType credential) {

    }

    @Override
    protected boolean passwordMatches(
            ConnectionEnvironment connEnv, @NotNull MidPointPrincipal principal,
            AttributeVerificationCredentialsType passwordType, AttributeVerificationAuthenticationContext authCtx) {
        Map<ItemPath, String> attrValuesMap = authCtx.getAttributeValuesMap();

        if (attrValuesMap == null || attrValuesMap.isEmpty()) {
            return false;
        }
        for (Map.Entry<ItemPath, String> entry : attrValuesMap.entrySet()) {
            if (!attributeValueMatches(principal.getFocus(), entry)) {
                return false;
            }
        }
        return true;
    }

    private boolean attributeValueMatches(FocusType principal, Map.Entry<ItemPath, String> entry) {
        PrismProperty<?> property = principal.asPrismObject().findProperty(entry.getKey());
        if (property == null) {
            return false;
        }
        return entry.getValue().equals(Objects.requireNonNullElse(property.getRealValue(), "").toString());
    }

    @Override
    protected AttributeVerificationCredentialsPolicyType getEffectiveCredentialPolicy(
            SecurityPolicyType securityPolicy, AttributeVerificationAuthenticationContext authnCtx) {
        AttributeVerificationCredentialsPolicyType policy = authnCtx.getPolicy();
        if (policy == null) {
            policy = SecurityUtil.getEffectiveAttributeVerificationCredentialsPolicy(securityPolicy);
        }
        authnCtx.setPolicy(policy);
        return policy;
    }

    @Override
    protected boolean supportsActivation() {
        return true;
    }

}
