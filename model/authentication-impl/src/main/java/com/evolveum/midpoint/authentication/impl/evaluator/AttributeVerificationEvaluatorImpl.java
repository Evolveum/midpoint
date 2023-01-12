/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.evaluator;

import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.model.api.context.AttributeVerificationAuthenticationContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component("attributeVerificationEvaluator")
public class AttributeVerificationEvaluatorImpl extends AuthenticationEvaluatorImpl<AttributeVerificationCredentialsType, AttributeVerificationAuthenticationContext> {

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
        //todo hack
        return new AttributeVerificationCredentialsType();
//        return credentials.getAttributeVerification();
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
    protected CredentialPolicyType getEffectiveCredentialPolicy(
            SecurityPolicyType securityPolicy, AttributeVerificationAuthenticationContext authnCtx) {

        return null;
    }

    @Override
    protected boolean supportsActivation() {
        return true;
    }

}
