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
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

public abstract class PropertyValueFilter<T extends PrismValue> extends ValueFilter{

	private List<T> values;

	PropertyValueFilter(){
		
	}
	
	PropertyValueFilter(ItemPath path, ItemDefinition definition, String matchingRule, List<T> values) {
		super(path, definition, matchingRule);
		this.values = values;
	}
	
	
	PropertyValueFilter(ItemPath path, ItemDefinition definition, T value) { 
		super(path, definition);
		setValue(value);
	}
	
	PropertyValueFilter(ItemPath path, ItemDefinition definition, Element expression) {
		super(path, definition, expression);
	}
	
	PropertyValueFilter(ItemPath path, ItemDefinition definition, String matchingRule, Element expression) {
		super(path, definition, matchingRule, expression);
	}
	
	static <T extends PrismValue> PropertyValueFilter create(Class filterClass, ItemPath path, ItemDefinition itemDef, String matchingRule, T value) throws SchemaException{
		if (filterClass.isAssignableFrom(EqualsFilter.class)){
			return EqualsFilter.createEqual(path, (PrismPropertyDefinition) itemDef, matchingRule, value);
		} else if (filterClass.isAssignableFrom(LessFilter.class)){
			return LessFilter.createLessFilter(path, (PrismPropertyDefinition) itemDef, value, false);
		} else if (filterClass.isAssignableFrom(GreaterFilter.class)){
			return GreaterFilter.createGreaterFilter(path, (PrismPropertyDefinition) itemDef, value, false);
		}
		throw new IllegalArgumentException("Bad filter class");
	}

	static <T extends PrismValue> PropertyValueFilter create(Class filterClass, ItemPath path, ItemDefinition itemDef, String matchingRule, List<T> values) throws SchemaException {
		if (filterClass.isAssignableFrom(EqualsFilter.class)) {
			return EqualsFilter.createEqual(path, (PrismPropertyDefinition) itemDef, matchingRule, values);
		}
		throw new IllegalArgumentException("Bad filter class");
	}
		
	public static <F extends ValueFilter, I extends ItemDefinition, V extends Object> PropertyValueFilter createPropertyFilter(Class<F> filterClass, ItemPath parentPath, I item, String matchingRule, V realValue) throws SchemaException {

		if (realValue == null){
			return create(filterClass, parentPath, item, matchingRule, (PrismPropertyValue)null);
		}
		if (List.class.isAssignableFrom(realValue.getClass())) {
			List<PrismValue> prismValues = new ArrayList<PrismValue>();
			for (Object o : (List) realValue) {
				if (o instanceof PrismPropertyValue) {
					PrismPropertyValue pval = (PrismPropertyValue) o;
					PrismUtil.recomputePrismPropertyValue(pval, item.getPrismContext());
					prismValues.add(pval);
				} else {
					PrismUtil.recomputeRealValue(o, item.getPrismContext());
					PrismPropertyValue val = new PrismPropertyValue(o);
					prismValues.add(val);
				}
			}
			return create(filterClass, parentPath, item, matchingRule, prismValues);
		}
		
		//temporary hack to not allow polystring type to go to the filter..we want polyString
		PrismPropertyValue value = null;
		if (realValue instanceof PolyStringType){
			realValue = (V) ((PolyStringType) realValue).toPolyString();
		}
		PrismUtil.recomputeRealValue(realValue, item.getPrismContext());
		value = new PrismPropertyValue(realValue);
		return create(filterClass, parentPath, item, matchingRule, value);
	}

	
	public static PropertyValueFilter createPropertyFilter(Class filterClass, ItemPath parentPath, PrismContainerDefinition<? extends Containerable> containerDef,
			PrismValue... values) throws SchemaException {
//		ItemDefinition itemDef = containerDef.findItemDefinition(parentPath);
//		if (itemDef == null) {
//			throw new SchemaException("No definition for item " + parentPath + " in container definition "
//					+ containerDef);
//		}
		ItemDefinition itemDef = findItemDefinition(parentPath, containerDef);

		return create(filterClass, parentPath, itemDef, null, Arrays.asList(values));
	}
	
