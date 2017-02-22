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

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

public class FullTextFilter extends ObjectFilter {

	private Collection<String> values;
	private ExpressionWrapper expression;

	private FullTextFilter(Collection<String> values) {
		this.values = values;
	}

	private FullTextFilter(ExpressionWrapper expression) {
		this.expression = expression;
	}

	public static FullTextFilter createFullText(Collection<String> values){
		return new FullTextFilter(values);
	}
	
	public static FullTextFilter createFullText(String... values){
		return new FullTextFilter(Arrays.asList(values));
	}

	public static FullTextFilter createFullText(@NotNull ExpressionWrapper expression) {
		return new FullTextFilter(expression);
	}

	public Collection<String> getValues() {
		return values;
	}

	public void setValues(Collection<String> values) {
		this.values = values;
	}

	public ExpressionWrapper getExpression() {
		return expression;
	}

	public void setExpression(ExpressionWrapper expression) {
		this.expression = expression;
	}

	@Override
	public void checkConsistence(boolean requireDefinitions) {
		if (values == null) {
			throw new IllegalArgumentException("Null 'values' in "+this);
		}
		if (values.isEmpty()) {
			throw new IllegalArgumentException("No values in "+this);
		}
		for (String value: values) {
			if (StringUtils.isBlank(value)) {
				throw new IllegalArgumentException("Empty value in "+this);
			}
		}
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		sb.append("FULLTEXT: ");
		sb.append("VALUE:");
		if (values != null) {
			sb.append("\n");
			for (String value : values) {
				DebugUtil.indentDebugDump(sb, indent+1);
				sb.append(value);
				sb.append("\n");
			}
		} else {
			sb.append(" null\n");
		}
		return sb.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("IN OID: ");
		if (values != null) {
			sb.append(values.stream().collect(Collectors.joining("; ")));
		}
		return sb.toString();
	}

	@Override
	public FullTextFilter clone() {
		FullTextFilter clone = new FullTextFilter(values);
		clone.expression = expression;
		return clone;
	}

	@Override
	public boolean match(PrismContainerValue value, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
		throw new UnsupportedOperationException("match is not supported for " + this);
	}

	@Override
	public boolean equals(Object o, boolean exact) {
		if (this == o)
			return true;
		if (!(o instanceof FullTextFilter))
			return false;
		FullTextFilter that = (FullTextFilter) o;
		return Objects.equals(values, that.values) &&
				Objects.equals(expression, that.expression);
	}

	@Override
	public int hashCode() {
		return Objects.hash(values);
	}
}
