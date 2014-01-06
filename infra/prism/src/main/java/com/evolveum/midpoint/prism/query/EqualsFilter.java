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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.match.PolyStringOrigMatchingRule;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Itemable;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class EqualsFilter<T extends Object> extends PropertyValueFilter<PrismPropertyValue<T>> implements Itemable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 3284478412180258355L;

	//constructors
	EqualsFilter(){	
	}
	
	EqualsFilter(ItemPath parentPath, PrismPropertyDefinition definition, QName matchingRule, List<PrismPropertyValue<T>> values) {
		super(parentPath, definition, matchingRule, values);
	}

	private EqualsFilter(ItemPath parentPath, PrismPropertyDefinition definition, QName matchingRule) {
		super(parentPath, definition, matchingRule);
	}
		
	EqualsFilter(ItemPath parentPath, PrismPropertyDefinition definition, QName matchingRule, Element expression) {
		super(parentPath, definition, matchingRule, expression);
	}
	
	public static EqualsFilter createEqual(ItemPath path, PrismPropertyDefinition definition, QName matchingRule, Element expression){
		Validate.notNull(definition, "Item must not be null");
		Validate.notNull(path, "Path must not be null");
		return new EqualsFilter(path, definition, matchingRule, expression);
	}

	//factory methods
	public static EqualsFilter createEqual(ItemPath path, PrismProperty item){
		return createEqual(path, item, null);
	}
	
	public static EqualsFilter createEqual(ItemPath path, PrismProperty item, QName matchingRule){
		Validate.notNull(item, "Item must not be null");
		Validate.notNull(path, "Path must not be null");
		return new EqualsFilter(path, item.getDefinition(), matchingRule, item.getValues());
	}
	
	public static <T> EqualsFilter createEqual(ItemPath path, PrismPropertyDefinition itemDefinition, T realValues){
		return createEqual(path, itemDefinition, null, realValues);
	}

	public static <T> EqualsFilter createEqual(ItemPath path, PrismPropertyDefinition itemDefinition, QName matchingRule, T realValue){
		Validate.notNull(itemDefinition, "Item definition in the filter must not be null");
		Validate.notNull(path, "Path in the filter must not be null");
		if (realValue == null){
			//TODO: create null filter
			return createNullEqual(path, itemDefinition, matchingRule);
		}
		List<PrismPropertyValue<T>> pVals = createPropertyList(itemDefinition, realValue);
		return new EqualsFilter(path, itemDefinition, matchingRule, pVals);
	}
	
	public static <T> EqualsFilter createEqual(QName propertyName, PrismPropertyDefinition propertyDefinition, QName matchingRule, T realValue){
		return createEqual(new ItemPath(propertyName), propertyDefinition, matchingRule, realValue);
	}
	
	public static <T> EqualsFilter createEqual(QName propertyName, PrismPropertyDefinition propertyDefinition, T realValues){
		return createEqual(new ItemPath(propertyName), propertyDefinition, null, realValues);
	}
