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

package com.evolveum.midpoint.prism.query;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.util.exception.SchemaException;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AndFilter extends NaryLogicalFilter {
	
	public static final QName ELEMENT_NAME = new QName(PrismConstants.NS_QUERY, "and");

	public AndFilter(List<ObjectFilter> condition) {
		super(condition);
	}
	
	public static AndFilter createAnd(ObjectFilter... conditions){
		List<ObjectFilter> filters = new ArrayList<>(conditions.length);
		Collections.addAll(filters, conditions);
		return new AndFilter(filters);
	}
	
	public static AndFilter createAnd(List<ObjectFilter> conditions){
		return new AndFilter(conditions);
	}
	
	@SuppressWarnings("CloneDoesntCallSuperClone")
	@Override
	public AndFilter clone() {
		return new AndFilter(getClonedConditions());
	}
	
	@Override
	public AndFilter cloneEmpty() {
		return new AndFilter(new ArrayList<>());
	}
	
	@Override
	protected String getDebugDumpOperationName() {
		return "AND";
	}
	
	@Override
	public boolean match(PrismContainerValue value, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
		for (ObjectFilter filter : getConditions()){
			if (!filter.match(value, matchingRuleRegistry)){
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean equals(Object obj, boolean exact) {
		return super.equals(obj, exact) && obj instanceof AndFilter;
	}
}
