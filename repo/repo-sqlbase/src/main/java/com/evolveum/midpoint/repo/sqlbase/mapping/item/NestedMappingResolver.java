/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.mapping.item;

import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Resolver that maps the nested items (next component of the path) to the same query type columns.
 *
 * @param <Q> type of source entity path
 */
public class NestedMappingResolver<Q extends FlexibleRelationalPathBase<?>>
        implements ItemRelationResolver {

    private final QueryModelMapping<?, Q, ?> mapping;

    public NestedMappingResolver(QueryModelMapping<?, Q, ?> mapping) {
        this.mapping = mapping;
    }

    /** Returns the same context and nested mapping. */
    @Override
    public ResolutionResult resolve(SqlQueryContext<?, ?, ?> context) {
        return new ResolutionResult(context, mapping);
    }
}
