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

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class SubstringFilter<T> extends PropertyValueFilter<PrismPropertyValue<T>> {

	private boolean anchorStart;
	private boolean anchorEnd;

	SubstringFilter(ItemPath parentPath, ItemDefinition definition, QName matchingRule, List<PrismPropertyValue<T>> values, boolean anchorStart, boolean anchorEnd) {
		super(parentPath, definition, matchingRule, values);
		this.anchorStart = anchorStart;
		this.anchorEnd = anchorEnd;
	}

	/**
	 * Creates a substring filter.
	 *
	 * @param itemDefinition TODO about nullability
	 * @param value real value or prism property value; TODO about nullability
	 */
	public static <T> SubstringFilter<T> createSubstring(@NotNull ItemPath path, @Nullable PrismPropertyDefinition<T> itemDefinition,
														 @NotNull PrismContext prismContext,
														 @Nullable QName matchingRule, Object value, boolean anchorStart, boolean anchorEnd) {
		List<PrismPropertyValue<T>> pValues = createPropertyListFromValue(prismContext, value);
		return new SubstringFilter<T>(path, itemDefinition, matchingRule, pValues, anchorStart, anchorEnd);
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
		return new SubstringFilter<>(getFullPath(), getDefinition(), getMatchingRule(), getCloneValuesList(), anchorStart, anchorEnd);
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("SUBSTRING:");
		if (anchorStart) {
			sb.append(" anchorStart");
		}
		if (anchorEnd) {
			sb.append(" anchorEnd");
		}
		return debugDump(indent, sb);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("SUBSTRING: ");
		String rv = toString(sb);
		if (anchorStart) {
			rv += ",S";
		}
		if (anchorEnd) {
			rv += ",E";
		}
		return rv;
	}

	@Override
	public boolean match(PrismContainerValue containerValue, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
		Item item = getObjectItem(containerValue);
		
		MatchingRule matching = getMatchingRuleFromRegistry(matchingRuleRegistry, item);
		
		for (Object val : item.getValues()){
			if (val instanceof PrismPropertyValue){
				Object value = ((PrismPropertyValue) val).getValue();
				Iterator<String> iterator = (Iterator<String>) toRealValues().iterator();
				while(iterator.hasNext()){
					StringBuilder sb = new StringBuilder();
					if (!anchorStart) {
						sb.append(".*");
					}
					sb.append(Pattern.quote(iterator.next()));
					if (!anchorEnd) {
						sb.append(".*");
					}
					if (matching.matchRegex(value, sb.toString())){
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
	public PrismContext getPrismContext() {
		return getDefinition().getPrismContext();
	}

	@Override
	public ItemPath getPath() {
		return getFullPath();
	}
	
	@Override
	public PrismPropertyDefinition<T> getDefinition() {
		return (PrismPropertyDefinition<T>) super.getDefinition();
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

		if (anchorStart != that.anchorStart)
			return false;
		return anchorEnd == that.anchorEnd;

	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (anchorStart ? 1 : 0);
		result = 31 * result + (anchorEnd ? 1 : 0);
		return result;
	}
}
