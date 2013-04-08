/**
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */
package com.evolveum.midpoint.model.lens.projector;

import java.util.Collection;

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
	
	public boolean hasRealValue(PrismProperty<T> property, PrismPropertyValue<T> pValue) {
		for (T existingRealValue: property.getRealValues()) {
			if (matchingRule.match(existingRealValue, pValue.getValue())) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isRealValueToAdd(PropertyDelta<T> delta, PrismPropertyValue<T> pValue) {
		for (PrismPropertyValue<T> existingPValue: delta.getValuesToAdd()) {
			if (matchingRule.match(existingPValue.getValue(), pValue.getValue())) {
				return true;
			}
		}
		return false;
	} 
}
