/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.match;

import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

/**
 * @author semancik
 *
 */
public class MatchingRuleRegistryImpl implements MatchingRuleRegistry {

	@NotNull private final MatchingRule<?> defaultMatchingRule;
	@NotNull private final Map<QName, ? extends MatchingRule<?>> matchingRules = new HashMap<>();

	MatchingRuleRegistryImpl() {
		this.defaultMatchingRule = new DefaultMatchingRule<>();
	}

	// if typeQName is null, we skip the rule-type correspondence test
	@Override
	@NotNull
	public <T> MatchingRule<T> getMatchingRule(QName ruleName, QName typeQName) throws SchemaException {
		if (ruleName == null) {
			return (MatchingRule<T>) defaultMatchingRule;
		}
		MatchingRule<T> matchingRule = (MatchingRule<T>) matchingRules.get(ruleName);
		if (matchingRule == null) {
			//try match according to the localPart
			if (QNameUtil.matchAny(ruleName, matchingRules.keySet())){
				ruleName = QNameUtil.resolveNs(ruleName, matchingRules.keySet());
				matchingRule = (MatchingRule<T>) matchingRules.get(ruleName);
			}
			if (matchingRule == null) {
				throw new SchemaException("Unknown matching rule for name " + ruleName);
			}
		}
		if (typeQName != null && !matchingRule.isSupported(typeQName)) {
			throw new SchemaException("Matching rule "+ruleName+" does not support type "+typeQName);
		}
		return matchingRule;
	}

	<T> void registerMatchingRule(MatchingRule<T> rule) {
		((Map)this.matchingRules).put(rule.getName(), rule);
	}

}
