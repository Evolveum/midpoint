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

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class GreaterFilter<T> extends ComparativeFilter<T> {
	

	public GreaterFilter() {
	}
	
	GreaterFilter(ItemPath parentPath, PrismPropertyDefinition definition, PrismPropertyValue<T> value, boolean equals) {
		super(parentPath, definition, value, equals);
	}
	
	public static <T, O extends Objectable> GreaterFilter createGreater(ItemPath parentPath, PrismPropertyDefinition definition, PrismPropertyValue<T> value, boolean equals){
		return new GreaterFilter(parentPath, definition, value, equals);
	}
	
	public static <T, C extends Containerable> GreaterFilter createGreater(ItemPath parentPath, PrismContainerDefinition<C> containerDef,
			PrismPropertyValue<T> value, boolean equals) throws SchemaException {
		PrismPropertyDefinition def = (PrismPropertyDefinition) FilterUtils.findItemDefinition(parentPath, containerDef);
		return createGreater(parentPath, def, value, equals);
	}

	public static <T> GreaterFilter createGreater(ItemPath parentPath, PrismPropertyDefinition itemDefinition, T realValue, boolean equals) throws SchemaException{
		PrismPropertyValue<T> value = createPropertyValue(itemDefinition, realValue);
		
		if (value == null){
			//TODO: create null
		}
		
		return createGreater(parentPath, itemDefinition, value, equals);
	}

	public static <T, C extends Containerable> GreaterFilter createGreater(ItemPath parentPath, PrismContainerDefinition<C> containerDef,
			T realValue, boolean equals) throws SchemaException {
		PrismPropertyDefinition def = (PrismPropertyDefinition) FilterUtils.findItemDefinition(parentPath, containerDef);
		return createGreater(parentPath, def, realValue, equals);
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
	
	@Override
	public GreaterFilter clone() {
		return new GreaterFilter(getFullPath(), getDefinition(), (PrismPropertyValue<T>) getValues().get(0), isEquals());
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

}
