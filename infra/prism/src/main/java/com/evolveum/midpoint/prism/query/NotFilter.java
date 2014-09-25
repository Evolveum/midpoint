/*
 * Copyright (c) 2010-2014 Evolveum
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

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;


public class NotFilter extends UnaryLogicalFilter {

//	private ObjectFilter filter;

	public NotFilter() {

	}

	public NotFilter(ObjectFilter filter) {
		setFilter(filter);
	}

	public static NotFilter createNot(ObjectFilter filter) {
		return new NotFilter(filter);
	}
	
	@Override
	public NotFilter clone() {
		return new NotFilter(getFilter().clone());
	}
	
	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("NOT:");
		if (getFilter() != null) {
			sb.append("\n");
			sb.append(getFilter().debugDump(indent + 1));
		}

		return sb.toString();

	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("NOT(");
		if (getFilter() != null){
			sb.append(getFilter().toString());
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	public <T extends Objectable> boolean match(PrismObject<T> object, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException{
		return !getFilter().match(object, matchingRuleRegistry);
		
	}
}
