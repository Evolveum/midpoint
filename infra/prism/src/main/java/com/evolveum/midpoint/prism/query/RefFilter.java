/*
 * Copyright (c) 2010-2017 Evolveum
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
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RefFilter extends ValueFilter<PrismReferenceValue, PrismReferenceDefinition> {
	private static final long serialVersionUID = 1L;

	// these are currently supported only by built-in match(..) method; e.g. the repo query interpreter simply ignores them
	private boolean oidNullAsAny = false;
	private boolean targetTypeNullAsAny = false;
	private boolean relationNullAsAny = false;          // currently not supported at all
	

	public RefFilter(@NotNull ItemPath fullPath, @Nullable PrismReferenceDefinition definition,
			@Nullable List<PrismReferenceValue> values, @Nullable ExpressionWrapper expression) {
		super(fullPath, definition, null, values, expression, null, null);
	}

	public static RefFilter createReferenceEqual(ItemPath path, PrismReferenceDefinition definition, Collection<PrismReferenceValue> values) {
		return new RefFilter(path, definition, values != null ? new ArrayList<>(values) : null, null);
	}
	
	public static RefFilter createReferenceEqual(ItemPath path, PrismReferenceDefinition definition, ExpressionWrapper expression) {
		return new RefFilter(path, definition, null, expression);
	}
		
	@SuppressWarnings("CloneDoesntCallSuperClone")
	@Override
	public RefFilter clone() {
		return new RefFilter(getFullPath(), getDefinition(), getClonedValues(), getExpression());
	}

	@Override
	protected String getFilterName() {
		return "REF";
	}

	@Override
	public boolean match(PrismContainerValue objectValue, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {

		Item filterItem = getFilterItem();
		Collection<PrismValue> objectItemValues = getObjectItemValues(objectValue);

		if (!super.match(objectValue, matchingRuleRegistry)) {
			return false;
		}

		boolean filterItemIsEmpty = getValues() == null || getValues().isEmpty();
		boolean objectItemIsEmpty = objectItemValues.isEmpty();
		if (filterItemIsEmpty && objectItemIsEmpty) {
			return true;
		}
		assert !filterItemIsEmpty;	// if both are empty, the previous statement causes 'return true'
		assert !objectItemIsEmpty;	// if only one of them is empty, the super.match() returned false

		for (Object filterItemValue : filterItem.getValues()) {
			checkPrismReferenceValue(filterItemValue);
			for (Object objectItemValue : objectItemValues) {
				checkPrismReferenceValue(objectItemValue);
				if (valuesMatch(((PrismReferenceValue) filterItemValue), (PrismReferenceValue) objectItemValue)) {
					return true;
				}
			}
		}
		return false;
	}

	private void checkPrismReferenceValue(Object value) {
		if (!(value instanceof PrismReferenceValue)) {
			throw new IllegalArgumentException("Not supported prism value for ref filter. It must be an instance of PrismReferenceValue but it is " + value.getClass());
		}
	}

	private boolean valuesMatch(PrismReferenceValue filterValue, PrismReferenceValue objectValue) {
		if (!matchOid(filterValue.getOid(), objectValue.getOid())) {
			return false;
		}
		if (!QNameUtil.match(PrismConstants.Q_ANY, filterValue.getRelation())) {
			// similar to relation-matching code in PrismReferenceValue (but awkward to unify, so keeping separate)
			PrismContext prismContext = getPrismContext();
			QName objectRelation = objectValue.getRelation();
			QName filterRelation = filterValue.getRelation();
			if (prismContext != null) {
				if (objectRelation == null) {
					objectRelation = prismContext.getDefaultRelation();
				}
				if (filterRelation == null) {
					filterRelation = prismContext.getDefaultRelation();
				}
			}
			if (!QNameUtil.match(filterRelation, objectRelation)) {
				return false;
			}
		}
		if (!matchTargetType(filterValue.getTargetType(), objectValue.getTargetType())) {
			return false;
		}
		return true;
	}

	private boolean matchOid(String filterOid, String objectOid) {
		return oidNullAsAny && filterOid == null || objectOid != null && objectOid.equals(filterOid);
	}

	private boolean matchTargetType(QName filterType, QName objectType) {
		return targetTypeNullAsAny && filterType == null || QNameUtil.match(objectType, filterType);

	}
	
	@Override
	public boolean equals(Object obj, boolean exact) {
		return obj instanceof RefFilter && super.equals(obj, exact);
	}
	
	public void setOidNullAsAny(boolean oidNullAsAny) {
		this.oidNullAsAny = oidNullAsAny;
	}
	
	public void setTargetTypeNullAsAny(boolean targetTypeNullAsAny) {
		this.targetTypeNullAsAny = targetTypeNullAsAny;
	}
	
	public void setRelationNullAsAny(boolean relationNullAsAny) {
		this.relationNullAsAny = relationNullAsAny;
	}

}
