/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.query;

import com.evolveum.midpoint.prism.ExpressionWrapper;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.FullTextFilter;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

public final class FullTextFilterImpl extends ObjectFilterImpl implements FullTextFilter {

    private Collection<String> values;
    private ExpressionWrapper expression;

    private FullTextFilterImpl(Collection<String> values) {
        this.values = values;
    }

    private FullTextFilterImpl(ExpressionWrapper expression) {
        this.expression = expression;
    }

    public static FullTextFilter createFullText(Collection<String> values){
        return new FullTextFilterImpl(values);
    }

    public static FullTextFilter createFullText(String... values){
        return new FullTextFilterImpl(Arrays.asList(values));
    }

    public static FullTextFilter createFullText(@NotNull ExpressionWrapper expression) {
        return new FullTextFilterImpl(expression);
    }

    @Override
    public Collection<String> getValues() {
        return values;
    }

    @Override
    public void setValues(Collection<String> values) {
        checkMutable();
        this.values = values;
    }

    @Override
    public ExpressionWrapper getExpression() {
        return expression;
    }

    @Override
    public void setExpression(ExpressionWrapper expression) {
        checkMutable();
        this.expression = expression;
    }

    @Override
    protected void performFreeze() {
        values = ImmutableList.copyOf(values);
        freeze(expression);
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
        sb.append("FULLTEXT: ");
        if (values != null) {
            sb.append(values.stream().collect(Collectors.joining("; ")));
        }
        return sb.toString();
    }

    @Override
    public FullTextFilterImpl clone() {
        FullTextFilterImpl clone = new FullTextFilterImpl(values);
        clone.expression = expression;
        return clone;
    }

    @Override
    public boolean match(PrismContainerValue value, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
        throw new UnsupportedOperationException("match is not supported for " + this);
    }

    @Override
    public boolean equals(Object o, boolean exact) {
        if (this == o) return true;
        if (!(o instanceof FullTextFilterImpl)) return false;
        FullTextFilterImpl that = (FullTextFilterImpl) o;
        return Objects.equals(values, that.values) &&
                Objects.equals(expression, that.expression);
    }

    // Just to make checkstyle happy
    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }
}
