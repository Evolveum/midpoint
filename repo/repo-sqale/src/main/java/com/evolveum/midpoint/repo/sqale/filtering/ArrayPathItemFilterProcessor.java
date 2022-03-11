/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.filtering;

import static com.querydsl.core.types.dsl.Expressions.stringTemplate;

import java.lang.reflect.Array;
import java.util.function.Function;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.ArrayPath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringTemplate;
import com.querydsl.sql.SQLQuery;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.ValueFilterValues;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.FilterOperation;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.SinglePathItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Filter processor for multi-value property represented by single array column.
 * These paths support only value equality (of any value), which is "contains" in DB terminology.
 * Our filter "contains" (meaning substring) is *not* supported.
 *
 * @param <T> type of value in schema
 * @param <E> type of element in DB (can be the same as `T`)
 */
public class ArrayPathItemFilterProcessor<T, E>
        extends SinglePathItemFilterProcessor<T, ArrayPath<E[], E>> {

    private final String dbType;
    private final Class<E> elementType;
    @Nullable private final Function<T, E> conversionFunction;

    /**
     * Creates filter processor for array column.
     *
     * @param dbType name of the type for element in DB (without []) for the cast part of the condition
     * @param elementType class of {@link E} necessary for array creation
     * @param conversionFunction optional conversion function, can be null if no conversion is necessary
     */
    public <Q extends FlexibleRelationalPathBase<R>, R> ArrayPathItemFilterProcessor(
            SqlQueryContext<?, Q, R> context,
            Function<Q, ArrayPath<E[], E>> rootToPath,
            String dbType,
            Class<E> elementType,
            @Nullable Function<T, E> conversionFunction) {
        super(context, rootToPath);
        this.dbType = dbType;
        this.elementType = elementType;
        this.conversionFunction = conversionFunction;
    }

    @Override
    public Predicate process(PropertyValueFilter<T> filter) throws RepositoryException {
        ValueFilterValues<T, E> values = ValueFilterValues.from(filter, conversionFunction);
        FilterOperation operation = operation(filter);
        if (values.isEmpty()) {
            return Expressions.booleanTemplate("({0} = '{}' OR {0} is null)", path);
        }

        if (operation.isEqualOperation()) {
            // valueArray can't be just Object[], it must be concrete type, e.g. String[],
            // otherwise PG JDBC driver will complain.
            //noinspection unchecked
            E[] valueArray = values.allValues().toArray(i -> (E[]) Array.newInstance(elementType, i));
            return Expressions.booleanTemplate("{0} && {1}::" + dbType + "[]", path, valueArray);
        } else if (!values.isMultiValue()) {
            if (operation.isTextOnlyOperation() && elementType != String.class) {
                throw new QueryException(
                        "Unsupported operation for multi-value non-textual item; used filter: " + filter);
            }

            // e.g. for substring: WHERE exists (select val from unnest(subtypes) as val where val like '%A%')
            // This can't use index, but it works.
            StringTemplate valPath = stringTemplate("val");
            SQLQuery<String> subselect = new SQLQuery<>().select(valPath)
                    .from(stringTemplate("unnest({0}) as val", path))
                    .where(singleValuePredicate(valPath, operation, values.singleValue()));
            return subselect.exists();
        } else {
            throw new QueryException("Non-equal operation not supported for multi-value items"
                    + " and multiple values on the right-hand; used filter: " + filter);
        }
    }
}
