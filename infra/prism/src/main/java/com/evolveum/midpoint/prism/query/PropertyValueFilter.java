/*
 * Copyright (c) 2010-2015 Evolveum
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

import com.evolveum.midpoint.prism.PrismContainerValue;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Itemable;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public abstract class PropertyValueFilter<T extends PrismValue> extends ValueFilter implements Itemable {

	private ExpressionWrapper expression;
	private List<T> values;
	private ItemPath rightHandSidePath;							// alternative to "values"
	private ItemDefinition rightHandSideDefinition;				// optional (needed only if path points to extension item)

	/*
	 *  TODO clean up the right side path/definition mess
	 */

	PropertyValueFilter() {
		super();
	}

	PropertyValueFilter(ItemPath path, ItemDefinition definition, QName matchingRule, ItemPath rightHandSidePath, ItemDefinition rightHandSideDefinition) {
		super(path, definition, matchingRule);
		Validate.notNull(rightHandSidePath, "rightHandSidePath");
		this.rightHandSidePath = rightHandSidePath;
		this.rightHandSideDefinition = rightHandSideDefinition;
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
	
	PropertyValueFilter(ItemPath path, ItemDefinition definition, ExpressionWrapper expression) {
		super(path, definition);
		this.expression = expression;
	}
	PropertyValueFilter(ItemPath path, ItemDefinition definition, ExpressionWrapper expression, List<T> values) {
		super(path, definition);
		this.values = values;
		this.expression = expression;
	}
	
	PropertyValueFilter(ItemPath path, ItemDefinition definition, QName matchingRule, ExpressionWrapper expression) {
		super(path, definition, matchingRule);
		this.expression = expression;
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

	static <T> List<PrismPropertyValue<T>> createPropertyListFromArray(PrismPropertyDefinition itemDefinition, T... realValues) {
		List<PrismPropertyValue<T>> pVals = new ArrayList<PrismPropertyValue<T>>();

		for (T realValue : realValues) {
			if (realValue instanceof PrismPropertyValue) {
				PrismPropertyValue<T> pVal = (PrismPropertyValue<T>) realValue;
				PrismUtil.recomputePrismPropertyValue(pVal, itemDefinition.getPrismContext());
				pVals.add(pVal);
			} else {
				pVals.add(new PrismPropertyValue<>(realValue));
			}
		}
		return pVals;
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
					// TODO what's this???
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

	public T getSingleValue() {
		if (values == null || values.isEmpty()) {
			return null;
		}
		if (values.size() > 1) {
			throw new IllegalArgumentException("Filter '" + this + "' should contain at most one value, but it has " + values.size() + " of them.");
		}
		return values.iterator().next();
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
	
	public Item getObjectItem(PrismContainerValue value){
		ItemPath path = getFullPath();
		return value.findItem(path);
	}
	
	public Item getFilterItem() throws SchemaException{

		if (getDefinition() == null){
			throw new SchemaException("Could not find definition for item " + getPath());
		}
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

	public ItemPath getRightHandSidePath() {
		return rightHandSidePath;
	}

	public ItemDefinition getRightHandSideDefinition() {
		return rightHandSideDefinition;
	}

	public void setRightHandSidePath(ItemPath rightHandSidePath) {
		this.rightHandSidePath = rightHandSidePath;
		if (rightHandSidePath != null) {
			values = null;
		}
	}

	public void setRightHandSideDefinition(ItemDefinition rightHandSideDefinition) {
		this.rightHandSideDefinition = rightHandSideDefinition;
	}

	public ExpressionWrapper getExpression() {
		return expression;
	}

	public void setExpression(ExpressionWrapper expression) {
		this.expression = expression;
	}
	
	@Override
	public void checkConsistence() {
		if (values == null) {
			return; // this is OK, searching for item with no value
		}
		for (T value: values) {
			if (value == null) {
				throw new IllegalArgumentException("Null value in "+this);
			}
			if (value.isEmpty() && !value.isRaw()) {
				throw new IllegalArgumentException("Empty value in "+this);
			}
		}
	}

	@Override
	public boolean isRaw() {
		if (values != null) {
			for (T value: values) {
				if (value.isRaw()) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean match(PrismContainerValue cvalue, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {

		Item item = getObjectItem(cvalue);

		boolean filterItemIsEmpty = getValues() == null || getValues().isEmpty();
		boolean objectItemIsEmpty = item == null || item.isEmpty();

		if (filterItemIsEmpty && !objectItemIsEmpty) {
			return false;
		}

		if (!filterItemIsEmpty && objectItemIsEmpty) {
			return false;
		}
		
		return true;
	}

	@Override
	public boolean equals(Object o) {
		return equals(o, true);
	}

	@Override
	public boolean equals(Object o, boolean exact) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		if (!super.equals(o, exact))
			return false;

		PropertyValueFilter<?> that = (PropertyValueFilter<?>) o;

		if (expression != null ? !expression.equals(that.expression) : that.expression != null)
			return false;
		if (values != null ? !values.equals(that.values) : that.values != null)
			return false;
		if (rightHandSidePath != null ? !rightHandSidePath.equals(that.rightHandSidePath, exact) : that.rightHandSidePath != null)
			return false;
		if (exact) {
			return rightHandSideDefinition != null ?
					rightHandSideDefinition.equals(that.rightHandSideDefinition) :
					that.rightHandSideDefinition == null;
		} else {
			return true;
		}
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (expression != null ? expression.hashCode() : 0);
		result = 31 * result + (values != null ? values.hashCode() : 0);
		result = 31 * result + (rightHandSidePath != null ? rightHandSidePath.hashCode() : 0);
		result = 31 * result + (rightHandSideDefinition != null ? rightHandSideDefinition.hashCode() : 0);
		return result;
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
		
		List<T> values = getValues();
		if (values != null) {
			sb.append("\n");
			DebugUtil.indentDebugDump(sb, indent+1);
			sb.append("VALUE:");
			sb.append("\n");
			for (PrismValue val : getValues()) {
				sb.append(DebugUtil.debugDump(val, indent + 2));
			}
		}

        ExpressionWrapper expression = getExpression();
		if (expression != null && expression.getExpression() != null) {
			sb.append("\n");
			DebugUtil.indentDebugDump(sb, indent+1);
			sb.append("EXPRESSION:");
			sb.append("\n");
			sb.append(DebugUtil.debugDump(expression.getExpression(), indent + 2));
		}

		if (getRightHandSidePath() != null) {
			sb.append("\n");
			DebugUtil.indentDebugDump(sb, indent+1);
			sb.append("RIGHT SIDE PATH: ");
			sb.append(getFullPath().toString());
			sb.append("\n");
			DebugUtil.indentDebugDump(sb, indent+1);
			sb.append("RIGHT SIDE DEF: ");
			if (getRightHandSideDefinition() != null) {
				sb.append(getRightHandSideDefinition().toString());
			} else {
				sb.append("null");
			}
		}

		QName matchingRule = getMatchingRule();
		if (matchingRule != null) {
			sb.append("\n");
			DebugUtil.indentDebugDump(sb, indent+1);
			sb.append("MATCHING: ");
			sb.append(matchingRule);
		}
		
		return sb.toString();
	}
	
	public String toString(StringBuilder sb){
		if (getFullPath() != null){
			sb.append(getFullPath().toString());
			sb.append(",");
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
					sb.append(",");
				}
			}
		}
		if (rightHandSidePath != null) {
			sb.append(getRightHandSidePath());
		}
		return sb.toString();
	}

	// TODO cleanup this mess - how values are cloned, that expression is not cloned in LT/GT filter etc

	public abstract PropertyValueFilter clone();

	protected void copyRightSideThingsFrom(PropertyValueFilter original) {
		if (original.getRightHandSidePath() != null) {
			setRightHandSidePath(original.getRightHandSidePath());
		}
		if (original.getRightHandSideDefinition() != null) {
			setRightHandSideDefinition(original.getRightHandSideDefinition());
		}
	}
}
