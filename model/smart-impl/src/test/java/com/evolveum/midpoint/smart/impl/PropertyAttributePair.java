/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl;

import static com.evolveum.midpoint.smart.impl.DescriptiveItemPath.asStringSimple;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.test.AttrName;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

class PropertyAttributePair {

    private final String simplePropertyPath;
    private final String simpleAttributePath;

    private PropertyAttributePair(String simplePropertyPath, String simpleAttributePath) {
        this.simplePropertyPath = simplePropertyPath;
        this.simpleAttributePath = simpleAttributePath;
    }

    static PropertyAttributePair of(ItemName property, AttrName attribute) {
        return new PropertyAttributePair(asStringSimple(ItemPath.create(property)),
                asStringSimple(attribute.path()));
    }

    boolean matchesMappingRequest(SiSuggestMappingRequestType request) {
        final String midPointAttr = request.getMidPointAttribute().get(0).getName();
        final String applicationAttr = request.getApplicationAttribute().get(0).getName();
        return midPointAttr.equals(this.simplePropertyPath) && applicationAttr.equals(this.simpleAttributePath);
    }

    SiAttributeMatchSuggestionType toAttributeMatchResponse() {
        return new SiAttributeMatchSuggestionType()
                .applicationAttribute(this.simpleAttributePath)
                .midPointAttribute(this.simplePropertyPath);
    }

}
