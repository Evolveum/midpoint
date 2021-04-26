/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping;

import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Resolver that maps the nested items (next component of the path) to the same query type columns.
 *
 * @param <Q> query type of source entity (where the mapping is declared)
 * @param <R> row type of {@link Q}
 */
public class NestedMappingResolver<Q extends FlexibleRelationalPathBase<R>, R>
        implements SqaleItemRelationResolver<Q, R> {

    private final QueryModelMapping<?, Q, R> mapping;

    public NestedMappingResolver(QueryModelMapping<?, Q, R> mapping) {
        this.mapping = mapping;
    }

    /** Returns the same context and nested mapping. */
    @Override
    public ResolutionResult resolve(SqlQueryContext<?, Q, R> context) {
        return new ResolutionResult(context, mapping);
    }

    @Override
    public UpdateResolutionResult resolve(SqaleUpdateContext<?, Q, R> context) {
        // TODO this is OK unless we need to capture new schema object, e.g. nested metadata,
        //  in the context (SqaleUpdateContext#object). Row stays still the same, update clause
        //  doesn't change either, that's OK. Currently we're just losing type information.
        return new UpdateResolutionResult(context, mapping);
    }
}
