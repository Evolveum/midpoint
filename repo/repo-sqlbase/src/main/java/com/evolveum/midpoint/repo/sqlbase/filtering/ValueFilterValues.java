/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.filtering;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.querydsl.core.types.Expression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;

/**
 * Object wraps zero, one or multiple values and makes their processing easier.
 * Instead of just wrapping the values it uses the whole filter object
 * to utilize its convenience methods.
 *
 * Returned values are typed to Object, because they can be converted from original type.
 * Conversion is moved into this class, so the client code doesn't have to handle translation
 * from {@link PrismPropertyValue} to "real value" and then to convert it.
 * Both {@link #singleValue()} and {@link #allValues()} are handled the same way.
 *
 * @param <T> type of filter value
 * @param <V> type of value after conversion (can be the same as T)
 */
public abstract class ValueFilterValues<T, V> {

    @NotNull protected final PropertyValueFilter<T> filter;

    public static <T> ValueFilterValues<T, T> from(@NotNull PropertyValueFilter<T> filter) {
        return new Constant<>(filter, null);
    }

    public static <T, V> ValueFilterValues<T, V> from(
            @NotNull PropertyValueFilter<T> filter, Expression<?> expression) {
        return new Expr<>(filter, expression);
    }

    public static <T, V> ValueFilterValues<T, V> from(
            @NotNull PropertyValueFilter<T> filter,
            @Nullable Function<T, V> conversionFunction) {
        return new Constant<>(filter, conversionFunction);
    }

    private ValueFilterValues(
            @NotNull PropertyValueFilter<T> filter) {
        this.filter = Objects.requireNonNull(filter);
    }

    /**
     * Returns single value or null or fails if there are multiple values, all converted.
     * If conversion function was provided any {@link IllegalArgumentException}
     * or {@link ClassCastException} will be re-wrapped as {@link QueryException},
     * other runtime exceptions are not intercepted.
     */
    public abstract @Nullable V singleValue() throws QueryException;

    /**
     * Returns single value or null or fails if there are multiple values without conversion.
     * Null-safe version of {@link ValueFilter#getSingleValue()} followed by
     * {@link PrismPropertyValue#getRealValue()}.
     */
    public abstract @Nullable T singleValueRaw();

    /**
     * Returns multiple values, all converted, or empty list - never null.
     */
    public @NotNull List<V> allValues() {
        return Collections.emptyList();
    }

    /**
     * Returns multiple real values without conversion or empty list - never null.
     */
    public @NotNull List<T> allValuesRaw() {
        return Collections.emptyList();
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean isMultiValue() {
        return false;
    }

    @NotNull
    public PolyString singleValuePolyString() throws QueryException {
        Object value = singleValueRaw();
        assert value != null; // empty values treated in main process()
        if (!(value instanceof PolyString)) {
            throw new QueryException(
                    "PolyString value must be provided to match both orig and norm values, was: "
                            + value + " (type " + value.getClass() + "), filter: " + filter);
        }

        return (PolyString) value;
    }

    @NotNull
    public static ValueFilterValues<?, ?> convertPolyValuesToString(ValueFilterValues<?, ?> values,
            PropertyValueFilter<?> filter, Function<PolyString, String> extractor) {
        // In case it is string already we don't need to do anything.
        if (values.singleValueRaw() instanceof String) {
            return values;
        }

        //noinspection unchecked
        return ValueFilterValues.from((PropertyValueFilter<PolyString>) filter, extractor);
    }

    private static class Constant<T, V> extends ValueFilterValues<T, V> {

        @Nullable private final Function<T, V> conversionFunction;

        public Constant(@NotNull PropertyValueFilter<T> filter, @Nullable Function<T, V> conversionFunction) {
            super(filter);
            this.conversionFunction = conversionFunction;
        }

        @Override
        public @Nullable V singleValue() throws QueryException {
            return convert(filter.getSingleValue());
        }

        @Override
        public @Nullable T singleValueRaw() {
            final PrismPropertyValue<T> singleValue = filter.getSingleValue();
            return singleValue != null ? singleValue.getRealValue() : null;
        }

        @Override
        public @NotNull List<V> allValues() {
            if (filter.getValues() == null) {
                return Collections.emptyList();
            }
            Stream<T> realValueStream = filter.getValues().stream()
                    .map(ppv -> ppv.getRealValue());
            if (conversionFunction == null) {
                //noinspection unchecked
                return (List<V>) realValueStream.collect(Collectors.toList());
            }
            return realValueStream
                    .map(conversionFunction)
                    .collect(Collectors.toList());
        }

        @Override
        public @NotNull List<T> allValuesRaw() {
            if (filter.getValues() == null) {
                return Collections.emptyList();
            }
            return filter.getValues().stream()
                    .map(ppv -> ppv.getRealValue())
                    .collect(Collectors.toList());
        }

        private V convert(PrismPropertyValue<T> value) throws QueryException {
            if (value == null) {
                return null;
            }
            if (conversionFunction == null) {
                //noinspection unchecked
                return (V) value.getRealValue();
            }
            try {
                return conversionFunction.apply(value.getRealValue());
            } catch (IllegalArgumentException | ClassCastException e) {
                throw new QueryException(e);
            }
        }

        @Override
        public boolean isEmpty() {
            return filter.getValues() == null || filter.getValues().isEmpty();
        }

        @Override
        public boolean isMultiValue() {
            return filter.getValues() != null && filter.getValues().size() > 1;
        }
    }

    private static class Expr<T, V> extends ValueFilterValues<T, V> {

        private final @Nullable Expression<?> expression;

        public Expr(@NotNull PropertyValueFilter<T> filter, @Nullable Expression<?> expression) {
            super(filter);
            this.expression = expression;
        }

        @Override
        public @Nullable V singleValue() throws QueryException {
            //noinspection unchecked
            return (V) expression;
        }

        @Override
        public @Nullable T singleValueRaw() {
            //noinspection unchecked
            return (T) expression;
        }
    }
}
