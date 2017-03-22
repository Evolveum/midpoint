/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
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
	
	private Collection<ResourceAttribute<?>> identifiers;
	private RefinedObjectClassDefinition rOcDef;
	private ObjectFilter objectFilter;
	
	public ResourceObjectPattern(RefinedObjectClassDefinition rOcDef) {
		this.rOcDef = rOcDef;
	}
	
	public Collection<ResourceAttribute<?>> getIdentifiers() {
		if (identifiers == null) {
			identifiers = new ArrayList<ResourceAttribute<?>>();
		}
		return identifiers;
	}
	
	public void addIdentifier(ResourceAttribute<?> identifier) {
		getIdentifiers().add(identifier);
	}

	public static boolean matches(PrismObject<ShadowType> shadowToMatch,
			Collection<ResourceObjectPattern> protectedAccountPatterns, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
		for (ResourceObjectPattern pattern: protectedAccountPatterns) {
			if (pattern.matches(shadowToMatch, matchingRuleRegistry)) {
				return true;
			}
		}
		return false;
	}

	public boolean matches(PrismObject<ShadowType> shadowToMatch, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
		if (objectFilter != null) {
			ObjectTypeUtil.normalizeFilter(objectFilter);	// we suppose references in shadowToMatch are normalized (on return from repo)
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
			return identifier.equalsRealValue(attributeToMatch);
		}
		MatchingRule<Object> matchingRule = matchingRuleRegistry.getMatchingRule(matchingRuleQName, rAttrDef.getTypeName());
		return matchingRule.match(identifier.getRealValue(), attributeToMatch.getRealValue());
	}

	public void addFilter(ObjectFilter filter) {
		this.objectFilter = filter;
	}

}
