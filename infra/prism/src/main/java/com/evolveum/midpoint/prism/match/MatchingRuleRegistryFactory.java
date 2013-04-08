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
		
		return registry;
	}

}
