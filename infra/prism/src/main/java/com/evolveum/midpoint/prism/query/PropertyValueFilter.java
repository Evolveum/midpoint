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
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

public abstract class PropertyValueFilter<T extends PrismValue> extends ValueFilter implements Itemable{

	private List<T> values;

	PropertyValueFilter(){
		
	}
	
	PropertyValueFilter(ItemPath path, ItemDefinition definition, QName matchingRule, List<T> values) {
		super(path, definition, matchingRule);
		this.values = values;
	}
	
	PropertyValueFilter(ItemPath path, ItemDefinition definition, T value){
		super(path, definition);
		setValue(value);
	}
	
	PropertyValueFilter(ItemPath path, ItemDefinition definition, QName matchingRule) { 
		super(path, definition, matchingRule);
		this.values = null;
	}
	
	PropertyValueFilter(ItemPath path, ItemDefinition definition, Element expression) {
		super(path, definition, expression);
	}
	
	PropertyValueFilter(ItemPath path, ItemDefinition definition, QName matchingRule, Element expression) {
		super(path, definition, matchingRule, expression);
	}
	
	static <T> List<PrismPropertyValue<T>> createPropertyList(PrismPropertyDefinition itemDefinition, PrismPropertyValue<T> values) {
		Validate.notNull(itemDefinition, "Item definition in substring filter must not be null.");
		
		List<PrismPropertyValue<T>> pValues = new ArrayList<PrismPropertyValue<T>>();
		PrismUtil.recomputePrismPropertyValue(values, itemDefinition.getPrismContext());
		pValues.add(values);
		
		return pValues;
	}
	
	static <T> List<PrismPropertyValue<T>> createPropertyList(PrismPropertyDefinition itemDefinition, PrismPropertyValue<T>[] values) {
		Validate.notNull(itemDefinition, "Item definition in substring filter must not be null.");
		
		List<PrismPropertyValue<T>> pValues = new ArrayList<PrismPropertyValue<T>>();
		
		for (PrismPropertyValue<T> val : values){
			PrismUtil.recomputePrismPropertyValue(val, itemDefinition.getPrismContext());
			pValues.add(val);
		}
		
		return pValues;
	}
	
	
	 static <T> List<PrismPropertyValue<T>> createPropertyList(PrismPropertyDefinition itemDefinition, T realValue){
		List<PrismPropertyValue<T>> pVals = new ArrayList<PrismPropertyValue<T>>();

		if (realValue.getClass() != null && Collection.class.isAssignableFrom(realValue.getClass())) {
			for (Object o : (Iterable)realValue){
				if (o instanceof PrismPropertyValue){
					PrismPropertyValue pVal = (PrismPropertyValue) o;
					PrismUtil.recomputePrismPropertyValue(pVal, itemDefinition.getPrismContext());
					pVals.add(pVal);
				}else{
					pVals.addAll(PrismPropertyValue.createCollection((Collection<T>) realValue));
				}
			}
			
		} else {
			PrismUtil.recomputeRealValue(realValue, itemDefinition.getPrismContext());
			pVals.add(new PrismPropertyValue<T>(realValue));
		}
		return pVals;
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
	
	public Item getObjectItem(PrismObject object){
		
		ItemPath path = getFullPath();
		return object.findItem(path);
		
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
		if (getObjectItem(object) == null && getValues() == null) {
			return true;
		}
		
		Item filterItem = getFilterItem();
		MatchingRule matching = getMatchingRuleFromRegistry(matchingRuleRegistry, filterItem);
		
		Item item = getObjectItem(object);
		
		if (item == null && getValues() == null) {
			return true;
		}
		
		if (item != null && !item.isEmpty() && getValues() == null){
			return false;
		}
		
		return true;
	}
	
	public String debugDump(int indent, StringBuilder sb){
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
	
	public String toString(StringBuilder sb){
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
	
}
