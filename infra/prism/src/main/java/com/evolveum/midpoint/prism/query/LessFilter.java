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

import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class LessFilter<T> extends ComparativeFilter<T>{

	LessFilter(ItemPath parentPath, PrismPropertyDefinition definition, PrismPropertyValue<T> value, boolean equals) {
		super(parentPath, definition, value, equals);
	}
	
	public LessFilter() {
	}
		
	public static <T> LessFilter createLessFilter(ItemPath parentPath, PrismPropertyDefinition definition, PrismPropertyValue<T> value, boolean equals){
		return new LessFilter(parentPath, definition, value, equals);
	}
	
	public static <T> LessFilter createLessFilter(PrismPropertyDefinition definition, T realValue, boolean equals) throws SchemaException{
		return createLessFilter(new ItemPath(definition.getName()), definition, realValue, equals);
	}
	
	public static <T, O extends Objectable> LessFilter createLessFilter(ItemPath parentPath, PrismObjectDefinition<O> containerDef,
			PrismPropertyValue<T> value, boolean equals) throws SchemaException {
		
		return (LessFilter) createComparativeFilter(LessFilter.class, parentPath, containerDef, value, equals);
	}

	public static <T> LessFilter createLessFilter(ItemPath parentPath, PrismPropertyDefinition item, T realValue, boolean equals) throws SchemaException{
		return (LessFilter) createComparativeFilter(LessFilter.class, parentPath, item, realValue, equals);
	}

	public static <T, O extends Objectable> LessFilter createLessFilter(ItemPath parentPath, PrismObjectDefinition<O> containerDef,
			T realValue, boolean equals) throws SchemaException {
		return (LessFilter) createComparativeFilter(LessFilter.class, parentPath, containerDef, realValue, equals);
	}

	public static <T, O extends Objectable> LessFilter createLessFilter(Class<O> type, PrismContext prismContext, QName propertyName, T realValue, boolean equals)
			throws SchemaException {
		return (LessFilter) createComparativeFilter(LessFilter.class, type, prismContext, propertyName, realValue, equals);
	}
	
	@Override
	public LessFilter clone() {
		return new LessFilter(getFullPath(), getDefinition(), (PrismPropertyValue<T>) getValues().get(0), isEquals());
	}

	@Override
	public String dump() {
		return debugDump(0);
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("LESS: \n");
		
		if (getFullPath() != null){
			DebugUtil.indentDebugDump(sb, indent+1);
			sb.append("PATH: ");
			sb.append(getFullPath().toString());
			sb.append("\n");
		} 
		DebugUtil.indentDebugDump(sb, indent+1);
		sb.append("DEF: ");
		if (getDefinition() != null) {
			sb.append(getDefinition().debugDump(indent));
			sb.append("\n");
		} else {
			DebugUtil.indentDebugDump(sb, indent);
			sb.append("null\n");
		}
		DebugUtil.indentDebugDump(sb, indent+1);
		sb.append("VALUE: ");
		if (getValues() != null) {
			indent += 1;
			for (PrismValue val : getValues()) {
				sb.append(val.debugDump(indent));
			}
		} else {
			DebugUtil.indentDebugDump(sb, indent);
			sb.append("null\n");
		}
		return sb.toString();
	}

	@Override
	public <T extends Objectable> boolean match(PrismObject<T> object, MatchingRuleRegistry matchingRuleRegistry) {
		throw new UnsupportedOperationException("Matching object and greater filter not supported yet");
	}
	
	@Override
	public PrismPropertyDefinition getDefinition() {
		// TODO Auto-generated method stub
		return (PrismPropertyDefinition) super.getDefinition();
	}

}
