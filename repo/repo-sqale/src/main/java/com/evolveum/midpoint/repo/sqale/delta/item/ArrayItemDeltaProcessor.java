/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.delta.item;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.IntFunction;

import com.querydsl.core.types.dsl.ArrayPath;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Delta processor for multi-value property represented by single array column.
 *
 * @param <V> type of value in schema
 * @param <E> type of element in DB (can be the same as `V`)
 */
public class ArrayItemDeltaProcessor<V, E> extends FinalValueDeltaProcessor<E> {

    private final ArrayPath<E[], E> path;
    private final Class<E> elementType;
    @Nullable private final Function<V, E> conversionFunction;

    /**
     * @param <Q> entity query type from which the attribute is resolved
     * @param <R> row type related to {@link Q}
     * @param elementType class of {@link E} necessary for array creation
     * @param conversionFunction optional conversion function, can be null if no conversion is necessary
     */
    public <Q extends FlexibleRelationalPathBase<R>, R> ArrayItemDeltaProcessor(
            SqaleUpdateContext<?, Q, R> context,
            Function<Q, ArrayPath<E[], E>> rootToQueryItem,
            Class<E> elementType,
            @Nullable Function<V, E> conversionFunction) {
        super(context);
        this.path = rootToQueryItem.apply(context.entityPath());
        this.elementType = elementType;
        this.conversionFunction = conversionFunction;
    }

    @Override
    public void setRealValues(Collection<?> values) {
        //noinspection unchecked
        IntFunction<E[]> arrayConstructor = i -> (E[]) Array.newInstance(elementType, i);

        // valueArray can't be just Object[], it must be concrete type, e.g. String[],
        // otherwise PG JDBC driver will complain.
        //noinspection SuspiciousToArrayCall,unchecked
        E[] valueArray = conversionFunction != null
                ? ((Collection<V>) values).stream().map(conversionFunction).toArray(arrayConstructor)
                : values.toArray(arrayConstructor);
        context.set(path, valueArray);
    }

    @Override
    public void delete() {
        context.setNull(path);
    }
}
