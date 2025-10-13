/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.api.evaluator.context;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeVerificationCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import java.util.List;
import java.util.Map;

public class AttributeVerificationAuthenticationContext  extends AbstractAuthenticationContext {

    private Map<ItemPath, String> attributeValuesMap;
    private AttributeVerificationCredentialsPolicyType policy;


    public AttributeVerificationAuthenticationContext(String username,
            Class<? extends FocusType> principalType,
            Map<ItemPath, String> attributeValuesMap,
            List<ObjectReferenceType> requireAssignment,
            AuthenticationChannel channel) {
        super(username, principalType, requireAssignment, channel);
        this.attributeValuesMap = attributeValuesMap;
    }


    public Map<ItemPath, String> getAttributeValuesMap() {
        return attributeValuesMap;
    }

    @Override
    public Object getEnteredCredential() {
        return getAttributeValuesMap();
    }

    public AttributeVerificationCredentialsPolicyType getPolicy() {
        return policy;
    }

    public void setPolicy(AttributeVerificationCredentialsPolicyType policy) {
        this.policy = policy;
    }
}
