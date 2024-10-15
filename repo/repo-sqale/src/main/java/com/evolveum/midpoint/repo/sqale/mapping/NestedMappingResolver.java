/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sqale.SqaleQueryContext;
import com.evolveum.midpoint.repo.sqale.update.NestedContainerUpdateContext;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Resolver that maps the nested items (next component of the path) to the same table (query type).
 *
 * @param <S> schema type for the nested container
 * @param <Q> query type of entity where the mapping is nested and declared
 * @param <R> row type of {@link Q}
 */
public class NestedMappingResolver<S extends Containerable, Q extends FlexibleRelationalPathBase<R>, R>
        implements SqaleItemRelationResolver<Q, R, Q, R> {

    private final SqaleNestedMapping<S, Q, R> mapping;

    public NestedMappingResolver(SqaleNestedMapping<S, Q, R> mapping) {
        this.mapping = mapping;
    }

    /** Returns the same context and nested mapping. */
    @SuppressWarnings("unchecked")
    @Override
    public ResolutionResult<Q, R> resolve(SqlQueryContext<?, Q, R> context, boolean parent) {
        context = ((SqaleQueryContext<?, Q, R>) context).nestedContext(mapping);
        return new ResolutionResult<>(context, mapping);
    }

    @Override
    public NestedContainerUpdateContext<S, Q, R> resolve(
            SqaleUpdateContext<?, Q, R> context, ItemPath ignored) {
        return new NestedContainerUpdateContext<>(context, mapping);
    }
}
