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

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.sqale.delta.ItemDeltaValueProcessor;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Filter processor for multi-value property represented by single array column.
 * These paths support only value equality (of any value), which is "contains" in DB terminology.
 * Our filter "contains" (meaning substring) is *not* supported.
 *
 * @param <V> type of value in schema
 * @param <E> type of element in DB (can be the same like `V`)
 */
public class ArrayItemDeltaProcessor<V, E> extends ItemDeltaValueProcessor<E> {

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
        this.path = rootToQueryItem.apply(context.path());
        this.elementType = elementType;
        this.conversionFunction = conversionFunction;
    }

    @Override
    public void process(ItemDelta<?, ?> modification) throws RepositoryException, SchemaException {
        Item<PrismValue, ?> item = context.findItem(modification.getPath());
        Collection<?> realValues = item != null ? item.getRealValues() : null;

        if (realValues == null || realValues.isEmpty()) {
            delete();
        } else {
            // Whatever the operation is, we just set the new value here.
            setRealValues(realValues);
        }
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
        context.set(path, null);
    }
}
