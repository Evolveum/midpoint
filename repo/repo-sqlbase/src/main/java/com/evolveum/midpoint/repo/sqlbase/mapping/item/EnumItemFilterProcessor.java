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
import com.querydsl.core.types.dsl.EnumPath;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.ValueFilterValues;
import com.evolveum.midpoint.repo.sqlbase.querydsl.QuerydslUtils;

/**
 * Filter processor for a an attribute path (Prism item) of enum type that is mapped
 * to matching PostgreSQL enum type - this allows to use schema enums directly.
 * Use only enums that change rarely-to-never, enum type defined in SQL schema must be changed
 * accordingly - but this is still less complicated than with old repo where each enum was doubled.
 *
 * Each enum type must be registered in {@link QuerydslUtils#querydslConfiguration}.
 */
public class EnumItemFilterProcessor<E extends Enum<E>>
        extends SinglePathItemFilterProcessor<PropertyValueFilter<E>, EnumPath<E>> {

    /**
     * Returns the mapper creating the enum filter processor from context.
     */
    public static <E extends Enum<E>> ItemSqlMapper mapper(
            @NotNull Function<EntityPath<?>, EnumPath<E>> rootToQueryItem) {
        return new ItemSqlMapper(ctx ->
                new EnumItemFilterProcessor<>(ctx, rootToQueryItem),
                rootToQueryItem);
    }

    private EnumItemFilterProcessor(
            SqlQueryContext<?, ?, ?> context,
            Function<EntityPath<?>, EnumPath<E>> rootToQueryItem) {
        super(context, rootToQueryItem);
    }

    @Override
    public Predicate process(PropertyValueFilter<E> filter) throws QueryException {
        return createBinaryCondition(filter, path, ValueFilterValues.from(filter));
    }
}
