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

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class GreaterFilter<T> extends ComparativeFilter<T> {
	

	public GreaterFilter() {
	}
	
	GreaterFilter(ItemPath itemPath, PrismPropertyDefinition definition, PrismPropertyValue<T> value, boolean equals) {
		super(itemPath, definition, value, equals);
	}

	GreaterFilter(ItemPath itemPath, PrismPropertyDefinition<T> definition, ItemPath rightSidePath, ItemDefinition rightSideDefinition, boolean equals) {
		super(itemPath, definition, rightSidePath, rightSideDefinition, equals);
	}
	
	public static <T, O extends Objectable> GreaterFilter<T> createGreater(ItemPath itemPath, PrismPropertyDefinition definition, PrismPropertyValue<T> value, boolean equals){
		GreaterFilter<T> greaterFilter = new GreaterFilter<>(itemPath, definition, value, equals);
		if (value != null) {
			value.setParent(greaterFilter);
		}
		return greaterFilter;
	}
	
	public static <T, C extends Containerable> GreaterFilter<T> createGreater(ItemPath itemPath, PrismContainerDefinition<C> containerDef,
			PrismPropertyValue<T> value, boolean equals) throws SchemaException {
		PrismPropertyDefinition def = (PrismPropertyDefinition) FilterUtils.findItemDefinition(itemPath, containerDef);
		GreaterFilter<T> greaterFilter = createGreater(itemPath, def, value, equals);
		if (value != null) {
			value.setParent(greaterFilter);
		}
		return greaterFilter;
	}

	public static <T> GreaterFilter<T> createGreater(ItemPath itemPath, PrismPropertyDefinition itemDefinition, T realValue, boolean equals) throws SchemaException{
		PrismPropertyValue<T> value = createPropertyValue(itemDefinition, realValue);
		
		if (value == null){
			//TODO: create null
		}
		
		GreaterFilter<T> greaterFilter =  createGreater(itemPath, itemDefinition, value, equals);
		value.setParent(greaterFilter);
		return greaterFilter;
	}

	public static <T, C extends Containerable> GreaterFilter<T> createGreater(ItemPath itemPath, PrismContainerDefinition<C> containerDef,
			T realValue, boolean equals) throws SchemaException {
		PrismPropertyDefinition def = (PrismPropertyDefinition) FilterUtils.findItemDefinition(itemPath, containerDef);
		return createGreater(itemPath, def, realValue, equals);
	}

	public static <T, O extends Objectable> GreaterFilter createGreater(QName propertyName, Class<O> type, PrismContext prismContext, T realValue, boolean equals)
			throws SchemaException {
		return createGreater(new ItemPath(propertyName), type, prismContext, realValue, equals);
	}
	
	public static <T, O extends Objectable> GreaterFilter createGreater(ItemPath path, Class<O> type, PrismContext prismContext, T realValue, boolean equals)
			throws SchemaException {
	
		PrismPropertyDefinition def = (PrismPropertyDefinition) FilterUtils.findItemDefinition(path, type, prismContext);
		
		return createGreater(path, def, realValue, equals);
	}

	public static <C extends Containerable, T> GreaterFilter<T> createGreater(ItemPath propertyPath, PrismPropertyDefinition propertyDefinition, ItemPath rightSidePath, ItemDefinition rightSideDefinition, boolean equals) {
		return new GreaterFilter(propertyPath, propertyDefinition, rightSidePath, rightSideDefinition, equals);
	}
	
	@Override
	public GreaterFilter<T> clone() {
		PrismPropertyValue<T> clonedValue = null;
		PrismPropertyValue<T> value = getSingleValue();
		if (value != null) {
			clonedValue = value.clone();
		}
		GreaterFilter<T> clone = new GreaterFilter<>(getFullPath(), getDefinition(), clonedValue, isEquals());
		if (clonedValue != null) {
			clonedValue.setParent(clone);
		}
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
		sb.append("GREATER:");
		return debugDump(indent, sb);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("GREATER: ");
		return toString(sb);
	}

	@Override
	public boolean match(PrismContainerValue value, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
		throw new UnsupportedOperationException("Matching object and greater filter not supported yet");
	}
	
	@Override
	public PrismPropertyDefinition getDefinition() {
		return (PrismPropertyDefinition) super.getDefinition();
	}

	@Override
	public QName getElementName() {
		return getDefinition().getName();
	}

	@Override
	public PrismContext getPrismContext() {
		return getDefinition().getPrismContext();
	}

	@Override
	public ItemPath getPath() {
		return getFullPath();
	}

	public static <T> GreaterFilter<T> createGreaterThanItem(ItemPath itemPath, PrismPropertyDefinition propertyDefinition, ItemPath rightSidePath, ItemDefinition rightSideDefinition, boolean equals) {
		return new GreaterFilter<>(itemPath, propertyDefinition, rightSidePath, rightSideDefinition, equals);
	}

	@Override
	public boolean equals(Object obj, boolean exact) {
		return super.equals(obj, exact) && obj instanceof GreaterFilter;
	}

}
