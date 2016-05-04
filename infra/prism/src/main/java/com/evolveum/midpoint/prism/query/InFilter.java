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

import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;

@Deprecated			// deprecated because of confusing name/semantics; see https://wiki.evolveum.com/display/midPoint/Query+API+Evolution
public class InFilter<T> extends PropertyValueFilter<PrismPropertyValue> {
	
	InFilter(ItemPath path, PrismPropertyDefinition definition, QName matchingRule, List<PrismPropertyValue> values ) {
		super(path, definition, matchingRule, values);
	}
	
	public static <V> InFilter createIn(ItemPath path, PrismPropertyDefinition definition, QName matchingRule, V values){
		
		List<PrismPropertyValue<V>> pVals = createPropertyList(definition, values);
		
		return new InFilter(path, definition, matchingRule, pVals);
	}
	
	
	public static <V> InFilter createIn(ItemPath path, PrismPropertyDefinition definition, QName matchingRule, PrismPropertyValue<V>... values) {

		List<PrismPropertyValue<V>> pVals = createPropertyList(definition, values);

		return new InFilter(path, definition, matchingRule, pVals);
	}
	
	public static <V> InFilter createIn(ItemPath path, PrismPropertyDefinition definition, PrismPropertyValue<V>... values) {
		return createIn(path, definition, null, values);
	}
	
	public static <V> InFilter createIn(ItemPath path, Class type, PrismContext prismContext, QName matchingRule, V values) {

		PrismPropertyDefinition definition = (PrismPropertyDefinition) FilterUtils.findItemDefinition(path, type, prismContext);
		return createIn(path, definition, null, values);
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

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("IN: ");
		return debugDump(indent, sb);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("IN: ");
		return toString(sb);
	}

	@Override
	public InFilter clone() {
		return new InFilter(getFullPath(), getDefinition(),getMatchingRule(), getValues());
	}
	
	@Override
	public PrismPropertyDefinition getDefinition() {
		return (PrismPropertyDefinition) super.getDefinition();
	}

	@Override
	public boolean equals(Object obj, boolean exact) {
		return super.equals(obj, exact) && obj instanceof InFilter;
	}

}
