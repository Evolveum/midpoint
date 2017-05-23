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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class OrFilter extends NaryLogicalFilter {

	public OrFilter(List<ObjectFilter> condition) {
		super(condition);
	}

	public static OrFilter createOr(ObjectFilter... conditions){
		List<ObjectFilter> filters = new ArrayList<ObjectFilter>();
		Collections.addAll(filters, conditions);
		return new OrFilter(filters);
	}
	
	public static OrFilter createOr(List<ObjectFilter> conditions){	
		return new OrFilter(conditions);
	}
	
	@SuppressWarnings("CloneDoesntCallSuperClone")
	@Override
	public OrFilter clone() {
		return new OrFilter(getClonedConditions());
	}
	
	@Override
	public OrFilter cloneEmpty() {
		return new OrFilter(new ArrayList<ObjectFilter>());
	}

	@Override
	protected String getDebugDumpOperationName() {
		return "OR";
	}

	@Override
	public boolean match(PrismContainerValue value, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
		for (ObjectFilter filter : getConditions()){
			if (filter.match(value, matchingRuleRegistry)){
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean equals(Object obj, boolean exact) {
		return super.equals(obj, exact) && obj instanceof OrFilter;
	}

}
