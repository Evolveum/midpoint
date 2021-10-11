/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class ResourceObjectPattern implements Serializable {
    private static final long serialVersionUID = 1L;

    private Collection<ResourceAttribute<?>> identifiers;
    private RefinedObjectClassDefinition rOcDef;
    private ObjectFilter objectFilter;

    public ResourceObjectPattern(RefinedObjectClassDefinition rOcDef) {
        this.rOcDef = rOcDef;
    }

    public static boolean matches(PrismObject<ShadowType> shadowToMatch,
            Collection<ResourceObjectPattern> protectedAccountPatterns, MatchingRuleRegistry matchingRuleRegistry,
            RelationRegistry relationRegistry) throws SchemaException {
        for (ResourceObjectPattern pattern: protectedAccountPatterns) {
            if (pattern.matches(shadowToMatch, matchingRuleRegistry, relationRegistry)) {
                return true;
            }
        }
        return false;
    }

    public boolean matches(PrismObject<ShadowType> shadowToMatch, MatchingRuleRegistry matchingRuleRegistry, RelationRegistry relationRegistry) throws SchemaException {
        if (objectFilter != null) {
            ObjectTypeUtil.normalizeFilter(objectFilter, relationRegistry);    // we suppose references in shadowToMatch are normalized (on return from repo)
            return ObjectQuery.match(shadowToMatch, objectFilter, matchingRuleRegistry);
        } else {
            // Deprecated method
            ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(shadowToMatch);
            if (attributesContainer == null) {
                return false;
            }
            Collection<ResourceAttribute<?>> attributesToMatch = attributesContainer.getAttributes();
            for (ResourceAttribute<?> identifier: identifiers) {
                if (!matches(identifier, attributesToMatch, matchingRuleRegistry)) {
                    return false;
                }
            }
            return true;
        }
    }

    private boolean matches(ResourceAttribute<?> identifier, Collection<? extends ResourceAttribute<?>> attributesToMatch, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
        for (ResourceAttribute<?> attributeToMatch: attributesToMatch) {
            if (matches(identifier, attributeToMatch, matchingRuleRegistry)) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(ResourceAttribute<?> identifier, ResourceAttribute<?> attributeToMatch, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
        if (!identifier.getElementName().equals(attributeToMatch.getElementName())) {
            return false;
        }
        RefinedAttributeDefinition rAttrDef = rOcDef.findAttributeDefinition(identifier.getElementName());
        QName matchingRuleQName = rAttrDef.getMatchingRuleQName();
        if (matchingRuleQName == null || matchingRuleRegistry == null) {
            return identifier.equals(attributeToMatch, EquivalenceStrategy.REAL_VALUE);
        }
        MatchingRule<Object> matchingRule = matchingRuleRegistry.getMatchingRule(matchingRuleQName, rAttrDef.getTypeName());
        return matchingRule.match(identifier.getRealValue(), attributeToMatch.getRealValue());
    }

    public ObjectFilter getObjectFilter() {
        return objectFilter;
    }

    public void addFilter(ObjectFilter filter) {
        this.objectFilter = filter;
    }

}
