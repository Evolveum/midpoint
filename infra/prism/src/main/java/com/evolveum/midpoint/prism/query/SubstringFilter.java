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
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

public class SubstringFilter<T> extends PropertyValueFilter<PrismPropertyValue<T>> {

	SubstringFilter(ItemPath parentPath, ItemDefinition definition, PrismPropertyValue<T> value) {
		super(parentPath, definition, value);
	}
	
	SubstringFilter(ItemPath parentPath, ItemDefinition definition, QName matchingRule, List<PrismPropertyValue<T>> value) {
		super(parentPath, definition, matchingRule, value);
	}
	
	SubstringFilter(ItemPath parentPath, ItemDefinition definition, QName matchingRule) {
		super(parentPath, definition, matchingRule);
	}
	

	public static <T> SubstringFilter createSubstring(QName path, PrismPropertyDefinition itemDefinition, PrismPropertyValue<T> values) {
		return createSubstring(new ItemPath(path), itemDefinition, null, values);
	}
	
	public static <T> SubstringFilter createSubstring(ItemPath path, PrismPropertyDefinition itemDefinition, PrismPropertyValue<T> values) {
		return createSubstring(path, itemDefinition, null, values);
	}
	
	public static <T> SubstringFilter createSubstring(QName path, PrismPropertyDefinition itemDefinition, QName matchingRule, PrismPropertyValue<T> values) {
		return createSubstring(new ItemPath(path), itemDefinition, matchingRule, values);
	}
	
	public static <T> SubstringFilter createSubstring(QName path, PrismPropertyDefinition itemDefinition, T realValues) {
		return createSubstring(new ItemPath(path), itemDefinition, null, realValues);
	}
	
	public static <T> SubstringFilter createSubstring(ItemPath path, PrismPropertyDefinition itemDefinition, T realValues) {
		return createSubstring(path, itemDefinition, null, realValues);
	}
	
	public static <T> SubstringFilter createSubstring(QName path, PrismPropertyDefinition itemDefinition, QName matchingRule, T realValues) {
		return createSubstring(new ItemPath(path), itemDefinition, matchingRule, realValues);
	}
	
	public static <T> SubstringFilter createSubstring(ItemPath path, PrismPropertyDefinition itemDefinition, QName matchingRule, T realValues) {
		if (realValues == null){
			return createNullSubstring(path, itemDefinition, matchingRule);
		}
		List<PrismPropertyValue<T>> pValues = createPropertyList(itemDefinition, realValues);
		return new SubstringFilter<T>(path, itemDefinition, matchingRule, pValues);
	}
	
	public static <T> SubstringFilter createSubstring(ItemPath path, PrismPropertyDefinition itemDefinition, QName matchingRule, PrismPropertyValue<T> values) {
		Validate.notNull(path, "Item path in substring filter must not be null.");
		Validate.notNull(itemDefinition, "Item definition in substring filter must not be null.");
		
		if (values == null){
			return createNullSubstring(path, itemDefinition, matchingRule);
		}
		
		List<PrismPropertyValue<T>> pValues = createPropertyList(itemDefinition, values);
				
		return new SubstringFilter(path, itemDefinition, matchingRule, pValues);
	}
	
	public static <O extends Objectable, T> SubstringFilter createSubstring(ItemPath path, Class<O> clazz, PrismContext prismContext, T value) {
		
		return createSubstring(path, clazz, prismContext, null, value);
//		return createSubstring(clazz, prismContext, propertyName, null, value);
	}
	
	public static <O extends Objectable, T> SubstringFilter createSubstring(ItemPath path, Class<O> clazz, PrismContext prismContext, QName matchingRule, T realValue) {
		
		ItemDefinition itemDefinition = findItemDefinition(path, clazz, prismContext);
		
		if (!(itemDefinition instanceof PrismPropertyDefinition)){
			throw new IllegalStateException("Bad definition. Expected property definition, but got " + itemDefinition);
		}
		
		if (realValue == null){
			//TODO: create null filter
			return createNullSubstring(path, (PrismPropertyDefinition) itemDefinition, matchingRule);
		}
		
		List<PrismPropertyValue<T>> pVals = createPropertyList((PrismPropertyDefinition) itemDefinition, realValue);
		
		return new SubstringFilter(path, itemDefinition, matchingRule, pVals);
	}
	
	public static <O extends Objectable> SubstringFilter createSubstring(QName propertyName, Class<O> clazz, PrismContext prismContext, String value) {
//        PrismObjectDefinition objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz);
//        ItemDefinition itemDef = objDef.findItemDefinition(propertyName);
////        return new SubstringFilter(null, itemDef, matchingRule, value);
		return createSubstring(propertyName, clazz, prismContext, null, value);
    }

    public static <O extends Objectable> SubstringFilter createSubstring(QName propertyName, Class<O> clazz, PrismContext prismContext, QName matchingRule, String value) {
        return createSubstring(new ItemPath(propertyName), clazz, prismContext, matchingRule, value);
    }
    
    private static SubstringFilter createNullSubstring(ItemPath itemPath, PrismPropertyDefinition propertyDef, QName matchingRule){
		return new SubstringFilter(itemPath, propertyDef, matchingRule);
		
	}
    

	@Override
	public SubstringFilter clone() {
		return new SubstringFilter(getFullPath(),getDefinition(), getMatchingRule(), getValues());
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
		if (getValues() != null) {
			DebugUtil.indentDebugDump(sb, indent);
			sb.append(getValues());
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
		if (getValues() != null){
			sb.append(getValues());
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
				return matching.matches(value, ".*"+getValues()+".*");
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

	@Override
	public QName getElementName() {
		// TODO Auto-generated method stub
		return getDefinition().getName();
	}

	@Override
	public PrismContext getPrismContext() {
		// TODO Auto-generated method stub
		return getDefinition().getPrismContext();
	}

	@Override
	public ItemPath getPath() {
		// TODO Auto-generated method stub
		return getFullPath();
	}

}
