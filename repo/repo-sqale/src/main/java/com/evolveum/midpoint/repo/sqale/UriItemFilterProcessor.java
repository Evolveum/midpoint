/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import java.util.function.Function;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.NumberPath;

import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.ValueFilterValues;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.ItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.ItemSqlMapper;

/**
 * Filter processor for URI item paths - represented as strings in schema and by int ID in DB.
 * TODO: if used for order it would require join to QUri, otherwise it just sorts by URI ID.
 */
public class UriItemFilterProcessor extends ItemFilterProcessor<PropertyValueFilter<String>> {

    /**
     * Returns the mapper function creating the ref-filter processor from query context.
     */
    public static ItemSqlMapper mapper(
            Function<EntityPath<?>, NumberPath<Integer>> rootToPath) {
        return new ItemSqlMapper(ctx -> new UriItemFilterProcessor(ctx, rootToPath));
    }

    private final NumberPath<Integer> path;

    private UriItemFilterProcessor(
            SqlQueryContext<?, ?, ?> context,
            Function<EntityPath<?>, NumberPath<Integer>> rootToPath) {
        super(context);
        this.path = rootToPath.apply(context.path());
    }

    @Override
    public Predicate process(PropertyValueFilter<String> filter) throws QueryException {
        return createBinaryCondition(filter, path,
                ValueFilterValues.from(filter,
                        u -> ((SqaleRepoContext) context.sqlRepoContext()).searchCachedUriId(u)));
    }
}
