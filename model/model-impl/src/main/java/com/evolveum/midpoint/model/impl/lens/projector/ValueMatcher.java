/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.model.impl.lens.projector;

import java.util.Collection;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class ValueMatcher<T> {
	
	MatchingRule<T> matchingRule;

	public ValueMatcher(MatchingRule<T> matchingRule) {
		this.matchingRule = matchingRule;
	}

	public static <T> ValueMatcher<T> createMatcher(RefinedAttributeDefinition rAttrDef, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
		QName matchingRuleQName = rAttrDef.getMatchingRuleQName();
		MatchingRule<Object> matchingRule = matchingRuleRegistry.getMatchingRule(matchingRuleQName, rAttrDef.getTypeName());
		return new ValueMatcher<T>((MatchingRule<T>) matchingRule);
	}
	
	public boolean match(T realA, T realB) {
		return matchingRule.match(realA, realB);
	}
	
	public boolean matches(T realValue, String regex){
		return matchingRule.matchRegex(realValue, regex);
	}
	
	public boolean hasRealValue(PrismProperty<T> property, PrismPropertyValue<T> pValue) {
		for (T existingRealValue: property.getRealValues()) {
			if (matchingRule.match(existingRealValue, pValue.getValue())) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isRealValueToAdd(PropertyDelta<T> delta, PrismPropertyValue<T> pValue) {
		if (delta.getValuesToAdd() == null){
			return false;
		}
		for (PrismPropertyValue<T> existingPValue: delta.getValuesToAdd()) {
			if (matchingRule.match(existingPValue.getValue(), pValue.getValue())) {
				return true;
			}
		}
		return false;
	} 
}
