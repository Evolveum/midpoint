/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.query;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

public abstract class ComparativeFilterImpl<T> extends PropertyValueFilterImpl<T> implements PropertyValueFilter<T> {

    private boolean equals;

    ComparativeFilterImpl(@NotNull ItemPath path,
            @Nullable PrismPropertyDefinition<T> definition,
            @Nullable QName matchingRule,
            @Nullable PrismPropertyValue<T> value,
            @Nullable ExpressionWrapper expression, @Nullable ItemPath rightHandSidePath,
            @Nullable ItemDefinition rightHandSideDefinition, boolean equals) {
        super(path, definition, matchingRule,
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
        ComparativeFilterImpl<?> that = (ComparativeFilterImpl<?>) o;
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

    @Override
    abstract public PropertyValueFilterImpl clone();
}
