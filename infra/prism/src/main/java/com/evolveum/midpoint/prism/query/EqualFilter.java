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
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class EqualFilter<T> extends PropertyValueFilter<T> implements Itemable {
	private static final long serialVersionUID = 3284478412180258355L;
	
	public static final QName ELEMENT_NAME = new QName(PrismConstants.NS_QUERY, "equal");

	/*
	 *  The pattern for factory methods and constructors signatures is:
	 *   - path and definition
	 *   - matching rule (if applicable)
	 *   - values (incl. prismContext if needed)
	 *   - expressionWrapper
	 *   - right hand things
	 *   - filter-specific flags (equal, anchors)
	 *
	 *  Ordering of methods:
	 *   - constructor
	 *   - factory methods: [null], value(s), expression, right-side
	 *   - match
	 *   - equals
	 *
	 *  Parent for prism values is set in the appropriate constructor; so there's no need to do that at other places.
	 *
	 *  Normalization of "Object"-typed values is done in anyArrayToXXX and anyValueToXXX methods. This includes cloning
	 *  of values that have a parent (note that we recompute the PolyString values as part of conversion process; if that's
	 *  a problem for the client, it has to do cloning itself).
	 *
	 *  Please respect these conventions in order to make these classes understandable and maintainable.
	 */

	public EqualFilter(@NotNull ItemPath path, @Nullable PrismPropertyDefinition<T> definition,
			@Nullable QName matchingRule,
			@Nullable List<PrismPropertyValue<T>> prismPropertyValues,
			@Nullable ExpressionWrapper expression, @Nullable ItemPath rightHandSidePath,
			@Nullable ItemDefinition rightHandSideDefinition) {
		super(path, definition, matchingRule, prismPropertyValues, expression, rightHandSidePath, rightHandSideDefinition);
	}

	// factory methods

	// empty (different from values as it generates filter with null 'values' attribute)
	@NotNull
	public static <T> EqualFilter<T> createEqual(@NotNull ItemPath path, @Nullable PrismPropertyDefinition<T> definition,
			@Nullable QName matchingRule) {
		return new EqualFilter<>(path, definition, matchingRule, null, null, null, null);
	}

	// values
	@NotNull
	public static <T> EqualFilter<T> createEqual(@NotNull ItemPath path, @Nullable PrismPropertyDefinition<T> definition,
			@Nullable QName matchingRule, @NotNull PrismContext prismContext, Object... values) {
		List<PrismPropertyValue<T>> propertyValues = anyArrayToPropertyValueList(prismContext, values);
		return new EqualFilter<>(path, definition, matchingRule, propertyValues, null, null, null);
	}

	// expression-related
	@NotNull
	public static <T> EqualFilter<T> createEqual(@NotNull ItemPath path, @Nullable PrismPropertyDefinition<T> definition,
			@Nullable QName matchingRule, @NotNull ExpressionWrapper expression) {
		return new EqualFilter<>(path, definition, matchingRule, null, expression, null, null);
	}

	// right-side-related; right side can be supplied later (therefore it's nullable)
	@NotNull
	public static <T> EqualFilter<T> createEqual(@NotNull ItemPath propertyPath, PrismPropertyDefinition<T> propertyDefinition,
			QName matchingRule, @NotNull ItemPath rightSidePath, ItemDefinition rightSideDefinition) {
		return new EqualFilter<>(propertyPath, propertyDefinition, matchingRule, null, null, rightSidePath, rightSideDefinition);
	}

    @SuppressWarnings("CloneDoesntCallSuperClone")
	@Override
	public EqualFilter<T> clone() {
		return new EqualFilter<>(getFullPath(), getDefinition(), getMatchingRule(), getClonedValues(),
				getExpression(), getRightHandSidePath(), getRightHandSideDefinition());
	}

	@Override
	protected String getFilterName() {
		return "EQUAL";
	}

	@Override
	public boolean match(PrismContainerValue objectValue, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
		if (!super.match(objectValue, matchingRuleRegistry)){
			return false;
		}
		Collection<PrismValue> objectItemValues = getObjectItemValues(objectValue);
		if (objectItemValues.isEmpty()) {
			return true;					// because filter item is empty as well (checked by super.match)
		}
		Item filterItem = getFilterItem();
		MatchingRule<?> matchingRule = getMatchingRuleFromRegistry(matchingRuleRegistry, filterItem);
		for (Object filterItemValue : filterItem.getValues()) {
			checkPrismPropertyValue(filterItemValue);
			for (Object objectItemValue : objectItemValues) {
				checkPrismPropertyValue(objectItemValue);
				if (matches((PrismPropertyValue<?>) filterItemValue, (PrismPropertyValue<?>) objectItemValue, matchingRule)) {
					return true;
				}
			}
		}
		return false;
	}

	private void checkPrismPropertyValue(Object value) {
		if (!(value instanceof PrismPropertyValue)) {
			throw new IllegalArgumentException("Not supported prism value for equals filter. It must be an instance of PrismPropertyValue but it is " + value.getClass());
		}
	}

	private boolean matches(PrismPropertyValue<?> value1, PrismPropertyValue<?> value2, MatchingRule<?> matchingRule) {
		try {
			if (matchingRule.match(value1.getRealValue(), value2.getRealValue())) {
				return true;
			}
		} catch (SchemaException e) {
			// At least one of the values is invalid. But we do not want to throw exception from
			// a comparison operation. That will make the system very fragile. Let's fall back to
			// ordinary equality mechanism instead.
			if (Objects.equals(value1.getRealValue(), value2.getRealValue())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean equals(Object obj, boolean exact) {
		return obj instanceof EqualFilter && super.equals(obj, exact);
	}

}

