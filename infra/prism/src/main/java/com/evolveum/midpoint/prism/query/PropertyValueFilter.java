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

import java.util.*;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PropertyValueFilter<V extends PrismValue> extends ValueFilter implements Itemable {

	private ExpressionWrapper expression;
	private List<V> values;
	private ItemPath rightHandSidePath;							// alternative to "values"
	private ItemDefinition rightHandSideDefinition;				// optional (needed only if path points to extension item)

	/*
	 *  TODO clean up the right side path/definition mess
	 */

	PropertyValueFilter(@NotNull ItemPath path, @Nullable ItemDefinition definition, @Nullable QName matchingRule,
			@NotNull ItemPath rightHandSidePath, @Nullable ItemDefinition rightHandSideDefinition) {
		super(path, definition, matchingRule);
		this.rightHandSidePath = rightHandSidePath;
		this.rightHandSideDefinition = rightHandSideDefinition;
	}

	protected PropertyValueFilter(@NotNull ItemPath path, @Nullable ItemDefinition definition, @Nullable QName matchingRule,
			@Nullable List<V> values, @Nullable ExpressionWrapper expression) {
		super(path, definition, matchingRule);
		if (values != null) {
			for (V value : values) {
				value.setParent(this);
			}
		}
		this.values = values;
		this.expression = expression;
	}
	
	PropertyValueFilter(@NotNull ItemPath path, @Nullable ItemDefinition definition, @Nullable QName matchingRule, @Nullable List<V> values) {
		this(path, definition, matchingRule, values, null);
	}
	
	PropertyValueFilter(@NotNull ItemPath path, @Nullable ItemDefinition definition, @Nullable V value) {
		this(path, definition, (QName) null, value != null ? Collections.singletonList(value) : null);
	}
	
	PropertyValueFilter(@NotNull ItemPath path, @Nullable ItemDefinition definition, @Nullable QName matchingRule) {
		this(path, definition, matchingRule, (List<V>) null);
	}
	
	PropertyValueFilter(@NotNull ItemPath path, @Nullable ItemDefinition definition, @Nullable ExpressionWrapper expression) {
		this(path, definition, null, null, expression);
	}
	
	PropertyValueFilter(@NotNull ItemPath path, @Nullable ItemDefinition definition, @Nullable ExpressionWrapper expression, @Nullable List<V> values) {
		this(path, definition, null, values, expression);
	}
	
	PropertyValueFilter(ItemPath path, ItemDefinition definition, QName matchingRule, ExpressionWrapper expression) {
		this(path, definition, matchingRule, null, expression);
	}
	
	static protected <T> List<PrismPropertyValue<T>> createPropertyList(@NotNull PrismPropertyDefinition itemDefinition, @Nullable PrismPropertyValue<T> pValue) {
		List<PrismPropertyValue<T>> pValues = new ArrayList<>();
		if (pValue == null) {
			return pValues;
		} else {
			PrismUtil.recomputePrismPropertyValue(pValue, itemDefinition.getPrismContext());
			pValues.add(pValue);
			return pValues;
		}
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

	static List<PrismPropertyValue<?>> createPropertyListFromArray(PrismContext prismContext, Object[] values) {
		List<PrismPropertyValue<?>> pVals = new ArrayList<>();
		if (values != null) {
			for (Object value : values) {
				addToPrismValues(pVals, prismContext, value);
			}
		}
		return pVals;
	}

//	static <T> List<PrismPropertyValue<T>> createPropertyListFromCollection(PrismContext prismContext, Collection<T> realValues) {
//		List<PrismPropertyValue<T>> pVals = new ArrayList<>();
//		for (T realValue : realValues) {
//			addToPrismValues(pVals, realValue, prismContext);
//		}
//		return pVals;
//	}

	private static void addToPrismValues(List<PrismPropertyValue<?>> pVals, PrismContext prismContext, Object value) {
		if (value == null) {
			return;
		}
		if (value instanceof Collection) {
			for (Object o : (Collection) value) {
				addToPrismValues(pVals, prismContext, o);
			}
			return;
		}
		if (value.getClass().isArray()) {
			throw new IllegalStateException("Array within array in filter creation: " + value);
		}

		PrismPropertyValue<?> pVal;
		if (value instanceof PrismPropertyValue) {
			pVal = (PrismPropertyValue<?>) value;
			if (pVal.getParent() != null) {
				pVal = pVal.clone();
			}
		} else {
			pVal = new PrismPropertyValue<>(value);
		}
		PrismUtil.recomputePrismPropertyValue(pVal, prismContext);
		pVals.add(pVal);
	}

	static <T> List<PrismPropertyValue<T>> realValueToPropertyList(PrismPropertyDefinition itemDefinition, T realValue) {
		List<PrismPropertyValue<T>> pVals = new ArrayList<>();
		if (realValue == null) {
			return pVals;
		}

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
	
	public List<V> getValues() {
		return values;
	}

	public V getSingleValue() {
		if (values == null || values.isEmpty()) {
			return null;
		}
		if (values.size() > 1) {
			throw new IllegalArgumentException("Filter '" + this + "' should contain at most one value, but it has " + values.size() + " of them.");
		}
		return values.iterator().next();
	}
	
	public void setValue(V value) {
		List<V> values = new ArrayList<V>();
		if (value != null) {
			value.setParent(this);
			values.add(value);
		}
		this.values = values;
	}
	
	protected void cloneValues(PropertyValueFilter<V> clone) {
		clone.values = getCloneValuesList();
		if (clone.values != null) {
			for (V clonedValue: clone.values) {
				clonedValue.setParent(clone);
			}
		}
	}
	
	protected List<V> getCloneValuesList() {
		if (values == null) {
			return null;
		}
		List<V> clonedValues = new ArrayList<V>(values.size());
		for(V value: values) {
			clonedValues.add((V) value.clone());
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
	public void checkConsistence(boolean requireDefinitions) {
		super.checkConsistence(requireDefinitions);
		if (values == null) {
			return; // this is OK, searching for item with no value
		}
		for (V value: values) {
			if (value.getParent() != this) {
				throw new IllegalArgumentException("Value "+value+" in "+this+" has a bad parent "+value.getParent());
			}
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
			for (V value: values) {
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
		
		List<V> values = getValues();
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
