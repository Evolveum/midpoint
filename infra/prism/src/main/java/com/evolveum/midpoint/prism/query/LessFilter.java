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

import com.evolveum.midpoint.prism.ExpressionWrapper;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

public class LessFilter<T> extends ComparativeFilter<T> {

	LessFilter(@NotNull ItemPath path, @Nullable PrismPropertyDefinition<T> definition,
			@Nullable QName matchingRule,
			@Nullable PrismPropertyValue<T> value, @Nullable ExpressionWrapper expression,
			@Nullable ItemPath rightHandSidePath, @Nullable ItemDefinition rightHandSideDefinition, boolean equals) {
		super(path, definition, matchingRule, value, expression, rightHandSidePath, rightHandSideDefinition, equals);
	}

	// factory methods

	// empty (can be filled-in later)
	@NotNull
	public static <T> LessFilter<T> createLess(@NotNull ItemPath itemPath, PrismPropertyDefinition<T> definition, boolean equals) {
		return new LessFilter<>(itemPath, definition, null, null, null, null, null, equals);
	}

	// value
	@NotNull
	public static <T> LessFilter<T> createLess(@NotNull ItemPath itemPath, PrismPropertyDefinition<T> definition,
			QName matchingRule, Object anyValue, boolean equals, @NotNull PrismContext prismContext) {
		PrismPropertyValue<T> propertyValue = anyValueToPropertyValue(prismContext, anyValue);
		return new LessFilter<>(itemPath, definition, matchingRule, propertyValue, null, null, null, equals);
	}

	// expression-related
	@NotNull
	public static <T> LessFilter<T> createLess(@NotNull ItemPath itemPath, PrismPropertyDefinition<T> itemDefinition, QName matchingRule,
			@NotNull ExpressionWrapper expressionWrapper, boolean equals) {
		return new LessFilter<>(itemPath, itemDefinition, matchingRule, null, expressionWrapper, null, null, equals);
	}

	// right-side-related
	@NotNull
	public static <T> LessFilter<T> createLess(@NotNull ItemPath propertyPath, PrismPropertyDefinition<T> definition,
			QName matchingRule, @NotNull ItemPath rightSidePath, ItemDefinition rightSideDefinition, boolean equals) {
		return new LessFilter<>(propertyPath, definition, matchingRule, null, null, rightSidePath, rightSideDefinition, equals);
	}

	@SuppressWarnings("CloneDoesntCallSuperClone")
	@Override
	public LessFilter<T> clone() {
		return new LessFilter<>(getFullPath(), getDefinition(), getMatchingRule(), getClonedValue(), getExpression(),
				getRightHandSidePath(), getRightHandSideDefinition(), isEquals());
	}

	@Override
	protected String getFilterName() {
		return isEquals() ? "LESS-OR-EQUAL" : "LESS";
	}

	@Override
	public boolean equals(Object obj, boolean exact) {
		return obj instanceof LessFilter && super.equals(obj, exact);
	}
}
