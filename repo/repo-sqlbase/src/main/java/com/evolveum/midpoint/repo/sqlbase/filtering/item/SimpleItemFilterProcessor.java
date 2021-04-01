/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.filtering.item;

import java.util.function.Function;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.NumberPath;

import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.ValueFilterValues;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.ItemSqlMapper;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Filter processor for a single path with straightforward type mapping and no conversions.
 */
public class SimpleItemFilterProcessor<T, P extends Path<T>>
        extends SinglePathItemFilterProcessor<PropertyValueFilter<T>, P> {

    // TODO these factory methods go away from filter processor as we want to create
    //  also delta processor in one go.

    /** Returns the mapper creating the integer filter processor from context. */
    public static ItemSqlMapper integerMapper(
            Function<EntityPath<?>, NumberPath<Integer>> rootToQueryItem) {
        return new ItemSqlMapper(ctx ->
                new SimpleItemFilterProcessor<>(ctx, rootToQueryItem), rootToQueryItem);
    }

    /** Returns the mapper creating the boolean filter processor from context. */
    public static ItemSqlMapper booleanMapper(
            Function<EntityPath<?>, BooleanPath> rootToQueryItem) {
        return new ItemSqlMapper(ctx ->
                new SimpleItemFilterProcessor<>(ctx, rootToQueryItem), rootToQueryItem);
    }

    /** Returns the mapper creating the OID (UUID) filter processor from context. */
    public static ItemSqlMapper uuidMapper(
            Function<EntityPath<?>, UuidPath> rootToQueryItem) {
        return new ItemSqlMapper(ctx ->
                new SimpleItemFilterProcessor<>(ctx, rootToQueryItem), rootToQueryItem);
    }

    public SimpleItemFilterProcessor(
            SqlQueryContext<?, ?, ?> context, Function<EntityPath<?>, P> rootToQueryItem) {
        super(context, rootToQueryItem);
    }

    @Override
    public Predicate process(PropertyValueFilter<T> filter) throws QueryException {
        return createBinaryCondition(filter, path, ValueFilterValues.from(filter));
    }
}
