/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.match;

import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;

/**
 * Creates MatchingRuleRegistry populated with standard matching rules.
 *
 * @author Radovan Semancik
 *
 */
public class MatchingRuleRegistryFactory {

	public static MatchingRuleRegistry createRegistry() {

		MatchingRuleRegistryImpl registry = new MatchingRuleRegistryImpl();
		registry.registerMatchingRule(new StringIgnoreCaseMatchingRule());
		registry.registerMatchingRule(new PolyStringStrictMatchingRule());
		registry.registerMatchingRule(new PolyStringOrigMatchingRule());
		registry.registerMatchingRule(new PolyStringNormMatchingRule());
		registry.registerMatchingRule(new ExchangeEmailAddressesMatchingRule());
		registry.registerMatchingRule(new DistinguishedNameMatchingRule());
		registry.registerMatchingRule(new XmlMatchingRule());
		registry.registerMatchingRule(new UuidMatchingRule());
		registry.registerMatchingRule(new DefaultMatchingRule<>());

		return registry;
	}

}