//	
	
	public static <T> EqualsFilter createEqual(ItemPath path, PrismPropertyDefinition itemDefinition, PrismPropertyValue<T> values) {
		return createEqual(path, itemDefinition, null, values);
	}
	
	public static <T> EqualsFilter createEqual(ItemPath path, PrismPropertyDefinition itemDefinition, QName matchingRule, PrismPropertyValue<T> values){
		Validate.notNull(itemDefinition, "Item definition in the filter must not be null");
		Validate.notNull(path, "Path in the filter must not be null");
		if (values == null){
			//TODO: create null filter
			return createNullEqual(path, itemDefinition, matchingRule);
		}
		
		List<PrismPropertyValue<T>> pValues = createPropertyList(itemDefinition, values);
		
		return new EqualsFilter(path, itemDefinition, matchingRule, pValues);
	}
	
	
	
	public static <O extends Containerable, T> EqualsFilter createEqual(ItemPath parentPath, PrismContainerDefinition<O> containerDef,
			PrismPropertyValue<T> values) throws SchemaException {
		PrismPropertyDefinition propertyDef = (PrismPropertyDefinition) findItemDefinition(parentPath, containerDef);
		return createEqual(parentPath, propertyDef, values);
//		return (EqualsFilter) createPropertyFilter(EqualsFilter.class, parentPath, containerDef, values);
	}

	public static <O extends Containerable, T> EqualsFilter createEqual(ItemPath parentPath, PrismContainerDefinition<O> containerDef,
			T realValues) throws SchemaException {
		PrismPropertyDefinition propertyDef = (PrismPropertyDefinition) findItemDefinition(parentPath, containerDef);
		return createEqual(parentPath, propertyDef, realValues);
//		return (EqualsFilter) createPropertyFilter(EqualsFilter.class, parentPath, containerDef, realValue);
	}

	public static <O extends Objectable, T> EqualsFilter createEqual(QName propertyName, Class<O> type, PrismContext prismContext, T realValues)
			throws SchemaException {
		return createEqual(propertyName, type, prismContext, null, realValues);
		
		
	}
	
	 public static <O extends Objectable, T> EqualsFilter createEqual(QName propertyName, Class<O> type, PrismContext prismContext,
             QName matchingRule, T realValues) {
		 
		return createEqual(new ItemPath(propertyName), type, prismContext, matchingRule, realValues);
}
	
	public static <O extends Objectable, T> EqualsFilter createEqual(ItemPath propertyPath, Class<O> type, PrismContext prismContext, T realValue)
			throws SchemaException {
		return createEqual(propertyPath, type, prismContext, null, realValue);
	}
	
	public static <O extends Objectable, T> EqualsFilter createEqual(ItemPath propertyPath, Class<O> type, PrismContext prismContext, QName matchingRule, T realValue)
			{
		PrismPropertyDefinition propertyDefinition = (PrismPropertyDefinition) findItemDefinition(propertyPath, type, prismContext);
		return createEqual(propertyPath, propertyDefinition, matchingRule, realValue);
	}

   
	private static EqualsFilter createNullEqual(ItemPath itemPath, PrismPropertyDefinition propertyDef, QName matchingRule){
		return new EqualsFilter(itemPath, propertyDef, matchingRule);
		
	}

