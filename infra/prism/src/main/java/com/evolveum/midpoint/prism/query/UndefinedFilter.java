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
package com.evolveum.midpoint.prism.query;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class UndefinedFilter extends ObjectFilter {

	public UndefinedFilter() {
		super();
	}

	public static UndefinedFilter createUndefined() {
		return new UndefinedFilter();
	}
	
	@Override
	public UndefinedFilter clone() {
		return new UndefinedFilter();
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
		sb.append("UNDEFINED");
		return sb.toString();

	}
	
	@Override
	public String toString() {
		return "UNDEFINED";
	}

	@Override
	public boolean match(PrismContainerValue value, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
		return true;
	}

	@Override
	public boolean equals(Object obj, boolean exact) {
		return obj instanceof UndefinedFilter;
	}

	@Override
	public int hashCode() {
		return 0;
	}


}
