/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeVerificationCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import java.util.List;
import java.util.Map;

public class AttributeVerificationAuthenticationContext  extends AbstractAuthenticationContext{

    private Map<ItemPath, String> attributeValuesMap;
    private AttributeVerificationCredentialsPolicyType policy;

    public AttributeVerificationAuthenticationContext(String username, Class<? extends FocusType> principalType, Map<ItemPath, String> attributeValuesMap) {
        this(username, principalType, attributeValuesMap, null);
    }

    public AttributeVerificationAuthenticationContext(String username, Class<? extends FocusType> principalType, Map<ItemPath, String> attributeValuesMap, List<ObjectReferenceType> requireAssignment) {
        super(username, principalType, requireAssignment);
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
