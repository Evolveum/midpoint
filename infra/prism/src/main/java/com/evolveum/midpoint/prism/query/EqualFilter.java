/*
 * Copyright (c) 2010-2016 Evolveum
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

import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.Itemable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EqualFilter<T> extends PropertyValueFilter<PrismPropertyValue<T>> implements Itemable {
	private static final long serialVersionUID = 3284478412180258355L;
	
	public static final QName ELEMENT_NAME = new QName(PrismConstants.NS_QUERY, "equal");

	private EqualFilter(@NotNull ItemPath fullPath, PrismPropertyDefinition<T> definition, QName matchingRule,
			@NotNull ItemPath rightSidePath, ItemDefinition rightSideDefinition) {
		super(fullPath, definition, matchingRule, rightSidePath, rightSideDefinition);
	}

	private EqualFilter(@NotNull ItemPath fullPath, PrismPropertyDefinition<T> definition, QName matchingRule,
			List<PrismPropertyValue<T>> values, ExpressionWrapper expression) {
		super(fullPath, definition, matchingRule, values, expression);
	}

	//factory methods
	// Do not require definition. We may want queries for which the definition is supplied later.

	// right-side-related
	@NotNull
	public static <C extends Containerable, T> EqualFilter<T> createEqual(ItemPath propertyPath, PrismPropertyDefinition<T> propertyDefinition, QName matchingRule, ItemPath rightSidePath, ItemDefinition rightSideDefinition) {
		return new EqualFilter<>(propertyPath, propertyDefinition, matchingRule, rightSidePath, rightSideDefinition);
	}

	// expression-related
	@NotNull
	public static <T> EqualFilter<T> createEqual(@NotNull ItemPath path, @Nullable PrismPropertyDefinition<T> definition,
			@Nullable QName matchingRule, @Nullable ExpressionWrapper expression) {
		return new EqualFilter<>(path, definition, matchingRule, null, expression);
	}

	// Collection<PPV>
	@NotNull
	public static <T> EqualFilter<T> createEqual(@NotNull ItemPath path, @Nullable PrismPropertyDefinition<T> definition,
			@Nullable QName matchingRule, @NotNull List<PrismPropertyValue<T>> values) {
		return new EqualFilter<T>(path, definition, matchingRule, values, null);
	}

	@NotNull
	@SafeVarargs
	public static <T> EqualFilter<T> createEqualMultiple(@NotNull ItemPath path, @NotNull PrismPropertyDefinition<T> definition,
			@Nullable QName matchingRule, T... realValues) {
		return new EqualFilter<>(path, definition, matchingRule, createPropertyListFromArray(definition, realValues), null);
	}

	@NotNull
	public static <T> EqualFilter<T> createEqual(@NotNull ItemPath path, @NotNull PrismPropertyDefinition<T> definition,
			@Nullable QName matchingRule, @Nullable T realValue) {
		return new EqualFilter<>(path, definition, matchingRule, realValueToPropertyList(definition, realValue), null);
	}

	private static <C extends Containerable, T> EqualFilter<T> createEqual(@NotNull ItemPath propertyPath, @NotNull Class<C> type, @NotNull PrismContext prismContext,
			@Nullable QName matchingRule, T realValue) {
		@SuppressWarnings("unchecked")
		PrismPropertyDefinition<T> propertyDefinition = (PrismPropertyDefinition) FilterUtils.findItemDefinition(propertyPath, type, prismContext);
		return createEqual(propertyPath, propertyDefinition, matchingRule, realValue);
	}

	@NotNull
	public static <C extends Containerable, T> EqualFilter<T> createEqual(@NotNull ItemPath propertyPath, @NotNull Class<C> type, @NotNull PrismContext prismContext, T realValue)
			throws SchemaException {
		return createEqual(propertyPath, type, prismContext, null, realValue);
	}

	@NotNull
	public static <C extends Containerable, T> EqualFilter<T> createEqual(@NotNull QName propertyName, @NotNull Class<C> type, @NotNull PrismContext prismContext,
			QName matchingRule, T realValues) {
		return createEqual(new ItemPath(propertyName), type, prismContext, matchingRule, realValues);
	}

	@NotNull
	public static <C extends Containerable, T> EqualFilter<T> createEqual(@NotNull QName propertyName, @NotNull Class<C> type, @NotNull PrismContext prismContext, T realValue)
			throws SchemaException {
		return createEqual(propertyName, type, prismContext, null, realValue);
	}


	// taking values from existing item (item's values are cloned)
	@NotNull
	@Deprecated
	public static <T> EqualFilter<T> createEqual(@NotNull ItemPath path, @NotNull PrismProperty<T> item) {
		List<PrismPropertyValue<T>> clonedValues = (List<PrismPropertyValue<T>>) PrismPropertyValue.cloneCollection(item.getValues());
		return createEqual(path, item.getDefinition(), null, clonedValues);
	}


	public static <T> EqualFilter<T> createNullEqual(ItemPath itemPath, PrismPropertyDefinition<T> propertyDef, QName matchingRule){
		return new EqualFilter(itemPath, propertyDef, matchingRule, (List) null, null);
		
	}

    @Override
	public EqualFilter<T> clone() {
		EqualFilter<T> clone = new EqualFilter<>(getFullPath(), getDefinition(), getMatchingRule(), (List<PrismPropertyValue<T>>) getValues(), null);
		clone.setExpression(getExpression());
		cloneValues(clone);
		clone.copyRightSideThingsFrom(this);
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
		sb.append("EQUAL: ");
		return toString(sb);
	}

	@Override
	public PrismContext getPrismContext() {
		PrismPropertyDefinition<T> def = getDefinition();
		if (def == null) {
			return null;
		}
		return def.getPrismContext();
	}

	@Override
	public ItemPath getPath() {
		return getFullPath();
	}

	@Override
	public boolean match(PrismContainerValue cvalue, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
		Item filterItem = getFilterItem();
		if (!super.match(cvalue, matchingRuleRegistry)){
			return false;
		}

		Item objectItem = getObjectItem(cvalue);
		if (objectItem == null) {
			return true;
		}
		List<Object> values = objectItem.getValues();
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
			try {
				if (matchingRule.match(filterV.getValue(), v.getValue())){
					return true;
				}
			} catch (SchemaException e) {
				// At least one of the values is invalid. But we do not want to throw exception from
				// a comparison operation. That will make the system very fragile. Let's fall back to
				// ordinary equality mechanism instead.
				if (filterV.getValue().equals(v.getValue())) {
					return true;
				}
			}
		}
		
		return false;		
	}
	
	@Override
	public PrismPropertyDefinition<T> getDefinition(){
		return (PrismPropertyDefinition<T>) super.getDefinition();
	}
	
	@Override
	public List<PrismPropertyValue<T>> getValues() {
		return super.getValues();
	}

	@Override
	public boolean equals(Object obj, boolean exact) {
		return super.equals(obj, exact) && obj instanceof EqualFilter;
	}

}

