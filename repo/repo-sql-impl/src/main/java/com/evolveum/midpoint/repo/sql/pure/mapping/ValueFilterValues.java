/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.pure.mapping;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;

/**
 * Object wraps zero, one or multiple values and makes their processing easier.
 * Instead of just wrapping the values it uses the whole filter object
 * to utilize its convenience methods.
 * <p>
 * Returned values are typed to Object, because they can be converted from original type.
 * Conversion is moved into this class, so the client code doesn't have to handle translation
 * from {@link PrismPropertyValue} to "real value" and then to convert it.
 * Both {@link #singleValue()} and {@link #allValues()} are handled the same way.
 * <p>
 * If {@link #conversionFunction} is used any {@link IllegalArgumentException} will be rewrapped
 * as {@link QueryException}, other runtime exceptions are not intercepted.
 */
public class ValueFilterValues<T> {

    private final PropertyValueFilter<T> filter;
    private final Function<T, ?> conversionFunction;

    public ValueFilterValues(PropertyValueFilter<T> filter) {
        this.filter = filter;
        conversionFunction = null;
    }

    public ValueFilterValues(PropertyValueFilter<T> filter, Function<T, ?> conversionFunction) {
        this.filter = filter;
        this.conversionFunction = conversionFunction;
    }

    /**
     * Returns single value or null or fails if there are multiple values, all converted.
     */
    public Object singleValue() throws QueryException {
        return convert(filter.getSingleValue());
    }

    /**
     * Returns multiple values, all converted, or empty list - never null.
     */
    public List<?> allValues() {
        if (filter.getValues() == null) {
            return Collections.emptyList();
        }
        Stream<T> realValueStream = filter.getValues().stream()
                .map(ppv -> ppv.getRealValue());
        if (conversionFunction == null) {
            return realValueStream.collect(Collectors.toList());
        }
        return realValueStream
                .map(conversionFunction)
                .collect(Collectors.toList());
    }

    private Object convert(PrismPropertyValue<T> value) throws QueryException {
        if (value == null) {
            return null;
        }
        if (conversionFunction == null) {
            return value.getRealValue();
        }
        try {
            return conversionFunction.apply(value.getRealValue());
        } catch (IllegalArgumentException e) {
            throw new QueryException(e);
        }
    }

    public boolean isEmpty() {
        return filter.getValues() == null || filter.getValues().isEmpty();
    }

    public boolean isMultiValue() {
        return filter.getValues() != null && filter.getValues().size() > 1;
    }
}
