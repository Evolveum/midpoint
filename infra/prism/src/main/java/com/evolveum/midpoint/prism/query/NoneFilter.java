/*
 * Copyright (c) 2014-2016 Evolveum
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
package com.evolveum.midpoint.prism.query;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Filter designed to explicitly match nothing. It is used in some special cases, e.g.
 * a security component explicitly indicating that no object should be returned. 
 * 
 * @author Radovan Semancik
 */
public class NoneFilter extends ObjectFilter {

	public NoneFilter() {
		super();
	}

	public static NoneFilter createNone() {
		return new NoneFilter();
	}
	
	@Override
	public NoneFilter clone() {
		return new NoneFilter();
	}
	
	@Override
	public void checkConsistence(boolean requireDefinitions) {
		// nothing to do
	}
	
	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("NONE");
		return sb.toString();

	}
	
	@Override
	public String toString() {
		return "NONE";
	}

	@Override
	public boolean match(PrismContainerValue value, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
		return false;
	}

	@Override
	public boolean equals(Object obj, boolean exact) {
		return obj instanceof NoneFilter;
	}

	@Override
	public int hashCode() {
		return 0;
	}

}