//    public static <O extends Objectable> EqualsFilter createPolyStringOrigEqual(Class<O> type, PrismContext prismContext,
//                                           QName propertyName, PolyString realValue)
//            throws SchemaException {
//        return createEqual(propertyName, type, prismContext, PolyStringOrigMatchingRule.NAME.getLocalPart(), realValue);
//    }

    @Override
	public EqualsFilter clone() {
		EqualsFilter clone = new EqualsFilter(getFullPath(), getDefinition(), getMatchingRule(), (List<PrismPropertyValue<T>>) getValues());
		cloneValues(clone);
		return clone;
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
		sb.append("EQUALS:");
		
		if (getFullPath() != null){
			sb.append("\n");
			DebugUtil.indentDebugDump(sb, indent+1);
			sb.append("PATH: ");
			sb.append(getFullPath().toString());
		} 
		
		sb.append("\n");
		DebugUtil.indentDebugDump(sb, indent+1);
		sb.append("DEF: ");
		if (getDefinition() != null) {
			sb.append(getDefinition().toString());
		} else {
			sb.append("null");
		}
		
		sb.append("\n");
		DebugUtil.indentDebugDump(sb, indent+1);
		sb.append("VALUE:");
		if (getValues() != null) {
			sb.append("\n");
			for (PrismValue val : getValues()) {
				sb.append(DebugUtil.debugDump(val, indent + 2));
			}
		} else {
			sb.append(" null");
		}
		
		sb.append("\n");
		DebugUtil.indentDebugDump(sb, indent+1);
		sb.append("MATCHING: ");
		if (getMatchingRule() != null) {
			sb.append(getMatchingRule());
		} else {
			sb.append("default");
		}
		return sb.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("EQUALS: ");
		if (getFullPath() != null){
			sb.append(getFullPath().toString());
			sb.append(", ");
		}
		if (getDefinition() != null){
			sb.append(getDefinition().getName().getLocalPart());
			sb.append(", ");
		}
		if (getValues() != null){
			for (int i = 0; i< getValues().size() ; i++){
				PrismValue value = getValues().get(i);
				if (value == null) {
					sb.append("null");
				} else {
					sb.append(value.toString());
				}
				if ( i != getValues().size() -1){
					sb.append(", ");
				}
			}
		}
		return sb.toString();
	}

	@Override
	public QName getName() {
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

	@Override
	public <T extends Objectable> boolean match(PrismObject<T> object, MatchingRuleRegistry matchingRuleRegistry) {
		if (getObjectItem(object) == null && getValues() == null) {
			return true;
		}
		
		Item filterItem = getFilterItem();
		MatchingRule matching = getMatchingRuleFromRegistry(matchingRuleRegistry, filterItem);
		
//		QName matchingRule = null;
//		if (StringUtils.isNotBlank(getMatchingRule())){
//			matchingRule = new QName(PrismConstants.NS_MATCHING_RULE, getMatchingRule());
//		} else {
//			matchingRule = new QName(PrismConstants.NS_MATCHING_RULE, "default");
//		}
//		
//		MatchingRule matching = null;
//		try{
//		matching = matchingRuleRegistry.getMatchingRule( matchingRule, filterItem.getDefinition().getTypeName());
//		} catch (SchemaException ex){
//			throw new IllegalArgumentException(ex.getMessage(), ex);
//		}
		
		Item item = getObjectItem(object);
		
		if (item == null && getValues() == null) {
			return true;
		}
		
		if (item != null && !item.isEmpty() && getValues() == null){
			return false;
		}
		
		for (Object v : item.getValues()){
			if (!(v instanceof PrismPropertyValue)){
				throw new IllegalArgumentException("Not supported prism value for equals filter. It must be an instance of PrismPropertyValue but it is " + v.getClass());
			}
			
			if (!isInFilterItem((PrismPropertyValue) v, filterItem, matching)){
				return false;
			}
		}
	
		return true;
//		return item.match(filterItem);
		
//		return super.match(object, matchingRuleRegistry);
	}

	private boolean isInFilterItem(PrismPropertyValue v, Item filterItem, MatchingRule matchingRule) {
		for (Object filterValue : filterItem.getValues()){
			if (!(filterValue instanceof PrismPropertyValue)){
				throw new IllegalArgumentException("Not supported prism value for equals filter. It must be an instance of PrismPropertyValue but it is " + v.getClass());
			}
			
			PrismPropertyValue filterV = (PrismPropertyValue) filterValue;
			if (matchingRule.match(filterV.getValue(), v.getValue())){
				return true;
			}
		}
		
		return false;
		
	}
	
	@Override
	public PrismPropertyDefinition getDefinition(){
		return (PrismPropertyDefinition) super.getDefinition();
	}
	
	@Override
	public List<PrismPropertyValue<T>> getValues() {
		// TODO Auto-generated method stub
		return super.getValues();
	}
	
	
	
	
//	public static <T> EqualsFilter createEqual(ItemPath parentPath, PrismPropertyDefinition itemDef, List<PrismPropertyValue<T>> value) {
//		Validate.notNull(itemDef, "Item definition in the equals filter must not be null");
//		return new EqualsFilter(parentPath, itemDef, null, value);
//	}
	
//	public static <T> EqualsFilter createEqual(ItemPath parentPath, PrismPropertyDefinition itemDef, String matchingRule, PrismPropertyValue<T> value) {
//		Validate.notNull(itemDef, "Item definition in the equals filter must not be null");
//		Validate.notNull(itemDefinition, "Item path in the filter must not be null");
//		return new EqualsFilter(parentPath, itemDef, matchingRule, value);
//	}

//	public static <T> EqualsFilter createEqual(ItemPath parentPath, PrismPropertyDefinition itemDef, String matchingRule, List<PrismPropertyValue<T>> values) {
//		Validate.notNull(itemDef, "Item definition in the equals filter must not be null");
//		return new EqualsFilter(parentPath, itemDef, matchingRule, values);
//	}

//	public static EqualsFilter createEqual(ItemPath parentPath, PrismPropertyDefinition itemDef, Element expression) {
//		Validate.notNull(itemDef, "Item definition in the equals filter must not be null");
//		return new EqualsFilter(parentPath, itemDef, expression);
//	}
	
//	public static EqualsFilter createEqual(ItemPath parentPath, PrismPropertyDefinition itemDef, String matchingRule, Element expression) {
//		Validate.notNull(itemDef, "Item definition in the equals filter must not be null");
//		return new EqualsFilter(parentPath, itemDef, matchingRule, expression);
//	}

//	public static <T> EqualsFilter createEqual(ItemPath parentPath, PrismPropertyDefinition item, String matchingRule, T realValue) throws SchemaException {
//		return (EqualsFilter) createPropertyFilter(EqualsFilter.class, parentPath, item, matchingRule, realValue);
//	}	

	
}

