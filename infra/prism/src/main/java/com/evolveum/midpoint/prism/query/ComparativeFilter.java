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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ComparativeFilter<T extends Object> extends PropertyValueFilter<T> {

	private boolean equals;

	ComparativeFilter(@NotNull ItemPath path,
			@Nullable PrismPropertyDefinition<T> definition,
			@Nullable PrismPropertyValue<T> value,
			@Nullable ExpressionWrapper expression, @Nullable ItemPath rightHandSidePath,
			@Nullable ItemDefinition rightHandSideDefinition, boolean equals) {
		super(path, definition, null,
				value != null ? Collections.singletonList(value) : null,
				expression, rightHandSidePath, rightHandSideDefinition);
		this.equals = equals;
	}

	public boolean isEquals() {
		return equals;
	}

	public void setEquals(boolean equals) {
		this.equals = equals;
	}

	@Nullable
	static <T> PrismPropertyValue<T> anyValueToPropertyValue(@NotNull PrismContext prismContext, Object value) {
		List<PrismPropertyValue<T>> values = anyValueToPropertyValueList(prismContext, value);
		if (values.isEmpty()) {
			return null;
		} else if (values.size() > 1) {
			throw new UnsupportedOperationException("Comparative filter with more than one value is not supported");
		} else {
			return values.iterator().next();
		}
	}

	@Override
	public boolean equals(Object o, boolean exact) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		if (!super.equals(o, exact))
			return false;
		ComparativeFilter<?> that = (ComparativeFilter<?>) o;
		return equals == that.equals;
	}

	@Override
	public boolean match(PrismContainerValue value, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
		throw new UnsupportedOperationException("Matching object and greater/less filter is not supported yet");
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), equals);
	}
}
