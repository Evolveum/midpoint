/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.prism.match;

/**
 * Creates MatchingRuleRegistry populated with standard matching rules.
 * 
 * @author Radovan Semancik
 *
 */
public class MatchingRuleRegistryFactory {
	
	public static MatchingRuleRegistry createRegistry() {
		
		MatchingRuleRegistry registry = new MatchingRuleRegistry();
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