	public static PropertyValueFilter createPropertyFilter(Class filterClass, ItemPath parentPath, PrismContainerDefinition<? extends Containerable> containerDef,
			Object realValue) throws SchemaException {
//		ItemDefinition itemDef = containerDef.findItemDefinition(parentPath);
//		if (itemDef == null) {
//			throw new SchemaException("No definition for item " + parentPath + " in container definition "
//					+ containerDef);
//		}
		ItemDefinition itemDef = findItemDefinition(parentPath, containerDef);

		return createPropertyFilter(filterClass, parentPath, itemDef, null, realValue);
	}

	private static ItemDefinition findItemDefinition(ItemPath parentPath, PrismContainerDefinition<? extends Containerable> containerDef) throws SchemaException {
		ItemDefinition itemDef = containerDef.findItemDefinition(parentPath);
		if (itemDef == null) {
			throw new SchemaException("No definition for item " + parentPath + " in container definition "
					+ containerDef);
		}

		return itemDef;
	}

	public static PropertyValueFilter createPropertyFilter(Class filterClass, Class<? extends Objectable> type, PrismContext prismContext, QName propertyName, Object realValue)
			throws SchemaException {
		ItemPath path = new ItemPath(propertyName);
		PrismObjectDefinition<?> objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
		ItemDefinition def = findItemDefinition(path, objDef);
		
		return createPropertyFilter(filterClass, path, def, null, realValue);		
	}
	
	public static PropertyValueFilter createPropertyFilter(Class filterClass, Class<? extends Objectable> type, 
			PrismContext prismContext, ItemPath propertyPath, Object realValue) throws SchemaException {
		PrismObjectDefinition<? extends Objectable> objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
		ItemDefinition itemDef = findItemDefinition(propertyPath, objDef);
		return createPropertyFilter(filterClass, propertyPath, itemDef, null, realValue);
	}
	
	public List<T> getValues() {
		return values;
	}
	
	public void setValues(List<T> values) {
		this.values = values;
	}
	
	public void setValue(T value) {
		List<T> values = new ArrayList<T>();
		if (value != null) {
			values.add(value);
		}
		this.values = values;
	}
	
	protected void cloneValues(PropertyValueFilter clone) {
		super.cloneValues(clone);
		clone.values = getCloneValuesList();
	}
	private List<T> getCloneValuesList() {
		if (values == null) {
			return null;
		}
		List<T> clonedValues = new ArrayList<T>(values.size());
		for(T value: values) {
			clonedValues.add((T) value.clone());
		}
		return clonedValues;
	}
	
//	private ItemPath getFullPath(){
//		ItemPath path = null;
//		if (getParentPath() != null){
//			return new ItemPath(getParentPath(), getDefinition().getName());
//		} else{
//			return new ItemPath(getDefinition().getName());
//		}
//	}
	
	public Item getObjectItem(PrismObject object){
		
		ItemPath path = getFullPath();
		return object.findItem(path);
//		if (item == null && getValues() == null) {
//			return true;
//		}
		
	}
	
	public Item getFilterItem(){

		Item filterItem = getDefinition().instantiate();
		if (getValues() != null && !getValues().isEmpty()) {
			try {
				for (PrismValue v : getValues()){
					filterItem.add(v.clone());
				}
			} catch (SchemaException e) {
				throw new IllegalArgumentException(e.getMessage(), e);
			}
		}
		
		return filterItem;
	}
	
	@Override
	public <T extends Objectable> boolean match(PrismObject<T> object, MatchingRuleRegistry matchingRuleRegistry){
//		if (getObjectItem(object) == null && getValues() == null) {
//			return true;
//		}
		return false;
	}
	
}
