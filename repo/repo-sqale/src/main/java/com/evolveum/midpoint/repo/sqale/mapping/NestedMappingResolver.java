/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.sqale.qmodel.SqaleNestedMapping;
import com.evolveum.midpoint.repo.sqale.update.NestedContainerUpdateContext;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Resolver that maps the nested items (next component of the path) to the same query type columns.
 *
 * @param <S> schema type for the nested container
 * @param <Q> query type of entity where the mapping nested and declared
 * @param <R> row type of {@link Q}
 */
public class NestedMappingResolver<S extends Containerable, Q extends FlexibleRelationalPathBase<R>, R>
        implements SqaleItemRelationResolver<Q, R> {

    private final SqaleNestedMapping<S, Q, R> mapping;

    public NestedMappingResolver(SqaleNestedMapping<S, Q, R> mapping) {
        this.mapping = mapping;
    }

    /** Returns the same context and nested mapping. */
    @Override
    public ResolutionResult resolve(SqlQueryContext<?, Q, R> context) {
        return new ResolutionResult(context, mapping);
    }

    @Override
    public NestedContainerUpdateContext<S, Q, R> resolve(SqaleUpdateContext<?, Q, R> context) {
        return new NestedContainerUpdateContext<>(context, mapping);
    }
}
