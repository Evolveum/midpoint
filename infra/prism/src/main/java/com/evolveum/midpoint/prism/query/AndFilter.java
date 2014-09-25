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

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class AndFilter extends NaryLogicalFilter {
	
	public static final QName ELEMENT_NAME = new QName(PrismConstants.NS_QUERY, "and");

	public AndFilter(List<ObjectFilter> condition) {
		super(condition);

	}
	
	public static AndFilter createAnd(ObjectFilter... conditions){
		List<ObjectFilter> filters = new ArrayList<ObjectFilter>();
		for (ObjectFilter condition : conditions){
			filters.add(condition);
		}
		
		return new AndFilter(filters);
	}
	
	public static AndFilter createAnd(List<ObjectFilter> conditions){
		return new AndFilter(conditions);
	}
	
	@Override
	public AndFilter clone() {
		return new AndFilter(getClonedConditions());
	}
	
	@Override
	public String debugDump() {
		return debugDump(0);
	}
	
	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("AND:");
		for (ObjectFilter filter : getConditions()){
			sb.append("\n");
			sb.append(filter.debugDump(indent + 1));
		}

		return sb.toString();

	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("AND");
		sb.append("(");
		for (int i = 0; i < getConditions().size(); i++){
			sb.append(getConditions().get(i));
			if (i != getConditions().size() -1){
				sb.append(",");
			}
		}
		sb.append(")");
		return sb.toString();
	}


	@Override
	public <T extends Objectable> boolean match(PrismObject<T> object, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException{
		for (ObjectFilter filter : getConditions()){
			if (!filter.match(object, matchingRuleRegistry)){
				return false;
			}
		}
		return true;
	}

}
