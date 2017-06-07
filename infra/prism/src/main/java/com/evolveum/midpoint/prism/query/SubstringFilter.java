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
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public class SubstringFilter<T> extends PropertyValueFilter<T> {

	private boolean anchorStart;
	private boolean anchorEnd;

	public SubstringFilter(@NotNull ItemPath path, @Nullable PrismPropertyDefinition<T> definition,
			@Nullable QName matchingRule,
			@Nullable List<PrismPropertyValue<T>> prismPropertyValues,
			@Nullable ExpressionWrapper expression, boolean anchorStart, boolean anchorEnd) {
		super(path, definition, matchingRule, prismPropertyValues, expression, null, null);
		this.anchorStart = anchorStart;
		this.anchorEnd = anchorEnd;
	}

	/**
	 * Creates a substring filter.
	 *
	 * @param itemDefinition TODO about nullability
	 * @param anyValue real value or prism property value; TODO about nullability
	 */
	public static <T> SubstringFilter<T> createSubstring(@NotNull ItemPath path, @Nullable PrismPropertyDefinition<T> itemDefinition,
														 @NotNull PrismContext prismContext,
														 @Nullable QName matchingRule, Object anyValue, boolean anchorStart, boolean anchorEnd) {
		List<PrismPropertyValue<T>> values = anyValueToPropertyValueList(prismContext, anyValue);
		return new SubstringFilter<>(path, itemDefinition, matchingRule, values, null, anchorStart, anchorEnd);
	}

	// TODO expression based substring filter

	public boolean isAnchorStart() {
		return anchorStart;
	}

	public boolean isAnchorEnd() {
		return anchorEnd;
	}

    @Override
	public SubstringFilter<T> clone() {
		return new SubstringFilter<>(getFullPath(), getDefinition(), getMatchingRule(), getClonedValues(),
				getExpression(), anchorStart, anchorEnd);
	}

	@Override
	protected String getFilterName() {
		return "SUBSTRING("
				+ (anchorStart ? "S" : "")
				+ (anchorStart && anchorEnd ? "," : "")
				+ (anchorEnd ? "E" : "")
				+ ")";
	}

	@Override
	public boolean match(PrismContainerValue containerValue, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
		Item item = getObjectItem(containerValue);
		
		MatchingRule matching = getMatchingRuleFromRegistry(matchingRuleRegistry, item);
		
		for (Object val : item.getValues()) {
			if (val instanceof PrismPropertyValue) {
				Object value = ((PrismPropertyValue) val).getValue();
				for (Object o : toRealValues()) {
					if (o == null) {
						continue;			// shouldn't occur
					}
					StringBuilder sb = new StringBuilder();
					if (!anchorStart) {
						sb.append(".*");
					}
					sb.append(Pattern.quote(o.toString()));
					if (!anchorEnd) {
						sb.append(".*");
					}
					if (matching.matchRegex(value, sb.toString())) {
						return true;
					}
				}
			}
			if (val instanceof PrismReferenceValue) {
				throw new UnsupportedOperationException(
						"matching substring on the prism reference value not supported yet");
			}
		}
		
		return false;
	}

	private Set<T> toRealValues(){
		 return PrismPropertyValue.getRealValuesOfCollection(getValues());
	}

	@Override
	public boolean equals(Object o, boolean exact) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		if (!super.equals(o, exact))
			return false;
		SubstringFilter<?> that = (SubstringFilter<?>) o;
		return anchorStart == that.anchorStart &&
				anchorEnd == that.anchorEnd;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), anchorStart, anchorEnd);
	}
}
