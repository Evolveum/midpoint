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

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

public abstract class PropertyValueFilter extends ValueFilter{

	private List<? extends PrismValue> values;

	PropertyValueFilter(){
		
	}
	
	PropertyValueFilter(ItemPath path, ItemDefinition definition, String matchingRule, List<? extends PrismValue> values) {
		super(path, definition, matchingRule);
		this.values = values;
	}
	
	
	PropertyValueFilter(ItemPath path, ItemDefinition definition, PrismValue value) {
		super(path, definition);
		setValue(value);
	}
	
	PropertyValueFilter(ItemPath path, ItemDefinition definition, Element expression) {
		super(path, definition, expression);
	}
	
	PropertyValueFilter(ItemPath path, ItemDefinition definition, String matchingRule, Element expression) {
		super(path, definition, matchingRule, expression);
	}
	
	static PropertyValueFilter create(Class filterClass, ItemPath path, ItemDefinition itemDef, String matchingRule, PrismValue value){
		if (filterClass.isAssignableFrom(EqualsFilter.class)){
			return EqualsFilter.createEqual(path, itemDef, matchingRule, value);
		} else if (filterClass.isAssignableFrom(LessFilter.class)){
			return LessFilter.createLessFilter(path, itemDef, value, false);
		} else if (filterClass.isAssignableFrom(GreaterFilter.class)){
			return GreaterFilter.createGreaterFilter(path, itemDef, value, false);
		}
		throw new IllegalArgumentException("Bad filter class");
	}

	static PropertyValueFilter create(Class filterClass, ItemPath path, ItemDefinition itemDef, String matchingRule, List<PrismValue> values) {
		if (filterClass.isAssignableFrom(EqualsFilter.class)) {
			return EqualsFilter.createEqual(path, itemDef, matchingRule, values);
		}
		throw new IllegalArgumentException("Bad filter class");
	}
		
	public static PropertyValueFilter createPropertyFilter(Class filterClass, ItemPath parentPath, ItemDefinition item, String matchingRule, Object realValue) {

		if (realValue == null){
			return create(filterClass, parentPath, item, matchingRule, (PrismPropertyValue)null);
		}
		if (List.class.isAssignableFrom(realValue.getClass())) {
			List<PrismValue> prismValues = new ArrayList<PrismValue>();
			for (Object o : (List) realValue) {
				if (o instanceof PrismPropertyValue) {
					prismValues.add((PrismPropertyValue) o);
				} else {
					PrismPropertyValue val = new PrismPropertyValue(o);
					prismValues.add(val);
				}
			}
			return create(filterClass, parentPath, item, matchingRule, prismValues);
		}
		
		//temporary hack to not allow polystring type to go to the filter..we want polyString
		PrismPropertyValue value = null;
		if (realValue instanceof PolyStringType){
			value = new PrismPropertyValue(((PolyStringType) realValue).toPolyString());
		} else{
			value = new PrismPropertyValue(realValue);
		}
		
		return create(filterClass, parentPath, item, matchingRule, value);
	}

	
	public static PropertyValueFilter createPropertyFilter(Class filterClass, ItemPath parentPath, PrismContainerDefinition<? extends Containerable> containerDef,
			QName propertyName, PrismValue... values) throws SchemaException {
		ItemDefinition itemDef = containerDef.findItemDefinition(propertyName);
		if (itemDef == null) {
			throw new SchemaException("No definition for item " + propertyName + " in container definition "
					+ containerDef);
		}

		return create(filterClass, parentPath, itemDef, null, Arrays.asList(values));
	}

	public static PropertyValueFilter createPropertyFilter(Class filterClass, ItemPath parentPath, PrismContainerDefinition<? extends Containerable> containerDef,
			QName propertyName, Object realValue) throws SchemaException {
		ItemDefinition itemDef = containerDef.findItemDefinition(propertyName);
		if (itemDef == null) {
			throw new SchemaException("No definition for item " + propertyName + " in container definition "
					+ containerDef);
		}

		return createPropertyFilter(filterClass, parentPath, itemDef, null, realValue);
	}

	public static PropertyValueFilter createPropertyFilter(Class filterClass, Class<? extends Objectable> type, PrismContext prismContext, QName propertyName, Object realValue)
			throws SchemaException {
		PrismObjectDefinition<?> objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
		return createPropertyFilter(filterClass, null, objDef, propertyName, realValue);
	}
	
	public static PropertyValueFilter createPropertyFilter(Class filterClass, Class<? extends Objectable> type, 
			PrismContext prismContext, ItemPath propertyPath, Object realValue) throws SchemaException {
		PrismObjectDefinition<? extends Objectable> objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
		PrismContainerDefinition<? extends Containerable> containerDef = objDef;
		ItemPath parentPath = propertyPath.allExceptLast();
		if (!parentPath.isEmpty()) {
			containerDef = objDef.findContainerDefinition(parentPath);
			if (containerDef == null) {
				throw new SchemaException("No definition for container " + parentPath + " in object definition "
						+ objDef);
			}
		}
		return createPropertyFilter(filterClass, propertyPath.allExceptLast(), containerDef, ItemPath.getName(propertyPath.last()), realValue);
	}
	
	public List<? extends PrismValue> getValues() {
		return values;
	}
	
	public void setValues(List<? extends PrismValue> values) {
		this.values = values;
	}
	
	public void setValue(PrismValue value) {
		List<PrismValue> values = new ArrayList<PrismValue>();
		if (value != null) {
			values.add(value);
		}
		this.values = values;
	}
	
	protected void cloneValues(PropertyValueFilter clone) {
		super.cloneValues(clone);
		clone.values = getCloneValuesList();
	}
	private List<? extends PrismValue> getCloneValuesList() {
		if (values == null) {
			return null;
		}
		List<PrismValue> clonedValues = new ArrayList<PrismValue>(values.size());
		for(PrismValue value: values) {
			clonedValues.add(value.clone());
		}
		return clonedValues;
	}
	
	public <T extends Objectable> boolean match(PrismObject<T> object){
		ItemPath path = null;
		if (getParentPath() != null){
			path = new ItemPath(getParentPath(), getDefinition().getName());
		} else{
			path = new ItemPath(getDefinition().getName());
		}
		
		Item<?> item = object.findItem(path);
		if (item == null && getValues() == null) {
			return true;
		}
		Item filterItem = getDefinition().instantiate();
		if (getValues() != null) {
			try {
				filterItem.addAll(getValues());
			} catch (SchemaException e) {
				throw new IllegalArgumentException(e.getMessage(), e);
			}
		}
		
		return item.equals(filterItem);
	}
	
}
