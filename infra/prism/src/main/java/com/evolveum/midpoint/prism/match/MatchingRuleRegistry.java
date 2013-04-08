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
package com.evolveum.midpoint.prism.match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class MatchingRuleRegistry {
	
	public MatchingRule <? extends Object> defaultMatchingRule;
	public Map<QName, ? extends MatchingRule<?>> matchingRules = new HashMap<QName, MatchingRule<?>>();
	
	public MatchingRuleRegistry() {
		super();
		this.defaultMatchingRule = new DefaultMatchingRule<Object>();
	}
	
	public MatchingRuleRegistry(Collection<? extends MatchingRule<?>> matchingRules) {
		this();
		for (MatchingRule<?> matchingRule: matchingRules) {
			registerMatchingRule(matchingRule);
		}
	}

	public <T> MatchingRule<T> getMatchingRule(QName ruleName, QName typeQName) throws SchemaException {
		if (ruleName == null) {
			return (MatchingRule<T>) defaultMatchingRule;
		}
		MatchingRule<T> matchingRule = (MatchingRule<T>) matchingRules.get(ruleName);
		if (matchingRule == null) {
			throw new SchemaException("Unknown matching rule for name "+ruleName);
		}
		if (!matchingRule.isSupported(typeQName)) {
			throw new SchemaException("Matching rule "+ruleName+" does not support type "+typeQName);
		}
		return matchingRule;
	}
	
	public <T> void registerMatchingRule(MatchingRule<T> rule) {
		((Map)this.matchingRules).put(rule.getName(), rule);
	}

}
