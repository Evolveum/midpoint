/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.mapping.item;

import java.util.function.Function;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.StringPath;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.ValueFilterValues;

/**
 * Filter processor for a an attribute path (Prism item) of enum type that is mapped
 * to SQL as {@link Enum#name()} (typically upper-case constant).
 * This may be processed as enum type on databases that support it (e.g. PostgreSQL enum type).
 */
public class EnumItemFilterProcessor<E extends Enum<E>>
        extends SinglePathItemFilterProcessor<PropertyValueFilter<E>, StringPath> {

    /**
     * Returns the mapper creating the enum filter processor from context.
     */
    public static ItemSqlMapper mapper(
            @NotNull Function<EntityPath<?>, StringPath> rootToQueryItem) {
        return new ItemSqlMapper(ctx ->
                new EnumItemFilterProcessor<>(ctx, rootToQueryItem),
                rootToQueryItem);
    }

    private EnumItemFilterProcessor(
            SqlQueryContext<?, ?, ?> context,
            Function<EntityPath<?>, StringPath> rootToQueryItem) {
        super(context, rootToQueryItem);
    }

    @Override
    public Predicate process(PropertyValueFilter<E> filter) throws QueryException {
        ValueFilterValues<E> values = new ValueFilterValues<>(filter, e -> e.name());
        return createBinaryCondition(filter, path, values);
    }
}
