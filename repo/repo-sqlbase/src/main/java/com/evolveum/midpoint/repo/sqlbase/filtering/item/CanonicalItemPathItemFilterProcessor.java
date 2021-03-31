/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.filtering.item;

import java.util.function.Function;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.StringPath;

import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.ValueFilterValues;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.ItemSqlMapper;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Filter processor for an {@link ItemPathType} attribute path (Prism item)
 * with canonicalization of the path before using it as a value for condition.
 *
 * @see com.evolveum.midpoint.prism.path.CanonicalItemPath
 */
public class CanonicalItemPathItemFilterProcessor
        extends SinglePathItemFilterProcessor<PropertyValueFilter<ItemPathType>, StringPath> {

    /**
     * Returns the mapper creating the item path filter processor from the context.
     */
    public static ItemSqlMapper mapper(
            Function<EntityPath<?>, StringPath> rootToQueryItem) {
        return new ItemSqlMapper(ctx ->
                new CanonicalItemPathItemFilterProcessor(ctx, rootToQueryItem), rootToQueryItem);
    }

    private CanonicalItemPathItemFilterProcessor(
            SqlQueryContext<?, ?, ?> context, Function<EntityPath<?>, StringPath> rootToQueryItem) {
        super(context, rootToQueryItem);
    }

    @Override
    public Predicate process(PropertyValueFilter<ItemPathType> filter) throws QueryException {
        return createBinaryCondition(filter, path,
                ValueFilterValues.from(filter,
                        value -> context.createCanonicalItemPath(value.getItemPath()).asString()));
    }
}
