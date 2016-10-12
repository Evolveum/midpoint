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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RefFilter extends PropertyValueFilter<PrismReferenceValue> {
	private static final long serialVersionUID = 1L;

	private RefFilter(ItemPath path, PrismReferenceDefinition definition, ExpressionWrapper expression, List<PrismReferenceValue> values) {
		super(path, definition, expression, values);
	}
		
	private RefFilter(ItemPath path, PrismReferenceDefinition definition, ExpressionWrapper expression) {
		super(path, definition, expression, null);
	}

	public static RefFilter createReferenceEqual(ItemPath path, PrismReferenceDefinition definition, Collection<PrismReferenceValue> values) {
		return new RefFilter(path, definition, null, values != null ? new ArrayList<>(values) : null);
	}
	
	public static RefFilter createReferenceEqual(ItemPath path, PrismReferenceDefinition definition, ExpressionWrapper expression) {
		return new RefFilter(path, definition, expression);
	}
		
	@Override
	public RefFilter clone() {
		return new RefFilter(getFullPath(), getDefinition(), getExpression(), getCloneValuesList());
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("REF:");
		
		return debugDump(indent, sb);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("REF: ");
		return toString(sb);
	}

	@Override
	public boolean match(PrismContainerValue value, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {

		Item filterItem = getFilterItem();
		Item objectItem = getObjectItem(value);

		if (!super.match(value, matchingRuleRegistry)) {
			return false;
		}

		boolean filterItemIsEmpty = getValues() == null || getValues().isEmpty();
		boolean objectItemIsEmpty = objectItem == null || objectItem.isEmpty();

		if (filterItemIsEmpty && objectItemIsEmpty) {
			return true;
		}

		assert !filterItemIsEmpty;	// if both are empty, the previous statement causes 'return true'
		assert !objectItemIsEmpty;	// if only one of them is empty, the super.match() returnsed false

		List<Object> objectValues = objectItem.getValues();
		for (Object v : objectValues) {
			if (!(v instanceof PrismReferenceValue)) {
				throw new IllegalArgumentException("Not supported prism value for ref equals filter. It must be an instance of PrismReferenceValue but it is " + v.getClass());
			}
			if (!isInFilterItem((PrismReferenceValue) v, filterItem)){
				return false;
			}
		}

		return true;
	}

	private boolean isInFilterItem(PrismReferenceValue v, Item filterItem) {
		for (Object filterValue : filterItem.getValues()) {
			if (!(filterValue instanceof PrismReferenceValue)) {
				throw new IllegalArgumentException("Not supported prism value for ref equals filter. It must be an instance of PrismReferenceValue but it is " + v.getClass());
			}
			PrismReferenceValue filterRV = (PrismReferenceValue) filterValue;
			if (filterRV.getOid().equals(v.getOid())) {
				return true;
			}
			// TODO compare relation and target type as well (see repo implementation in ReferenceRestriction)
		}
		return false;
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
	public PrismReferenceDefinition getDefinition() {
		// TODO Auto-generated method stub
		return (PrismReferenceDefinition) super.getDefinition();
	}

	@Override
	public boolean equals(Object obj, boolean exact) {
		return super.equals(obj, exact) && obj instanceof RefFilter;
	}

}
