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

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

public class SubstringFilter extends StringValueFilter {

	SubstringFilter(ItemPath parentPath, ItemDefinition definition, String value) {
		super(parentPath, definition, value);
	}
	
	SubstringFilter(ItemPath parentPath, ItemDefinition definition, String matchingRule, String value) {
		super(parentPath, definition, matchingRule, value);
	}

	public static SubstringFilter createSubstring(ItemDefinition definition, String value) {
		Validate.notNull(definition, "Item definition must not be null");
		return createSubstring(definition.getName(), definition, value);
	}
	
	public static SubstringFilter createSubstring(QName itemName, ItemDefinition definition, String value) {
		return new SubstringFilter(new ItemPath(itemName), definition, value);
	}
	
	public static SubstringFilter createSubstring(ItemPath path, ItemDefinition definition, String value) {
		return new SubstringFilter(path, definition, value);
	}
	
	public static SubstringFilter createSubstring(ItemPath path, ItemDefinition definition, String matchingRule, String value) {
		return new SubstringFilter(path, definition, matchingRule, value);
	}
	
	public static SubstringFilter createSubstring(Class clazz, PrismContext prismContext, QName propertyName, String value) {
		return createSubstring(clazz, prismContext, propertyName, null, value);
	}

    public static SubstringFilter createSubstring(Class clazz, PrismContext prismContext, QName propertyName, String matchingRule, String value) {
        PrismObjectDefinition objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz);
        ItemDefinition itemDef = objDef.findItemDefinition(propertyName);
        return new SubstringFilter(null, itemDef, matchingRule, value);
    }

	@Override
	public SubstringFilter clone() {
		return new SubstringFilter(getFullPath(),getDefinition(),getValue());
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
		sb.append("SUBSTRING: \n");
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
		if (getValue() != null) {
			DebugUtil.indentDebugDump(sb, indent);
			sb.append(getValue());
			sb.append("\n");
		} else {
			DebugUtil.indentDebugDump(sb, indent);
			sb.append("null\n");
		}
		DebugUtil.indentDebugDump(sb, indent+1);
		sb.append("MATCHING: ");
		if (getMatchingRule() != null) {
			indent += 1;
				sb.append(getMatchingRule());
		} else {
			DebugUtil.indentDebugDump(sb, indent);
			sb.append("default\n");
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("SUBSTRING: ");
		if (getFullPath() != null){
			sb.append(getFullPath().toString());
			sb.append(", ");
		}
		if (getDefinition() != null){
			sb.append(getDefinition().getName().getLocalPart());
			sb.append(", ");
		}
		if (getValue() != null){
			sb.append(getValue());
		}
		return sb.toString();
	}

	@Override
	public <T extends Objectable> boolean match(PrismObject<T> object, MatchingRuleRegistry matchingRuleRegistry) {
		ItemPath path = null;
		if (getFullPath() != null){
			path = new ItemPath(getFullPath(), getDefinition().getName());
		} else{
			path = new ItemPath(getDefinition().getName());
		}
		
		Item item = object.findItem(path);
		
		MatchingRule matching = getMatchingRuleFromRegistry(matchingRuleRegistry, item);
		
		for (Object val : item.getValues()){
			if (val instanceof PrismPropertyValue){
				Object value = ((PrismPropertyValue) val).getValue();
				return matching.matches(value, ".*"+getValue()+".*");
//				if (value instanceof PolyStringType){
//					if (StringUtils.contains(((PolyStringType) value).getNorm(), getValue())){
//						return true;
//					}
//				} else if (value instanceof PolyString){
//					if (StringUtils.contains(((PolyString) value).getNorm(), getValue())){
//						return true;
//					}
//				} else if (value instanceof String){
//					if (StringUtils.contains((String)value, getValue())){
//						return true;
//					}
//				}
			}
			if (val instanceof PrismReferenceValue) {
				throw new UnsupportedOperationException(
						"matching substring on the prism reference value not supported yet");
			}
		}
		
		return false;
	}

}
