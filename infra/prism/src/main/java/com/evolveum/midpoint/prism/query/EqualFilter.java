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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.match.PolyStringOrigMatchingRule;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

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
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class EqualFilter<T extends Object> extends PropertyValueFilter<PrismPropertyValue<T>> implements Itemable {
	private static final long serialVersionUID = 3284478412180258355L;
	
	public static final QName ELEMENT_NAME = new QName(PrismConstants.NS_QUERY, "equal");

	//constructors
	EqualFilter(){	
	}
	
	EqualFilter(ItemPath parentPath, PrismPropertyDefinition definition, QName matchingRule, List<PrismPropertyValue<T>> values) {
		super(parentPath, definition, matchingRule, values);
	}

	private EqualFilter(ItemPath parentPath, PrismPropertyDefinition definition, QName matchingRule) {
		super(parentPath, definition, matchingRule);
	}
		
	EqualFilter(ItemPath parentPath, PrismPropertyDefinition definition, QName matchingRule, ExpressionWrapper expression) {
		super(parentPath, definition, matchingRule, expression);
	}
	
	public static EqualFilter createEqual(ItemPath path, PrismPropertyDefinition definition, QName matchingRule, ExpressionWrapper expression){
		Validate.notNull(path, "Path must not be null");
		// Do not check definition. We may want queries for which the definition is supplied later.
		return new EqualFilter(path, definition, matchingRule, expression);
	}

	//factory methods
	public static EqualFilter createEqual(ItemPath path, PrismProperty item){
		return createEqual(path, item, null);
	}
	
	public static EqualFilter createEqual(ItemPath path, PrismProperty item, QName matchingRule){
		Validate.notNull(item, "Item must not be null");
		Validate.notNull(path, "Path must not be null");
		return new EqualFilter(path, item.getDefinition(), matchingRule, item.getValues());
	}
	
	public static <T> EqualFilter createEqual(ItemPath path, PrismPropertyDefinition itemDefinition, T realValues){
		return createEqual(path, itemDefinition, null, realValues);
	}
	
	public static <T> EqualFilter createEqual(ItemPath path, PrismPropertyDefinition itemDefinition, QName matchingRule, T realValue){
		Validate.notNull(itemDefinition, "Item definition in the filter must not be null");
		Validate.notNull(path, "Path in the filter must not be null");
		if (realValue == null){
			//TODO: create null filter
			return createNullEqual(path, itemDefinition, matchingRule);
		}
		List<PrismPropertyValue<T>> pVals = createPropertyList(itemDefinition, realValue);
		return new EqualFilter(path, itemDefinition, matchingRule, pVals);
	}
	
	public static <T> EqualFilter createEqual(QName propertyName, PrismPropertyDefinition propertyDefinition, QName matchingRule, T realValue){
		return createEqual(new ItemPath(propertyName), propertyDefinition, matchingRule, realValue);
	}
	
	public static <T> EqualFilter createEqual(QName propertyName, PrismPropertyDefinition propertyDefinition, T realValues){
		return createEqual(new ItemPath(propertyName), propertyDefinition, null, realValues);
	}
//	
	
	public static <T> EqualFilter createEqual(ItemPath path, PrismPropertyDefinition itemDefinition, PrismPropertyValue<T> values) {
		return createEqual(path, itemDefinition, null, values);
	}
	
	public static <T> EqualFilter createEqual(ItemPath path, PrismPropertyDefinition itemDefinition, QName matchingRule, PrismPropertyValue<T> values){
		Validate.notNull(itemDefinition, "Item definition in the filter must not be null");
		Validate.notNull(path, "Path in the filter must not be null");
		if (values == null){
			//TODO: create null filter
			return createNullEqual(path, itemDefinition, matchingRule);
		}
		
		List<PrismPropertyValue<T>> pValues = createPropertyList(itemDefinition, values);
		
		return new EqualFilter(path, itemDefinition, matchingRule, pValues);
	}
	
	
	
	public static <O extends Containerable, T> EqualFilter createEqual(ItemPath parentPath, PrismContainerDefinition<O> containerDef,
			PrismPropertyValue<T> values) throws SchemaException {
		PrismPropertyDefinition propertyDef = (PrismPropertyDefinition) findItemDefinition(parentPath, containerDef);
		return createEqual(parentPath, propertyDef, values);
//		return (EqualsFilter) createPropertyFilter(EqualsFilter.class, parentPath, containerDef, values);
	}

	public static <O extends Containerable, T> EqualFilter createEqual(ItemPath parentPath, PrismContainerDefinition<O> containerDef,
			T realValues) throws SchemaException {
		PrismPropertyDefinition propertyDef = (PrismPropertyDefinition) findItemDefinition(parentPath, containerDef);
		return createEqual(parentPath, propertyDef, realValues);
//		return (EqualsFilter) createPropertyFilter(EqualsFilter.class, parentPath, containerDef, realValue);
	}

	public static <O extends Objectable, T> EqualFilter createEqual(QName propertyName, Class<O> type, PrismContext prismContext, T realValues)
			throws SchemaException {
		return createEqual(propertyName, type, prismContext, null, realValues);
		
		
	}
	
	 public static <O extends Objectable, T> EqualFilter createEqual(QName propertyName, Class<O> type, PrismContext prismContext,
             QName matchingRule, T realValues) {
		 
		return createEqual(new ItemPath(propertyName), type, prismContext, matchingRule, realValues);
}
	
	public static <O extends Objectable, T> EqualFilter createEqual(ItemPath propertyPath, Class<O> type, PrismContext prismContext, T realValue)
			throws SchemaException {
		return createEqual(propertyPath, type, prismContext, null, realValue);
	}
	
	public static <O extends Objectable, T> EqualFilter createEqual(ItemPath propertyPath, Class<O> type, PrismContext prismContext, QName matchingRule, T realValue)
			{
		PrismPropertyDefinition propertyDefinition = (PrismPropertyDefinition) findItemDefinition(propertyPath, type, prismContext);
		return createEqual(propertyPath, propertyDefinition, matchingRule, realValue);
	}

   
	public static EqualFilter createNullEqual(ItemPath itemPath, PrismPropertyDefinition propertyDef, QName matchingRule){
		return new EqualFilter(itemPath, propertyDef, matchingRule);
		
	}

//    public static <O extends Objectable> EqualsFilter createPolyStringOrigEqual(Class<O> type, PrismContext prismContext,
//                                           QName propertyName, PolyString realValue)
//            throws SchemaException {
//        return createEqual(propertyName, type, prismContext, PolyStringOrigMatchingRule.NAME.getLocalPart(), realValue);
//    }

    @Override
	public EqualFilter clone() {
		EqualFilter clone = new EqualFilter(getFullPath(), getDefinition(), getMatchingRule(), (List<PrismPropertyValue<T>>) getValues());
		clone.setExpression(getExpression());
		cloneValues(clone);
		return clone;
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("EQUAL:");
		return debugDump(indent, sb);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("EQUAL(");
		return toString(sb);
	}

	@Override
	public PrismContext getPrismContext() {
		return getDefinition().getPrismContext();
	}

	@Override
	public ItemPath getPath() {
		return getFullPath();
	}

	@Override
	public <T extends Objectable> boolean match(PrismObject<T> object, MatchingRuleRegistry matchingRuleRegistry) {
		Item filterItem = getFilterItem();
		if (!super.match(object, matchingRuleRegistry)){
			return false;
		}
		
		List<Object> values = getObjectItem(object).getValues();
		if (values == null){
			return true;
		}
		
		for (Object v : values){
			if (!(v instanceof PrismPropertyValue)){
				throw new IllegalArgumentException("Not supported prism value for equals filter. It must be an instance of PrismPropertyValue but it is " + v.getClass());
			}
			
			if (!isInFilterItem((PrismPropertyValue) v, filterItem, getMatchingRuleFromRegistry(matchingRuleRegistry, filterItem))){
				return false;
			}
		}
	
		return true;
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
		
}

